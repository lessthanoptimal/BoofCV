/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.factory.feature.detect.interest;

import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.extract.GeneralFeatureDetector;
import boofcv.abst.feature.detect.interest.*;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.filter.derivative.ImageHessian;
import boofcv.alg.feature.detect.interest.*;
import boofcv.alg.transform.gss.ScaleSpacePyramid;
import boofcv.core.image.ImageGenerator;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.transform.gss.FactoryGaussianScaleSpace;
import boofcv.struct.gss.GaussianScaleSpace;
import boofcv.struct.image.ImageBase;

/**
 * Factory for creating/wrapping interest points detectors.
 *
 * @author Peter Abeles
 */
// todo comment
public class FactoryInterestPoint {

	public static <T extends ImageBase, D extends ImageBase >
	InterestPointDetector<T> fromCorner( GeneralFeatureDetector<T,D> feature, Class<T> inputType , Class<D> derivType) {

		ImageGradient<T,D> gradient = null;
		ImageHessian<D> hessian = null;
		ImageGenerator<D> derivativeGenerator = null;

		if( feature.getRequiresGradient() || feature.getRequiresHessian() )
			gradient = FactoryDerivative.sobel(inputType,derivType);
		if( feature.getRequiresHessian() )
			hessian  = FactoryDerivative.hessianSobel(derivType);
		if( gradient != null || hessian != null )
			derivativeGenerator = FactoryImageGenerator.create(derivType);

		return new WrapCornerToInterestPoint<T,D>(feature,gradient,hessian,derivativeGenerator);
	}

	public static <T extends ImageBase, D extends ImageBase >
	InterestPointDetector<T> fromFeatureLaplace( FeatureLaplaceScaleSpace<T,D> feature,
												 double []scales ,
												 Class<T> inputType ) {

		GaussianScaleSpace<T,D> ss = FactoryGaussianScaleSpace.nocache(inputType);
		ss.setScales(scales);

		return new WrapFLSStoInterestPoint<T,D>(feature,ss);
	}

	public static <T extends ImageBase, D extends ImageBase >
	InterestPointDetector<T> fromFeatureLaplace( FeatureLaplacePyramid<T,D> feature,
												 double []scales ,
												 Class<T> inputType ) {

		ScaleSpacePyramid<T> ss = new ScaleSpacePyramid<T>(inputType,scales);

		return new WrapFLPtoInterestPoint<T,D>(feature,ss);
	}

	public static <T extends ImageBase, D extends ImageBase >
	InterestPointDetector<T> fromFeature( FeatureScaleSpace<T,D> feature,
											double []scales ,
											Class<T> inputType ) {

		GaussianScaleSpace<T,D> ss = FactoryGaussianScaleSpace.nocache(inputType);
		ss.setScales(scales);

		return new WrapFSStoInterestPoint<T,D>(feature,ss);
	}


	public static <T extends ImageBase, D extends ImageBase >
	InterestPointDetector<T> fromFeature( FeaturePyramid<T,D> feature,
										  double []scales ,
										  Class<T> inputType ) {

		ScaleSpacePyramid<T> ss = new ScaleSpacePyramid<T>(inputType,scales);

		return new WrapFPtoInterestPoint<T,D>(feature,ss);
	}

	/**
	 * Detects features using a Fast Hessian detector as described in the SURF paper.
	 *
	 * @param threshold Minimum feature intensity.
	 * @param maxFeaturesPerScale Number of features it will find or if <= 0 it will return all features it finds.
	 * @param initialSize Typically 9.
	 * @param numberScalesPerOctave Typically 4.
	 * @param numberOfOctaves Typically 4.
	 * @return Fast hessian detector.
	 */
	public static <T extends ImageBase>
	InterestPointDetector<T> fromFastHessian(float threshold,
											 int maxFeaturesPerScale,
											 int initialSize,
											 int numberScalesPerOctave,
											 int numberOfOctaves)
	{
		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(2, threshold, 5, false, true);
		FastHessianFeatureDetector<T> feature = new FastHessianFeatureDetector<T>(extractor,maxFeaturesPerScale,
				initialSize,numberScalesPerOctave,numberOfOctaves);

		return new WrapFHtoInterestPoint<T>(feature);
	}

}
