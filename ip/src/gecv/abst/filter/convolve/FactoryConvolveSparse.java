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

package gecv.abst.filter.convolve;

import gecv.alg.filter.convolve.ConvolveWithBorderSparse;
import gecv.core.image.border.ImageBorder_F32;
import gecv.core.image.border.ImageBorder_I32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInteger;

/**
 * Factory for creating sparse convolutions.
 *
 * @author Peter Abeles
 */
public class FactoryConvolveSparse {

	public static Convolve_F32 create_F32() {
		return new Convolve_F32();
	}

	public static Convolve_I create_I() {
		return new Convolve_I();
	}

	public static class Convolve_F32 extends ImageConvolveSparse<ImageFloat32, Kernel2D_F32, ImageBorder_F32> {
		@Override
		public double compute(int x, int y) {
			return ConvolveWithBorderSparse.convolve(kernel,image,x,y);
		}
	}

	public static class Convolve_I extends ImageConvolveSparse<ImageInteger, Kernel2D_I32, ImageBorder_I32> {
		@Override
		public double compute(int x, int y) {
			return ConvolveWithBorderSparse.convolve(kernel,image,x,y);
		}
	}
}
