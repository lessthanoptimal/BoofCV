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

import boofcv.alg.color.impl.ImplColorLab;
import boofcv.alg.color.impl.ImplColorLab_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;

/**
 * <p>Conversion between RGB and CIE LAB color space.  LAB color is designed to approximate human vision.
 * L for lightness and a and b for the color-opponent dimensions,  The reference white used in
 * the conversions below is D65.</p>
 *
 * <p>
 * Note:
 * <ul>
 * <li>L has a range of 0 to 100</li>
 * <li>L = 0 is black and L* = 100 is diffuse white</li>
 * </ul>
 * </p>
 *
 * @author Peter Abeles
 */
public class ColorLab {

	// 64 bit
	public static final double epsilon = 0.008856;	//actual CIE standard
	public static final double kappa   = 903.3;		//actual CIE standard

	public static final double Xr = 0.950456;	//reference white
	public static final double Yr = 1.0;		//reference white
	public static final double Zr = 1.088754;	//reference white

	// 32 bit
	public static final float epsilon_f = 0.008856f;	//actual CIE standard
	public static final float kappa_f   = 903.3f;		//actual CIE standard

	public static final float Xr_f = 0.950456f;	//reference white
	public static final float Yr_f = 1.0f;		//reference white
	public static final float Zr_f = 1.088754f;	//reference white

	/**
	 * Conversion from normalized RGB into LAB.  Normalized RGB values have a range of 0:1
	 */
	public static void srgbToLab( double r , double g , double b , double lab[] ) {
		ColorXyz.srgbToXyz(r,g,b,lab);

		double X = lab[0];
		double Y = lab[1];
		double Z = lab[2];


		double xr = X/Xr;
		double yr = Y/Yr;
		double zr = Z/Zr;

		double fx, fy, fz;
		if(xr > epsilon)	fx = Math.pow(xr, 1.0 / 3.0);
		else				fx = (kappa*xr + 16.0)/116.0;
		if(yr > epsilon)	fy = Math.pow(yr, 1.0 / 3.0);
		else				fy = (kappa*yr + 16.0)/116.0;
		if(zr > epsilon)	fz = Math.pow(zr, 1.0 / 3.0);
		else				fz = (kappa*zr + 16.0)/116.0;

		lab[0] = 116.0*fy-16.0;
		lab[1] = 500.0*(fx-fy);
		lab[2] = 200.0*(fy-fz);
	}

	/**
	 * Conversion from normalized RGB into LAB.  Normalized RGB values have a range of 0:1
	 */
	public static void srgbToLab( float r , float g , float b , float lab[] ) {
		ColorXyz.srgbToXyz(r,g,b,lab);

		float X = lab[0];
		float Y = lab[1];
		float Z = lab[2];

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

		lab[0] = 116.0f*fy-16.0f;
		lab[1] = 500.0f*(fx-fy);
		lab[2] = 200.0f*(fy-fz);
	}

	/**
	 * Convert a 3-channel {@link Planar} image from RGB into LAB.  RGB is assumed
	 * to have a range from 0:255
	 *
	 * NOTE: Input and output image can be the same instance.
	 *
	 * @param rgb (Input) RGB encoded image
	 * @param lab (Output) LAB encoded image. L = channel 0, A = channel 1, B = channel 2
	 */
	public static <T extends ImageGray<T>>
	void rgbToLab(Planar<T> rgb , Planar<GrayF32> lab ) {
		lab.reshape(rgb.width,rgb.height,3);

		if( rgb.getBandType() == GrayU8.class ) {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplColorLab_MT.rgbToLab_U8((Planar<GrayU8>) rgb, lab);
			} else {
				ImplColorLab.rgbToLab_U8((Planar<GrayU8>) rgb, lab);
			}
		} else if( rgb.getBandType() == GrayF32.class ) {
			if(BoofConcurrency.USE_CONCURRENT ) {
				ImplColorLab_MT.rgbToLab_F32((Planar<GrayF32>)rgb,lab);
			} else {
				ImplColorLab.rgbToLab_F32((Planar<GrayF32>)rgb,lab);
			}
		} else {
			throw new IllegalArgumentException("Unsupported band type "+rgb.getBandType().getSimpleName());
		}
	}
}
