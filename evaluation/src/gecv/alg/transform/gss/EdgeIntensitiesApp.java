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

import gecv.abst.filter.blur.FactoryBlurFilter;
import gecv.abst.filter.blur.impl.BlurStorageFilter;
import gecv.abst.filter.derivative.ImageGradient;
import gecv.alg.misc.ImageTestingOps;
import gecv.factory.filter.derivative.FactoryDerivative;
import gecv.factory.filter.kernel.FactoryKernelGaussian;
import gecv.struct.image.ImageFloat32;


/**
 * Empirically validates some theoretical predictions.
 *
 * @author Peter Abeles
 */
public class EdgeIntensitiesApp {

	int width = 200;
	int height = 200;

	ImageFloat32 input = new ImageFloat32(width,height);
	ImageFloat32 derivX = new ImageFloat32(width,height);
	ImageFloat32 derivY = new ImageFloat32(width,height);

	double sigma = 1;
	int radius = FactoryKernelGaussian.radiusForSigma(sigma,1);

	public void init() {
		ImageTestingOps.fillRectangle(input,100,width/2,0,width/2,height);
	}

	private void printIntensity( String message , ImageFloat32 deriv ) {
		System.out.printf("%20s: ",message);
		int middle = width/2;
		for( int i = middle-10; i <= middle+10; i++ ) {
			System.out.printf("%5.1f ",deriv.get(i,height/2));
		}
		System.out.println();
	}

	/**
	 * Validate that convolution/derivative is in fact associative
	 */
	public void convolveDerivOrder() {
		ImageFloat32 blur = new ImageFloat32(width,height);
		ImageFloat32 blurDeriv = new ImageFloat32(width,height);
		ImageFloat32 deriv = new ImageFloat32(width,height);
		ImageFloat32 derivBlur = new ImageFloat32(width,height);

		BlurStorageFilter<ImageFloat32> funcBlur = FactoryBlurFilter.gaussian(ImageFloat32.class,sigma,radius);
		ImageGradient<ImageFloat32,ImageFloat32> funcDeriv = FactoryDerivative.three_F32();

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
		ImageFloat32 blur = new ImageFloat32(width,height);
		ImageFloat32 blurDeriv = new ImageFloat32(width,height);
		ImageFloat32 gaussDeriv = new ImageFloat32(width,height);

		BlurStorageFilter<ImageFloat32> funcBlur = FactoryBlurFilter.gaussian(ImageFloat32.class,sigma,radius);
		ImageGradient<ImageFloat32,ImageFloat32> funcDeriv = FactoryDerivative.three_F32();
		ImageGradient<ImageFloat32,ImageFloat32> funcGaussDeriv = FactoryDerivative.gaussian_F32(sigma,radius);

		funcBlur.process(input,blur);
		funcDeriv.process(blur,blurDeriv,derivY);

		funcGaussDeriv.process(input,gaussDeriv,derivY);

		printIntensity("Blur->Deriv",blurDeriv);
		printIntensity("Gauss Deriv",gaussDeriv);
	}

	public void derivByGaussDeriv() {
		System.out.println("DxG*I");
		ImageFloat32 blurDeriv = new ImageFloat32(width,height);

		for( int level = 1; level <= 3; level++ ) {
			ImageGradient<ImageFloat32,ImageFloat32> funcGaussDeriv = FactoryDerivative.gaussian_F32(level,-1);
			funcGaussDeriv.process(input,blurDeriv,derivY);

			printIntensity("Sigma "+level,blurDeriv);
		}
	}

	public void derivByBlurThenDeriv() {
		System.out.println("Dx*(G*I)");
		ImageGradient<ImageFloat32,ImageFloat32> funcDeriv = FactoryDerivative.three_F32();
		ImageFloat32 blur = new ImageFloat32(width,height);
		ImageFloat32 blurDeriv = new ImageFloat32(width,height);

		for( int sigma = 1; sigma <= 3; sigma++ ) {
			int radius = FactoryKernelGaussian.radiusForSigma(sigma,1);
			BlurStorageFilter<ImageFloat32> funcBlur = FactoryBlurFilter.gaussian(ImageFloat32.class,sigma,radius);
			funcBlur.process(input,blur);
			funcDeriv.process(blur,blurDeriv,derivY);

			printIntensity("Sigma "+sigma,blurDeriv);
		}
	}

	public void derivByDerivThenBlur() {
		System.out.println("G*(Dx*I)");
		ImageGradient<ImageFloat32,ImageFloat32> funcDeriv = FactoryDerivative.three_F32();
		ImageFloat32 blur = new ImageFloat32(width,height);
		ImageFloat32 deriv = new ImageFloat32(width,height);

		for( int sigma = 1; sigma <= 3; sigma++ ) {
			int radius = FactoryKernelGaussian.radiusForSigma(sigma,1);
			BlurStorageFilter<ImageFloat32> funcBlur = FactoryBlurFilter.gaussian(ImageFloat32.class,sigma,radius);
			funcDeriv.process(input,deriv,derivY);
			funcBlur.process(deriv,blur);

			printIntensity("Sigma "+sigma,blur);
		}
	}

	public void derivByGaussThenGausDeriv() {
		System.out.println("DxG*(G*I)");
		ImageFloat32 blur = new ImageFloat32(width,height);
		ImageFloat32 blurDeriv = new ImageFloat32(width,height);

		for( int sigma = 1; sigma <= 3; sigma++ ) {
			int radius = FactoryKernelGaussian.radiusForSigma(sigma,1);
			ImageGradient<ImageFloat32,ImageFloat32> funcGaussDeriv = FactoryDerivative.gaussian_F32(sigma,radius);
			BlurStorageFilter<ImageFloat32> funcBlur = FactoryBlurFilter.gaussian(ImageFloat32.class,sigma,radius);
			funcBlur.process(input,blur);
			funcGaussDeriv.process(blur,blurDeriv,derivY);

			printIntensity("Sigma "+sigma,blurDeriv);
		}
	}

	public void derivByGaussGausThenDeriv() {
		System.out.println("Dx*(G*(G*I))");
		ImageGradient<ImageFloat32,ImageFloat32> funcDeriv = FactoryDerivative.three_F32();
		ImageFloat32 blur = new ImageFloat32(width,height);
		ImageFloat32 blur2 = new ImageFloat32(width,height);
		ImageFloat32 blurDeriv = new ImageFloat32(width,height);

		for( int sigma = 1; sigma <= 3; sigma++ ) {
			int radius = FactoryKernelGaussian.radiusForSigma(sigma,1);
			BlurStorageFilter<ImageFloat32> funcBlur = FactoryBlurFilter.gaussian(ImageFloat32.class,sigma,radius);

			funcBlur.process(input,blur);
			funcBlur.process(blur,blur2);

			funcDeriv.process(blur2,blurDeriv,derivY);

			printIntensity("Sigma "+sigma,blurDeriv);
		}
	}

	public static void main( String args[] ) {
		EdgeIntensitiesApp app = new EdgeIntensitiesApp();
		app.init();

		// see how similar the result is if the order in which the operations is done is swapped
//		app.convolveDerivOrder();
//		app.gaussianDerivToDirectDeriv();

		// The derivative's magnitude should be proportional to 1/sigma
		app.derivByBlurThenDeriv();
		System.out.println("-----");
		app.derivByDerivThenBlur();
		System.out.println("-----");
		app.derivByGaussDeriv();
		System.out.println("-----");
		app.derivByGaussThenGausDeriv();
		System.out.println("-----");
		app.derivByGaussGausThenDeriv();
	}

}
