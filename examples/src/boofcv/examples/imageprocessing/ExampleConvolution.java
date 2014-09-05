/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.imageprocessing;

import boofcv.alg.filter.convolve.GConvolveImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * Several examples demonstrating convolution.
 *
 * @author Peter Abeles
 */
public class ExampleConvolution {

	public static void main(String[] args) {
		BufferedImage image = UtilImageIO.loadImage("../data/applet/sunflowers.jpg");

		ImageUInt8 gray = ConvertBufferedImage.convertFromSingle(image, null, ImageUInt8.class);

		convolve1D(gray);
		convolve2D(gray);
		normalize2D(gray);
	}

	/**
	 * Convolves a 1D kernel horizontally and vertically
	 */
	private static void convolve1D(ImageUInt8 gray) {
		ImageBorder<ImageUInt8> border = FactoryImageBorder.general(gray, BorderType.EXTENDED);
		Kernel1D_I32 kernel = new Kernel1D_I32(2);
		kernel.offset = 1; // specify the kernel's origin
		kernel.data[0] = 1;
		kernel.data[1] = -1;

		ImageSInt16 output = new ImageSInt16(gray.width,gray.height);

		GConvolveImageOps.horizontal(kernel, gray, output, border);
		ShowImages.showWindow(VisualizeImageData.standard(output, null), "1D Horizontal");

		GConvolveImageOps.vertical(kernel, gray, output, border);
		ShowImages.showWindow(VisualizeImageData.standard(output, null), "1D Vertical");
	}

	/**
	 * Convolves a 2D kernel
	 */
	private static void convolve2D(ImageUInt8 gray) {
		// By default 2D kernels will be centered around width/2
		Kernel2D_I32 kernel = new Kernel2D_I32(3);
		kernel.set(1,0,2);
		kernel.set(2,1,2);
		kernel.set(0,1,-2);
		kernel.set(1,2,-2);

		// Output needs to handle the increased domain after convolution.  Can't be 8bit
		ImageSInt16 output = new ImageSInt16(gray.width,gray.height);
		ImageBorder<ImageUInt8> border = FactoryImageBorder.general(gray, BorderType.EXTENDED);

		GConvolveImageOps.convolve(kernel, gray, output, border);
		ShowImages.showWindow(VisualizeImageData.standard(output, null), "2D Kernel");
	}

	/**
	 * Convolves a 2D normalized kernel.  This kernel is divided by its sum after computation.
	 */
	private static void normalize2D(ImageUInt8 gray) {
		// Create a Gaussian kernel with radius of 3
		Kernel2D_I32 kernel = FactoryKernelGaussian.gaussian2D(ImageUInt8.class, -1, 3);
		// Note that there is a more efficient way to compute this convolution since it is a separable kernel
		// just use BlurImageOps instead.

		// Since it's normalized it can be saved inside an 8bit image
		ImageUInt8 output = new ImageUInt8(gray.width,gray.height);

		GConvolveImageOps.convolveNormalized(kernel, gray, output);
		ShowImages.showWindow(VisualizeImageData.standard(output, null), "2D Normalized Kernel");
	}
}
