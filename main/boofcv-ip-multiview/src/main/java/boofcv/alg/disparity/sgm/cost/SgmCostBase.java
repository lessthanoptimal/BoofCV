/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

/**
 * Base class for computing SGM cost using single pixel error metrics. It handles iterating through all possible
 * disparity values for all pixels in the image and any other book keeping. Only the score needs to be implemented.
 *
 * @author Peter Abeles
 */
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

		for (int y = 0; y < left.height; y++) {
			costXD = costYXD.getBand(y);

			int idxLeft = left.startIndex + y*left.stride + disparityMin;

			for (int x = disparityMin; x < left.width; x++, idxLeft++) {
				int idxOut = costXD.startIndex + (x - disparityMin)*costYXD.stride;

				// The local limits on ranges that can be examined
				int localRange = Math.min(disparityRange, x - disparityMin + 1);

				// start reading the right image at the smallest disparity then increase disparity size
				int idxRight = right.startIndex + y*right.stride + x - disparityMin;

				computeDisparityErrors(idxLeft, idxRight, idxOut, localRange);

				// Fill in the disparity values outside the image with max cost
				for (int d = localRange; d < disparityRange; d++) {
					costXD.data[idxOut + d] = SgmDisparityCost.MAX_COST;
				}
			}
		}
	}

	protected abstract void computeDisparityErrors( int idxLeft, int idxRight, int idxOut, int localRange );
}
