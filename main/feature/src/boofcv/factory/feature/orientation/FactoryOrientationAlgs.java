/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.orientation;

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

	public static <T extends ImageSingleBand>
	OrientationIntegral<T> average_ii( int radius , boolean weighted , Class<T> imageType)
	{
		if( imageType == ImageFloat32.class )
			return (OrientationIntegral<T>)new ImplOrientationAverageIntegral_F32(radius,weighted);
		else if( imageType == ImageSInt32.class )
			return (OrientationIntegral<T>)new ImplOrientationAverageIntegral_I32(radius,weighted);
		else
			throw new IllegalArgumentException("Image type not supported. "+imageType.getSimpleName());
	}

	public static <T extends ImageSingleBand>
	OrientationIntegral<T> sliding_ii( int numAngles, double windowSize ,
									   int radius , boolean weighted , Class<T> imageType)
	{
		if( imageType == ImageFloat32.class )
			return (OrientationIntegral<T>)new ImplOrientationSlidingWindowIntegral_F32(numAngles,windowSize,radius,weighted, 4);
		else if( imageType == ImageSInt32.class )
			return (OrientationIntegral<T>)new ImplOrientationSlidingWindowIntegral_I32(numAngles,windowSize,radius,weighted, 4);
		else
			throw new IllegalArgumentException("Image type not supported. "+imageType.getSimpleName());
	}
}
