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

package boofcv.factory.feature.orientation;

import boofcv.abst.feature.orientation.ConfigAverageIntegral;
import boofcv.abst.feature.orientation.ConfigSiftOrientation;
import boofcv.abst.feature.orientation.ConfigSlidingIntegral;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.orientation.*;
import boofcv.alg.feature.orientation.impl.*;
import boofcv.struct.image.*;
import org.jetbrains.annotations.Nullable;

/**
 * Creates specific implementations of local region orientation estimators.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryOrientationAlgs {

	public static <T extends ImageGray<T>>
	OrientationHistogram<T> histogram( double objectToSample, int numAngles, int radius, boolean weighted,
									   Class<T> derivType ) {
		OrientationHistogram<T> ret;

		if (derivType == GrayF32.class) {
			ret = (OrientationHistogram<T>)new ImplOrientationHistogram_F32(objectToSample, numAngles, weighted);
		} else if (derivType == GrayS16.class) {
			ret = (OrientationHistogram<T>)new ImplOrientationHistogram_S16(objectToSample, numAngles, weighted);
		} else if (derivType == GrayS32.class) {
			ret = (OrientationHistogram<T>)new ImplOrientationHistogram_S32(objectToSample, numAngles, weighted);
		} else {
			throw new IllegalArgumentException("Unknown image type.");
		}

		ret.setObjectToSample(radius);

		return ret;
	}

	public static <T extends ImageGray<T>>
	OrientationImageAverage<T> nogradient( double objectToSample, int radius, Class<T> imageType ) {
		OrientationImageAverage<T> ret;

		if (imageType == GrayF32.class) {
			ret = (OrientationImageAverage<T>)new ImplOrientationImageAverage_F32(objectToSample, radius);
		} else if (imageType == GrayU8.class) {
			ret = (OrientationImageAverage<T>)new ImplOrientationImageAverage_U8(objectToSample, radius);
		} else {
			throw new IllegalArgumentException("Unknown image type.");
		}

		ret.setObjectRadius(radius);

		return ret;
	}

	public static <T extends ImageGray<T>>
	OrientationAverage<T> average( double objectToSample, int radius, boolean weighted, Class<T> derivType ) {
		OrientationAverage<T> ret;

		if (derivType == GrayF32.class) {
			ret = (OrientationAverage<T>)new ImplOrientationAverage_F32(objectToSample, weighted);
		} else if (derivType == GrayS16.class) {
			ret = (OrientationAverage<T>)new ImplOrientationAverage_S16(objectToSample, weighted);
		} else if (derivType == GrayS32.class) {
			ret = (OrientationAverage<T>)new ImplOrientationAverage_S32(objectToSample, weighted);
		} else {
			throw new IllegalArgumentException("Unknown image type.");
		}

		ret.setSampleRadius(radius);

		return ret;
	}

	public static <T extends ImageGray<T>>
	OrientationSlidingWindow<T> sliding( double objectRadiusToScale, int numAngles, double windowSize,
										 int radius, boolean weighted, Class<T> derivType ) {
		OrientationSlidingWindow<T> ret;

		if (derivType == GrayF32.class) {
			ret = (OrientationSlidingWindow<T>)new ImplOrientationSlidingWindow_F32(objectRadiusToScale, numAngles, windowSize, weighted);
		} else if (derivType == GrayS16.class) {
			ret = (OrientationSlidingWindow<T>)new ImplOrientationSlidingWindow_S16(objectRadiusToScale, numAngles, windowSize, weighted);
		} else if (derivType == GrayS32.class) {
			ret = (OrientationSlidingWindow<T>)new ImplOrientationSlidingWindow_S32(objectRadiusToScale, numAngles, windowSize, weighted);
		} else {
			throw new IllegalArgumentException("Unknown image type.");
		}

		ret.setObjectRadius(radius);

		return ret;
	}

	/**
	 * Orientation using "average" approach on integral images.
	 *
	 * @param config Configuration for algorithm.
	 * @param integralType Type of image being processed.
	 * @return OrientationIntegral
	 * @see ImplOrientationAverageGradientIntegral
	 */
	public static <II extends ImageGray<II>>
	OrientationIntegral<II> average_ii( @Nullable ConfigAverageIntegral config, Class<II> integralType ) {
		if (config == null)
			config = new ConfigAverageIntegral();

		return (OrientationIntegral<II>)
				new ImplOrientationAverageGradientIntegral(config.objectRadiusToScale,
						config.radius, config.samplePeriod, config.sampleWidth,
						config.weightSigma, integralType);
	}

	/**
	 * Estimates the orientation without calculating the image derivative.
	 *
	 * @param sampleRadius Radius of the region being considered in terms of samples. Typically 6.
	 * @param samplePeriod How often the image is sampled. This number is scaled. Typically 1.
	 * @param sampleWidth How wide of a kernel should be used to sample. Try 4
	 * @param weightSigma Sigma for weighting. zero for unweighted.
	 * @param integralImage Type of image being processed.
	 * @return OrientationIntegral
	 * @see ImplOrientationImageAverageIntegral
	 */
	public static <II extends ImageGray<II>>
	OrientationIntegral<II> image_ii( double objectRadiusToScale,
									  int sampleRadius, double samplePeriod, int sampleWidth,
									  double weightSigma, Class<II> integralImage ) {
		return (OrientationIntegral<II>)
				new ImplOrientationImageAverageIntegral(objectRadiusToScale,
						sampleRadius, samplePeriod, sampleWidth, weightSigma, integralImage);
	}

	/**
	 * Estimates the orientation of a region by using a sliding window across the different potential
	 * angles.
	 *
	 * @param config Configuration for algorithm. If null defaults will be used.
	 * @param integralType Type of integral image being processed.
	 * @return OrientationIntegral
	 * @see OrientationSlidingWindow
	 */
	public static <II extends ImageGray<II>>
	OrientationIntegral<II> sliding_ii( @Nullable ConfigSlidingIntegral config, Class<II> integralType ) {
		if (config == null)
			config = new ConfigSlidingIntegral();
		config.checkValidity();

		return (OrientationIntegral<II>)
				new ImplOrientationSlidingWindowIntegral(config.objectRadiusToScale, config.samplePeriod,
						config.windowSize, config.radius, config.weightSigma, config.sampleWidth, integralType);
	}

	/**
	 * Estimates multiple orientations as specified in SIFT paper.
	 *
	 * @param config Configuration for algorithm. If null defaults will be used.
	 * @param derivType Type of derivative image it takes as input
	 * @return OrientationHistogramSift
	 */
	public static <D extends ImageGray<D>>
	OrientationHistogramSift<D> sift( @Nullable ConfigSiftOrientation config, Class<D> derivType ) {
		if (config == null)
			config = new ConfigSiftOrientation();
		config.checkValidity();

		return new OrientationHistogramSift(config.histogramSize, config.sigmaEnlarge, derivType);
	}
}
