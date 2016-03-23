/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.filter.convolve;

import boofcv.abst.filter.convolve.ImageConvolveSparse;
import boofcv.alg.filter.convolve.ConvolveWithBorderSparse;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.struct.convolve.*;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.ImageGray;

/**
 * Factory for creating sparse convolutions.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryConvolveSparse {

	public static <T extends ImageGray, K extends Kernel2D>
	ImageConvolveSparse<T,K> convolve2D(Class<T> imageType, K kernel) {
		if( GeneralizedImageOps.isFloatingPoint(imageType)) {
			return (ImageConvolveSparse<T,K>)new Convolve2D_F32((Kernel2D_F32)kernel);
		} else {
			return (ImageConvolveSparse<T,K>)new Convolve2D_I32((Kernel2D_I32)kernel);
		}
	}

	public static <T extends ImageGray, K extends Kernel1D>
	ImageConvolveSparse<T,K> vertical1D(Class<T> imageType, K kernel) {
		if( GeneralizedImageOps.isFloatingPoint(imageType)) {
			return (ImageConvolveSparse<T,K>)new Vertical1D_F32((Kernel1D_F32)kernel);
		} else {
			return (ImageConvolveSparse<T,K>)new Vertical1D_I32((Kernel1D_I32)kernel);
		}
	}

	public static <T extends ImageGray, K extends Kernel1D>
	ImageConvolveSparse<T,K> horizontal1D(Class<T> imageType, K kernel) {
		if( GeneralizedImageOps.isFloatingPoint(imageType)) {
			return (ImageConvolveSparse<T,K>)new Horizontal1D_F32((Kernel1D_F32)kernel);
		} else {
			return (ImageConvolveSparse<T,K>)new Horizontal1D_I32((Kernel1D_I32)kernel);
		}
	}

	public static class Convolve2D_F32 extends ImageConvolveSparse<GrayF32, Kernel2D_F32> {

		public Convolve2D_F32(Kernel2D_F32 kernel) {
			super(kernel);
		}

		@Override
		public double compute(int x, int y) {
			return ConvolveWithBorderSparse.convolve(kernel,(ImageBorder_F32)image,x,y);
		}
	}

	public static class Convolve2D_I32 extends ImageConvolveSparse<GrayI, Kernel2D_I32> {

		public Convolve2D_I32(Kernel2D_I32 kernel) {
			super(kernel);
		}

		@Override
		public double compute(int x, int y) {
			return ConvolveWithBorderSparse.convolve(kernel,(ImageBorder_S32)image,x,y);
		}
	}

	public static class Horizontal1D_F32 extends ImageConvolveSparse<GrayF32, Kernel1D_F32> {

		public Horizontal1D_F32(Kernel1D_F32 kernel) {
			super(kernel);
		}

		@Override
		public double compute(int x, int y) {
			return ConvolveWithBorderSparse.horizontal(kernel, (ImageBorder_F32) image, x, y);
		}
	}

	public static class Horizontal1D_I32 extends ImageConvolveSparse<GrayF32, Kernel1D_I32> {

		public Horizontal1D_I32(Kernel1D_I32 kernel) {
			super(kernel);
		}

		@Override
		public double compute(int x, int y) {
			return ConvolveWithBorderSparse.horizontal(kernel, (ImageBorder_S32) image, x, y);
		}
	}

	public static class Vertical1D_F32 extends ImageConvolveSparse<GrayF32, Kernel1D_F32> {

		public Vertical1D_F32(Kernel1D_F32 kernel) {
			super(kernel);
		}

		@Override
		public double compute(int x, int y) {
			return ConvolveWithBorderSparse.vertical(kernel, (ImageBorder_F32) image, x, y);
		}
	}

	public static class Vertical1D_I32 extends ImageConvolveSparse<GrayF32, Kernel1D_I32> {

		public Vertical1D_I32(Kernel1D_I32 kernel) {
			super(kernel);
		}

		@Override
		public double compute(int x, int y) {
			return ConvolveWithBorderSparse.vertical(kernel,(ImageBorder_S32)image,x,y);
		}
	}
}
