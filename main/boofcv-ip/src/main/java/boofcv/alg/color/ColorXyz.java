/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
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

package boofcv.alg.color;

import boofcv.alg.color.impl.ImplColorXyz;
import boofcv.alg.color.impl.ImplColorXyz_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;

/**
 * <p>Color conversion between CIE XYZ and RGB color models.</p>
 * <p>The XYZ color space is an international standard
 * developed by the CIE (Commission Internationale de l’Eclairage). This model is based on three hypothetical
 * primaries, XYZ, and all visible colors can be represented by using only positive values of X, Y, and Z.
 * The CIE XYZ primaries are hypothetical because they do not correspond to any real light wavelengths.
 * The Y primary is intentionally defined to match closely to luminance, while X and Z primaries give
 * color information. The main advantage of the CIE XYZ space (and any color space based on it) is
 * that this space is completely device-independent.<br>
 * </p>
 *
 * <p>The above text is and equations below are copied from [1], which cites [2] as their source.
 *
 * <p>
 * [1] <a href="http://software.intel.com/sites/products/documentation/hpc/ipp/ippi/ippi_ch6/ch6_color_models.html">
 *     Intel IPP Color Models</a><br>
 * [2] David Rogers. Procedural Elements for Computer Graphics. McGraw-Hill, 1985.
 * </p>
 *
 * @author Peter Abeles
 */
public class ColorXyz {


	/**
	 * Conversion from 8-bit RGB into XYZ.  8-bit = range of 0 to 255.
	 */
	public static void rgbToXyz( int r , int g , int b , double xyz[] ) {
		srgbToXyz(r/255.0,g/255.0,b/255.0,xyz);
	}

	/**
	 * Conversion from 8-bit RGB into XYZ.  8-bit = range of 0 to 255.
	 */
	public static void rgbToXyz( int r , int g , int b , float xyz[] ) {
		srgbToXyz(r/255.0f,g/255.0f,b/255.0f,xyz);
	}

	/**
	 * Conversion from normalized RGB into XYZ.  Normalized RGB values have a range of 0:1
	 */
	public static void srgbToXyz( double r , double g , double b , double xyz[] ) {
		xyz[0] = 0.412453*r + 0.35758*g + 0.180423*b;
		xyz[1] = 0.212671*r + 0.71516*g + 0.072169*b;
		xyz[2] = 0.019334*r + 0.119193*g + 0.950227*b;
	}

	/**
	 * Conversion from normalized RGB into XYZ.  Normalized RGB values have a range of 0:1
	 */
	public static void srgbToXyz( float r , float g , float b , float xyz[] ) {
		xyz[0] = 0.412453f*r + 0.35758f*g + 0.180423f*b;
		xyz[1] = 0.212671f*r + 0.71516f*g + 0.072169f*b;
		xyz[2] = 0.019334f*r + 0.119193f*g + 0.950227f*b;
	}

	/**
	 * Conversion from normalized RGB into XYZ.  Normalized RGB values have a range of 0:1
	 */
	public static void xyzToSrgb( float x , float y , float z , float srgb[] ) {
		srgb[0] = 3.240479f*x - 1.53715f*y - 0.498535f*z;
		srgb[1] = -0.969256f*x + 1.875991f*y + 0.041556f*z;
		srgb[2] = 0.055648f*x - 0.204043f*y + 1.057311f*z;
	}

	/**
	 * Conversion from normalized RGB into XYZ.  Normalized RGB values have a range of 0:1
	 */
	public static void xyzToSrgb( double x , double y , double z , double srgb[] ) {
		srgb[0] = 3.240479*x - 1.53715*y - 0.498535*z;
		srgb[1] = -0.969256*x + 1.875991*y + 0.041556*z;
		srgb[2] = 0.055648*x - 0.204043*y + 1.057311*z;
	}

	/**
	 * Convert a 3-channel {@link Planar} image from RGB into XYZ.  RGB is assumed
	 * to have a range from 0:255
	 *
	 * NOTE: Input and output image can be the same instance.
	 *
	 * @param rgb (Input) RGB encoded image
	 * @param xyz (Output) XYZ encoded image
	 */
	public static  <T extends ImageGray<T>>
	void rgbToXyz(Planar<T> rgb , Planar<GrayF32> xyz ) {
		xyz.reshape(rgb.width,rgb.height,3);

		if( rgb.getBandType() == GrayU8.class ) {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplColorXyz_MT.rgbToXyz_U8((Planar<GrayU8>) rgb, xyz);
			} else {
				ImplColorXyz.rgbToXyz_U8((Planar<GrayU8>) rgb, xyz);
			}
		} else if( rgb.getBandType() == GrayF32.class ) {
			if(BoofConcurrency.USE_CONCURRENT ) {
				ImplColorXyz_MT.rgbToXyz_F32((Planar<GrayF32>)rgb,xyz);
			} else {
				ImplColorXyz.rgbToXyz_F32((Planar<GrayF32>)rgb,xyz);
			}
		} else {
			throw new IllegalArgumentException("Unsupported band type "+rgb.getBandType().getSimpleName());
		}
	}
}
