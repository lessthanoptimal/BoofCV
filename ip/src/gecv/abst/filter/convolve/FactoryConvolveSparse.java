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
import gecv.core.image.GeneralizedImageOps;
import gecv.core.image.border.ImageBorder;
import gecv.core.image.border.ImageBorder_F32;
import gecv.core.image.border.ImageBorder_I32;
import gecv.struct.convolve.Kernel2D;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInteger;

/**
 * Factory for creating sparse convolutions.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryConvolveSparse {

	public static <T extends ImageBase, K extends Kernel2D, B extends ImageBorder<T>>
		ImageConvolveSparse<T,K> create( Class<T> imageType ) {
		if( GeneralizedImageOps.isFloatingPoint(imageType)) {
			return (ImageConvolveSparse<T,K>)new Convolve_F32();
		} else {
			return (ImageConvolveSparse<T,K>)new Convolve_I();
		}
	}

	public static class Convolve_F32 extends ImageConvolveSparse<ImageFloat32, Kernel2D_F32> {
		@Override
		public double compute(int x, int y) {
			return ConvolveWithBorderSparse.convolve(kernel,(ImageBorder_F32)image,x,y);
		}
	}

	public static class Convolve_I extends ImageConvolveSparse<ImageInteger, Kernel2D_I32> {
		@Override
		public double compute(int x, int y) {
			return ConvolveWithBorderSparse.convolve(kernel,(ImageBorder_I32)image,x,y);
		}
	}
}
