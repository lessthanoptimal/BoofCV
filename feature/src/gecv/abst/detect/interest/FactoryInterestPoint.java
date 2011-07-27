/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.abst.detect.interest;

import gecv.abst.detect.corner.GeneralFeatureDetector;
import gecv.abst.filter.derivative.FactoryDerivative;
import gecv.abst.filter.derivative.ImageGradient;
import gecv.abst.filter.derivative.ImageHessian;
import gecv.alg.detect.interest.FeatureLaplaceScaleSpace;
import gecv.core.image.ImageGenerator;
import gecv.core.image.inst.FactoryImageGenerator;
import gecv.struct.image.ImageBase;

/**
 * Factory for creating/wrapping interest points detectors.
 *
 * @author Peter Abeles
 */
// todo comment
public class FactoryInterestPoint {

	public static <T extends ImageBase, D extends ImageBase >
	InterestPointDetector<T> fromCorner( GeneralFeatureDetector<T,D> feature, Class<T> inputType , Class<D> derivType) {

		ImageGradient<T,D> gradient = FactoryDerivative.sobel(inputType,derivType);
		ImageHessian<D> hessian  = FactoryDerivative.hessianSobel(derivType);
		ImageGenerator<D> derivativeGenerator = FactoryImageGenerator.create(derivType);

		return new WrapCornerToInterestPoint<T,D>(feature,gradient,hessian,derivativeGenerator);
	}

	public static <T extends ImageBase, D extends ImageBase >
	InterestPointDetector<T> fromFeatureLaplace( FeatureLaplaceScaleSpace<T,D> feature, Class<T> inputType , Class<D> derivType) {
		return null;
	}
}
