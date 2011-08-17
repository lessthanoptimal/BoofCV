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

package gecv.alg.describe;

import gecv.alg.describe.impl.*;
import gecv.alg.filter.kernel.FactoryKernelGaussian;
import gecv.alg.filter.kernel.KernelMath;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageSInt32;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryRegionOrientationAlgs {

	public static <T extends ImageBase>
	OrientationHistogram<T> histogram( int numAngles , int radius , boolean weighted ,
									   Class<T> imageType )
	{
		OrientationHistogram<T> ret;

		if( imageType == ImageFloat32.class ) {
			ret = (OrientationHistogram<T>)new ImplOrientationHistogram_F32(numAngles);
		} else if( imageType == ImageSInt16.class ) {
			ret = (OrientationHistogram<T>)new ImplOrientationHistogram_S16(numAngles);
		} else if( imageType == ImageSInt32.class ) {
			ret = (OrientationHistogram<T>)new ImplOrientationHistogram_S32(numAngles);
		} else {
			throw new IllegalArgumentException("Unknown image type.");
		}

		ret.setRadius(radius);
		if( weighted ) {
			Kernel2D_F32 weight = FactoryKernelGaussian.gaussian(2,true,-1,radius);
			KernelMath.normalizeSumToOne(weight);
			ret.setWeights(weight);
		}

		return ret;
	}

	public static <T extends ImageBase>
	OrientationAverage<T> average( int radius , boolean weighted , Class<T> imageType )
	{
		OrientationAverage<T> ret;

		if( imageType == ImageFloat32.class ) {
			ret = (OrientationAverage<T>)new ImplOrientationAverage_F32();
		} else if( imageType == ImageSInt16.class ) {
			ret = (OrientationAverage<T>)new ImplOrientationAverage_S16();
		} else if( imageType == ImageSInt32.class ) {
			ret = (OrientationAverage<T>)new ImplOrientationAverage_S32();
		} else {
			throw new IllegalArgumentException("Unknown image type.");
		}

		ret.setRadius(radius);
		if( weighted ) {
			Kernel2D_F32 weight = FactoryKernelGaussian.gaussian(2,true,-1,radius);
			KernelMath.normalizeSumToOne(weight);
			ret.setWeights(weight);
		}

		return ret;
	}
}
