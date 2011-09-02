/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.pyramid;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.image.ImagePyramidPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.pyramid.PyramidFloat;

import java.awt.image.BufferedImage;

/**
 * Displays an image pyramid.
 *
 * @author Peter Abeles
 */
public class VisualizePyramidFloatApp {

	public static void main( String args[] ) {
		double scales[] = new double[]{1,1.2,2.4,3.6,4.8,6.0};

		BufferedImage original = UtilImageIO.loadImage("evaluation/data/standard/boat.png");

		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(ImageFloat32.class);
//		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bicubic(ImageFloat32.class,-0.5f);
		PyramidUpdateSubsampleScale<ImageFloat32> updater = new PyramidUpdateSubsampleScale<ImageFloat32>(interp);
		PyramidFloat<ImageFloat32> pyramid = new PyramidFloat<ImageFloat32>(ImageFloat32.class,scales);

		ImageFloat32 input = ConvertBufferedImage.convertFrom(original,(ImageFloat32)null);

		updater.update(input,pyramid);

		ImagePyramidPanel<ImageFloat32> gui = new ImagePyramidPanel<ImageFloat32>(pyramid,true);
		gui.render();

		ShowImages.showWindow(gui,"Image Float Pyramid");
	}
}
