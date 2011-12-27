/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.examples;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.gui.image.ImagePyramidPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.struct.pyramid.PyramidFloat;
import boofcv.struct.pyramid.PyramidUpdaterFloat;

import java.awt.image.BufferedImage;

/**
 * Demonstrates how to construct and display a {@link PyramidFloat}.  Float pyramids require only require
 * that each layer's scale be larger than the scale of the previous layer. Interpolation is used to allow
 * sub-sampling at arbitrary scales.  All of this additional flexibility comes at the cost of speed
 * when compared to a {@link PyramidDiscrete}.
 *
 * @author Peter Abeles
 */
public class ExamplePyramidFloat<T extends ImageSingleBand> {

	// specifies the image type
	Class<T> imageType;
	// The pyramid data structure
	PyramidFloat<T> pyramid;
	// update the pyramid given from
	PyramidUpdaterFloat<T> updater;

	public ExamplePyramidFloat(Class<T> imageType) {
		this.imageType = imageType;
	}

	/**
	 * Creates a fairly standard pyramid and updater.
	 */
	public void standard() {
		// Scale factory for each layer can be any floating point value which is larger than
		// the previous layer's scale.
		pyramid = new PyramidFloat<T>(imageType,1,1.5,2,2.5,3,5,8,15);

		// Specify the amount of blur to apply to each scale
		// Using a custom updater other types of blur and interpolation can be applied
		updater = FactoryPyramid.floatGaussian(imageType, 1,1,1,1,1,1,1,1);
	}

	/**
	 * Updates and displays the pyramid.
	 */
	public void process( BufferedImage image ) {
		T input = ConvertBufferedImage.convertFromSingle(image, null, imageType);
		updater.update(input,pyramid);

		ImagePyramidPanel<T> gui = new ImagePyramidPanel<T>();
		gui.set(pyramid, true);
		gui.render();

		ShowImages.showWindow(gui,"Image Pyramid Float");

		// To get an image at any of the scales simply call this get function
		T imageAtScale = pyramid.getLayer(1);

		ShowImages.showWindow(ConvertBufferedImage.convertTo(imageAtScale,null),"Image at layer 1");
	}


	public static void main( String[] args ) {
		BufferedImage image = UtilImageIO.loadImage("data/standard/barbara.png");

		ExamplePyramidFloat<ImageFloat32> app = new ExamplePyramidFloat<ImageFloat32>(ImageFloat32.class);
//		ExamplePyramidFloat<ImageUInt8> app = new ExamplePyramidFloat<ImageUInt8>(ImageUInt8.class);

		app.standard();
		app.process(image);
	}
}
