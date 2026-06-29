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

package boofcv.alg.disparity.sgm.cost;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.disparity.sgm.SgmDisparityCost;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.Planar;

/// Base class for computing SGM cost using single pixel error metrics. It handles iterating through all possible
/// disparity values for all pixels in the image and any other book keeping. Only the score needs to be implemented.
@SuppressWarnings({"NullAway.Init"})
public abstract class SgmCostBase<T extends ImageBase<T>> implements SgmDisparityCost<T> {
	protected T left, right;
	protected GrayU16 costXD;

	protected int disparityMin;
	protected int disparityRange;

	@Override
	public void configure( int disparityMin, int disparityRange ) {
		this.disparityMin = disparityMin;
		this.disparityRange = disparityRange;
	}

	@Override
	public void process( T left, T right, Planar<GrayU16> costYXD ) {
		InputSanityCheck.checkSameShape(left, right);
		if (disparityRange == 0)
			throw new IllegalArgumentException("disparityRange is 0. Did you call configure()?");
		this.left = left;
		this.right = right;

		// Declare the "tensor" with shape (lengthY,lengthX,lengthD)
		costYXD.reshape(/* width= */disparityRange, /* height= */left.width, /* numberOfBands= */left.height);

		// First stored column. The cost tensor's x-coordinate is relative to this, which equals disparityMin for
		// a non-negative search and 0 once a negative disparity is allowed.
		int xOffset = Math.max(0, disparityMin);

		for (int y = 0; y < left.height; y++) {
			costXD = costYXD.getBand(y);

			int idxLeft = left.startIndex + y*left.stride + xOffset;

			for (int x = xOffset; x < left.width; x++, idxLeft++) {
				int idxOut = costXD.startIndex + (x - xOffset)*costYXD.stride;

				// Per-column disparity window. Index d maps to disparity = disparityMin + d and the matching
				// right-image column is (x - disparityMin - d), which must lie inside [0, width-1].
				// The right border (column <= width-1) sets the low bound, the left border (column >= 0) the high.
				// loD is capped at disparityRange so a column entirely off the right border becomes all max cost.
				int loD = Math.min(disparityRange, Math.max(0, x - disparityMin - (right.width - 1)));
				int hiD = Math.min(disparityRange, x - disparityMin + 1);

				// Fill disparities that fall off the right border of the right image with max cost
				for (int d = 0; d < loD; d++) {
					costXD.data[idxOut + d] = SgmDisparityCost.MAX_COST;
				}

				// start reading the right image at the largest in-bounds disparity then decrease disparity size
				int idxRight = right.startIndex + y*right.stride + x - disparityMin - loD;

				computeDisparityErrors(idxLeft, idxRight, idxOut + loD, hiD - loD);

				// Fill disparities that fall off the left border of the right image with max cost
				for (int d = hiD; d < disparityRange; d++) {
					costXD.data[idxOut + d] = SgmDisparityCost.MAX_COST;
				}
			}
		}
	}

	protected abstract void computeDisparityErrors( int idxLeft, int idxRight, int idxOut, int localRange );
}
