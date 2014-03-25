/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.filter.derivative.impl.GradientTwo_Standard;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_I32;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;


/**
 * <p>
 * Computes the image's first derivative along the x and y axises using [-1 1] kernel.
 * </p>
 * <p>
 * The 1-D kernel allows the image's gradient to be computed efficiently but is more sensitive to local noise.
 * </p>
 * <p>
 * For example in an integer image:<br>
 * derivX(x,y) = img(x+1,y) - img(x,y)<br>
 * derivY(x,y) = img(x,y+1) - img(x,y)<br>
 * </p>
 *
 * @author Peter Abeles
 */
public class GradientTwo {

	public static Kernel1D_I32 kernelDeriv_I32 = new Kernel1D_I32(new int[]{-1,1},0,2);
	public static Kernel1D_F32 kernelDeriv_F32 = new Kernel1D_F32(new float[]{-1,1},0,2);

	/**
	 * Returns the kernel for computing the derivative along the x-axis.
	 */
	public static Kernel1D getKernelX( boolean isInteger ) {
		if( isInteger )
			return kernelDeriv_I32;
		else
			return kernelDeriv_F32;
	}

	/**
	 * Computes the derivative of an {@link boofcv.struct.image.ImageUInt8} along the x and y axes.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivX Derivative along the x-axis. Modified.
	 * @param derivY Derivative along the y-axis. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process(ImageUInt8 orig,
							   ImageSInt16 derivX,ImageSInt16 derivY, ImageBorder_I32 border ) {
		InputSanityCheck.checkSameShape(orig, derivX, derivY);
		GradientTwo_Standard.process(orig, derivX, derivY);

		if( border != null ) {
			DerivativeHelperFunctions.processBorderHorizontal(orig, derivX , kernelDeriv_I32, border);
			DerivativeHelperFunctions.processBorderVertical(orig, derivY , kernelDeriv_I32, border);
		}
	}

	/**
	 * Computes the derivative of an {@link boofcv.struct.image.ImageSInt16} along the x and y axes.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivX Derivative along the x-axis. Modified.
	 * @param derivY Derivative along the y-axis. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process(ImageSInt16 orig,
							   ImageSInt16 derivX,ImageSInt16 derivY, ImageBorder_I32 border) {
		InputSanityCheck.checkSameShape(orig, derivX, derivY);
		GradientTwo_Standard.process(orig, derivX, derivY);

		if( border != null ) {
			DerivativeHelperFunctions.processBorderHorizontal(orig, derivX , kernelDeriv_I32, border);
			DerivativeHelperFunctions.processBorderVertical(orig, derivY , kernelDeriv_I32, border);
		}
	}

	/**
	 * Computes the derivative of an {@link boofcv.struct.image.ImageFloat32} along the x and y axes.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivX Derivative along the x-axis. Modified.
	 * @param derivY Derivative along the y-axis. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process(ImageFloat32 orig,
							   ImageFloat32 derivX,ImageFloat32 derivY, ImageBorder_F32 border) {
		InputSanityCheck.checkSameShape(orig, derivX, derivY);
		GradientTwo_Standard.process(orig, derivX, derivY);

		if( border != null ) {
			DerivativeHelperFunctions.processBorderHorizontal(orig, derivX , kernelDeriv_F32, border);
			DerivativeHelperFunctions.processBorderVertical(orig, derivY , kernelDeriv_F32, border);
		}
	}

}
