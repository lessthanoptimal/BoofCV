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
import boofcv.alg.filter.convolve.ConvolveImageNoBorder;
import boofcv.alg.filter.convolve.ConvolveWithBorder;
import boofcv.core.image.border.*;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;

/**
 * <p>
 * These functions compute the image hessian by computing the image gradient twice.  While about twice as fast
 * as computing the Hessian directly from a larger kernel, it requires additional storage space.
 * </p>
 *
 * <p>
 * NOTE: A subtle design flaw in many of these operations is that they add more image blur each time the derivative
 * is computed.  For example, with the sobel operator is is derived by convolving a blur kernel with a derivative kernel.
 * So applying it twice to computer the second order x-derivative is the same as blurring it twice then applying
 * the derivative operator twice.  Thus it is not th true second order derivative.  Having said that, this
 * issue is of little practical importance and is hard to detect..
 * </p>
 *
 * @author Peter Abeles
 */
public class HessianFromGradient {

	/**
	 * Computes the hessian given an image's gradient using a Prewitt operator.
	 *
	 * @param inputDerivX Already computed image x-derivative.
	 * @param inputDerivY Already computed image y-derivative.
	 * @param derivXX Output second XX partial derivative.
	 * @param derivYY Output second YY partial derivative.
	 * @param derivXY Output second XY partial derivative.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void hessianPrewitt(GrayS16 inputDerivX , GrayS16 inputDerivY ,
									  GrayS16 derivXX, GrayS16 derivYY, GrayS16 derivXY ,
									  ImageBorder_S32 border ) {
		InputSanityCheck.checkSameShape(inputDerivX, inputDerivY, derivXX, derivYY, derivXY);

		GradientPrewitt.process(inputDerivX,derivXX,derivXY,border);

		if( border != null )
			ConvolveWithBorder.convolve(GradientPrewitt.kernelDerivY_I32,inputDerivY,derivYY,border);
		else
			ConvolveImageNoBorder.convolve(GradientPrewitt.kernelDerivY_I32,inputDerivY,derivYY);
	}

	/**
	 * Computes the hessian given an image's gradient using a Prewitt operator.
	 *
	 * @param inputDerivX Already computed image x-derivative.
	 * @param inputDerivY Already computed image y-derivative.
	 * @param derivXX Output second XX partial derivative.
	 * @param derivYY Output second YY partial derivative.
	 * @param derivXY Output second XY partial derivative.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void hessianPrewitt(GrayF32 inputDerivX , GrayF32 inputDerivY,
									  GrayF32 derivXX, GrayF32 derivYY, GrayF32 derivXY ,
									  ImageBorder_F32 border ) {
		InputSanityCheck.checkSameShape(inputDerivX, inputDerivY, derivXX, derivYY, derivXY);

		GradientPrewitt.process(inputDerivX,derivXX,derivXY,border);

		if( border != null )
			ConvolveWithBorder.convolve(GradientPrewitt.kernelDerivY_F32,inputDerivY,derivYY,border);
		else
			ConvolveImageNoBorder.convolve(GradientPrewitt.kernelDerivY_F32,inputDerivY,derivYY);
	}

	/**
	 * Computes the hessian given an image's gradient using a Sobel operator.
	 *
	 * @param inputDerivX Already computed image x-derivative.
	 * @param inputDerivY Already computed image y-derivative.
	 * @param derivXX Output second XX partial derivative.
	 * @param derivYY Output second YY partial derivative.
	 * @param derivXY Output second XY partial derivative.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void hessianSobel(GrayS16 inputDerivX , GrayS16 inputDerivY ,
									GrayS16 derivXX, GrayS16 derivYY, GrayS16 derivXY ,
									ImageBorder_S32 border ) {
		InputSanityCheck.checkSameShape(inputDerivX, inputDerivY, derivXX, derivYY, derivXY);

		GradientSobel.process(inputDerivX,derivXX,derivXY,border);

		if( border != null )
			ConvolveWithBorder.convolve(GradientSobel.kernelDerivY_I32,inputDerivY,derivYY,
					new ImageBorder1D_S32(BorderIndex1D_Extend.class));
		else
			ConvolveImageNoBorder.convolve(GradientSobel.kernelDerivY_I32,inputDerivY,derivYY);
	}

	/**
	 * Computes the hessian given an image's gradient using a Sobel operator.
	 *
	 * @param inputDerivX Already computed image x-derivative.
	 * @param inputDerivY Already computed image y-derivative.
	 * @param derivXX Output second XX partial derivative.
	 * @param derivYY Output second YY partial derivative.
	 * @param derivXY Output second XY partial derivative.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void hessianSobel(GrayF32 inputDerivX , GrayF32 inputDerivY,
									GrayF32 derivXX, GrayF32 derivYY, GrayF32 derivXY ,
									ImageBorder_F32 border ) {
		InputSanityCheck.checkSameShape(inputDerivX, inputDerivY, derivXX, derivYY, derivXY);

		GradientSobel.process(inputDerivX,derivXX,derivXY,border);

		if( border != null )
			ConvolveWithBorder.convolve(GradientSobel.kernelDerivY_F32,inputDerivY,derivYY,
					new ImageBorder1D_F32(BorderIndex1D_Extend.class));
		else
			ConvolveImageNoBorder.convolve(GradientSobel.kernelDerivY_F32,inputDerivY,derivYY);
	}

	/**
	 * Computes the hessian given an image's gradient using a three derivative operator.
	 *
	 * @param inputDerivX Already computed image x-derivative.
	 * @param inputDerivY Already computed image y-derivative.
	 * @param derivXX Output second XX partial derivative.
	 * @param derivYY Output second YY partial derivative.
	 * @param derivXY Output second XY partial derivative.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void hessianThree(GrayS16 inputDerivX , GrayS16 inputDerivY ,
									GrayS16 derivXX, GrayS16 derivYY, GrayS16 derivXY ,
									ImageBorder_S32 border ) {
		InputSanityCheck.checkSameShape(inputDerivX, inputDerivY, derivXX, derivYY, derivXY);

		GradientThree.process(inputDerivX,derivXX,derivXY,border);

		if( border != null )
			ConvolveWithBorder.vertical(GradientThree.kernelDeriv_I32,inputDerivY,derivYY,
					new ImageBorder1D_S32(BorderIndex1D_Extend.class));
		else
			ConvolveImageNoBorder.vertical(GradientThree.kernelDeriv_I32,inputDerivY,derivYY);
	}

	/**
	 * Computes the hessian given an image's gradient using a three derivative operator.
	 *
	 * @param inputDerivX Already computed image x-derivative.
	 * @param inputDerivY Already computed image y-derivative.
	 * @param derivXX Output second XX partial derivative.
	 * @param derivYY Output second YY partial derivative.
	 * @param derivXY Output second XY partial derivative.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void hessianThree(GrayF32 inputDerivX , GrayF32 inputDerivY,
									GrayF32 derivXX, GrayF32 derivYY, GrayF32 derivXY ,
									ImageBorder_F32 border ) {
		InputSanityCheck.checkSameShape(inputDerivX, inputDerivY, derivXX, derivYY, derivXY);

		GradientThree.process(inputDerivX,derivXX,derivXY,border);

		if( border != null )
			ConvolveWithBorder.vertical(GradientThree.kernelDeriv_F32,inputDerivY,derivYY,
					new ImageBorder1D_F32(BorderIndex1D_Extend.class));
		else
			ConvolveImageNoBorder.vertical(GradientThree.kernelDeriv_F32,inputDerivY,derivYY);
	}
}
