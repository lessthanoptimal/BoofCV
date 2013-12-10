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

package boofcv.examples.imageprocessing;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.gui.image.DiscretePyramidPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidDiscrete;

import java.awt.image.BufferedImage;

/**
 * Demonstrates how to construct and display a {@link PyramidDiscrete}.  Discrete pyramids require that
 * each level has a relative scale with an integer ratio and is updated by sparsely sub-sampling.  These
 * restrictions allows a very quick update across scale space.
 *
 * @author Peter Abeles
 */
public class ExamplePyramidDiscrete<T extends ImageSingleBand> {

	// specifies the image type
	Class<T> imageType;
	// The pyramid data structure
	PyramidDiscrete<T> pyramid;

	public ExamplePyramidDiscrete(Class<T> imageType) {
		this.imageType = imageType;
	}

	/**
	 * Creates a fairly standard pyramid and updater.
	 */
	public void standard() {
		// Each level in the pyramid must have a ratio with the previously layer that is an integer value
		pyramid = FactoryPyramid.discreteGaussian(new int[]{1,2,4,8},-1,2,true,imageType);
	}

	/**
	 * Creates a more unusual pyramid and updater.
	 */
	public void unusual() {
		// Note that the first level does not have to be one
		pyramid = FactoryPyramid.discreteGaussian(new int[]{2,6},-1,2,true,imageType);

		// Other kernels can also be used besides Gaussian
		Kernel1D kernel;
		if(GeneralizedImageOps.isFloatingPoint(imageType) ) {
			kernel = FactoryKernel.table1D_F32(2,true);
		} else {
			kernel = FactoryKernel.table1D_I32(2);
		}
	}

	/**
	 * Updates and displays the pyramid.
	 */
	public void process( BufferedImage image ) {
		T input = ConvertBufferedImage.convertFromSingle(image, null, imageType);
		pyramid.process(input);

		DiscretePyramidPanel gui = new DiscretePyramidPanel();
		gui.setPyramid(pyramid);
		gui.render();

		ShowImages.showWindow(gui,"Image Pyramid");

		// To get an image at any of the scales simply call this get function
		T imageAtScale = pyramid.getLayer(1);

		ShowImages.showWindow(ConvertBufferedImage.convertTo(imageAtScale,null,true),"Image at layer 1");
	}

	public static void main( String[] args ) {
		BufferedImage image = UtilImageIO.loadImage("../data/evaluation/standard/barbara.png");

		ExamplePyramidDiscrete<ImageFloat32> app = new ExamplePyramidDiscrete<ImageFloat32>(ImageFloat32.class);
//		ExamplePyramidDiscrete<ImageUInt8> app = new ExamplePyramidDiscrete<ImageUInt8>(ImageUInt8.class);

		app.standard();
//		app.unusual();
		app.process(image);
	}
}
