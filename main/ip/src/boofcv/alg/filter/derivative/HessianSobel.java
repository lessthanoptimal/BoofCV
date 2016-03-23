/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.derivative;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.convolve.border.ConvolveJustBorder_General;
import boofcv.alg.filter.derivative.impl.HessianSobel_Shared;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;


/**
 * <p>
 * Computes the second derivative (Hessian) of an image using.  This hessian is derived using the {@link GradientSobel}
 * gradient function.
 * </p>
 *
 * <p>
 * WARNING: It is computationally more expensive to compute the Hessian with this operation than applying the Sobel
 * gradient operator multiple times.  However, this does not require the creation additional storage to save
 * intermediate results.
 * </p>
 *
 * <p>
 * Kernel for &part; <sup>2</sup>f/&part; y<sup>2</sup>:
 * <table border="1">
 * <tr> <td> 1 </td> <td> 4 </td> <td> 6 </td> <td> 4 </td> <td> 1 </td> </tr>
 * <tr> <td> 0 </td> <td> 0 </td> <td> 0 </td> <td> 0 </td> <td> 0 </td> </tr>
 * <tr> <td> -2 </td> <td> -8 </td> <td> -12 </td> <td> -8 </td> <td> -2 </td> </tr>
 * <tr> <td> 0 </td> <td> 0 </td> <td> 0 </td> <td> 0 </td> <td> 0 </td> </tr>
 * <tr> <td> 1 </td> <td> 4 </td> <td> 6 </td> <td> 4 </td> <td> 1 </td> </tr>
 * </table}
 * [1 0 -2 0 1] and &part;<sup>2</sup>f/&part; x&part;y is:<br>
 * <table border="1">
 * <tr> <td> 1 </td> <td> 2 </td> <td> 0 </td> <td> -2 </td> <td> -1 </td> </tr>
 * <tr> <td> 2 </td> <td> 4 </td> <td> 0 </td> <td> -4 </td> <td> -2 </td> </tr>
 * <tr> <td> 0 </td> <td> 0 </td> <td> 0 </td> <td>  0 </td> <td>  0 </td> </tr>
 * <tr> <td> -2 </td> <td> -4 </td> <td> 0 </td> <td> 4 </td> <td> 2 </td> </tr>
 * <tr> <td> -1 </td> <td> -2 </td> <td> 0 </td> <td> 2 </td> <td> 1 </td> </tr>
 * </table}
 * </p>
 *
 * @author Peter Abeles
 */
public class HessianSobel {

	public static Kernel2D_I32 kernelYY_I32 = new Kernel2D_I32(5, new int[]
			{1, 4, 6 , 4, 1,
			 0, 0, 0 , 0, 0,
			-2,-8,-12,-8,-2,
		     0, 0, 0 , 0, 0,
		     1, 4, 6 , 4, 1});
	public static Kernel2D_I32 kernelXX_I32 = new Kernel2D_I32(5, new int[]
			{1, 0,-2 , 0, 1,
			 4, 0,-8 , 0, 4,
		     6, 0,-12, 0, 6,
		     4, 0,-8 , 0, 4,
			 1, 0,-2 , 0, 1});
	public static Kernel2D_I32 kernelXY_I32 = new Kernel2D_I32(5, new int[]
			{1, 2,0,-2,-1,
			 2, 4,0,-4,-2,
		     0, 0,0, 0, 0,
		    -2,-4,0, 4, 2,
			-1,-2,0, 2, 1});
	public static Kernel2D_F32 kernelYY_F32 = new Kernel2D_F32(5, new float[]
			{1, 4, 6 , 4, 1,
			 0, 0, 0 , 0, 0,
			-2,-8,-12,-8,-2,
		     0, 0, 0 , 0, 0,
		     1, 4, 6 , 4, 1});
	public static Kernel2D_F32 kernelXX_F32 = new Kernel2D_F32(5, new float[]
			{1, 0,-2 , 0, 1,
			 4, 0,-8 , 0, 4,
		     6, 0,-12, 0, 6,
		     4, 0,-8 , 0, 4,
			 1, 0,-2 , 0, 1});
	public static Kernel2D_F32 kernelXY_F32 = new Kernel2D_F32(5, new float[]
			{1, 2,0,-2,-1,
			 2, 4,0,-4,-2,
		     0, 0,0, 0, 0,
		    -2,-4,0, 4, 2,
			-1,-2,0, 2, 1});

	/**
	 * Computes the image's second derivatives.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivXX Second derivative along the x-axis. Modified.
	 * @param derivYY Second derivative along the y-axis. Modified.
	 * @param derivXY Second cross derivative. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process(GrayU8 orig,
							   GrayS16 derivXX, GrayS16 derivYY, GrayS16 derivXY ,
							   ImageBorder_S32 border ) {
		InputSanityCheck.checkSameShape(orig, derivXX, derivYY, derivXY);
		HessianSobel_Shared.process(orig, derivXX, derivYY, derivXY);

		if( border != null ) {
			border.setImage(orig);
			ConvolveJustBorder_General.convolve(kernelXX_I32, border,derivXX);
			ConvolveJustBorder_General.convolve(kernelYY_I32, border,derivYY);
			ConvolveJustBorder_General.convolve(kernelXY_I32, border,derivXY);
		}
	}

	/**
	 * Computes the image's second derivatives.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivXX Second derivative along the x-axis. Modified.
	 * @param derivYY Second derivative along the y-axis. Modified.
	 * @param derivXY Second cross derivative. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process(GrayF32 orig,
							   GrayF32 derivXX, GrayF32 derivYY, GrayF32 derivXY ,
							   ImageBorder_F32 border ) {
		InputSanityCheck.checkSameShape(orig, derivXX, derivYY, derivXY);
		HessianSobel_Shared.process(orig, derivXX, derivYY, derivXY);

		if( border != null ) {
			border.setImage(orig);
			ConvolveJustBorder_General.convolve(kernelXX_F32, border , derivXX);
			ConvolveJustBorder_General.convolve(kernelYY_F32, border , derivYY);
			ConvolveJustBorder_General.convolve(kernelXY_F32, border , derivXY);
		}
	}
}
