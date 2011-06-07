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

package gecv.alg.misc;

import gecv.alg.interpolate.InterpolatePixel;
import gecv.alg.misc.impl.ImageDistort_F32;
import gecv.alg.misc.impl.ImageDistort_I16;
import gecv.alg.misc.impl.ImageDistort_I8;
import gecv.struct.distort.PixelDistort;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryImageDistort {

	public static <T extends ImageBase> ImageDistort<T>
	create( Class<T> imageType , PixelDistort dstToSrc, InterpolatePixel<T> interpolation ) {
		if( imageType == ImageFloat32.class ) {
			return (ImageDistort<T>)
					new ImageDistort_F32(dstToSrc,(InterpolatePixel<ImageFloat32>)interpolation);
		} else if( ImageInt8.class.isAssignableFrom(imageType) ) {
			return (ImageDistort<T>)
					new ImageDistort_I8(dstToSrc,(InterpolatePixel<ImageInt8>)interpolation);
		} else if( ImageInt16.class.isAssignableFrom(imageType) ) {
			return (ImageDistort<T>)
					new ImageDistort_I16(dstToSrc,(InterpolatePixel<ImageInt16>)interpolation);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
	}
}
