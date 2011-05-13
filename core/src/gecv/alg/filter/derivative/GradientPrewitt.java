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
import gecv.alg.filter.convolve.border.ConvolveJustBorder_General;
import gecv.alg.filter.derivative.impl.GradientPrewitt_Shared;
import gecv.core.image.border.ImageBorderExtended;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
public class GradientPrewitt {
	public static Kernel2D_I32 kernelDerivX_I32 = new Kernel2D_I32(new int[]{-1,0,1,-1,0,1,-1,0,1},3);
	public static Kernel2D_I32 kernelDerivY_I32 = new Kernel2D_I32(new int[]{-1,-1,-1,0,0,0,1,1,1},3);
	public static Kernel2D_F32 kernelDerivX_F32 = new Kernel2D_F32(
			new float[]{-1f,0,1f,-1f,0,1f,-1f,0,1f},3);
	public static Kernel2D_F32 kernelDerivY_F32 = new Kernel2D_F32(
			new float[]{-1f,-1f,-1f,0,0,0,1f,1f,1f},3);

	/**
	 * Computes the derivative in the X and Y direction using an integer Prewitt edge detector.
	 *
	 * @param orig   Input image.  Not modified.
	 * @param derivX Storage for image derivative along the x-axis. Modified.
	 * @param derivY Storage for image derivative along the y-axis. Modified.
	 * @param processBorder If the image's border is processed or not.
	 */
	public static void process(ImageUInt8 orig, ImageSInt16 derivX, ImageSInt16 derivY, boolean processBorder) {
		InputSanityCheck.checkSameShape(orig, derivX, derivY);
		GradientPrewitt_Shared.process(orig, derivX, derivY);

		if( processBorder ) {
			ConvolveJustBorder_General.convolve(kernelDerivX_I32, ImageBorderExtended.wrap(orig),derivX,1);
			ConvolveJustBorder_General.convolve(kernelDerivY_I32, ImageBorderExtended.wrap(orig),derivY,1);
		}
	}

	/**
	 * Computes the derivative in the X and Y direction using a floating point Prewitt edge detector.
	 *
	 * @param orig   Input image.  Not modified.
	 * @param derivX Storage for image derivative along the x-axis. Modified.
	 * @param derivY Storage for image derivative along the y-axis. Modified.
	 * @param processBorder If the image's border is processed or not.
	 */
	public static void process(ImageFloat32 orig, ImageFloat32 derivX, ImageFloat32 derivY, boolean processBorder) {
		InputSanityCheck.checkSameShape(orig, derivX, derivY);

		GradientPrewitt_Shared.process(orig, derivX, derivY);

		if( processBorder ) {
			ConvolveJustBorder_General.convolve(kernelDerivX_F32, ImageBorderExtended.wrap(orig),derivX,1);
			ConvolveJustBorder_General.convolve(kernelDerivY_F32, ImageBorderExtended.wrap(orig),derivY,1);
		}
	}
}
