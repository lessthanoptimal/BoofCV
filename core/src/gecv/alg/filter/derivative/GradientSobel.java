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

import gecv.alg.InputSanityCheck;
import gecv.alg.filter.convolve.ConvolveExtended;
import gecv.alg.filter.convolve.border.ConvolveJustBorder_General;
import gecv.alg.filter.derivative.impl.GradientSobel_Outer;
import gecv.alg.filter.derivative.impl.GradientSobel_UnrolledOuter;
import gecv.core.image.border.ImageBorderExtended;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

/**
 * <p>
 * Computes the image's first derivative along the x and y axises using the Sobel operator.
 * </p>
 * <p>
 * The Sobel kernel weights the inner most pixels more than ones farther away.  This tends to produce better results,
 * but not as good as a gaussian kernel with larger kernel.  However, it can be optimized so that it is much faster than a Gaussian.
 * </p>
 * <p>
 * For integer images, the derivatives in the x and y direction are computed by convolving the following kernels:<br>
 * <br>
 * y-axis<br>
 * <table border="1">
 * <tr> <td> -0.25 </td> <td> -0.5 </td> <td> -0.25 </td> </tr>
 * <tr> <td> 0 </td> <td> 0 </td> <td> 0 </td> </tr>
 * <tr> <td> 0.25 </td> <td> 0.5 </td> <td> 0.25 </td> </tr>
 * </table}
 * x-axis<br>
 * <table border="1">
 * <tr> <td> -1 </td> <td> 0 </td> <td> 1 </td> </tr>
 * <tr> <td> -2 </td> <td> 0 </td> <td> 2 </td> </tr>
 * <tr> <td> -1 </td> <td> 0 </td> <td> 1 </td> </tr>
 * </table}
 * Floating point images use a kernel which is similar to the ones above, but divided by 4.0.
 * As a side note, the sobel operator is equivalent to convolving the image with the following 1D
 * kernels: [0.25 0.5 0.25] [1 0 1]
 * </p>
 *
 * @author Peter Abeles
 */
public class GradientSobel {

	public static Kernel2D_I32 kernelDerivX_I32 = new Kernel2D_I32(new int[]{-1,0,1,-2,0,2,-1,0,1},3);
	public static Kernel2D_I32 kernelDerivY_I32 = new Kernel2D_I32(new int[]{-1,-2,-1,0,0,0,1,2,1},3);
	public static Kernel2D_F32 kernelDerivX_F32 = new Kernel2D_F32(
			new float[]{-0.25f,0,0.25f,-0.5f,0,0.5f,-0.25f,0,0.25f},3);
	public static Kernel2D_F32 kernelDerivY_F32 = new Kernel2D_F32(
			new float[]{-0.25f,-0.5f,-0.25f,0,0,0,0.25f,0.5f,0.25f},3);
	/**
	 * Computes the derivative in the X and Y direction using an integer Sobel edge detector.
	 *
	 * @param orig   Input image.  Not modified.
	 * @param derivX Storage for image derivative along the x-axis. Modified.
	 * @param derivY Storage for image derivative along the y-axis. Modified.
	 * @param processBorder If the image's border is processed or not.
	 */
	public static void process(ImageUInt8 orig, ImageSInt16 derivX, ImageSInt16 derivY, boolean processBorder) {
		InputSanityCheck.checkSameShape(orig, derivX, derivY);
		GradientSobel_Outer.process_I8_sub(orig, derivX, derivY);

		if( processBorder ) {
			ConvolveJustBorder_General.convolve(kernelDerivX_I32, ImageBorderExtended.wrap(orig),derivX,1);
			ConvolveJustBorder_General.convolve(kernelDerivY_I32, ImageBorderExtended.wrap(orig),derivY,1);
		}
	}

	/**
	 * Computes the derivative in the X and Y direction using an integer Sobel edge detector.
	 *
	 * @param orig   Input image.  Not modified.
	 * @param derivX Storage for image derivative along the x-axis. Modified.
	 * @param derivY Storage for image derivative along the y-axis. Modified.
	 * @param processBorder If the image's border is processed or not.
	 */
	public static void process(ImageFloat32 orig, ImageFloat32 derivX, ImageFloat32 derivY, boolean processBorder) {
		InputSanityCheck.checkSameShape(orig, derivX, derivY);

//		GradientSobel_Outer.process_F32(orig, derivX, derivY);
		GradientSobel_UnrolledOuter.process_F32_sub(orig, derivX, derivY);

		if( processBorder ) {
			ConvolveJustBorder_General.convolve(kernelDerivX_F32, ImageBorderExtended.wrap(orig),derivX,1);
			ConvolveJustBorder_General.convolve(kernelDerivY_F32, ImageBorderExtended.wrap(orig),derivY,1);
		}
	}
}
