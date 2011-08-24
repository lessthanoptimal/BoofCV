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

package gecv.alg.filter.blur;

import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageUInt8;


/**
 * @author Peter Abeles
 */
public class GBlurImageOps {

	public static <T extends ImageBase>
	T gaussian(T input, T output, double sigma , int radius, T storage ) {
		if( input instanceof ImageUInt8 ) {
			return (T)BlurImageOps.gaussian((ImageUInt8)input,(ImageUInt8)output,sigma,radius,(ImageUInt8)storage);
		} else if( input instanceof ImageFloat32) {
			return (T)BlurImageOps.gaussian((ImageFloat32)input,(ImageFloat32)output,sigma,radius,(ImageFloat32)storage);
		} else  {
			throw new IllegalArgumentException("Unsupported image type");
		}
	}
}
