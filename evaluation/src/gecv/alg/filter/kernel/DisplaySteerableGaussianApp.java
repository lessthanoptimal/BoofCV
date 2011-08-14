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

package gecv.alg.filter.kernel;

import gecv.alg.distort.DistortImageOps;
import gecv.alg.interpolate.TypeInterpolate;
import gecv.alg.misc.GPixelMath;
import gecv.core.image.GeneralizedImageOps;
import gecv.gui.ListDisplayPanel;
import gecv.gui.SelectAlgorithmPanel;
import gecv.gui.image.ShowImages;
import gecv.gui.image.VisualizeImageData;
import gecv.struct.convolve.Kernel2D;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;

import java.awt.*;
import java.awt.image.BufferedImage;


/**
 * @author Peter Abeles
 */
public class DisplaySteerableGaussianApp <T extends ImageBase, K extends Kernel2D> {
	private static int imageSize = 400;
	private static int radius = 100;

	Class<T> imageType;
	Class<K> kernelType;

	public DisplaySteerableGaussianApp(Class<T> imageType, Class<K> kernelType) {
		this.imageType = imageType;
		this.kernelType = kernelType;
	}

	public void process() {
		ShowBasis panelBasis = new ShowBasis();
		ShowSteering panelSteering = new ShowSteering();

		ShowImages.showWindow(panelBasis,"Steering Basis");
		ShowImages.showWindow(panelSteering,"Steering");
	}

	public class ShowBasis extends SelectAlgorithmPanel
	{
		T largeImg = GeneralizedImageOps.createImage(imageType,imageSize,imageSize);
		ListDisplayPanel panel = new ListDisplayPanel();

		public ShowBasis() {
			addAlgorithm("Deriv X",new DisplayGaussianKernelApp.DerivType(1,0));
			addAlgorithm("Deriv XX",new DisplayGaussianKernelApp.DerivType(2,0));
			addAlgorithm("Deriv XXX",new DisplayGaussianKernelApp.DerivType(3,0));
			addAlgorithm("Deriv XXXX",new DisplayGaussianKernelApp.DerivType(4,0));
			addAlgorithm("Deriv XY",new DisplayGaussianKernelApp.DerivType(1,1));
			addAlgorithm("Deriv XXY",new DisplayGaussianKernelApp.DerivType(2,1));
			addAlgorithm("Deriv XYY",new DisplayGaussianKernelApp.DerivType(1,2));
			addAlgorithm("Deriv XXXY",new DisplayGaussianKernelApp.DerivType(3,1));
			addAlgorithm("Deriv XXYY",new DisplayGaussianKernelApp.DerivType(2,2));
			addAlgorithm("Deriv XYYY",new DisplayGaussianKernelApp.DerivType(1,3));

			add(panel, BorderLayout.CENTER);
			setPreferredSize(new Dimension(imageSize+50,imageSize+20));
		}

		@Override
		public void setActiveAlgorithm(String name, Object cookie) {
			DisplayGaussianKernelApp.DerivType dt = (DisplayGaussianKernelApp.DerivType)cookie;

			SteerableKernel<K> steerable = FactorySteerable.gaussian(kernelType,dt.orderX,dt.orderY,radius);
			panel.reset();

			for( int i = 0; i < steerable.getBasisSize(); i++ ) {
				T smallImg = GKernelMath.convertToImage(steerable.getBasis(i));
				DistortImageOps.scale(smallImg,largeImg, TypeInterpolate.NEAREST_NEIGHBOR);

				double maxValue = GPixelMath.maxAbs(largeImg);
				BufferedImage out = VisualizeImageData.colorizeSign(largeImg,null,maxValue);
				panel.addImage(out,"Basis "+i);
			}
		}
	}

	public class ShowSteering extends SelectAlgorithmPanel
	{
		T largeImg = GeneralizedImageOps.createImage(imageType,imageSize,imageSize);
		ListDisplayPanel panel = new ListDisplayPanel();

		public ShowSteering() {
			addAlgorithm("Deriv X",new DisplayGaussianKernelApp.DerivType(1,0));
			addAlgorithm("Deriv XX",new DisplayGaussianKernelApp.DerivType(2,0));
			addAlgorithm("Deriv XXX",new DisplayGaussianKernelApp.DerivType(3,0));
			addAlgorithm("Deriv XXXX",new DisplayGaussianKernelApp.DerivType(4,0));
			addAlgorithm("Deriv XY",new DisplayGaussianKernelApp.DerivType(1,1));
			addAlgorithm("Deriv XXY",new DisplayGaussianKernelApp.DerivType(2,1));
			addAlgorithm("Deriv XYY",new DisplayGaussianKernelApp.DerivType(1,2));
			addAlgorithm("Deriv XXXY",new DisplayGaussianKernelApp.DerivType(3,1));
			addAlgorithm("Deriv XXYY",new DisplayGaussianKernelApp.DerivType(2,2));
			addAlgorithm("Deriv XYYY",new DisplayGaussianKernelApp.DerivType(1,3));

			add(panel, BorderLayout.CENTER);
			setPreferredSize(new Dimension(imageSize+50,imageSize+20));
		}

		@Override
		public void setActiveAlgorithm(String name, Object cookie) {
			DisplayGaussianKernelApp.DerivType dt = (DisplayGaussianKernelApp.DerivType)cookie;
			SteerableKernel<K> steerable = FactorySteerable.gaussian(kernelType,dt.orderX,dt.orderY,radius);
			panel.reset();

			for( int i = 0; i <= 20; i++  ) {
				double angle = Math.PI*i/20.0;

				K kernel = steerable.compute(angle);

				T smallImg = GKernelMath.convertToImage(kernel);
				DistortImageOps.scale(smallImg,largeImg, TypeInterpolate.NEAREST_NEIGHBOR);

				double maxValue = GPixelMath.maxAbs(largeImg);
				BufferedImage out = VisualizeImageData.colorizeSign(largeImg,null,maxValue);

				panel.addImage(out,String.format("%5d",(int)(180.0*angle/Math.PI)));
			}
		}
	}


	public static void main( String args[] ) {
		DisplaySteerableGaussianApp<ImageFloat32,Kernel2D_F32> app =
				new DisplaySteerableGaussianApp<ImageFloat32,Kernel2D_F32>(ImageFloat32.class,Kernel2D_F32.class);
		app.process();

//		DisplaySteerableGaussianApp<ImageSInt32, Kernel2D_I32> app =
//				new DisplaySteerableGaussianApp<ImageSInt32,Kernel2D_I32>(ImageSInt32.class,Kernel2D_I32.class);
//		app.process();
	}
}
