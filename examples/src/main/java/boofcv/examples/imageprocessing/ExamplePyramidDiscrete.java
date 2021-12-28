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

package boofcv.examples.imageprocessing;

import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.gui.image.DiscretePyramidPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import boofcv.struct.pyramid.PyramidDiscrete;

import java.awt.image.BufferedImage;

/**
 * Demonstrates how to construct and display a {@link PyramidDiscrete}. Discrete pyramids require that
 * each level has a relative scale with an integer ratio and is updated by sparsely sub-sampling. These
 * restrictions allows a very quick update across scale space.
 *
 * @author Peter Abeles
 */
public class ExamplePyramidDiscrete<T extends ImageGray<T>> {

	// specifies the image type
	Class<T> imageType;

	public ExamplePyramidDiscrete( Class<T> imageType ) {
		this.imageType = imageType;
	}

	/**
	 * Updates and displays the pyramid.
	 */
	public void process( BufferedImage image ) {
		T input = ConvertBufferedImage.convertFromSingle(image, null, imageType);
		PyramidDiscrete<T> pyramid = FactoryPyramid.discreteGaussian(
				ConfigDiscreteLevels.levels(4), -1, 2, true, ImageType.single(imageType));
		pyramid.process(input);

		var gui = new DiscretePyramidPanel<T>();
		gui.setPyramid(pyramid);
		gui.render();

		ShowImages.showWindow(gui, "Image Pyramid");

		// To get an image at any of the scales simply call this get function
		T imageAtScale = pyramid.getLayer(1);

		ShowImages.showWindow(ConvertBufferedImage.convertTo(imageAtScale, null, true), "Image at layer 1");
	}

	public static void main( String[] args ) {
		BufferedImage image = UtilImageIO.loadImageNotNull(UtilIO.pathExample("standard/barbara.jpg"));

		var app = new ExamplePyramidDiscrete<>(GrayF32.class);
//		var app = new ExamplePyramidDiscrete<>(GrayU8.class);

		app.process(image);
	}
}
