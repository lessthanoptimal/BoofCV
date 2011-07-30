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

package gecv.abst.filter.distort;

import gecv.alg.distort.DistortImageOps;
import gecv.alg.interpolate.InterpolatePixel;
import gecv.struct.image.*;


/**
 * <p> Generalized versions of functions inside of {@link DistortImageOps} which determine the image type
 * at runtime. </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class GeneralizedDistortImageOps {

	public static <T extends ImageBase> void scale( T input , T output , InterpolatePixel<T> interpolation )
	{
		ImageTypeInfo info = input.getTypeInfo();

		if( info.isInteger() ) {
			if( info.getNumBits() == 8 && !info.isSigned()) {
				DistortImageOps.scale((ImageUInt8)input,(ImageUInt8)output,(InterpolatePixel<ImageUInt8>)interpolation);
			} else if( info.getNumBits() == 16 && info.isSigned() ) {
				DistortImageOps.scale((ImageSInt16)input,(ImageSInt16)output,(InterpolatePixel<ImageSInt16>)interpolation);
			} else {
				throw new IllegalArgumentException("Image type not yet supported");
			}
		} else {
			if( info.getNumBits() == 32 ) {
				DistortImageOps.scale((ImageFloat32)input,(ImageFloat32)output,(InterpolatePixel<ImageFloat32>)interpolation);
			}else {
				throw new IllegalArgumentException("Image type not yet supported");
			}
		}
	}
}
