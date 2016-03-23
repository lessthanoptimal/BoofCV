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
import boofcv.alg.filter.derivative.impl.GradientThree_Standard;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;


/**
 * <p>
 * Computes the image's first derivative along the x and y axises using [-1 0 1] kernel.
 * </p>
 * <p>
 * Th 1-D kernel allows the image's gradient to be computed efficiently but is more sensitive to local noise.
 * </p>
 * <p>
 * For example in an integer image:<br>
 * derivX(x,y) = img(x+1,y) - img(x-1,y)<br>
 * derivY(x,y) = img(x,y+1) - img(x,y-1)<br>
 * </p>
 *
 * @author Peter Abeles
 */
public class GradientThree {

	public static Kernel1D_I32 kernelDeriv_I32 = new Kernel1D_I32(new int[]{-1,0,1},3);
	public static Kernel1D_F32 kernelDeriv_F32 = new Kernel1D_F32(new float[]{-0.5f,0,0.5f},3);

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
	 * Computes the derivative of an {@link GrayU8} along the x and y axes.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivX Derivative along the x-axis. Modified.
	 * @param derivY Derivative along the y-axis. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process(GrayU8 orig,
							   GrayS16 derivX,
							   GrayS16 derivY, ImageBorder_S32 border ) {
		InputSanityCheck.checkSameShape(orig, derivX, derivY);
		GradientThree_Standard.process(orig, derivX, derivY);

		if( border != null ) {
			DerivativeHelperFunctions.processBorderHorizontal(orig, derivX , kernelDeriv_I32, border);
			DerivativeHelperFunctions.processBorderVertical(orig, derivY , kernelDeriv_I32, border);
		}
	}

	/**
	 * Computes the derivative of an {@link GrayU8} along the x and y axes.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivX Derivative along the x-axis. Modified.
	 * @param derivY Derivative along the y-axis. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process(GrayU8 orig,
							   GrayS32 derivX,
							   GrayS32 derivY, ImageBorder_S32 border ) {
		InputSanityCheck.checkSameShape(orig, derivX, derivY);
		GradientThree_Standard.process(orig, derivX, derivY);

		if( border != null ) {
			DerivativeHelperFunctions.processBorderHorizontal(orig, derivX , kernelDeriv_I32, border);
			DerivativeHelperFunctions.processBorderVertical(orig, derivY , kernelDeriv_I32, border);
		}
	}

	/**
	 * Computes the derivative of an {@link GrayS16} along the x and y axes.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivX Derivative along the x-axis. Modified.
	 * @param derivY Derivative along the y-axis. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process(GrayS16 orig,
							   GrayS16 derivX,
							   GrayS16 derivY, ImageBorder_S32 border) {
		InputSanityCheck.checkSameShape(orig, derivX, derivY);
		GradientThree_Standard.process(orig, derivX, derivY);

		if( border != null ) {
			DerivativeHelperFunctions.processBorderHorizontal(orig, derivX , kernelDeriv_I32, border);
			DerivativeHelperFunctions.processBorderVertical(orig, derivY , kernelDeriv_I32, border);
		}
	}

	/**
	 * Computes the derivative of an {@link GrayF32} along the x and y axes.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivX Derivative along the x-axis. Modified.
	 * @param derivY Derivative along the y-axis. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process(GrayF32 orig,
							   GrayF32 derivX,
							   GrayF32 derivY, ImageBorder_F32 border) {
		InputSanityCheck.checkSameShape(orig, derivX, derivY);
		GradientThree_Standard.process(orig, derivX, derivY);

		if( border != null ) {
			DerivativeHelperFunctions.processBorderHorizontal(orig, derivX , kernelDeriv_F32, border);
			DerivativeHelperFunctions.processBorderVertical(orig, derivY , kernelDeriv_F32, border);
		}
	}

}
