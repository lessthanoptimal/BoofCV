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

package boofcv.alg.feature.detect.intensity;

import boofcv.alg.feature.detect.intensity.impl.ImplXCornerAbeles2019Intensity;
import boofcv.alg.feature.detect.intensity.impl.ImplXCornerAbeles2019Intensity_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayF32;

/**
 * X-Corner detector. Can be used to detector chessboard corners. Samples in a circle around a targeted pixel.
 * It's at a maximum when adjacent points in the circle are all above or below the mean.
 *
 * @author Peter Abeles
 */
public class XCornerAbeles2019Intensity {

	/**
	 * Computes the x-corner intensity. It's assumed the image has already had Gaussian blur applied to it
	 * with a radius of 1
	 *
	 * @param input Blurred input image. Recommended that it's normalized to have values from -1 to 1
	 * @param intensity x-corner intensity.
	 */
	public static void process( GrayF32 input, GrayF32 intensity ) {
		intensity.reshape(input);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplXCornerAbeles2019Intensity_MT.process(input, intensity);
		} else {
			ImplXCornerAbeles2019Intensity.process(input, intensity);
		}
	}
}
