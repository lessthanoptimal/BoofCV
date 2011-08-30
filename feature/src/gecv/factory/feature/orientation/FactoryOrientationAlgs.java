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

package gecv.factory.feature.orientation;

import gecv.alg.feature.orientation.*;
import gecv.alg.feature.orientation.impl.*;
import gecv.struct.image.*;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryOrientationAlgs {

	public static <T extends ImageBase>
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

	public static <T extends ImageBase>
	OrientationNoGradient<T> nogradient( int radius , Class<T> imageType )
	{
		OrientationNoGradient<T> ret;

		if( imageType == ImageFloat32.class ) {
			ret = (OrientationNoGradient<T>)new ImplOrientationNoGradient_F32(radius);
		} else if( imageType == ImageUInt8.class ) {
			ret = (OrientationNoGradient<T>)new ImplOrientationNoGradient_U8(radius);
		} else {
			throw new IllegalArgumentException("Unknown image type.");
		}

		ret.setRadius(radius);

		return ret;
	}

	public static <T extends ImageBase>
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

	public static <T extends ImageBase>
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

	public static <T extends ImageBase>
	OrientationIntegral<T> average_ii( int radius , boolean weighted , Class<T> imageType)
	{
		if( imageType == ImageFloat32.class )
			return (OrientationIntegral<T>)new ImplOrientationAverageIntegral_F32(radius,weighted);
		else if( imageType == ImageSInt32.class )
			return (OrientationIntegral<T>)new ImplOrientationAverageIntegral_I32(radius,weighted);
		else
			throw new IllegalArgumentException("Image type not supported. "+imageType.getSimpleName());
	}
}
