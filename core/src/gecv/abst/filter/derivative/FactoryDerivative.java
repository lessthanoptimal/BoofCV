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

package gecv.abst.filter.derivative;

import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

/**
 * Factory for creating different types of {@link DerivativeXY}, which are used to compute
 * the image's derivative.
 *
 * @author Peter Abeles
 */
public class FactoryDerivative {

	public static DerivativeXY<ImageFloat32,ImageFloat32> gaussian_F32( int radius ) {
		return new DerivativeXY_Gaussian_F32(radius);
	}

	public static DerivativeXY<ImageFloat32,ImageFloat32> sobel_F32() {
		return new DerivativeXY_Sobel_F32();
	}

	public static DerivativeXY<ImageFloat32,ImageFloat32> three_F32() {
		return new DerivativeXY_Three_F32();
	}

	public static DerivativeXY<ImageUInt8, ImageSInt16> sobel_I8() {
		return new DerivativeXY_Sobel_I8();
	}

	public static DerivativeXY<ImageUInt8, ImageSInt16> three_I8() {
		return new DerivativeXY_Three_I8();
	}
}
