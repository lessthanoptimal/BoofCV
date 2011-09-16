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

import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.gui.ProcessImage;
import boofcv.gui.image.DiscretePyramidPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.struct.pyramid.PyramidUpdaterDiscrete;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays an image pyramid.
 *
 * @author Peter Abeles
 */
public class VisualizePyramidDiscreteApp <T extends ImageBase>
	extends JPanel implements ProcessImage
{
	int scales[] = new int[]{1,2,4,8,16};

	Class<T> imageType;
	DiscretePyramidPanel gui = new DiscretePyramidPanel();
	PyramidUpdaterDiscrete<T> updater;
	PyramidDiscrete<T> pyramid;


	public VisualizePyramidDiscreteApp(Class<T> imageType) {
		this.imageType = imageType;

		pyramid = new PyramidDiscrete<T>(imageType,true,scales);
		updater = FactoryPyramid.discreteGaussian(imageType,-1,2);
		gui = new DiscretePyramidPanel();

		setLayout(new BorderLayout());
		add(gui,BorderLayout.CENTER);
	}

	@Override
	public void process( BufferedImage input ) {
		final T gray = ConvertBufferedImage.convertFrom(input,null,imageType);

		// update the pyramid
		updater.update(gray,pyramid);

		// render the pyramid
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setPyramid(pyramid);
				gui.render();
				gui.repaint();
//				setPreferredSize(new Dimension(gray.width,gray.height));
			}});
	}

	public static void main( String args[] ) {
		BufferedImage original = UtilImageIO.loadImage("data/standard/boat.png");

		VisualizePyramidDiscreteApp<ImageFloat32> app = new VisualizePyramidDiscreteApp<ImageFloat32>(ImageFloat32.class);
		app.process(original);

		ShowImages.showWindow(app,"Image Discrete Pyramid");
	}
}
