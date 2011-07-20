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

package gecv.abst.filter.derivative;

import gecv.abst.filter.FilterImageInterface;
import gecv.abst.filter.convolve.FactoryConvolve;
import gecv.alg.filter.convolve.KernelFactory;
import gecv.core.image.border.BorderType;
import gecv.struct.convolve.Kernel1D;
import gecv.struct.image.ImageBase;

import static gecv.alg.filter.convolve.KernelFactory.sigmaForRadius;


/**
 * Finds the derivative using a Gaussian kernel.  This is the same as convolving the image
 * and then computing the derivative
 *
 * @author Peter Abeles
 */
public class ImageGradient_Gaussian<I extends ImageBase, D extends ImageBase >
		implements ImageGradient<I, D> {

	// default border types.
	// These have been selected to maximize visual appearance while sacrificing some theoretical properties
	private BorderType borderBlur = BorderType.NORMALIZED;
	private BorderType borderDeriv = BorderType.EXTENDED;

	// filters for computing image derivatives
	private FilterImageInterface<I, D> derivX_H;
	private FilterImageInterface<I, I> blurX;
	private FilterImageInterface<I, I> blurY;
	private FilterImageInterface<I, D> derivY_V;
	// storage the results after the first gaussian blur
	private I storage;

	// kernel's radius and the image border
	private int borderSize;

	public ImageGradient_Gaussian(int radius , Class<I> inputType , Class<D> derivType) {
		this(sigmaForRadius(radius),radius,inputType,derivType);
	}

	public ImageGradient_Gaussian(double sigma, int radius,
								  Class<I> inputType , Class<D> derivType ) {
		this.borderSize = radius;
		Kernel1D kernel = KernelFactory.gaussian1D(inputType,sigma,radius);
		Kernel1D kernelDeriv = KernelFactory.gaussianDerivative1D(inputType,sigma,radius);

		derivX_H = FactoryConvolve.convolve(kernelDeriv,inputType,derivType, borderDeriv,true);
		blurX = FactoryConvolve.convolve(kernel,inputType,inputType, borderBlur,true);
		blurY = FactoryConvolve.convolve(kernel,inputType,inputType, borderBlur,false);
		derivY_V = FactoryConvolve.convolve(kernelDeriv,inputType,derivType, borderDeriv,false);
	}

	@SuppressWarnings({"unchecked"})
	@Override
	public void process( I inputImage , D derivX, D derivY ) {

		if( storage == null ) {
			storage = (I)inputImage._createNew(inputImage.width,inputImage.height );
		} else {
			storage.reshape(inputImage.width,inputImage.height);
		}

		blurY.process(inputImage,storage);
		derivX_H.process(storage,derivX);
		blurX.process(inputImage,storage);
		derivY_V.process(storage,derivY);
	}

	public BorderType getBorderBlur() {
		return borderBlur;
	}

	public void setBorderBlur(BorderType borderBlur) {
		this.borderBlur = borderBlur;
	}

	public BorderType getBorderDeriv() {
		return borderDeriv;
	}

	public void setBorderDeriv(BorderType borderDeriv) {
		this.borderDeriv = borderDeriv;
	}

	@Override
	public void setBorderType(BorderType type) {
		this.borderBlur = type;
		this.borderDeriv = type;
	}

	@Override
	public BorderType getBorderType() {
		return borderDeriv;
	}

	@Override
	public int getBorder() {
		if( borderDeriv == BorderType.SKIP )
			return 0;
		else
			return borderSize;
	}
}