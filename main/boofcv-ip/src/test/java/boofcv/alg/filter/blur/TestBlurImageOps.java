/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.BoofTesting;
import boofcv.alg.filter.blur.impl.ImplMedianSortNaive;
import boofcv.alg.filter.convolve.GConvolveImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;
import pabeles.concurrency.GrowArray;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("rawtypes") class TestBlurImageOps extends BoofStandardJUnit {
	// TODO full support of interleaved image for all types
	// TODO unit tests for different kernels along x and y axis. Can't do that just yet because
	//      2D kernels have to have the same width along each axis!

	int width = 15;
	int height = 20;

	ImageType[] imageTypes = new ImageType[]{
			ImageType.single(GrayU8.class), ImageType.single(GrayF32.class),
			ImageType.pl(2, GrayU8.class), ImageType.pl(2, GrayF32.class)};
//			ImageType.il(2,InterleavedU8.class),ImageType.il(2,InterleavedF32.class)}; TODO add in future

	ImageType[] imageTypesGaussian = new ImageType[]{
			ImageType.single(GrayU8.class), ImageType.single(GrayF32.class),
			ImageType.pl(2, GrayU8.class), ImageType.pl(2, GrayF32.class),
			ImageType.il(2, InterleavedU8.class), ImageType.il(2, InterleavedF32.class)}; // delete after all support interleaved

	@Test
	void mean() {
		for (ImageType type : imageTypes) {
			ImageBase input = type.createImage(width, height);
			ImageBase found = type.createImage(width, height);
			ImageBase expected = type.createImage(width, height);

			GImageMiscOps.fillUniform(input, rand, 0, 20);

			for (int radius = 1; radius <= 4; radius++) {
				GImageMiscOps.fill(expected, 0);
				GImageMiscOps.fill(found, 0);

				int w = radius*2 + 1;

				// convolve with a kernel to compute the expected value
				Kernel2D kernel = FactoryKernel.createKernelForImage(w, w/2, 2, type.getDataType());
				FactoryKernel.setTable(kernel);
				GConvolveImageOps.convolveNormalized(kernel, input, expected);
				try {
					Class storage = type.getFamily() == ImageType.Family.PLANAR ?
							ImageGray.class : input.getClass();
					Class work = GeneralizedImageOps.createGrowArray(type).getClass();
					if (type.getFamily() == ImageType.Family.PLANAR) {
						work = GrowArray.class;
						Method m = BlurImageOps.class.getMethod(
								"mean", input.getClass(), found.getClass(), int.class, storage, work);
						m.invoke(null, input, found, radius, null, null);
					} else {
						Method m = BlurImageOps.class.getMethod(
								"mean", input.getClass(), found.getClass(), int.class, storage, work);
						m.invoke(null, input, found, radius, null, null);
					}

					BoofTesting.assertEquals(expected, found, 2);
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Test
	void meanBorder() {
		for (ImageType type : imageTypes) {
			if (type.getFamily() == ImageType.Family.INTERLEAVED)
				continue;
			ImageBase input = type.createImage(width, height);
			ImageBase found = type.createImage(width, height);
			ImageBase expected = type.createImage(width, height);

			GImageMiscOps.fillUniform(input, rand, 0, 20);

			ImageBorder border = FactoryImageBorder.generic(BorderType.REFLECT, input.getImageType());

			for (int radius = 1; radius <= 4; radius++) {
				GImageMiscOps.fill(expected, 0);
				GImageMiscOps.fill(found, 0);

				int w = radius*2 + 1;

				// convolve with a kernel to compute the expected value
				Kernel2D kernel = FactoryKernel.createKernelForImage(w, w/2, 2, type.getDataType());
				FactoryKernel.setTable(kernel);
				GConvolveImageOps.convolveNormalized(kernel, input, expected, border);
				Class storage = type.getFamily() == ImageType.Family.PLANAR ?
						ImageGray.class : input.getClass();
				Class work = GeneralizedImageOps.createGrowArray(type).getClass();
				Class borderType = ImageBorder.class;
				if( type.getFamily() == ImageType.Family.GRAY ) {
					switch( type.getDataType() ) {
						case U8: borderType = ImageBorder_S32.class; break;
						case F32: borderType = ImageBorder_F32.class; break;
						default: break;
					}
				}
				try {
					// Compare with image border
					if (type.getFamily() == ImageType.Family.PLANAR) {
						work = GrowArray.class;
						Method m = BlurImageOps.class.getMethod(
								"meanB", input.getClass(), found.getClass(), int.class, int.class, borderType, storage, work);
						m.invoke(null, input, found, radius, radius, border, null, null);
					} else {
						Method m = BlurImageOps.class.getMethod(
								"meanB", input.getClass(), found.getClass(), int.class, int.class, borderType, storage, work);
						m.invoke(null, input, found, radius, radius, border, null, null);
					}

					BoofTesting.assertEquals(expected, found, 2);

					// Test will null border
					GImageMiscOps.fill(found, 0); // zero the image
					if (type.getFamily() == ImageType.Family.PLANAR) {
						work = GrowArray.class;
						Method m = BlurImageOps.class.getMethod(
								"meanB", input.getClass(), found.getClass(), int.class, int.class, borderType, storage, work);
						m.invoke(null, input, found, radius, radius, border, null, null);
					} else {
						Method m = BlurImageOps.class.getMethod(
								"meanB", input.getClass(), found.getClass(), int.class, int.class, borderType, storage, work);
						m.invoke(null, input, found, radius, radius, border, null, null);
					}
					// Inner should be the same
					BoofTesting.assertEqualsInner(expected, found, 2, radius, radius, false);
					// outer should be zeros
					// When a new image is created it is filled with zeros. That's why this test works
					BoofTesting.assertEqualsBorder(expected.createSameShape(), found, 2, radius, radius);
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Test
	void gaussian() {
		for (ImageType type : imageTypesGaussian) {
			ImageBase input = type.createImage(width, height);
			ImageBase found = type.createImage(width, height);
			ImageBase expected = type.createImage(width, height);

			GImageMiscOps.fillUniform(input, rand, 0, 20);

			for (int radius = 1; radius <= 4; radius++) {
				GImageMiscOps.fill(expected, 0);
				GImageMiscOps.fill(found, 0);

				// convolve with a kernel to compute the expected value
				Kernel2D kernel = FactoryKernelGaussian.gaussian2D(type.getDataType(), -1, radius);
				GConvolveImageOps.convolveNormalized(kernel, input, expected);
				try {
					Class storage = type.getFamily() == ImageType.Family.PLANAR ?
							ImageGray.class : input.getClass();

					Method m = BlurImageOps.class.getMethod(
							"gaussian", input.getClass(), found.getClass(), double.class, int.class, storage);

					m.invoke(null, input, found, -1, radius, null);
					BoofTesting.assertEquals(expected, found, 2);
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Test
	void median() {
		for (ImageType type : imageTypes) {
			ImageBase input = type.createImage(width, height);
			ImageBase found = type.createImage(width, height);
			ImageBase expected = type.createImage(width, height);

			GImageMiscOps.fillUniform(input, rand, 0, 20);

			for (int radiusX = 1; radiusX <= 4; radiusX++) {
				int radiusY = radiusX + 1;
				try {
					if (type.getFamily() == ImageType.Family.PLANAR) {
						Method m = BlurImageOps.class.getMethod("median", input.getClass(),
								found.getClass(), int.class, int.class, GrowArray.class);
						m.invoke(null, input, found, radiusX, radiusY, null);
					} else if (type.getDataType().isInteger()) {
						Method m = BlurImageOps.class.getMethod("median", input.getClass(),
								found.getClass(), int.class, int.class, GrowArray.class);
						m.invoke(null, input, found, radiusX, radiusY, null);
					} else {
						Method m = BlurImageOps.class.getMethod("median", input.getClass(),
								found.getClass(), int.class, int.class, GrowArray.class);
						m.invoke(null, input, found, radiusX, radiusY, null);
					}
					Class image = type.getFamily() == ImageType.Family.PLANAR ? Planar.class : ImageGray.class;

					Method m = ImplMedianSortNaive.class.getMethod("process", image, image,
							int.class, int.class, GrowArray.class);
					m.invoke(null, input, expected, radiusX, radiusY, null);

					BoofTesting.assertEquals(expected, found, 2);
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
