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

package gecv.alg.filter.derivative;

import gecv.struct.image.*;


/**
 * @author Peter Abeles
 */
public class ImageDerivativeOps {

	/**
	 * Returns the type of image the derivative should be for the specified input type.
	 * @param imageType Input image type.
	 * @return Appropriate output image type.
	 */
	public static <I extends ImageBase, D extends ImageBase>
		Class<D> getDerivativeType( Class<I> imageType ) {
		if( imageType == ImageFloat32.class ) {
			return (Class<D>)ImageFloat32.class;
		} else if( imageType == ImageUInt8.class ) {
			return (Class<D>) ImageSInt16.class;
		} else if( imageType == ImageUInt16.class ) {
			return (Class<D>) ImageSInt32.class;
		} else {
			throw new IllegalArgumentException("Unknown input image type: "+imageType.getSimpleName());
		}
	}
}
