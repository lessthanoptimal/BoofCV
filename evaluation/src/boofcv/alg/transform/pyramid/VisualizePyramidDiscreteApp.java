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

package boofcv.alg.transform.pyramid;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.gui.image.DiscretePyramidPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.struct.pyramid.PyramidUpdaterDiscrete;

import java.awt.image.BufferedImage;

/**
 * Displays an image pyramid.
 *
 * @author Peter Abeles
 */
public class VisualizePyramidDiscreteApp {

	public static void main( String args[] ) {
		int scales[] = new int[]{1,2,4,8,16};

		BufferedImage input = UtilImageIO.loadImage("evaluation/data/standard/boat.png");

		PyramidUpdaterDiscrete<ImageFloat32> updater = FactoryPyramid.discreteGaussian(ImageFloat32.class,-1,2);
		PyramidDiscrete<ImageFloat32> pyramid = new PyramidDiscrete<ImageFloat32>(ImageFloat32.class,true,scales);

		ImageFloat32 inputF32 = ConvertBufferedImage.convertFrom(input,(ImageFloat32)null);

		updater.update(inputF32,pyramid);

		DiscretePyramidPanel gui = new DiscretePyramidPanel(pyramid);
		gui.render();
		gui.repaint();

		ShowImages.showWindow(gui,"Image Discrete Pyramid");
	}
}
