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

package boofcv.factory.feature.detect.peak;

import boofcv.abst.feature.detect.peak.MeanShiftPeak_to_SearchLocalPeak;
import boofcv.abst.feature.detect.peak.SearchLocalPeak;
import boofcv.alg.feature.detect.peak.MeanShiftPeak;
import boofcv.alg.weights.WeightPixelGaussian_F32;
import boofcv.alg.weights.WeightPixelUniform_F32;
import boofcv.alg.weights.WeightPixel_F32;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.ImageGray;

/**
 * Factory for implementations of {@link SearchLocalPeak}
 *
 * @author Peter Abeles
 */
public class FactorySearchLocalPeak {

	/**
	 * Mean-shift based search with a uniform kernel
	 *
	 * @param config Configuration for the search
	 * @param imageType Type of input image
	 * @return mean-shift search
	 */
	public static <T extends ImageGray<T>>
	SearchLocalPeak<T> meanShiftUniform( ConfigMeanShiftSearch config, Class<T> imageType ) {
		WeightPixel_F32 weights = new WeightPixelUniform_F32();
		MeanShiftPeak<T> alg = new MeanShiftPeak<>(config.maxIterations, (float)config.convergenceTol,
				weights, config.odd, imageType, BorderType.EXTENDED);
		return new MeanShiftPeak_to_SearchLocalPeak<>(alg, config.positiveOnly);
	}

	/**
	 * Mean-shift based search with a Gaussian kernel
	 *
	 * @param config Configuration for the search
	 * @param imageType Type of input image
	 * @return mean-shift search
	 */
	public static <T extends ImageGray<T>>
	SearchLocalPeak<T> meanShiftGaussian( ConfigMeanShiftSearch config, Class<T> imageType ) {
		WeightPixel_F32 weights = new WeightPixelGaussian_F32();
		MeanShiftPeak<T> alg = new MeanShiftPeak<>(config.maxIterations, (float)config.convergenceTol,
				weights, config.odd, imageType, BorderType.EXTENDED);
		return new MeanShiftPeak_to_SearchLocalPeak<>(alg, config.positiveOnly);
	}
}
