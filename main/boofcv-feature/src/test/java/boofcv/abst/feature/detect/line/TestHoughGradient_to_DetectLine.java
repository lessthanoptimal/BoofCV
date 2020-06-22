/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.line;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.line.HoughParametersPolar;
import boofcv.alg.feature.detect.line.HoughTransformGradient;
import boofcv.alg.feature.detect.line.HoughTransformParameters;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class TestHoughGradient_to_DetectLine extends GeneralDetectLineGradientTests {
	@Override
	public <T extends ImageGray<T>> DetectLine<T> createAlg(Class<T> imageType) {
		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(4, 2, 0, true));
		HoughTransformParameters polar = new HoughParametersPolar(2,180);
		HoughTransformGradient hough = new HoughTransformGradient(extractor,polar,imageType);
		ImageGradient gradient = FactoryDerivative.gradient(DerivativeType.SOBEL, ImageType.single(imageType),null);

		return new HoughGradient_to_DetectLine(hough,gradient,imageType);
	}
}
