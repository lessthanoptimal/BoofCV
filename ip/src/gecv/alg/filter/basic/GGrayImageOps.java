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

package gecv.alg.filter.basic;

import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageUInt8;


/**
 * Weakly typed version of {@link GrayImageOps}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class GGrayImageOps {

	public static <T extends ImageBase> T stretch(T input, double gamma, double beta, double max , T output) {
		if( input instanceof ImageFloat32 ) {
			return (T)GrayImageOps.stretch((ImageFloat32)input,gamma,(float)beta,(float)max,(ImageFloat32)output);
		} else if( input instanceof ImageUInt8) {
			return (T)GrayImageOps.stretch((ImageUInt8)input,gamma,(int)beta,(int)max,(ImageUInt8)output);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getSimpleName());
		}
	}
}
