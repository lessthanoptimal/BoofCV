/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.sgm;

import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;

/**
 * Selects the best disparity for each pixel from aggregated SGM cost. If no valid match is or can be found then
 * it is set to {@link #invalidDisparity}.
 *
 * @author Peter Abeles
 */
public class SgmDisparitySelector {

	// TODO add texture ambiguity
	// TODO sub-pixel

	// tolerance for right to left validation. if < 0 then it's disabled
	protected int rightToLeftTolerance=1;

	// Maximum allowed error
	int maxError = Integer.MAX_VALUE;

	// reference to input cost tensor
	GrayU16 aggregatedXD;

	// The minimum possible disparity
	int minDisparity=0;
	// specified which value was used to indivate that a disparity is invalid
	int invalidDisparity=-1;

	// Shape of the input tensor
	// lengthD is the disparity range being considered
	int lengthY,lengthX,lengthD;

	/**
	 * Given the aggregated cost compute the best disparity to pixel level accuracy for all pixels
	 * @param aggregatedYXD (Input) Aggregated disparity cost for each pixel
	 * @param disparity (output) selected disparity
	 */
	public void select(Planar<GrayU16> aggregatedYXD , GrayU8 disparity ) {
		setup(aggregatedYXD);

		// Ensure that the output matches the input
		disparity.reshape(lengthX,lengthY);

		for (int y = 0; y < lengthY; y++) {
			aggregatedXD = aggregatedYXD.getBand(y);

			// if 'x' is less than minDisparity then that's nothing that it can compare against
			for (int x = 0; x < minDisparity; x++) {
				disparity.unsafe_set(x,y,invalidDisparity);
			}
			for (int x = minDisparity; x < lengthX; x++) {
				disparity.unsafe_set(x,y, processPixel(x));
			}
		}
	}

	/**
	 * Sets up internal data structures based on the aggregated cost
	 */
	void setup(Planar<GrayU16> aggregatedYXD) {
		this.lengthY = aggregatedYXD.getNumBands();
		this.lengthX = aggregatedYXD.height;
		this.lengthD = aggregatedYXD.width;
		this.invalidDisparity = lengthD;
		if( invalidDisparity > 255 )
			throw new IllegalArgumentException("Disparity range is too great. Must be < 256 not "+lengthD);
	}

	/**
	 * Selects the disparity for the specified pixel
	 */
	int processPixel(int x ) {
		// The maximum disparity range that can be considered at 'x'
		int maxLocalDisparity = Math.min(x-minDisparity+1,lengthD);
		int bestScore = maxError;
		int bestDisparity = invalidDisparity;

		int idx = aggregatedXD.getIndex(0,x);
		for (int d = 0; d < maxLocalDisparity; d++) {
			int cost = aggregatedXD.data[idx++] & 0xFFFF;
			if( cost < bestScore ) {
				bestScore = cost;
				bestDisparity = d;
			}
		}

		// right to left consistency check
		if( rightToLeftTolerance >= 0 && bestDisparity != invalidDisparity ) {
			int bestX = selectRightToLeft(x-bestDisparity);
			if( Math.abs(bestX-x) > rightToLeftTolerance )
				bestDisparity = invalidDisparity;
		}

		return bestDisparity;
	}

	/**
	 * Finds the best fit region going from the column (x-coordinate) in right image to left. To find
	 * the pixel in left image that's compared against a pixel in right image, take it's x-coordinate then add
	 * the disparity. e.g. x=10 in right matches x=15 and d=5 in left
	 * @param col x-coordinate of point in right image
	 * @return best fit disparity from right to left
	 */
	private int selectRightToLeft( int col ) {
		// The range of disparities it can search
		int maxLocalDisparity = Math.min(this.lengthX, col+this.lengthD)-col-minDisparity;

		int idx = aggregatedXD.getIndex(0,col); // disparity of zero at col

		// best column in left image that it matches up with col in right
		int bestD = 0;
		float scoreBest = aggregatedXD.data[idx];

		for( int i = 1; i < maxLocalDisparity; i++ ) {
			idx += lengthD; // go to index next x-coordinate
			int s = aggregatedXD.data[idx+i] & 0xFFFF;

			if( s < scoreBest ) {
				scoreBest = s;
				bestD = i;
			}
		}

		return col+minDisparity+bestD;
	}

	public int getRightToLeftTolerance() {
		return rightToLeftTolerance;
	}

	public void setRightToLeftTolerance(int rightToLeftTolerance) {
		this.rightToLeftTolerance = rightToLeftTolerance;
	}

	public int getMaxError() {
		return maxError;
	}

	public void setMaxError(int maxError) {
		this.maxError = maxError;
	}

	public int getMinDisparity() {
		return minDisparity;
	}

	public void setMinDisparity(int minDisparity) {
		this.minDisparity = minDisparity;
	}

	public int getInvalidDisparity() {
		return invalidDisparity;
	}
}
