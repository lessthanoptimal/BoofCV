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

package gecv.alg.transform.gss;

import gecv.core.image.ImageGenerator;
import gecv.core.image.inst.SingleBandGenerator;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

/**
 * Factory which removes some of the drudgery from creating {@link gecv.struct.gss.GaussianScaleSpace}
 *
 * @author Peter Abeles
 */
public class FactoryGaussianScaleSpace {

	public static NoCacheScaleSpace<?,?> nocache( Class<?> imageType , int maxDeriv ) {
		if( imageType == ImageFloat32.class ) {
			return nocache_F32(maxDeriv);
		} else if( imageType == ImageUInt8.class ) {
			return nocache_U8(maxDeriv);
		} else {
			throw new IllegalArgumentException("Doesn't handle "+imageType.getSimpleName()+" yet.");
		}
	}

	public static NoCacheScaleSpace<ImageFloat32,ImageFloat32> nocache_F32( int maxDeriv ) {
		ImageGenerator<ImageFloat32> imageGen = new SingleBandGenerator<ImageFloat32>(ImageFloat32.class);
		return new NoCacheScaleSpace<ImageFloat32,ImageFloat32>(imageGen,imageGen,maxDeriv);
	}

	public static NoCacheScaleSpace<ImageUInt8, ImageSInt16> nocache_U8( int maxDeriv ) {
		ImageGenerator<ImageUInt8> imageGen = new SingleBandGenerator<ImageUInt8>(ImageUInt8.class);
		ImageGenerator<ImageSInt16> derivGen = new SingleBandGenerator<ImageSInt16>(ImageSInt16.class);
		return new NoCacheScaleSpace<ImageUInt8,ImageSInt16>(imageGen,derivGen,maxDeriv);
	}
}
