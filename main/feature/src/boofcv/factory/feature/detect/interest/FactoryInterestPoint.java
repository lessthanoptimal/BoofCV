/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.detect.interest;

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
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

/**
 * Factory for creating interest point detectors which conform to the {@link InterestPointDetector}
 * interface
 * <p/>
 * <p>
 * NOTE: Higher level interface than {@link GeneralFeatureDetector}.  This will automatically
 * compute image derivatives across scale space as needed, unlike GeneralFeatureDetector which
 * just detects features at a particular scale and requires image derivatives be passed in.
 * </p>
 *
 * @author Peter Abeles
 * @see FactoryFeatureExtractor
 * @see FactoryInterestPoint
 */
public class FactoryInterestPoint {

	/**
	 * Wraps {@link GeneralFeatureDetector} inside an {@link InterestPointDetector}.
	 *
	 * @param feature   Feature detector.
	 * @param scale Scale of detected features
	 * @param inputType Image type of input image.
	 * @param inputType Image type for gradient.
	 * @return The interest point detector.
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	InterestPointDetector<T> wrapPoint(GeneralFeatureDetector<T, D> feature, double scale , Class<T> inputType, Class<D> derivType) {

		ImageGradient<T, D> gradient = null;
		ImageHessian<D> hessian = null;
		ImageGenerator<D> derivativeGenerator = null;

		if (feature.getRequiresGradient() || feature.getRequiresHessian())
			gradient = FactoryDerivative.sobel(inputType, derivType);
		if (feature.getRequiresHessian())
			hessian = FactoryDerivative.hessianSobel(derivType);
		if (gradient != null || hessian != null)
			derivativeGenerator = FactoryImageGenerator.create(derivType);

		return new WrapCornerToInterestPoint<T, D>(feature, gradient, hessian, derivativeGenerator,scale);
	}

	/**
	 * Wraps {@link FeatureLaplaceScaleSpace} inside an {@link InterestPointDetector}.
	 *
	 * @param feature   Feature detector.
	 * @param scales    Scales at which features are detected at.
	 * @param inputType Image type of input image.
	 * @return The interest point detector.
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	InterestPointDetector<T> wrapDetector(FeatureLaplaceScaleSpace<T, D> feature,
										  double[] scales,
										  Class<T> inputType) {

		GaussianScaleSpace<T, D> ss = FactoryGaussianScaleSpace.nocache(inputType);
		ss.setScales(scales);

		return new WrapFLSStoInterestPoint<T, D>(feature, ss);
	}

	/**
	 * Wraps {@link FeatureScaleSpace} inside an {@link InterestPointDetector}.
	 *
	 * @param feature   Feature detector.
	 * @param scales    Scales at which features are detected at.
	 * @param inputType Image type of input image.
	 * @return The interest point detector.
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	InterestPointDetector<T> wrapDetector(FeatureLaplacePyramid<T, D> feature,
										  double[] scales,
										  Class<T> inputType) {

		ScaleSpacePyramid<T> ss = new ScaleSpacePyramid<T>(inputType, scales);

		return new WrapFLPtoInterestPoint<T, D>(feature, ss);
	}

	/**
	 * Wraps {@link FeatureScaleSpace} inside an {@link InterestPointDetector}.
	 *
	 * @param feature   Feature detector.
	 * @param scales    Scales at which features are detected at.
	 * @param inputType Image type of input image.
	 * @return The interest point detector.
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	InterestPointDetector<T> wrapDetector(FeatureScaleSpace<T, D> feature,
										  double[] scales,
										  Class<T> inputType) {

		GaussianScaleSpace<T, D> ss = FactoryGaussianScaleSpace.nocache(inputType);
		ss.setScales(scales);

		return new WrapFSStoInterestPoint<T, D>(feature, ss);
	}

	/**
	 * Wraps {@link FeaturePyramid} inside an {@link InterestPointDetector}.
	 *
	 * @param feature   Feature detector.
	 * @param scales    Scales at which features are detected at.
	 * @param inputType Image type of input image.
	 * @return The interest point detector.
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	InterestPointDetector<T> wrapDetector(FeaturePyramid<T, D> feature,
										  double[] scales,
										  Class<T> inputType) {

		ScaleSpacePyramid<T> ss = new ScaleSpacePyramid<T>(inputType, scales);

		return new WrapFPtoInterestPoint<T, D>(feature, ss);
	}

	/**
	 * Creates a {@link FastHessianFeatureDetector} detector which is wrapped inside
	 * an {@link InterestPointDetector}
	 *
	 * @param config Configuration for detector.
	 * @return The interest point detector.
	 * @see FastHessianFeatureDetector
	 */
	public static <T extends ImageSingleBand>
	InterestPointDetector<T> fastHessian( ConfigFastHessian config ) {

		return new WrapFHtoInterestPoint(FactoryInterestPointAlgs.fastHessian(config));
	}

	/**
	 * Creates a SIFT feature detector.
	 *
	 * @see SiftDetector
	 * @see SiftImageScaleSpace
	 *
	 * @param scaleSigma Amount of blur applied to each scale inside an octaves.  Try 1.6
	 * @param numOfScales Number of scales per octaves.  Try 5.  Must be >= 3
	 * @param numOfOctaves Number of octaves to detect.  Try 4
	 * @param doubleInputImage Should the input image be doubled? Try false.
	 * @param extractRadius   Size of the feature used to detect the corners. Try 2
	 * @param detectThreshold Minimum corner intensity required.  Try 1
	 * @param maxFeaturesPerScale Max detected features per scale.  Image size dependent.  Try 500
	 * @param edgeThreshold Threshold for edge filtering.  Disable with a value <= 0.  Try 5
	 */
	public static InterestPointDetector<ImageFloat32> siftDetector( double scaleSigma ,
																	int numOfScales ,
																	int numOfOctaves ,
																	boolean doubleInputImage ,
																	int extractRadius,
																	float detectThreshold,
																	int maxFeaturesPerScale,
																	double edgeThreshold )
	{
		SiftDetector alg = FactoryInterestPointAlgs.siftDetector(extractRadius,detectThreshold,
				maxFeaturesPerScale,edgeThreshold);

		SiftImageScaleSpace ss = new SiftImageScaleSpace((float)scaleSigma, numOfScales, numOfOctaves,
				doubleInputImage);

		return new WrapSiftDetector(alg,ss);
	}

}
