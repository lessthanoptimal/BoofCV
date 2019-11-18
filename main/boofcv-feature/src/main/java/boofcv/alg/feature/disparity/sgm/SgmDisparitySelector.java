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
 * @author Peter Abeles
 */
public class SgmDisparitySelector {

	// TODO add texture ambiguity
	// TODO sub-pixel

	// tolerance for right to left validation. if < 0 then it's disabled
	protected int rightToLeftTolerance;

	// Maximum allowed error
	int maxError = Integer.MAX_VALUE;

	// reference to input cost tensor
	GrayU16 aggregatedXD;

	// The minimum possible disparity
	int minDisparity;

	// Shape of the input tensor
	int lengthY,lengthX,lengthD;

	public void select(Planar<GrayU16> aggregatedYXD , int minDisparity , int invalidDisparity, GrayU8 disparity ) {
		this.minDisparity = minDisparity;
		this.lengthY = aggregatedYXD.getNumBands();
		this.lengthX = aggregatedYXD.height;
		this.lengthD = aggregatedYXD.width;

		// Ensure that the output matches the input
		disparity.reshape(lengthX,lengthY);

		for (int y = 0; y < lengthY; y++) {
			aggregatedXD = aggregatedYXD.getBand(y);

			for (int x = 0; x < lengthX; x++) {
				int bestScore = maxError;
				int bestDisparity = invalidDisparity;

				// TODO see what the maximum disparity that can be considered is
				int idx = aggregatedXD.getIndex(0,x);
				for (int d = 0; d < lengthD; d++) {
					int cost = aggregatedXD.data[idx++] & 0xFFFF;
					if( cost < bestScore ) {
						bestScore = cost;
						bestDisparity = d;
					}
				}

				// right to left consistency check
				if( rightToLeftTolerance > 0 && bestDisparity != invalidDisparity ) {
					int bestX = selectRightToLeft(x-bestDisparity);
					if( Math.abs(bestX-x) > rightToLeftTolerance )
						bestDisparity = invalidDisparity;
				}

				disparity.unsafe_set(x,y,bestDisparity);
			}
		}
	}

	private int selectRightToLeft( int col ) {
		// The range of disparities it can search
		int maxLocalDisparity = Math.min(this.lengthX-1, col+this.lengthD)-col-minDisparity;

		int idx = aggregatedXD.getIndex(0,col);

		// best column in left image that it matches up with
		int bestCol = 0;
		float scoreBest = aggregatedXD.data[idx];

		for( int i = 1; i < maxLocalDisparity; i++ ) {
			int s = aggregatedXD.data[idx+i] & 0xFFFF;

			if( s < scoreBest ) {
				scoreBest = s;
				bestCol = i;
			}
		}

		return bestCol;
	}
}
