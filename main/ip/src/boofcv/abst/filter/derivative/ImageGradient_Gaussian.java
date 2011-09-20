/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.abst.filter.derivative;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.convolve.FactoryConvolve;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.image.ImageBase;

import static boofcv.factory.filter.kernel.FactoryKernelGaussian.sigmaForRadius;


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

	// type of input/output images
	private Class<D> derivType;

	public ImageGradient_Gaussian(int radius , Class<I> inputType , Class<D> derivType) {
		this(sigmaForRadius(radius,0),radius,inputType,derivType);
	}

	public ImageGradient_Gaussian(double sigma, int radius,
								  Class<I> inputType , Class<D> derivType ) {
		this.borderSize = radius;
		this.derivType = derivType;

		// need to do this here to make sure the blur and derivative functions have the same paramters.
		if( radius <= 0 )
			radius = FactoryKernelGaussian.radiusForSigma(sigma,1);
		else if( sigma <= 0 )
			sigma = FactoryKernelGaussian.sigmaForRadius(radius,1);

		Kernel1D kernel = FactoryKernelGaussian.gaussian1D(inputType,sigma,radius);
		Kernel1D kernelDeriv = FactoryKernelGaussian.derivativeI(inputType,1,sigma,radius);

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
		if( borderDeriv == BorderType.SKIP)
			return 0;
		else
			return borderSize;
	}

	@Override
	public Class getDerivType() {
		return derivType;
	}
}