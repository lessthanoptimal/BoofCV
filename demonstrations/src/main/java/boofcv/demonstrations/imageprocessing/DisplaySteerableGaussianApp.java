/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.imageprocessing;

import boofcv.alg.filter.kernel.SteerableKernel;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.factory.filter.kernel.FactorySteerable;
import boofcv.gui.image.ShowImages;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.convolve.Kernel2D_S32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageGray;

import java.awt.*;


/**
 * Visualizes steerable kernels.
 *
 * @author Peter Abeles
 */
public class DisplaySteerableGaussianApp <T extends ImageGray<T>, K extends Kernel2D>
	extends DisplaySteerableBase<T,K>
{
	public DisplaySteerableGaussianApp(Class<T> imageType) {
		super(imageType, (Class)FactoryKernel.getKernelType(imageType,2));
	}

	@Override
	protected SteerableKernel<K> createKernel(int orderX, int orderY) {
		return FactorySteerable.gaussian(kernelType,orderX,orderY, -1, radius);
	}

	public static void main( String[] args ) {
//		DisplaySteerableGaussianApp<GrayF32,Kernel2D_F32> app =
//				new DisplaySteerableGaussianApp<GrayF32,Kernel2D_F32>(GrayF32.class);

		DisplaySteerableGaussianApp<GrayS32, Kernel2D_S32> app =
				new DisplaySteerableGaussianApp<>(GrayS32.class);

		app.setPreferredSize(new Dimension(1000,480));

		ShowImages.showWindow(app,"Steerable Gaussian Kernels", true);
	}
}
