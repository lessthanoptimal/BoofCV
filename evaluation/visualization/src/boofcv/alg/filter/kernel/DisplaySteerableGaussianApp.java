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

package boofcv.alg.filter.kernel;

import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.factory.filter.kernel.FactorySteerable;
import boofcv.gui.image.ShowImages;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;

import java.awt.*;


/**
 * @author Peter Abeles
 */
public class DisplaySteerableGaussianApp <T extends ImageSingleBand, K extends Kernel2D>
	extends DisplaySteerableBase<T,K>
{
	public DisplaySteerableGaussianApp(Class<T> imageType) {
		super(imageType, (Class)FactoryKernel.getKernelType(imageType,2));
	}

	@Override
	protected SteerableKernel<K> createKernel(int orderX, int orderY) {
		return FactorySteerable.gaussian(kernelType,orderX,orderY, -1, radius);
	}

	public static void main( String args[] ) {
//		DisplaySteerableGaussianApp<ImageFloat32,Kernel2D_F32> app =
//				new DisplaySteerableGaussianApp<ImageFloat32,Kernel2D_F32>(ImageFloat32.class);

		DisplaySteerableGaussianApp<ImageSInt32, Kernel2D_I32> app =
				new DisplaySteerableGaussianApp<ImageSInt32,Kernel2D_I32>(ImageSInt32.class);

		app.setPreferredSize(new Dimension(1000,480));

		ShowImages.showWindow(app,"Steerable Gaussian Kernels");
	}
}
