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

package gecv.struct.gss;

import gecv.abst.filter.convolve.ConvolveInterface;
import gecv.abst.filter.convolve.FactoryConvolve;
import gecv.alg.filter.convolve.KernelFactory;
import gecv.core.image.ImageGenerator;
import gecv.core.image.border.BorderType;
import gecv.struct.GecvDefaults;
import gecv.struct.convolve.Kernel1D;
import gecv.struct.image.ImageBase;


/**
 * <p>
 * Implementation of {@link GaussianScaleSpace} that focuses on one scale space at a time.
 * When the scale space is changed the scaled image is recomputed and previously computed derivatives
 * are marked as stale.  Then the derivatives are recomputed as needed.
 * </p>
 *
 * @author Peter Abeles
 */
public class NoCacheScaleSpace<I extends ImageBase, D extends ImageBase>
		implements GaussianScaleSpace<I,D>
{
	// reference to the original input image
	private I originalImage;

	// types of input images
	private ImageGenerator<I> inputGen;
	private ImageGenerator<D> derivGen;

	// gaussian blur the input image
	private ConvolveInterface<I, I> blurX;
	private ConvolveInterface<I, I> blurY;

	// gaussian blur the derivative image
	private ConvolveInterface<D, D> blurDerivX;
	private ConvolveInterface<D, D> blurDerivY;

	// filters for computing image derivatives
	private ConvolveInterface<I, D> derivX_H;
	private ConvolveInterface<I, D> derivY_V;

	// gaussian blur the derivative image
	private ConvolveInterface<D, D> derivDerivX_H;
	private ConvolveInterface<D, D> derivDerivY_V;

	private double scales[];
	private int currentScale;

	private I workImage;
	private D workImageD;
	private I scaledImage;

	// stores computed derivative images
	private D[][] derivatives;
	// if true then 
	private boolean[][] stale;

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

		derivatives = (D[][])new ImageBase[maxDerivativeOrder][];
		stale = new boolean[maxDerivativeOrder][];

		for( int i = 0; i < maxDerivativeOrder; i++) {
			int N = (int)Math.pow(2,i+1);
			derivatives[i] = (D[])new ImageBase[N];
			stale[i] = new boolean[N];
			for( int j = 0; j < N; j++ ) {
				stale[i][j] = true;
			}
		}
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
			workImageD = derivGen.createInstance(input.getWidth(),input.getHeight());
		}
	}

	@Override
	public void setActiveScale(int index) {
		this.currentScale = index;
		double sigma = scales[index];
		int radius = KernelFactory.radiusForSigma(sigma);

		Class<I> inputType = inputGen.getType();
		Class<D> derivType = derivGen.getType();

		Kernel1D kernel = KernelFactory.gaussian1D(inputType,sigma,radius);
		Kernel1D kernelDeriv = KernelFactory.gaussianDerivative1D(inputType,sigma,radius);

		derivX_H = FactoryConvolve.convolve(kernelDeriv,inputType,derivType, borderDeriv,true);
		derivY_V = FactoryConvolve.convolve(kernelDeriv,inputType,derivType, borderDeriv,false);

		derivDerivX_H = FactoryConvolve.convolve(kernelDeriv,derivType,derivType, borderDeriv,true);
		derivDerivY_V = FactoryConvolve.convolve(kernelDeriv,derivType,derivType, borderDeriv,false);

		blurX = FactoryConvolve.convolve(kernel,inputType,inputType, borderBlur ,true);
		blurY = FactoryConvolve.convolve(kernel,inputType,inputType, borderBlur ,false);

		blurDerivX = FactoryConvolve.convolve(kernel,derivType,derivType, borderBlur ,true);
		blurDerivY = FactoryConvolve.convolve(kernel,derivType,derivType, borderBlur ,false);

		// compute the scale image
		blurX.process(originalImage,workImage);
		blurY.process(workImage,scaledImage);

		// set the derivatives to be stale
		for( int i = 0; i < stale.length; i++) {
			for( int j = 0; j < stale[i].length; j++ ) {
				stale[i][j] = true;
			}
		}
	}

	@Override
	public double getCurrentScale() {
		return scales[currentScale];
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
		if( isX.length > stale.length )
			throw new IllegalArgumentException("The derivatives degree is too great");

		int index = 0;
		int prevIndex = 0;
		for( int level = 0; level < isX.length; level++ ) {
			index |= isX[level] ? 0 : 1 << level;

			if( stale[level][index] ) {
				stale[level][index] = false;
				if( derivatives[level][index] == null ) {
					derivatives[level][index] = derivGen.createInstance(originalImage.getWidth(),originalImage.getHeight());
				}

				if( level == 0 ) {
					if( isX[level]) {
						blurY.process(originalImage,workImage);
						derivX_H.process(workImage,derivatives[level][index]);
					} else {
						blurX.process(originalImage,workImage);
						derivY_V.process(workImage,derivatives[level][index]);
					}
				} else {
					D prev = derivatives[level-1][prevIndex];
					if( isX[level]) {
						blurDerivY.process(prev,workImageD);
						derivDerivX_H.process(workImageD,derivatives[level][index]);
					} else {
						blurDerivX.process(prev,workImageD);
						derivDerivY_V.process(workImageD,derivatives[level][index]);
					}
				}
			}
			prevIndex = index;
		}

		return derivatives[isX.length-1][index];
	}
}
