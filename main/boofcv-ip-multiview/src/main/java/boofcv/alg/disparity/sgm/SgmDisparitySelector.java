/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.disparity.sgm;

import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import lombok.Getter;
import lombok.Setter;

/// Selects the best disparity for each pixel from aggregated SGM cost. If no valid match is or can be found then
/// it is set to [#invalidDisparity].
public class SgmDisparitySelector {

	protected final SgmHelper helper = new SgmHelper();

	/// tolerance for right to left validation. if < 0 then it's disabled
	@Setter @Getter protected int rightToLeftTolerance = 1;

	/// Maximum allowed error
	@Setter @Getter int maxError = Integer.MAX_VALUE;

	@Setter @Getter double textureThreshold = 0.0;

	/// The minimum possible disparity
	@Setter @Getter int disparityMin = 0;
	// First stored column in the cost tensor. The tensor's x-coordinate is relative to this, which equals
	// disparityMin for a non-negative search and 0 once a negative disparity is allowed.
	int xOffset = 0;
	/// specified which value was used to indicate that a disparity is invalid
	@Getter int invalidDisparity = -1;

	// Shape of the input tensor
	// lengthD is the disparity range being considered
	int lengthY, lengthX, lengthD;

	/// Given the aggregated cost compute the best disparity to pixel level accuracy for all pixels
	///
	/// @param aggregatedYXD (Input) Aggregated disparity cost for each pixel
	/// @param disparity (output) selected disparity
	public void select( Planar<GrayU16> costYXD, Planar<GrayU16> aggregatedYXD, GrayU8 disparity ) {
		setup(aggregatedYXD);

		// Ensure that the output matches the input
		disparity.reshape(lengthX, lengthY);

		for (int y = 0; y < lengthY; y++) {
			GrayU16 aggregatedXD = aggregatedYXD.getBand(y);

			// columns before xOffset have nothing stored in the cost tensor that they can compare against
			for (int x = 0; x < xOffset; x++) {
				disparity.unsafe_set(x, y, invalidDisparity);
			}
			for (int x = xOffset; x < lengthX; x++) {
				disparity.unsafe_set(x, y, findBestDisparity(x, aggregatedXD));
			}
		}
	}

	/// Sets up internal data structures based on the aggregated cost
	void setup( Planar<GrayU16> aggregatedYXD ) {
		this.lengthY = aggregatedYXD.getNumBands();
		this.lengthX = aggregatedYXD.height;
		this.lengthD = aggregatedYXD.width;
		this.invalidDisparity = invalidGivenRange(lengthD);
		this.xOffset = Math.max(0, disparityMin);
		helper.configure(lengthX, disparityMin, lengthD);
		if (invalidDisparity > 255)
			throw new IllegalArgumentException("Disparity range is too great. Must be < 256 not " + lengthD);
	}

	/// Selects the disparity for the specified pixel using a winner takes all strategy
	///
	/// @param x x-coordinate in original image coordinates. DO NOT SUBTRACT disparityMin
	int findBestDisparity( int x, GrayU16 aggregatedXD ) {
		// The disparity window that can be considered at 'x', clamped against both borders of the right image
		int localMinRange = helper.localDisparityMinLeft(x);
		int localMaxRange = helper.localDisparityRangeLeft(x);
		// Every disparity falls off a border of the right image, so there's nothing to match against
		if (localMinRange >= localMaxRange)
			return invalidDisparity;
		int bestScore = Integer.MAX_VALUE;
		int bestRange = invalidDisparity;

		// Select the disparity with the lowest aggregated cost
		final int idx = aggregatedXD.getIndex(0, x - xOffset);
		for (int d = localMinRange; d < localMaxRange; d++) {
			int cost = aggregatedXD.data[idx + d] & 0xFFFF;
			if (cost < bestScore) {
				bestScore = cost;
				bestRange = d;
			}
		}

		if (bestRange == invalidDisparity)
			return invalidDisparity;

		// See if the maximum error is exceeded
		if (bestScore > maxError) {
			return invalidDisparity;
		}

		// right to left consistency check
		if (rightToLeftTolerance >= 0) {
			// TODO why isn't this pruning the left side of the disparity image as much as block matching does?
			// Not nearly as effective at pruning as it is with
			int bestRange_R_to_L = selectRightToLeft(x - bestRange - disparityMin, aggregatedXD);
			if (Math.abs(bestRange_R_to_L - bestRange) > rightToLeftTolerance)
				return invalidDisparity;
		}

		// See if the best solution is ambiguous
		if (localMaxRange - localMinRange > 3 && textureThreshold > 0) {
			// find the second-best disparity value and exclude its neighbors
			int secondBest = Integer.MAX_VALUE;
			for (int d = localMinRange; d < bestRange - 1; d++) {
				int v = aggregatedXD.data[idx + d] & 0xFFFF;
				if (v < secondBest) {
					secondBest = v;
				}
			}
			for (int d = bestRange + 2; d < localMaxRange; d++) {
				int v = aggregatedXD.data[idx + d] & 0xFFFF;
				if (v < secondBest) {
					secondBest = v;
				}
			}

			// similar scores indicate lack of texture
			// C = (C2-C1)/C1
			if (secondBest - bestScore <= textureThreshold*bestScore)
				bestRange = invalidDisparity;
			// TODO try this same check for right to left disparity
		}

		return bestRange;
	}

	/// Finds the best fit region going from the column (x-coordinate) in right image to left. To find
	/// the pixel in left image that's compared against a pixel in right image, take it's x-coordinate then add
	/// the disparity. e.g. x=10 in right matches x=15 and d=5 in left
	///
	/// @param x x-coordinate of point in right image
	/// @return best fit disparity from right to left
	private int selectRightToLeft( int x, GrayU16 aggregatedXD ) {
		// Disparity index d maps to the left column (x + disparityMin + d). Restrict d so that the left column
		// lies within the stored cost tensor, i.e. column in [xOffset, lengthX-1].
		int loD = Math.max(0, xOffset - disparityMin - x);
		int hiD = Math.min(lengthD, lengthX - disparityMin - x);
		if (loD >= hiD) // it can't perform the check because it's too far right, just give it a pass
			return x;

		// Index of the aggregated cost for disparity 'loD'. The left column for disparity d is stored at
		// band column (x + disparityMin + d - xOffset), so each increment of d advances by lengthD + 1.
		int idx = aggregatedXD.getIndex(0, x + disparityMin + loD - xOffset) + loD;

		// best column in left image that it matches up with col in right
		int bestD = loD;
		float scoreBest = aggregatedXD.data[idx] & 0xFFFF;

		for (int d = loD + 1; d < hiD; d++) {
			idx += lengthD + 1; // advance one column and one disparity
			int s = aggregatedXD.data[idx] & 0xFFFF;

			if (s < scoreBest) {
				scoreBest = s;
				bestD = d;
			}
		}

		return bestD;
	}

	/// Convenience function to make it clear what the value assigned to an invalid disparity is. Any value
	public static int invalidGivenRange( int disparityRange ) {
		return disparityRange;
	}
}
