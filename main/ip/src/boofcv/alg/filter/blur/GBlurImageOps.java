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

package boofcv.alg.filter.blur;

import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;


/**
 * @author Peter Abeles
 */
public class GBlurImageOps {

	public static <T extends ImageSingleBand>
	T mean(T input, T output, int radius, T storage ) {
		if( input instanceof ImageUInt8 ) {
			return (T)BlurImageOps.mean((ImageUInt8)input,(ImageUInt8)output,radius,(ImageUInt8)storage);
		} else if( input instanceof ImageFloat32) {
			return (T)BlurImageOps.mean((ImageFloat32)input,(ImageFloat32)output,radius,(ImageFloat32)storage);
		} else  {
			throw new IllegalArgumentException("Unsupported image type");
		}
	}

	public static <T extends ImageSingleBand>
	T median(T input, T output, int radius ) {
		if( input instanceof ImageUInt8 ) {
			return (T)BlurImageOps.median((ImageUInt8)input,(ImageUInt8)output,radius);
		} else if( input instanceof ImageFloat32) {
			return (T)BlurImageOps.median((ImageFloat32)input,(ImageFloat32)output,radius);
		} else  {
			throw new IllegalArgumentException("Unsupported image type");
		}
	}

	public static <T extends ImageSingleBand>
	T gaussian(T input, T output, double sigma , int radius, T storage ) {
		if( input instanceof ImageUInt8 ) {
			return (T)BlurImageOps.gaussian((ImageUInt8)input,(ImageUInt8)output,sigma,radius,(ImageUInt8)storage);
		} else if( input instanceof ImageFloat32) {
			return (T)BlurImageOps.gaussian((ImageFloat32)input,(ImageFloat32)output,sigma,radius,(ImageFloat32)storage);
		} else  {
			throw new IllegalArgumentException("Unsupported image type: "+input.getClass().getSimpleName());
		}
	}
}
