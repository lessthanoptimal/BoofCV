/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import static boofcv.alg.color.ColorXyz.*;

/**
 * <p>Conversion between RGB and CIE LAB color space. LAB color is designed to approximate human vision.
 * L for lightness and a and b for the color-opponent dimensions, The reference white used in
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
 * @see ColorXyz
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
	 * Conversion from 8-bit RGB into CIE LAB. 8-bit = range of 0 to 255.
	 */
	public static void rgbToLab( int r , int g , int b , double []lab ) {
		linearRgbToLab(invGamma(r/255.0),invGamma(g/255.0),invGamma(b/255.0),lab);
	}

	/**
	 * Conversion from 8-bit RGB into CIE LAB. 8-bit = range of 0 to 255.
	 */
	public static void rgbToLab( int r , int g , int b , float []lab ) {
		linearRgbToLab((float)invGamma(r/255.0f),(float)invGamma(g/255.0f),(float)invGamma(b/255.0f),lab);
	}

	/**
	 * Conversion from 8-bit RGB into CIE LAB. 8-bit = range of 0 to 255.
	 */
	public static void rgbToLab( double r , double g , double b , float []lab ) {
		linearRgbToLab((float)invGamma(r/255.0f),(float)invGamma(g/255.0f),(float)invGamma(b/255.0f),lab);
	}

	/**
	 * Conversion of CEI LAB to 8-bit RGB. Converts to linearRgb and then applies gamma correction.
	 *
	 * @param linearRgb (output) Workspace to store intermediate linearRgb results
	 * @param rgb (output) 8-bit RGB color. 0 to 255
	 */
	public static void labToRgb( double L , double a , double b, double[]linearRgb , int[]rgb ) {
		labToLinearRgb(L,a,b,linearRgb);
		rgb[0] = (int)(255.0*gamma(linearRgb[0])+0.5) & 0xFF;
		rgb[1] = (int)(255.0*gamma(linearRgb[1])+0.5) & 0xFF;
		rgb[2] = (int)(255.0*gamma(linearRgb[2])+0.5) & 0xFF;
	}

	/**
	 * Conversion of CEI LAB to 8-bit RGB. Converts to Linear RGB and then applies gamma correction.
	 *
	 * @param linearRgb (output) Workspace to store intermediate linearRgb results
	 * @param rgb (output) 8-bit RGB color. 0 to 255
	 */
	public static void labToRgb( float L , float a , float b, float[]linearRgb , int[]rgb ) {
		labToLinearRgb(L,a,b,linearRgb);
		rgb[0] = (int)(255.0*gamma(linearRgb[0])+0.5) & 0xFF;
		rgb[1] = (int)(255.0*gamma(linearRgb[1])+0.5) & 0xFF;
		rgb[2] = (int)(255.0*gamma(linearRgb[2])+0.5) & 0xFF;
	}

	/**
	 * Conversion of CEI LAB to 8-bit RGB. Converts to Linear RGB and then applies gamma correction.
	 *
	 * @param linearRgb (output) Workspace to store intermediate Linear RGB results
	 * @param rgb (output) 8-bit RGB color. 0 to 255
	 */
	public static void labToRgb( float L , float a , float b, float[]linearRgb , float[]rgb ) {
		labToLinearRgb(L,a,b,linearRgb);
		rgb[0] = (float)(255.0*gamma(linearRgb[0]));
		rgb[1] = (float)(255.0*gamma(linearRgb[1]));
		rgb[2] = (float)(255.0*gamma(linearRgb[2]));
	}

	/**
	 * Conversion from CEI LAB to gamma corrected linear RGB. Linear RGB values have a range of 0:1
	 */
	public static void labToLinearRgb( float L , float a , float b , float []linearRgb ) {
		labToXyz(L,a,b,linearRgb);
		xyzToLinearRgb(linearRgb[0],linearRgb[1],linearRgb[2],linearRgb);
	}

	/**
	 * Conversion from CEI LAB to gamma corrected linear RGB. Linear RGB values have a range of 0:1
	 */
	public static void labToLinearRgb( double L , double a , double b , double []linearRgb ) {
		labToXyz(L,a,b,linearRgb);
		xyzToLinearRgb(linearRgb[0],linearRgb[1],linearRgb[2],linearRgb);
	}

	/**
	 * Convert CEI XYZ to CEI LAB color space
	 */
	public static void xyzToLab(double X, double Y, double Z, double[] lab ) {
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
	 * Convert CEI XYZ to CEI LAB color space
	 */
	public static void xyzToLab(float X, float Y, float Z, float[] lab ) {
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
	 * Convert CEI LAB to CEI XYZ color space
	 */
	public static void labToXyz(double L, double a, double b, double[] xyz ) {
		double left = (L+16.0)/116.0;
		xyz[0] = Xr_f*invTran(left+a/500.0);
		xyz[1] = Yr_f*invTran(left);
		xyz[2] = Zr_f*invTran(left-b/200.0);
	}

	/**
	 * Convert CEI LAB to CEI XYZ color space
	 */
	public static void labToXyz(float L, float a, float b, float[] xyz ) {
		float left = (L+16.0f)/116.0f;
		xyz[0] = Xr_f*invTran(left+a/500.0f);
		xyz[1] = Yr_f*invTran(left);
		xyz[2] = Zr_f*invTran(left-b/200.0f);
	}

	public static double invTran( double t ) {
		if( t > epsilon ) {
			return t*t*t;
		} else {
			return 3.0*t*t*(t-(4.0/29.0));
		}
	}

	public static float invTran( float t ) {
		if( t > epsilon_f ) {
			return t*t*t;
		} else {
			return 3.0f*t*t*(t-(4.0f/29.0f));
		}
	}

	/**
	 * Conversion from gamma corrected linear RGB into CEI LAB. Normalized RGB values have a range of 0:1
	 */
	public static void linearRgbToLab(double r , double g , double b , double[] lab) {
		ColorXyz.linearRgbToXyz(r,g,b,lab);
		xyzToLab(lab[0],lab[1],lab[2],lab);
	}

	/**
	 * Conversion from gamma corrected linear RGB into LAB. Normalized RGB values have a range of 0:1
	 */
	public static void linearRgbToLab(float r , float g , float b , float[] lab) {
		ColorXyz.linearRgbToXyz(r,g,b,lab);
		xyzToLab(lab[0],lab[1],lab[2],lab);
	}

	/**
	 * Convert a 3-channel {@link Planar} image from RGB into LAB. RGB is assumed
	 * to have a range from 0:255
	 *
	 * NOTE: Input and output image can be the same instance.
	 *
	 * @param rgb (Input) 8-bit RGB encoded image. R = channel 0, G = channel 1, B = channel 2
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

	/**
	 * Convert a 3-channel {@link Planar} image from LAB into RGB. RGB will have a range from 0 to 255.
	 *
	 * NOTE: Input and output image can be the same instance.
	 *
	 * @param lab (Input) LAB encoded image. L = channel 0, A = channel 1, B = channel 2
	 * @param rgb (Output) 8-bit RGB encoded image. R = channel 0, G = channel 1, B = channel 2
	 */
	public static <T extends ImageGray<T>>
	void labToRgb(Planar<GrayF32> lab , Planar<T> rgb ) {
		rgb.reshape(lab);

		if( rgb.getBandType() == GrayU8.class ) {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplColorLab_MT.labToRgb_U8(lab,(Planar<GrayU8>) rgb);
			} else {
				ImplColorLab.labToRgb_U8(lab,(Planar<GrayU8>) rgb);
			}
		} else if( rgb.getBandType() == GrayF32.class ) {
			if(BoofConcurrency.USE_CONCURRENT ) {
				ImplColorLab_MT.labToRgb_F32(lab,(Planar<GrayF32>)rgb);
			} else {
				ImplColorLab.labToRgb_F32(lab,(Planar<GrayF32>)rgb);
			}
		} else {
			throw new IllegalArgumentException("Unsupported band type "+rgb.getBandType().getSimpleName());
		}
	}
}
