/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryOrientationAlgs {

	public static <T extends ImageSingleBand>
	OrientationHistogram<T> histogram( int numAngles , int radius , boolean weighted ,
									   Class<T> derivType )
	{
		OrientationHistogram<T> ret;

		if( derivType == ImageFloat32.class ) {
			ret = (OrientationHistogram<T>)new ImplOrientationHistogram_F32(numAngles,weighted);
		} else if( derivType == ImageSInt16.class ) {
			ret = (OrientationHistogram<T>)new ImplOrientationHistogram_S16(numAngles,weighted);
		} else if( derivType == ImageSInt32.class ) {
			ret = (OrientationHistogram<T>)new ImplOrientationHistogram_S32(numAngles,weighted);
		} else {
			throw new IllegalArgumentException("Unknown image type.");
		}

		ret.setRadius(radius);

		return ret;
	}

	public static <T extends ImageSingleBand>
	OrientationImageAverage<T> nogradient( int radius , Class<T> imageType )
	{
		OrientationImageAverage<T> ret;

		if( imageType == ImageFloat32.class ) {
			ret = (OrientationImageAverage<T>)new ImplOrientationImageAverage_F32(radius);
		} else if( imageType == ImageUInt8.class ) {
			ret = (OrientationImageAverage<T>)new ImplOrientationImageAverage_U8(radius);
		} else {
			throw new IllegalArgumentException("Unknown image type.");
		}

		ret.setRadius(radius);

		return ret;
	}

	public static <T extends ImageSingleBand>
	OrientationAverage<T> average( int radius , boolean weighted , Class<T> derivType )
	{
		OrientationAverage<T> ret;

		if( derivType == ImageFloat32.class ) {
			ret = (OrientationAverage<T>)new ImplOrientationAverage_F32(weighted);
		} else if( derivType == ImageSInt16.class ) {
			ret = (OrientationAverage<T>)new ImplOrientationAverage_S16(weighted);
		} else if( derivType == ImageSInt32.class ) {
			ret = (OrientationAverage<T>)new ImplOrientationAverage_S32(weighted);
		} else {
			throw new IllegalArgumentException("Unknown image type.");
		}

		ret.setRadius(radius);

		return ret;
	}

	public static <T extends ImageSingleBand>
	OrientationSlidingWindow<T> sliding( int numAngles, double windowSize , 
										 int radius , boolean weighted , Class<T> derivType )
	{
		OrientationSlidingWindow<T> ret;

		if( derivType == ImageFloat32.class ) {
			ret = (OrientationSlidingWindow<T>)new ImplOrientationSlidingWindow_F32(numAngles,windowSize,weighted);
		} else if( derivType == ImageSInt16.class ) {
			ret = (OrientationSlidingWindow<T>)new ImplOrientationSlidingWindow_S16(numAngles,windowSize,weighted);
		} else if( derivType == ImageSInt32.class ) {
			ret = (OrientationSlidingWindow<T>)new ImplOrientationSlidingWindow_S32(numAngles,windowSize,weighted);
		} else {
			throw new IllegalArgumentException("Unknown image type.");
		}

		ret.setRadius(radius);

		return ret;
	}

	/**
	 *
	 * @see ImplOrientationAverageGradientIntegral
	 *
	 * @param config Configuration for algorithm.
	 * @param integralType Type of image being processed.
	 * @return OrientationIntegral
	 */
	public static <II extends ImageSingleBand>
	OrientationIntegral<II> average_ii( ConfigAverageIntegral config , Class<II> integralType)
	{
		if( config == null )
			config = new ConfigAverageIntegral();

		return (OrientationIntegral<II>)
				new ImplOrientationAverageGradientIntegral(config.radius,config.samplePeriod,config.sampleWidth,
						config.weightSigma ,integralType);
	}

	/**
	 * Estimates the orientation without calculating the image derivative.
	 *
	 * @see ImplOrientationImageAverageIntegral
	 *
	 * @param radius Radius of the region being considered in terms of samples. Typically 6.
	 * @param samplePeriod How often the image is sampled.  This number is scaled.  Typically 1.
	 * @param sampleWidth How wide of a kernel should be used to sample. Try 4
	 * @param weightSigma Sigma for weighting.  zero for unweighted.
	 * @param integralImage Type of image being processed.
	 * @return OrientationIntegral
	 */
	public static <II extends ImageSingleBand>
	OrientationIntegral<II> image_ii( int radius , double samplePeriod , int sampleWidth,
									 double weightSigma , Class<II> integralImage)
	{
		return (OrientationIntegral<II>)
				new ImplOrientationImageAverageIntegral(radius,samplePeriod,sampleWidth,weightSigma
						,integralImage);
	}

	/**
	 * Estimates the orientation of a region by using a sliding window across the different potential
	 * angles.
	 *
	 * @see OrientationSlidingWindow
	 *
	 * @param config Configuration for algorithm.  If null defaults will be used.
	 * @param integralType Type of integral image being processed.
	 * @return OrientationIntegral
	 */
	public static <II extends ImageSingleBand>
	OrientationIntegral<II> sliding_ii( ConfigSlidingIntegral config , Class<II> integralType)
	{
		if( config == null )
			config = new ConfigSlidingIntegral();
		config.checkValidity();

		return (OrientationIntegral<II>)
				new ImplOrientationSlidingWindowIntegral(config.samplePeriod,
						config.windowSize,config.radius,config.weightSigma, config.sampleWidth,integralType);
	}

	/**
	 * Estimates multiple orientations as specified in SIFT paper.
	 *
	 * @param config Configuration for algorithm.  If null defaults will be used.
	 * @return OrientationHistogramSift
	 */
	public static OrientationHistogramSift sift( ConfigSiftOrientation config ) {
		if( config == null )
			config = new ConfigSiftOrientation();
		config.checkValidity();

		return new OrientationHistogramSift(config.histogramSize,config.sigmaToRadius,config.sigmaEnlarge);
	}
}
