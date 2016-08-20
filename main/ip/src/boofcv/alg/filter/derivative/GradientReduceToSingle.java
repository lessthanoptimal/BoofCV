/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.derivative;

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;

/**
 * Contains functions that reduce the number of bands in the input image into a single band.
 *
 * @author Peter Abeles
 */
public class GradientReduceToSingle {

	/**
	 * Reduces the number of bands by selecting the band with the largest Frobenius norm and using
	 * its gradient to be the output gradient on a pixel-by-pixel basis
	 *
	 * @param inX Input gradient X
	 * @param inY Input gradient Y
	 * @param outX Output gradient X
	 * @param outY Output gradient Y
	 */
	public static void maxf(Planar<GrayF32> inX , Planar<GrayF32> inY , GrayF32 outX , GrayF32 outY )
	{
		// input and output should be the same shape
		InputSanityCheck.checkSameShape(inX,inY,outX,outY);

		// make sure that the pixel index is the same
		InputSanityCheck.checkIndexing(inX,inY);
		InputSanityCheck.checkIndexing(outX,outY);

		for (int y = 0; y < inX.height; y++) {
			int indexIn = inX.startIndex + inX.stride*y;
			int indexOut = outX.startIndex + outX.stride*y;

			for (int x = 0; x < inX.width; x++, indexIn++, indexOut++ ) {
				float maxValueX = inX.bands[0].data[indexIn];
				float maxValueY = inY.bands[0].data[indexIn];
				float maxNorm = maxValueX*maxValueX + maxValueY*maxValueY;

				for (int band = 1; band < inX.bands.length; band++) {
					float valueX = inX.bands[band].data[indexIn];
					float valueY = inY.bands[band].data[indexIn];

					float n = valueX*valueX + valueY*valueY;
					if( n > maxNorm ) {
						maxNorm = n;
						maxValueX = valueX;
						maxValueY = valueY;
					}
				}

				outX.data[indexOut] = maxValueX;
				outY.data[indexOut] = maxValueY;
			}
		}
	}

	/**
	 * Reduces the number of bands by selecting the band with the largest Frobenius norm and using
	 * its gradient to be the output gradient on a pixel-by-pixel basis
	 *
	 * @param inX Input gradient X
	 * @param inY Input gradient Y
	 * @param outX Output gradient X
	 * @param outY Output gradient Y
	 */
	public static void maxf(Planar<GrayU8> inX , Planar<GrayU8> inY ,
							GrayU8 outX , GrayU8 outY )
	{
		// input and output should be the same shape
		InputSanityCheck.checkSameShape(inX,inY,outX,outY);

		// make sure that the pixel index is the same
		InputSanityCheck.checkIndexing(inX,inY);
		InputSanityCheck.checkIndexing(outX,outY);

		for (int y = 0; y < inX.height; y++) {
			int indexIn = inX.startIndex + inX.stride*y;
			int indexOut = outX.startIndex + outX.stride*y;

			for (int x = 0; x < inX.width; x++, indexIn++, indexOut++ ) {
				int maxValueX = inX.bands[0].data[indexIn] & 0xFF;
				int maxValueY = inY.bands[0].data[indexIn] & 0xFF;
				int maxNorm = maxValueX*maxValueX + maxValueY*maxValueY;

				for (int band = 1; band < inX.bands.length; band++) {
					int valueX = inX.bands[band].data[indexIn] & 0xFF;
					int valueY = inY.bands[band].data[indexIn] & 0xFF;

					int n = valueX*valueX + valueY*valueY;
					if( n > maxNorm ) {
						maxNorm = n;
						maxValueX = valueX;
						maxValueY = valueY;
					}
				}

				outX.data[indexOut] = (byte)maxValueX;
				outY.data[indexOut] = (byte)maxValueY;
			}
		}
	}
}
