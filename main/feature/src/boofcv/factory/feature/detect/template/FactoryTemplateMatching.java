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

package boofcv.factory.feature.detect.template;

import boofcv.alg.feature.detect.template.*;
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
	 * Creates {@link TemplateMatchingIntensity} of the specified type.  Likely
	 * matches can be extracted using {@link boofcv.abst.feature.detect.extract.NonMaxSuppression}.
	 *
	 * @param type      Type of error function
	 * @param imageType Image type being processed
	 * @return {@link TemplateMatchingIntensity} of the specified type.
	 */
	public static <T extends ImageGray>
	TemplateMatchingIntensity<T> createIntensity(TemplateScoreType type, Class<T> imageType) {
		switch (type) {
			case SUM_DIFF_SQ:
				if (imageType == GrayU8.class) {
					return (TemplateMatchingIntensity<T>) new TemplateDiffSquared.U8();
				} else if (imageType == GrayF32.class) {
					return (TemplateMatchingIntensity<T>) new TemplateDiffSquared.F32();
				} else {
					throw new IllegalArgumentException("Image type not supported. " + imageType.getSimpleName());
				}

			case NCC:
				if (imageType == GrayU8.class) {
					return (TemplateMatchingIntensity<T>) new TemplateNCC.U8();
				} else if (imageType == GrayF32.class) {
					return (TemplateMatchingIntensity<T>) new TemplateNCC.F32();
				} else {
					throw new IllegalArgumentException("Image type not supported. " + imageType.getSimpleName());
				}

			case CORRELATION:
				if (imageType == GrayF32.class) {
					return (TemplateMatchingIntensity<T>) new TemplateCorrelationFFT();
				} else {
					throw new IllegalArgumentException("Image type not supported. " + imageType.getSimpleName());
				}
		}
		throw new IllegalArgumentException("Type not found: " + type);
	}

	/**
	 * Creates an instance of {@link TemplateMatching} for the specified score type.
	 *
	 * @param type      Type of error function
	 * @param imageType Image type being processed
	 * @return {@link TemplateMatching} of the specified type.
	 */
	public static <T extends ImageGray>
	TemplateMatching<T> createMatcher(TemplateScoreType type, Class<T> imageType) {
		TemplateMatchingIntensity<T> intensity = createIntensity(type, imageType);

		return new TemplateMatching<>(intensity);
	}
}
