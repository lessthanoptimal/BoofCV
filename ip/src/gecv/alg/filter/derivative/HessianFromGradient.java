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
import gecv.alg.filter.convolve.ConvolveImageNoBorder;
import gecv.alg.filter.convolve.ConvolveWithBorder;
import gecv.core.image.border.BorderIndex1D_Extend;
import gecv.core.image.border.ImageBorder1D_F32;
import gecv.core.image.border.ImageBorder1D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;

/**
 * <p>
 * These functions compute the image hessian by computing the image gradient twice.  While about twice as fast
 * as computing the Hessian directly from a larger kernel, it requires additional storage space.
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
	 * @param processBorder If the border should be processed or not.
	 */
	public static void hessianPrewitt( ImageSInt16 inputDerivX , ImageSInt16 inputDerivY ,
									   ImageSInt16 derivXX, ImageSInt16 derivYY, ImageSInt16 derivXY ,
									   boolean processBorder ) {
		InputSanityCheck.checkSameShape(inputDerivX, inputDerivY, derivXX, derivYY, derivXY);

		GradientPrewitt.process(inputDerivX,derivXX,derivXY,processBorder);

		if( processBorder )
			ConvolveWithBorder.convolve(GradientPrewitt.kernelDerivY_I32,inputDerivY,derivYY,new ImageBorder1D_I32(BorderIndex1D_Extend.class));
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
	 * @param processBorder If the border should be processed or not.
	 */
	public static void hessianPrewitt( ImageFloat32 inputDerivX , ImageFloat32 inputDerivY,
									   ImageFloat32 derivXX, ImageFloat32 derivYY, ImageFloat32 derivXY ,
									   boolean processBorder ) {
		InputSanityCheck.checkSameShape(inputDerivX, inputDerivY, derivXX, derivYY, derivXY);

		GradientPrewitt.process(inputDerivX,derivXX,derivXY,processBorder);

		if( processBorder )
			ConvolveWithBorder.convolve(GradientPrewitt.kernelDerivY_F32,inputDerivY,derivYY,new ImageBorder1D_F32(BorderIndex1D_Extend.class));
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
	 * @param processBorder If the border should be processed or not.
	 */
	public static void hessianSobel( ImageSInt16 inputDerivX , ImageSInt16 inputDerivY ,
									 ImageSInt16 derivXX, ImageSInt16 derivYY, ImageSInt16 derivXY ,
									 boolean processBorder ) {
		InputSanityCheck.checkSameShape(inputDerivX, inputDerivY, derivXX, derivYY, derivXY);

		GradientSobel.process(inputDerivX,derivXX,derivXY,processBorder);

		if( processBorder )
			ConvolveWithBorder.convolve(GradientSobel.kernelDerivY_I32,inputDerivY,derivYY,new ImageBorder1D_I32(BorderIndex1D_Extend.class));
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
	 * @param processBorder If the border should be processed or not.
	 */
	public static void hessianSobel( ImageFloat32 inputDerivX , ImageFloat32 inputDerivY,
									   ImageFloat32 derivXX, ImageFloat32 derivYY, ImageFloat32 derivXY ,
									   boolean processBorder ) {
		InputSanityCheck.checkSameShape(inputDerivX, inputDerivY, derivXX, derivYY, derivXY);

		GradientSobel.process(inputDerivX,derivXX,derivXY,processBorder);

		if( processBorder )
			ConvolveWithBorder.convolve(GradientSobel.kernelDerivY_F32,inputDerivY,derivYY,new ImageBorder1D_F32(BorderIndex1D_Extend.class));
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
	 * @param processBorder If the border should be processed or not.
	 */
	public static void hessianThree( ImageSInt16 inputDerivX , ImageSInt16 inputDerivY ,
									 ImageSInt16 derivXX, ImageSInt16 derivYY, ImageSInt16 derivXY ,
									 boolean processBorder ) {
		InputSanityCheck.checkSameShape(inputDerivX, inputDerivY, derivXX, derivYY, derivXY);

		GradientThree.process(inputDerivX,derivXX,derivXY,processBorder);

		if( processBorder )
			ConvolveWithBorder.vertical(GradientThree.kernelDeriv_I32,inputDerivY,derivYY,new ImageBorder1D_I32(BorderIndex1D_Extend.class));
		else
			ConvolveImageNoBorder.vertical(GradientThree.kernelDeriv_I32,inputDerivY,derivYY,false);
	}

	/**
	 * Computes the hessian given an image's gradient using a three derivative operator.
	 *
	 * @param inputDerivX Already computed image x-derivative.
	 * @param inputDerivY Already computed image y-derivative.
	 * @param derivXX Output second XX partial derivative.
	 * @param derivYY Output second YY partial derivative.
	 * @param derivXY Output second XY partial derivative.
	 * @param processBorder If the border should be processed or not.
	 */
	public static void hessianThree( ImageFloat32 inputDerivX , ImageFloat32 inputDerivY,
									 ImageFloat32 derivXX, ImageFloat32 derivYY, ImageFloat32 derivXY ,
									 boolean processBorder ) {
		InputSanityCheck.checkSameShape(inputDerivX, inputDerivY, derivXX, derivYY, derivXY);

		GradientThree.process(inputDerivX,derivXX,derivXY,processBorder);

		if( processBorder )
			ConvolveWithBorder.vertical(GradientThree.kernelDeriv_F32,inputDerivY,derivYY,new ImageBorder1D_F32(BorderIndex1D_Extend.class));
		else
			ConvolveImageNoBorder.vertical(GradientThree.kernelDeriv_F32,inputDerivY,derivYY,false);
	}
}
