/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.blur;

import boofcv.alg.filter.blur.impl.ImplMedianSortNaive;
import boofcv.alg.filter.convolve.GConvolveImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.IWorkArrays;
import boofcv.concurrency.WorkArrays;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.image.*;
import boofcv.testing.BoofTesting;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestBlurImageOps {
	// TODO full support of interleaved image for all types
	// TODO unit tests for different kernels along x and y axis. Can't do that just yet because
	//      2D kernels have to have the same width along each axis!

	Random rand = new Random(234);

	int width = 15;
	int height = 20;

	ImageType imageTypes[] = new ImageType[]{
			ImageType.single(GrayU8.class),ImageType.single(GrayF32.class),
			ImageType.pl(2,GrayU8.class),ImageType.pl(2,GrayF32.class)};
//			ImageType.il(2,InterleavedU8.class),ImageType.il(2,InterleavedF32.class)}; TODO add in future

	ImageType imageTypesGaussian[] = new ImageType[]{
			ImageType.single(GrayU8.class),ImageType.single(GrayF32.class),
			ImageType.pl(2,GrayU8.class),ImageType.pl(2,GrayF32.class),
			ImageType.il(2,InterleavedU8.class),ImageType.il(2,InterleavedF32.class)}; // delete after all support interleaved

	@Test
	public void mean() {
		for( ImageType type : imageTypes ) {
			ImageBase input = type.createImage(width,height);
			ImageBase found = type.createImage(width,height);
			ImageBase expected = type.createImage(width,height);

			GImageMiscOps.fillUniform(input, rand, 0, 20);

			for( int radius = 1; radius <= 4; radius++ ) {
				GImageMiscOps.fill(expected,0);
				GImageMiscOps.fill(found,0);

				int w = radius*2+1;

				// convolve with a kernel to compute the expected value
				Kernel2D kernel = FactoryKernel.createKernelForImage(w,w/2,2,type.getDataType());
				FactoryKernel.setTable(kernel);
				GConvolveImageOps.convolveNormalized(kernel, input, expected);
				try {
					Class storage = type.getFamily() == ImageType.Family.PLANAR ?
							ImageGray.class : input.getClass();
					Class work = GeneralizedImageOps.createWorkArray(type).getClass();
					if( type.getFamily() == ImageType.Family.PLANAR )
						work = WorkArrays.class;

					Method m = BlurImageOps.class.getMethod(
							"mean",input.getClass(), found.getClass(), int.class, storage,work);

					m.invoke(null,input,found, radius, null, null);
					BoofTesting.assertEquals(expected,found,2);
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Test
	public void gaussian() {
		for( ImageType type : imageTypesGaussian ) {
			ImageBase input = type.createImage(width,height);
			ImageBase found = type.createImage(width,height);
			ImageBase expected = type.createImage(width,height);

			GImageMiscOps.fillUniform(input, rand, 0, 20);

			for( int radius = 1; radius <= 4; radius++ ) {
				GImageMiscOps.fill(expected,0);
				GImageMiscOps.fill(found,0);

				// convolve with a kernel to compute the expected value
				Kernel2D kernel = FactoryKernelGaussian.gaussian2D(type.getDataType(),-1,radius);
				GConvolveImageOps.convolveNormalized(kernel, input, expected);
				try {
					Class storage = type.getFamily() == ImageType.Family.PLANAR ?
							ImageGray.class : input.getClass();

					Method m = BlurImageOps.class.getMethod(
							"gaussian",input.getClass(), found.getClass(), double.class , int.class, storage);

					m.invoke(null,input,found, -1, radius, null);
					BoofTesting.assertEquals(expected,found,2);
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Test
	public void median() {
		for( ImageType type : imageTypes ) {
			ImageBase input = type.createImage(width, height);
			ImageBase found = type.createImage(width, height);
			ImageBase expected = type.createImage(width, height);

			GImageMiscOps.fillUniform(input, rand, 0, 20);

			for( int radius = 1; radius <= 4; radius++ ) {
				try {
					if( type.getFamily() == ImageType.Family.PLANAR ) {
						Method m = BlurImageOps.class.getMethod("median", input.getClass(),
								found.getClass(), int.class, WorkArrays.class);
						m.invoke(null, input, found, radius, null);
					} else if( type.getDataType().isInteger() ) {
						Method m = BlurImageOps.class.getMethod("median", input.getClass(),
								found.getClass(), int.class, IWorkArrays.class);
						m.invoke(null, input, found, radius, null);
					} else {
						Method m = BlurImageOps.class.getMethod("median", input.getClass(),
								found.getClass(), int.class);
						m.invoke(null, input, found, radius);
					}
					Class image = type.getFamily() == ImageType.Family.PLANAR ? Planar.class : ImageGray.class;

					Method m = ImplMedianSortNaive.class.getMethod("process",image,image, int.class);
					m.invoke(null,input,expected, radius);

					BoofTesting.assertEquals(expected,found,2);
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
