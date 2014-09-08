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

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.feature.shapes.SplitMergeLineFitLoop;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

import java.util.Arrays;

/**
 * <p>
 * This fiducial encores a 12-bit number.  The inner region is broken up into 16-squares which are
 * either white or black.  One corner is always back and the others are always white.  This
 * allows orientation to be uniquely determined.
 * </p>
 * <p>
 * Canonical orientation is with the black square in the lower-left hand corner.
 * </p>
 * @author Peter Abeles
 */
// TODO classify the border.  If not all black discard the pattern
public class DetectFiducialSquareBinary<T extends ImageSingleBand>
		extends BaseDetectFiducialSquare<T> {

	// converts the input image into a binary one
	InputToBinary<ImageFloat32> threshold = FactoryThresholdBinary.globalOtsu(0,256,true,ImageFloat32.class);
	ImageUInt8 binary = new ImageUInt8(1,1);

	// helper data structures for computing the value of each grid point
	int counts[] = new int[16];
	int classified[] = new int[16];

	int tmp[] = new int[16];

	// storage for no border sub-image
	ImageFloat32 grayNoBorder = new ImageFloat32();

	// size of a square
	protected final static int r=5;
	protected final static int w=r*2+1;
	protected final static int N=w*w;

	/**
	 * Configures the fiducial detector
	 *
	 * @param fitPolygon used to fit a polygon to binary blobs
	 * @param inputType Type of image it's processing
	 */
	public DetectFiducialSquareBinary(InputToBinary<T> thresholder,
									  SplitMergeLineFitLoop fitPolygon,
									  double minContourFraction, Class<T> inputType) {
		super(thresholder,fitPolygon, w*8, minContourFraction, inputType);

		int widthNoBorder = w*4;

		binary.reshape(widthNoBorder,widthNoBorder);
	}

	@Override
	protected boolean processSquare(ImageFloat32 gray, Result result) {

		int off = (gray.width-binary.width)/2;
		gray.subimage(off,off,gray.width-off,gray.width-off,grayNoBorder);

//		grayNoBorder.printInt();

		// convert input image into binary number
		findBitCounts(grayNoBorder);

		if (thresholdBinaryNumber())
			return false;

		// adjust the orientation until the black corner is in the lower left
		if (rotateUntilInLowerCorner(result))
			return false;

		// extract the numerical value it encodes
		int val = 0;

		val |= classified[13] << 0;
		val |= classified[14] << 1;
		val |= classified[8] << 2;
		val |= classified[9] << 3;
		val |= classified[10] << 4;
		val |= classified[11] << 5;
		val |= classified[4] << 6;
		val |= classified[5] << 7;
		val |= classified[6] << 8;
		val |= classified[7] << 9;
		val |= classified[1] << 10;
		val |= classified[2] << 11;

		result.which = val;

		return true;
	}

	/**
	 * Rotate the pattern until the black corner is in the lower right.  Sanity check to make
	 * sure there is only one black corner
	 */
	private boolean rotateUntilInLowerCorner(Result result) {
		// sanity check corners.  There should only be one exactly one black
		if( classified[0] + classified[3] + classified[15] + classified[12] != 1 )
			return true;

		// Rotate until the black corner is in the lower left hand corner on the image.
		// remember that origin is the top left corner
		result.rotation = 0;
		while( classified[12] != 1 ) {
			result.rotation++;
			rotateClockWise();
		}
		return false;
	}

	/**
	 * Rotate the 4x4 binary clockwise
	 */
	protected void rotateClockWise() {
		tmp[0] = classified[12];
		tmp[1] = classified[8];
		tmp[2] = classified[4];
		tmp[3] = classified[0];

		tmp[4] = classified[13];
		tmp[5] = classified[9];
		tmp[6] = classified[5];
		tmp[7] = classified[1];

		tmp[8] = classified[14];
		tmp[9] = classified[10];
		tmp[10] = classified[6];
		tmp[11] = classified[2];

		tmp[12] = classified[15];
		tmp[13] = classified[11];
		tmp[14] = classified[7];
		tmp[15] = classified[3];

		System.arraycopy(tmp,0,classified,0,16);
	}

	/**
	 * Sees how many pixels were positive and negative in each square region.  Then decides if they
	 * should be 0 or 1 or unknown
	 */
	private boolean thresholdBinaryNumber() {

		int lower = (int)(N*0.15);
		int upper = (int)(N*0.85);

		for (int i = 0; i < 16; i++) {


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
		threshold.process(gray,binary);

		Arrays.fill(counts,0);
		for (int row = 0; row < 4; row++) {
			int y0 = row*binary.width/4;
			int y1 = (row+1)*binary.width/4;
			for (int col = 0; col < 4; col++) {
				int x0 = col*binary.width/4;
				int x1 = (col+1)*binary.width/4;

				int total = 0;
				for (int i = y0; i < y1; i++) {
					int index = i*binary.width + x0;
					for( int j = x0; j < x1; j++ ) {
						total += binary.data[index++];
					}
				}

				counts[row*4 + col] = total;
			}
		}
	}
}
