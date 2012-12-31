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

package boofcv.factory.filter.convolve;

import boofcv.abst.filter.convolve.ImageConvolveSparse;
import boofcv.alg.filter.convolve.ConvolveWithBorderSparse;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_I32;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageInteger;
import boofcv.struct.image.ImageSingleBand;

/**
 * Factory for creating sparse convolutions.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryConvolveSparse {

	public static <T extends ImageSingleBand, K extends Kernel2D>
	ImageConvolveSparse<T,K> create( Class<T> imageType , K kernel ) {
		if( GeneralizedImageOps.isFloatingPoint(imageType)) {
			return (ImageConvolveSparse<T,K>)new Convolve_F32((Kernel2D_F32)kernel);
		} else {
			return (ImageConvolveSparse<T,K>)new Convolve_I((Kernel2D_I32)kernel);
		}
	}

	public static <T extends ImageSingleBand, K extends Kernel2D>
	ImageConvolveSparse<T,K> create( Class<T> imageType ) {
		if( GeneralizedImageOps.isFloatingPoint(imageType)) {
			return (ImageConvolveSparse<T,K>)new Convolve_F32(null);
		} else {
			return (ImageConvolveSparse<T,K>)new Convolve_I(null);
		}
	}

	public static class Convolve_F32 extends ImageConvolveSparse<ImageFloat32, Kernel2D_F32> {

		public Convolve_F32(Kernel2D_F32 kernel) {
			super(kernel);
		}

		@Override
		public double compute(int x, int y) {
			return ConvolveWithBorderSparse.convolve(kernel,(ImageBorder_F32)image,x,y);
		}
	}

	public static class Convolve_I extends ImageConvolveSparse<ImageInteger, Kernel2D_I32> {

		public Convolve_I(Kernel2D_I32 kernel) {
			super(kernel);
		}

		@Override
		public double compute(int x, int y) {
			return ConvolveWithBorderSparse.convolve(kernel,(ImageBorder_I32)image,x,y);
		}
	}
}
