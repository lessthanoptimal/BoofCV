/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial;

import boofcv.alg.feature.shapes.SplitMergeLineFitLoop;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;

/**
 * This fiducial encores a 12-bit number.  The inner region is broken up into 16-squares which are
 * either white or black.  One corner is always back and the others are always white.  This
 * allows orientation to be uniquely determined.
 *
 * @author Peter Abeles
 */
public class DetectFiducialSquareBinary extends BaseDetectFiducialSquare {

	ImageUInt8 binary = new ImageUInt8(1,1);
	ImageFloat32 temp0 = new ImageFloat32(1,1);
	ImageFloat32 temp1 = new ImageFloat32(1,1);

	int radius;

	int counts[] = new int[16];
	int squareSize[] = new int[16];
	int classified[] = new int[16];

	protected DetectFiducialSquareBinary(SplitMergeLineFitLoop fitPolygon, int squarePixels) {
		super(fitPolygon, squarePixels);
		binary.reshape(squarePixels,squarePixels);
		temp0.reshape(squarePixels,squarePixels);
		temp1.reshape(squarePixels,squarePixels);

		// make the radius large enough so that it will include an entire square
		radius = (squarePixels/12 + squarePixels/24 );
	}

	@Override
	public boolean processSquare(ImageFloat32 gray, Result result) {
		// convert input image into binary number
		findBitCounts(gray);

		if (thresholdBinaryNumber())
			return false;

		// search for the black corner for orientation information

		// adjust binary number for rotating it

		// return the results

		return true;
	}

	private boolean thresholdBinaryNumber() {
		for (int i = 0; i < 16; i++) {
			int lower = (int)(squareSize[i]*0.15);
			int upper = (int)(squareSize[i]*0.85);

			if( counts[i] < lower ) {
				classified[i] = 0;
			} else if( counts[i] > upper ) {
				classified[i] = 1;
			} else {
				// it's ambiguous so just fail
				return true;
			}
		}
		return false;
	}

	private void findBitCounts(ImageFloat32 gray) {
		// compute binary image using an adaptive algorithm to handle shadows
		// TODO use fancy one
		ThresholdImageOps.adaptiveSquare(gray, binary, 3, -5, true, temp0, temp1);

		for (int row = 0; row < 4; row++) {
			int y0 = (row+1)*binary.width/6;
			int y1 = (row+2)*binary.width/6;
			for (int col = 0; col < 4; col++) {
				int x0 = (col+1)*binary.width/6;
				int x1 = (col+2)*binary.width/6;

				int total = 0;
				for (int i = y0; i < y1; i++) {
					int index = i*binary.width + x0;
					for( int j = x0; j < x1; j++ ) {
						total += binary.data[index++];
					}
				}

				squareSize[row*4 + col] = (x1-x0)*(y1-y0);
				counts[row*4 + col] = total;
			}
		}
	}
}
