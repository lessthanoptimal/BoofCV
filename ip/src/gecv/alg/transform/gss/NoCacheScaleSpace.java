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

package gecv.alg.transform.gss;

import gecv.abst.filter.convolve.ConvolveInterface;
import gecv.abst.filter.convolve.FactoryConvolve;
import gecv.abst.filter.derivative.AnyImageDerivative;
import gecv.alg.filter.convolve.FactoryKernelGaussian;
import gecv.alg.filter.derivative.GradientThree;
import gecv.core.image.GeneralizedImageOps;
import gecv.core.image.ImageGenerator;
import gecv.core.image.border.BorderType;
import gecv.struct.GecvDefaults;
import gecv.struct.convolve.Kernel1D;
import gecv.struct.gss.GaussianScaleSpace;
import gecv.struct.image.ImageBase;


/**
 * <p>
 * Implementation of {@link gecv.struct.gss.GaussianScaleSpace} that focuses on one scale space at a time.
 * When the scale space is changed the scaled image is recomputed and previously computed derivatives
 * are marked as stale.  Then the derivatives are recomputed as needed.
 * </p>
 *
 * @author Peter Abeles
 */
// todo remove the need to specify max derivative
public class NoCacheScaleSpace<I extends ImageBase, D extends ImageBase>
		implements GaussianScaleSpace<I,D>
{
	// reference to the original input image
	private I originalImage;

	// types of input images
	private ImageGenerator<I> inputGen;
	private ImageGenerator<D> derivGen;

	AnyImageDerivative<I,D> anyDeriv;

	private double scales[];
	private int currentScale;

	private I workImage;
	private I scaledImage;


	// how the borders are handled
	BorderType borderDeriv = GecvDefaults.DERIV_BORDER_TYPE;
	BorderType borderBlur = BorderType.NORMALIZED;

	/**
	 * Declares internal data structures.
	 *
	 * @param inputGen Used to create image of the same type as the input.
	 * @param derivGen Used to create derivative images.
	 * @param maxDerivativeOrder The maximum derivative order which can be computed.
	 */
	public NoCacheScaleSpace(ImageGenerator<I> inputGen, ImageGenerator<D> derivGen,
							 int maxDerivativeOrder ) {
		this.inputGen = inputGen;
		this.derivGen = derivGen;

		boolean isInteger = !GeneralizedImageOps.isFloatingPoint(inputGen.getType());

		// compute image using a kernel which does not involve any additional blurring
		// Using a Gaussian kernel is equivalent to blurring the image an additional time then computing the derivative
		// Other derivatives such as Sobel and Prewitt also blur the image.   Image bluing has already been done
		// once before the derivative is computed.
		anyDeriv = new AnyImageDerivative<I,D>(GradientThree.getKernelX(isInteger),inputGen.getType(),derivGen);
	}

	@Override
	public void setScales(double... scales) {
		this.scales = scales;
	}

	@Override
	public void setImage(I input) {
		this.originalImage = input;

		if( scaledImage == null ) {
			scaledImage = inputGen.createInstance(input.getWidth(),input.getHeight());
			workImage = inputGen.createInstance(input.getWidth(),input.getHeight());
		}
	}

	@Override
	public void setActiveScale(int index) {
		this.currentScale = index;
		double sigma = scales[index];
		int radius = FactoryKernelGaussian.radiusForSigma(sigma);

		Class<I> inputType = inputGen.getType();

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
