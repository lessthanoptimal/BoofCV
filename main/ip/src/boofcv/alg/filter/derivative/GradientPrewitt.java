/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.filter.derivative.impl.GradientPrewitt_Shared;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_I32;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
public class GradientPrewitt {
	public static Kernel2D_I32 kernelDerivX_I32 = new Kernel2D_I32(3, new int[]{-1,0,1,-1,0,1,-1,0,1});
	public static Kernel2D_I32 kernelDerivY_I32 = new Kernel2D_I32(3, new int[]{-1,-1,-1,0,0,0,1,1,1});
	public static Kernel2D_F32 kernelDerivX_F32 = new Kernel2D_F32(
			3, new float[]{-1f,0,1f,-1f,0,1f,-1f,0,1f});
	public static Kernel2D_F32 kernelDerivY_F32 = new Kernel2D_F32(
			3, new float[]{-1f,-1f,-1f,0,0,0,1f,1f,1f});

	/**
	 * Returns the kernel for computing the derivative along the x-axis.
	 */
	public static Kernel2D getKernelX( boolean isInteger ) {
		if( isInteger )
			return kernelDerivX_I32;
		else
			return kernelDerivX_F32;
	}

	/**
	 * Computes the derivative in the X and Y direction using an integer Prewitt edge detector.
	 *
	 * @param orig   Input image.  Not modified.
	 * @param derivX Storage for image derivative along the x-axis. Modified.
	 * @param derivY Storage for image derivative along the y-axis. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process(ImageUInt8 orig, ImageSInt16 derivX, ImageSInt16 derivY, ImageBorder_I32 border ) {
		InputSanityCheck.checkSameShape(orig, derivX, derivY);
		GradientPrewitt_Shared.process(orig, derivX, derivY);

		if( border != null ) {
			border.setImage(orig);
			ConvolveJustBorder_General.convolve(kernelDerivX_I32, border,derivX,1);
			ConvolveJustBorder_General.convolve(kernelDerivY_I32, border,derivY,1);
		}
	}

	/**
	 * Computes the derivative in the X and Y direction using an integer Prewitt edge detector.
	 *
	 * @param orig   Input image.  Not modified.
	 * @param derivX Storage for image derivative along the x-axis. Modified.
	 * @param derivY Storage for image derivative along the y-axis. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process(ImageSInt16 orig, ImageSInt16 derivX, ImageSInt16 derivY, ImageBorder_I32 border ) {
		InputSanityCheck.checkSameShape(orig, derivX, derivY);
		GradientPrewitt_Shared.process(orig, derivX, derivY);

		if( border != null ) {
			border.setImage(orig);
			ConvolveJustBorder_General.convolve(kernelDerivX_I32, border,derivX,1);
			ConvolveJustBorder_General.convolve(kernelDerivY_I32, border,derivY,1);
		}
	}

	/**
	 * Computes the derivative in the X and Y direction using a floating point Prewitt edge detector.
	 *
	 * @param orig   Input image.  Not modified.
	 * @param derivX Storage for image derivative along the x-axis. Modified.
	 * @param derivY Storage for image derivative along the y-axis. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process(ImageFloat32 orig, ImageFloat32 derivX, ImageFloat32 derivY, ImageBorder_F32 border ) {
		InputSanityCheck.checkSameShape(orig, derivX, derivY);

		GradientPrewitt_Shared.process(orig, derivX, derivY);

		if( border != null ) {
			border.setImage(orig);
			ConvolveJustBorder_General.convolve(kernelDerivX_F32, border, derivX,1);
			ConvolveJustBorder_General.convolve(kernelDerivY_F32, border, derivY,1);
		}
	}
}
