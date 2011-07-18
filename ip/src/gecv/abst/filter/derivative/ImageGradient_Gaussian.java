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
import gecv.core.image.GeneralizedImageOps;
import gecv.core.image.border.BorderType;
import gecv.struct.convolve.Kernel1D;
import gecv.struct.image.ImageBase;

import static gecv.alg.filter.convolve.KernelFactory.*;


/**
 * Finds the derivative using a Gaussian kernel.  This is the same as convolving the image
 * and then computing the derivative
 *
 * @author Peter Abeles
 */
public class ImageGradient_Gaussian<I extends ImageBase, D extends ImageBase >
		implements ImageGradient<I, D> {

	// filters for computing image derivatives
	FilterImageInterface<I, D> derivX_H;
	FilterImageInterface<I, I> derivX_V;
	FilterImageInterface<I, I> derivY_H;
	FilterImageInterface<I, D> derivY_V;
	// storage the results after the first gaussian blur
	I storage;

	public ImageGradient_Gaussian(int radius , Class<I> inputType , Class<D> derivType) {
		this(sigmaForRadius(radius),radius,inputType,derivType);
	}

	public ImageGradient_Gaussian(double sigma, int radius,
									  Class<I> inputType , Class<D> derivType ) {
		Kernel1D kernel;
		Kernel1D kernelDeriv;

		if( GeneralizedImageOps.isFloatingPoint(inputType)) {
			kernel = KernelFactory.gaussian1D_F32(sigma, radius, true);
			kernelDeriv = gaussianDerivative1D_F32(sigma, radius, true);
		} else {
			kernel = KernelFactory.gaussian1D_I32(sigma, radius);
			kernelDeriv = gaussianDerivative1D_I32(sigma, radius);
		}

		derivX_H = FactoryConvolve.convolve(kernelDeriv,inputType,derivType, BorderType.EXTENDED,true);
		derivX_V = FactoryConvolve.convolve(kernel,inputType,inputType, BorderType.NORMALIZED,true);
		derivY_H = FactoryConvolve.convolve(kernel,inputType,inputType, BorderType.NORMALIZED,false);
		derivY_V = FactoryConvolve.convolve(kernelDeriv,inputType,derivType, BorderType.EXTENDED,false);
	}

	@SuppressWarnings({"unchecked"})
	@Override
	public void process( I inputImage , D derivX, D derivY ) {

		if( storage == null ) {
			storage = (I)inputImage._createNew(inputImage.width,inputImage.height );
		} else {
			storage.reshape(inputImage.width,inputImage.height);
		}

		derivX_V.process(inputImage,storage);
		derivX_H.process(storage,derivX);
		derivY_H.process(inputImage,storage);
		derivY_V.process(storage,derivY);
	}

	@Override
	public int getBorder() {
		return 0;
	}
}