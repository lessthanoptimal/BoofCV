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

import gecv.factory.filter.kernel.FactorySteerable;
import gecv.struct.convolve.Kernel2D;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
public class DisplaySteerableGaussianApp <T extends ImageBase, K extends Kernel2D>
	extends DisplaySteerableBase<T,K>
{
	public DisplaySteerableGaussianApp(Class<T> imageType, Class<K> kernelType) {
		super(imageType, kernelType);
	}

	@Override
	protected SteerableKernel<K> createKernel(int orderX, int orderY) {
		return FactorySteerable.gaussian(kernelType,orderX,orderY, -1, radius);
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
