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

package boofcv.alg.transform.pyramid;

import boofcv.abst.filter.convolve.ConvolveInterface;
import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.convolve.FactoryConvolve;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.BoofDefaults;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.gss.GaussianScaleSpace;
import boofcv.struct.image.ImageGray;


/**
 * <p>
 * Implementation of {@link boofcv.struct.gss.GaussianScaleSpace} that focuses on one scale space at a time.
 * When the scale space is changed the scaled image is recomputed and previously computed derivatives
 * are marked as stale.  Then the derivatives are recomputed as needed.
 * </p>
 *
 * @author Peter Abeles
 */
public class NoCacheScaleSpace<I extends ImageGray, D extends ImageGray>
		implements GaussianScaleSpace<I,D>
{
	// reference to the original input image
	private I originalImage;

	// types of input images
	private Class<I> inputType;

	AnyImageDerivative<I,D> anyDeriv;

	private double scales[];
	private int currentScale;

	private I workImage;
	private I scaledImage;


	// how the borders are handled
	BorderType borderDeriv = BoofDefaults.DERIV_BORDER_TYPE;
	BorderType borderBlur = BorderType.NORMALIZED;

	/**
	 * Declares internal data structures.
	 *
	 * @param inputType Type of input image
	 * @param derivType Derivative image type.
	 */
	public NoCacheScaleSpace(Class<I> inputType, Class<D> derivType) {
		this.inputType = inputType;
		anyDeriv = GImageDerivativeOps.derivativeForScaleSpace(inputType, derivType);
	}

	@Override
	public void setScales(double... scales) {
		this.scales = scales;
	}

	@Override
	public double getScale(int level) {
		return scales[level];
	}

	@Override
	public void setImage(I input) {
		this.originalImage = input;

		if( scaledImage == null ) {
			scaledImage = GeneralizedImageOps.createSingleBand(inputType, input.getWidth(), input.getHeight());
			workImage = GeneralizedImageOps.createSingleBand(inputType, input.getWidth(), input.getHeight());
		} else if( scaledImage.width != input.width || scaledImage.height != input.height ) {
			scaledImage.reshape(input.width,input.height);
			workImage.reshape(input.width,input.height);
		}
	}

	@Override
	public void setActiveScale(int index) {
		this.currentScale = index;
		double sigma = scales[index];
		int radius = FactoryKernelGaussian.radiusForSigma(sigma, 0);

		Kernel1D kernel = FactoryKernelGaussian.gaussian1D(inputType,sigma,radius);

		ConvolveInterface<I, I> blurX = FactoryConvolve.convolve(kernel,inputType,inputType, borderBlur ,true);
		ConvolveInterface<I, I> blurY = FactoryConvolve.convolve(kernel,inputType,inputType, borderBlur ,false);

		// compute the scale image
		blurX.process(originalImage,workImage);
		blurY.process(workImage,scaledImage);

		anyDeriv.setInput(scaledImage);
	}

	@Override
	public double getCurrentScale() {
		return scales[currentScale];
	}

	@Override
	public int getTotalScales() {
		return scales.length;
	}

	@Override
	public I getScaledImage() {
		return scaledImage;
	}

	@Override
	public void setBorderType(BorderType type) {
		borderDeriv = type;
		borderBlur = type;
		setActiveScale(currentScale);
	}

	@Override
	public BorderType getBorderType() {
		return borderDeriv;
	}

	/**
	 * Computes derivative images using previously computed lower level derivatives.  Only
	 * computes/declares images as needed.
	 */
	@Override
	public D getDerivative(boolean... isX) {
		return anyDeriv.getDerivative(isX);
	}
}
