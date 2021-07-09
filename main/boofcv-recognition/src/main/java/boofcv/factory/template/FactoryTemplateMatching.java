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

package boofcv.factory.template;

import boofcv.alg.template.*;
import boofcv.alg.template.TemplateIntensityImage.EvaluatorMethod;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

/**
 * Factory for creating template matching algorithms.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class FactoryTemplateMatching {

	/**
	 * Creates {@link TemplateMatchingIntensity} of the specified type. Likely
	 * matches can be extracted using {@link boofcv.abst.feature.detect.extract.NonMaxSuppression}.
	 *
	 * @param type Type of error function
	 * @param imageType Image type being processed
	 * @return {@link TemplateMatchingIntensity} of the specified type.
	 */
	public static <T extends ImageGray<T>>
	TemplateMatchingIntensity<T> createIntensity( TemplateScoreType type, Class<T> imageType ) {
		if (type == TemplateScoreType.CORRELATION) {
			if (imageType == GrayF32.class) {
				return (TemplateMatchingIntensity<T>)new TemplateCorrelationFFT();
			} else {
				throw new IllegalArgumentException("Image type not supported. " + imageType.getSimpleName());
			}
		}

		EvaluatorMethod<T> method;

		switch (type) {
			case SUM_ABSOLUTE_DIFFERENCE:
				if (imageType == GrayU8.class) {
					method = (EvaluatorMethod<T>)new TemplateSumAbsoluteDifference.U8();
				} else if (imageType == GrayF32.class) {
					method = (EvaluatorMethod<T>)new TemplateSumAbsoluteDifference.F32();
				} else {
					throw new IllegalArgumentException("Image type not supported. " + imageType.getSimpleName());
				}
				break;

			case SUM_SQUARE_ERROR:
				if (imageType == GrayU8.class) {
					method = (EvaluatorMethod<T>)new TemplateSumSquaredError.U8();
				} else if (imageType == GrayF32.class) {
					method = (EvaluatorMethod<T>)new TemplateSumSquaredError.F32();
				} else {
					throw new IllegalArgumentException("Image type not supported. " + imageType.getSimpleName());
				}
				break;

			case SQUARED_DIFFERENCE_NORMED:
				if (imageType == GrayU8.class) {
					method = (EvaluatorMethod<T>)new TemplateSqDiffNormed.U8();
				} else if (imageType == GrayF32.class) {
					method = (EvaluatorMethod<T>)new TemplateSqDiffNormed.F32();
				} else {
					throw new IllegalArgumentException("Image type not supported. " + imageType.getSimpleName());
				}
				break;

			case NCC:
				if (imageType == GrayU8.class) {
					method = (EvaluatorMethod<T>)new TemplateNCC.U8();
				} else if (imageType == GrayF32.class) {
					method = (EvaluatorMethod<T>)new TemplateNCC.F32();
				} else {
					throw new IllegalArgumentException("Image type not supported. " + imageType.getSimpleName());
				}
				break;

			default:
				throw new IllegalArgumentException("Unknown");
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			return new TemplateIntensityImage_MT<>(method);
		} else {
			return new TemplateIntensityImage<>(method);
		}
	}

	/**
	 * Creates an instance of {@link TemplateMatching} for the specified score type.
	 *
	 * @param type Type of error function
	 * @param imageType Image type being processed
	 * @return {@link TemplateMatching} of the specified type.
	 */
	public static <T extends ImageGray<T>>
	TemplateMatching<T> createMatcher( TemplateScoreType type, Class<T> imageType ) {
		TemplateMatchingIntensity<T> intensity = createIntensity(type, imageType);

		return new TemplateMatching<>(intensity);
	}
}
