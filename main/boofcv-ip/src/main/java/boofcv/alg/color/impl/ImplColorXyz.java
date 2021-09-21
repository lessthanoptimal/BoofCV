/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.color.impl;

import boofcv.alg.color.ColorXyz;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Low level implementation of function for converting XYZ images.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class ImplColorXyz {
	public static void rgbToXyz_F32( Planar<GrayF32> rgb, Planar<GrayF32> xyz ) {
		xyz.reshape(rgb);

		GrayF32 R = rgb.getBand(0);
		GrayF32 G = rgb.getBand(1);
		GrayF32 B = rgb.getBand(2);

		GrayF32 X = xyz.getBand(0);
		GrayF32 Y = xyz.getBand(1);
		GrayF32 Z = xyz.getBand(2);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,rgb.height,row->{
		for (int row = 0; row < xyz.height; row++) {
			int indexXyz = xyz.startIndex + row*xyz.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;

			for (int col = 0; col < xyz.width; col++, indexXyz++, indexRgb++) {
				float r = (float)ColorXyz.invGamma(R.data[indexRgb]/255f);
				float g = (float)ColorXyz.invGamma(G.data[indexRgb]/255f);
				float b = (float)ColorXyz.invGamma(B.data[indexRgb]/255f);

				X.data[indexXyz] = 0.412453f*r + 0.35758f*g + 0.180423f*b;
				Y.data[indexXyz] = 0.212671f*r + 0.71516f*g + 0.072169f*b;
				Z.data[indexXyz] = 0.019334f*r + 0.119193f*g + 0.950227f*b;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rgbToXyz_U8( Planar<GrayU8> rgb, Planar<GrayF32> xyz ) {
		xyz.reshape(rgb);

		GrayU8 R = rgb.getBand(0);
		GrayU8 G = rgb.getBand(1);
		GrayU8 B = rgb.getBand(2);

		GrayF32 X = xyz.getBand(0);
		GrayF32 Y = xyz.getBand(1);
		GrayF32 Z = xyz.getBand(2);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,rgb.height,row->{
		for (int row = 0; row < xyz.height; row++) {
			int indexXyz = xyz.startIndex + row*xyz.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;

			for (int col = 0; col < xyz.width; col++, indexXyz++, indexRgb++) {
				float r = ColorXyz.table_invgamma_f[R.data[indexRgb] & 0xFF];
				float g = ColorXyz.table_invgamma_f[G.data[indexRgb] & 0xFF];
				float b = ColorXyz.table_invgamma_f[B.data[indexRgb] & 0xFF];

				X.data[indexXyz] = 0.412453f*r + 0.35758f*g + 0.180423f*b;
				Y.data[indexXyz] = 0.212671f*r + 0.71516f*g + 0.072169f*b;
				Z.data[indexXyz] = 0.019334f*r + 0.119193f*g + 0.950227f*b;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void xyzToRgb_F32( Planar<GrayF32> xyz, Planar<GrayF32> rgb ) {
		rgb.reshape(xyz);

		GrayF32 X = xyz.getBand(0);
		GrayF32 Y = xyz.getBand(1);
		GrayF32 Z = xyz.getBand(2);

		GrayF32 R = rgb.getBand(0);
		GrayF32 G = rgb.getBand(1);
		GrayF32 B = rgb.getBand(2);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,xyz.height,row->{
		for (int row = 0; row < xyz.height; row++) {
			int indexXyz = xyz.startIndex + row*xyz.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;

			for (int col = 0; col < xyz.width; col++, indexXyz++, indexRgb++) {
				float x = X.data[indexXyz];
				float y = Y.data[indexXyz];
				float z = Z.data[indexXyz];

				R.data[indexRgb] = (float)(255.0*ColorXyz.gamma(3.240479f*x - 1.53715f*y - 0.498535f*z));
				G.data[indexRgb] = (float)(255.0*ColorXyz.gamma(-0.969256f*x + 1.875991f*y + 0.041556f*z));
				B.data[indexRgb] = (float)(255.0*ColorXyz.gamma(0.055648f*x - 0.204043f*y + 1.057311f*z));
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void xyzToRgb_U8( Planar<GrayF32> xyz, Planar<GrayU8> rgb ) {
		rgb.reshape(xyz);

		GrayF32 X = xyz.getBand(0);
		GrayF32 Y = xyz.getBand(1);
		GrayF32 Z = xyz.getBand(2);

		GrayU8 R = rgb.getBand(0);
		GrayU8 G = rgb.getBand(1);
		GrayU8 B = rgb.getBand(2);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,xyz.height,row->{
		for (int row = 0; row < xyz.height; row++) {
			int indexXyz = xyz.startIndex + row*xyz.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;

			for (int col = 0; col < xyz.width; col++, indexXyz++, indexRgb++) {
				float x = X.data[indexXyz];
				float y = Y.data[indexXyz];
				float z = Z.data[indexXyz];

				R.data[indexRgb] = (byte)(255.0*ColorXyz.gamma(3.240479f*x - 1.53715f*y - 0.498535f*z) + 0.5f);
				G.data[indexRgb] = (byte)(255.0*ColorXyz.gamma(-0.969256f*x + 1.875991f*y + 0.041556f*z) + 0.5f);
				B.data[indexRgb] = (byte)(255.0*ColorXyz.gamma(0.055648f*x - 0.204043f*y + 1.057311f*z) + 0.5f);
			}
		}
		//CONCURRENT_ABOVE });
	}
}
