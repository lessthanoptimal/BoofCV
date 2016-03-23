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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;


/**
 * Implementations of {@link boofcv.alg.feature.detect.intensity.MedianCornerIntensity}.
 *
 * @author Peter Abeles
 */
public class ImplMedianCornerIntensity {

	public static void process(GrayF32 intensity , GrayF32 originalImage, GrayF32 medianImage)
	{
		final int width = originalImage.width;
		final int height = originalImage.height;

		for( int y = 0; y < height; y++ ) {

			int indexOrig = originalImage.startIndex + originalImage.stride*y;
			int indexMed = medianImage.startIndex + medianImage.stride*y;
			int indexInten = intensity.startIndex + intensity.stride*y;

			for( int x = 0; x < width; x++ ) {
				float val = originalImage.data[indexOrig++] - medianImage.data[indexMed++];

				intensity.data[indexInten++] = val < 0 ? -val : val;
			}
		}
	}

	public static void process(GrayF32 intensity , GrayU8 originalImage, GrayU8 medianImage)
	{
		final int width = originalImage.width;
		final int height = originalImage.height;

		for( int y = 0; y < height; y++ ) {

			int indexOrig = originalImage.startIndex + originalImage.stride*y;
			int indexMed = medianImage.startIndex + medianImage.stride*y;
			int indexInten = intensity.startIndex + intensity.stride*y;

			for( int x = 0; x < width; x++ ) {
				int val = (originalImage.data[indexOrig++] & 0xFF) - (medianImage.data[indexMed++] & 0xFF);

				intensity.data[indexInten++] = val < 0 ? -val : val;
			}
		}
	}
}
