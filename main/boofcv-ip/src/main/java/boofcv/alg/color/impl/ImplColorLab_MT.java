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

package boofcv.alg.color.impl;

import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;

import static boofcv.alg.color.ColorLab.*;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class ImplColorLab_MT {
	public static void rgbToLab_U8(Planar<GrayU8> rgb , Planar<GrayF32> lab ) {
		GrayU8 R = rgb.getBand(0);
		GrayU8 G = rgb.getBand(1);
		GrayU8 B = rgb.getBand(2);

		GrayF32 L_ = lab.getBand(0);
		GrayF32 A_ = lab.getBand(1);
		GrayF32 B_ = lab.getBand(2);

		BoofConcurrency.loopFor(0,lab.height,row->{
			int indexLab = lab.startIndex + row*lab.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;

			for( int col = 0; col < lab.width; col++ , indexLab++ , indexRgb++) {
				float r = (R.data[indexRgb]&0xFF)/255f;
				float g = (G.data[indexRgb]&0xFF)/255f;
				float b = (B.data[indexRgb]&0xFF)/255f;

				float X = 0.412453f*r + 0.35758f*g + 0.180423f*b;
				float Y = 0.212671f*r + 0.71516f*g + 0.072169f*b;
				float Z = 0.019334f*r + 0.119193f*g + 0.950227f*b;

				float xr = X/Xr_f;
				float yr = Y/Yr_f;
				float zr = Z/Zr_f;

				float fx, fy, fz;
				if(xr > epsilon_f)	fx = (float)Math.pow(xr, 1.0f/3.0f);
				else				fx = (kappa_f*xr + 16.0f)/116.0f;
				if(yr > epsilon_f)	fy = (float)Math.pow(yr, 1.0/3.0f);
				else				fy = (kappa_f*yr + 16.0f)/116.0f;
				if(zr > epsilon_f)	fz = (float)Math.pow(zr, 1.0/3.0f);
				else				fz = (kappa_f*zr + 16.0f)/116.0f;

				L_.data[indexLab] = 116.0f*fy-16.0f;
				A_.data[indexLab] = 500.0f*(fx-fy);
				B_.data[indexLab] = 200.0f*(fy-fz);
			}
		});
	}

	public static void rgbToLab_F32(Planar<GrayF32> rgb , Planar<GrayF32> lab ) {
		GrayF32 R = rgb.getBand(0);
		GrayF32 G = rgb.getBand(1);
		GrayF32 B = rgb.getBand(2);

		GrayF32 L_ = lab.getBand(0);
		GrayF32 A_ = lab.getBand(1);
		GrayF32 B_ = lab.getBand(2);

		BoofConcurrency.loopFor(0,lab.height,row->{
			int indexLab = lab.startIndex + row*lab.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;

			for( int col = 0; col < lab.width; col++ , indexLab++ , indexRgb++) {
				float r = R.data[indexRgb]/255f;
				float g = G.data[indexRgb]/255f;
				float b = B.data[indexRgb]/255f;

				float X = 0.412453f*r + 0.35758f*g + 0.180423f*b;
				float Y = 0.212671f*r + 0.71516f*g + 0.072169f*b;
				float Z = 0.019334f*r + 0.119193f*g + 0.950227f*b;

				float xr = X/Xr_f;
				float yr = Y/Yr_f;
				float zr = Z/Zr_f;

				float fx, fy, fz;
				if(xr > epsilon_f)	fx = (float)Math.pow(xr, 1.0f/3.0f);
				else				fx = (kappa_f*xr + 16.0f)/116.0f;
				if(yr > epsilon_f)	fy = (float)Math.pow(yr, 1.0/3.0f);
				else				fy = (kappa_f*yr + 16.0f)/116.0f;
				if(zr > epsilon_f)	fz = (float)Math.pow(zr, 1.0/3.0f);
				else				fz = (kappa_f*zr + 16.0f)/116.0f;

				L_.data[indexLab] = 116.0f*fy-16.0f;
				A_.data[indexLab] = 500.0f*(fx-fy);
				B_.data[indexLab] = 200.0f*(fy-fz);
			}
		});
	}
}
