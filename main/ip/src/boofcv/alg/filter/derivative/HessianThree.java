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
import boofcv.alg.filter.derivative.impl.HessianThree_Standard;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;

/**
 * <p>
 * Computes the second derivative (Hessian) of an image using.  This hessian is derived by using the same gradient
 * function used in {@link GradientThree}, which uses a kernel of [-1 0 1].
 * </p>
 *
 * <p>
 * WARNING: It is computationally more expensive to compute the Hessian with this operation than applying the
 * gradient operator multiple times.  However, this does not require the creation additional storage to save
 * intermediate results.
 * </p>
 *
 * <p>
 * Kernel for &part; <sup>2</sup>f/&part; x<sup>2</sup> and &part;<sup>2</sup>f /&part; y<sup>2</sup> is
 * [1 0 -2 0 1] and &part;<sup>2</sup>f/&part; x&part;y is:<br>
 * <table border="1">
 * <tr> <td> 1 </td> <td> 0 </td> <td> -1 </td> </tr>
 * <tr> <td> 0 </td> <td> 0 </td> <td> 0 </td> </tr>
 * <tr> <td> -1 </td> <td> 0 </td> <td> 1 </td> </tr>
 * </table}
 * </p>
 *
 * @author Peter Abeles
 */
public class HessianThree {

	public static Kernel1D_I32 kernelXXYY_I32 = new Kernel1D_I32(new int[]{1,0,-2,0,1},5);
	public static Kernel2D_I32 kernelCross_I32 = new Kernel2D_I32(3, new int[]{1,0,-1,0,0,0,-1,0,1});

	public static Kernel1D_F32 kernelXXYY_F32 = new Kernel1D_F32(new float[]{0.5f,0,-1,0,0.5f},5);
	public static Kernel2D_F32 kernelCross_F32 = new Kernel2D_F32(3, new float[]{0.5f,0,-0.5f,0,0,0,-0.5f,0,0.5f});

	/**
	 * <p>
	 * Computes the second derivative of an {@link GrayU8} along the x and y axes.
	 * </p>
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
		HessianThree_Standard.process(orig, derivXX, derivYY,derivXY);

		if( border != null ) {
			DerivativeHelperFunctions.processBorderHorizontal(orig, derivXX ,kernelXXYY_I32, border );
			DerivativeHelperFunctions.processBorderVertical(orig, derivYY ,kernelXXYY_I32, border );
			ConvolveJustBorder_General.convolve(kernelCross_I32, border,derivXY);
		}
	}

	/**
	 * Computes the second derivative of an {@link GrayU8} along the x and y axes.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivXX Second derivative along the x-axis. Modified.
	 * @param derivYY Second derivative along the y-axis. Modified.
	 * @param derivXY Second cross derivative. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process(GrayF32 orig,
							   GrayF32 derivXX, GrayF32 derivYY, GrayF32 derivXY,
							   ImageBorder_F32 border ) {
		InputSanityCheck.checkSameShape(orig, derivXX, derivYY, derivXY);
		HessianThree_Standard.process(orig, derivXX, derivYY, derivXY);

		if( border != null ) {
			DerivativeHelperFunctions.processBorderHorizontal(orig, derivXX ,kernelXXYY_F32, border );
			DerivativeHelperFunctions.processBorderVertical(orig, derivYY ,kernelXXYY_F32, border );
			ConvolveJustBorder_General.convolve(kernelCross_F32,border,derivXY);
		}
	}
}
