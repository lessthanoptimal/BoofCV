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

package boofcv.alg.filter.kernel;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.PixelMath;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.SelectAlgorithmPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.image.ImageFloat32;

import java.awt.*;
import java.awt.image.BufferedImage;


/**
 * Displays the Gaussian kernel and its derivatives.
 *
 * @author Peter Abeles
 */
public class DisplayGaussianKernelApp extends SelectAlgorithmPanel {
	int imageSize = 400;

	ImageFloat32 largeImg = new ImageFloat32(imageSize,imageSize);

	ListDisplayPanel panel = new ListDisplayPanel();

	public DisplayGaussianKernelApp() {
		addAlgorithm("Gaussian",new DerivType(0,0));
		addAlgorithm("Deriv X",new DerivType(1,0));
		addAlgorithm("Deriv XX",new DerivType(2,0));
		addAlgorithm("Deriv XXX",new DerivType(3,0));
		addAlgorithm("Deriv XXXX",new DerivType(4,0));
		addAlgorithm("Deriv XY",new DerivType(1,1));
		addAlgorithm("Deriv XXY",new DerivType(2,1));
		addAlgorithm("Deriv XYY",new DerivType(1,2));
		addAlgorithm("Deriv XXXY",new DerivType(3,1));
		addAlgorithm("Deriv XXYY",new DerivType(2,2));
		addAlgorithm("Deriv XYYY",new DerivType(1,3));

		add(panel, BorderLayout.CENTER);
		setPreferredSize(new Dimension(imageSize+50,imageSize+20));
	}

	@Override
	public void setActiveAlgorithm(String name, Object cookie) {
		DerivType type = (DerivType)cookie;
		panel.reset();

		for( int radius = 1; radius <= 40; radius += 2 ) {

			int maxOrder = Math.max(type.orderX,type.orderY);
			double sigma = FactoryKernelGaussian.sigmaForRadius(radius,maxOrder);
			Kernel1D_F32 kerX =  FactoryKernelGaussian.derivativeK(Kernel1D_F32.class,type.orderX,sigma,radius);
			Kernel1D_F32 kerY = FactoryKernelGaussian.derivativeK(Kernel1D_F32.class,type.orderY,sigma,radius);
			Kernel2D_F32 kernel = KernelMath.convolve(kerY,kerX);

			ImageFloat32 smallImg = KernelMath.convertToImage(kernel);
			DistortImageOps.scale(smallImg,largeImg, TypeInterpolate.NEAREST_NEIGHBOR);

			float maxValue = PixelMath.maxAbs(largeImg);
			BufferedImage out = VisualizeImageData.colorizeSign(largeImg,null,maxValue);

			panel.addImage(out,String.format("%5d",radius));
		}
	}

	public static class DerivType {
		int orderX;
		int orderY;

		public DerivType(int orderX, int orderY) {
			this.orderX = orderX;
			this.orderY = orderY;
		}
	}

	public static void main( String args[] ) {
		DisplayGaussianKernelApp panel = new DisplayGaussianKernelApp();

		ShowImages.showWindow(panel,"Gaussian Kernels");
	}
}
