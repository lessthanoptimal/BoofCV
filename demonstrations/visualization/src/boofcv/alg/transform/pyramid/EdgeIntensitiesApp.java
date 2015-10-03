/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.blur.BlurStorageFilter;
import boofcv.abst.filter.convolve.ConvolveInterface;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.derivative.GradientThree;
import boofcv.alg.filter.kernel.GKernelMath;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.convolve.FactoryConvolve;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;


/**
 * Empirically validates some theoretical predictions.
 *
 * @author Peter Abeles
 */
public class EdgeIntensitiesApp<T extends ImageSingleBand> {

	Class<T> imageType;

	int width = 200;
	int height = 200;

	T input;
	T derivY;

	double sigma = 1;
	int radius = FactoryKernelGaussian.radiusForSigma(sigma,1);

	public EdgeIntensitiesApp(Class<T> imageType) {
		this.imageType = imageType;
		input = GeneralizedImageOps.createSingleBand(imageType, width, height);
		derivY = GeneralizedImageOps.createSingleBand(imageType, width, height);
	}

	public void init() {
		GImageMiscOps.fillRectangle(input, 100, width / 2, 0, width / 2, height);
	}

	private void printIntensity( String message , T deriv ) {
		System.out.printf("%20s: ",message);
		int middle = width/2;
		for( int i = middle-10; i <= middle+10; i++ ) {
			double val = GeneralizedImageOps.get(deriv,i,height/2);
			System.out.printf("%5.1f ",val);
		}
		System.out.println();
	}

	/**
	 * Validate that convolution/derivative is in fact associative
	 */
	public void convolveDerivOrder() {
		T blur = GeneralizedImageOps.createSingleBand(imageType, width, height);
		T blurDeriv = GeneralizedImageOps.createSingleBand(imageType, width, height);
		T deriv = GeneralizedImageOps.createSingleBand(imageType, width, height);
		T derivBlur = GeneralizedImageOps.createSingleBand(imageType, width, height);

		BlurStorageFilter<T> funcBlur = FactoryBlurFilter.gaussian(imageType,sigma,radius);
		ImageGradient<T,T> funcDeriv = FactoryDerivative.three(imageType,imageType);

		funcBlur.process(input,blur);
		funcDeriv.process(blur,blurDeriv,derivY);

		funcDeriv.process(input,deriv,derivY);
		funcBlur.process(deriv,derivBlur);

		printIntensity("Blur->Deriv",blurDeriv);
		printIntensity("Deriv->Blur",blurDeriv);
	}

	/**
	 * Compare computing the image
	 */
	public void gaussianDerivToDirectDeriv() {
		T blur = GeneralizedImageOps.createSingleBand(imageType, width, height);
		T blurDeriv = GeneralizedImageOps.createSingleBand(imageType, width, height);
		T gaussDeriv = GeneralizedImageOps.createSingleBand(imageType, width, height);

		BlurStorageFilter<T> funcBlur = FactoryBlurFilter.gaussian(imageType,sigma,radius);
		ImageGradient<T,T> funcDeriv = FactoryDerivative.three(imageType,imageType);
		ImageGradient<T,T> funcGaussDeriv = FactoryDerivative.gaussian(sigma,radius,imageType,imageType);

		funcBlur.process(input,blur);
		funcDeriv.process(blur,blurDeriv,derivY);

		funcGaussDeriv.process(input,gaussDeriv,derivY);

		printIntensity("Blur->Deriv",blurDeriv);
		printIntensity("Gauss Deriv",gaussDeriv);
	}

	public void derivByGaussDeriv() {
		System.out.println("DxG*I");
		T blurDeriv = GeneralizedImageOps.createSingleBand(imageType, width, height);

		for( int level = 1; level <= 3; level++ ) {
			ImageGradient<T,T> funcGaussDeriv = FactoryDerivative.gaussian(level,-1,imageType,imageType);
			funcGaussDeriv.process(input,blurDeriv,derivY);

			printIntensity("Sigma "+level,blurDeriv);
		}
	}

	public void derivByBlurThenDeriv() {
		System.out.println("Dx*(G*I)");
		ImageGradient<T,T> funcDeriv = FactoryDerivative.three(imageType,imageType);
		T blur = GeneralizedImageOps.createSingleBand(imageType, width, height);
		T blurDeriv = GeneralizedImageOps.createSingleBand(imageType, width, height);

		for( int sigma = 1; sigma <= 3; sigma++ ) {
			BlurStorageFilter<T> funcBlur = FactoryBlurFilter.gaussian(imageType,sigma,-1);
			funcBlur.process(input,blur);
			funcDeriv.process(blur,blurDeriv,derivY);

			printIntensity("Sigma "+sigma,blurDeriv);
		}
	}

	public void derivByDerivThenBlur() {
		System.out.println("G*(Dx*I)");
		ImageGradient<T,T> funcDeriv = FactoryDerivative.three(imageType,imageType);
		T blur = GeneralizedImageOps.createSingleBand(imageType, width, height);
		T deriv = GeneralizedImageOps.createSingleBand(imageType, width, height);

		for( int sigma = 1; sigma <= 3; sigma++ ) {
			BlurStorageFilter<T> funcBlur = FactoryBlurFilter.gaussian(imageType,sigma,-1);
			funcDeriv.process(input,deriv,derivY);
			funcBlur.process(deriv,blur);

			printIntensity("Sigma "+sigma,blur);
		}
	}

	public void derivByDerivOfBlur() {
		System.out.println("(Dx*G)*I");
		T blur = GeneralizedImageOps.createSingleBand(imageType, width, height);

		for( int sigma = 1; sigma <= 3; sigma++ ) {
			Kernel1D g = FactoryKernelGaussian.gaussian1D(ImageFloat32.class,sigma,-1);
			Kernel1D d = GradientThree.getKernelX(false);
			Kernel1D god = GKernelMath.convolve1D(d,g);
			ConvolveInterface<T,T> f = FactoryConvolve.convolve(god,imageType,imageType, BorderType.EXTENDED,true);
			f.process(input,blur);

			printIntensity("Sigma "+sigma,blur);
		}
	}

	public void derivByGaussThenGausDeriv() {
		System.out.println("DxG*(G*I)");
		T blur = GeneralizedImageOps.createSingleBand(imageType, width, height);
		T blurDeriv = GeneralizedImageOps.createSingleBand(imageType, width, height);

		for( int sigma = 1; sigma <= 3; sigma++ ) {
			ImageGradient<T,T> funcGaussDeriv = FactoryDerivative.gaussian(sigma,-1,imageType,imageType);
			BlurStorageFilter<T> funcBlur = FactoryBlurFilter.gaussian(imageType,sigma,-1);
			funcBlur.process(input,blur);
			funcGaussDeriv.process(blur,blurDeriv,derivY);

			printIntensity("Sigma "+sigma,blurDeriv);
		}
	}

	public void derivByGaussGausThenDeriv() {
		System.out.println("Dx*(G*(G*I))");
		ImageGradient<T,T> funcDeriv = FactoryDerivative.three(imageType,imageType);
		T blur = GeneralizedImageOps.createSingleBand(imageType, width, height);
		T blur2 = GeneralizedImageOps.createSingleBand(imageType, width, height);
		T blurDeriv = GeneralizedImageOps.createSingleBand(imageType, width, height);

		for( int sigma = 1; sigma <= 3; sigma++ ) {
			BlurStorageFilter<T> funcBlur = FactoryBlurFilter.gaussian(imageType,sigma,-1);

			funcBlur.process(input,blur);
			funcBlur.process(blur,blur2);

			funcDeriv.process(blur2,blurDeriv,derivY);

			printIntensity("Sigma "+sigma,blurDeriv);
		}
	}

	public static void main( String args[] ) {
		EdgeIntensitiesApp app = new EdgeIntensitiesApp(ImageFloat32.class);
		app.init();

		// see how similar the result is if the order in which the operations is done is swapped
//		app.convolveDerivOrder();
//		app.gaussianDerivToDirectDeriv();

		// The derivative's magnitude should be proportional to 1/sigma
		app.derivByBlurThenDeriv();
		System.out.println("-----");
		app.derivByDerivThenBlur();
		System.out.println("-----");
		app.derivByDerivOfBlur();
		System.out.println("-----");
		app.derivByGaussDeriv();
		System.out.println("-----");
		app.derivByGaussThenGausDeriv();
		System.out.println("-----");
		app.derivByGaussGausThenDeriv();
	}

}
