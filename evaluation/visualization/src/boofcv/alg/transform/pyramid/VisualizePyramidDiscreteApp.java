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
import boofcv.gui.SelectImagePanel;
import boofcv.gui.image.DiscretePyramidPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.struct.pyramid.PyramidUpdaterDiscrete;

import javax.swing.*;
import java.awt.image.BufferedImage;

/**
 * Displays an image pyramid.
 *
 * @author Peter Abeles
 */
public class VisualizePyramidDiscreteApp <T extends ImageBase>
	extends SelectImagePanel implements ProcessImage
{
	int scales[] = new int[]{1,2,4,8,16};

	Class<T> imageType;
	DiscretePyramidPanel gui = new DiscretePyramidPanel();
	PyramidUpdaterDiscrete<T> updater;
	PyramidDiscrete<T> pyramid;

	boolean processedImage = false;

	public VisualizePyramidDiscreteApp(Class<T> imageType) {
		this.imageType = imageType;

		pyramid = new PyramidDiscrete<T>(imageType,true,scales);
		updater = FactoryPyramid.discreteGaussian(imageType,-1,2);
		gui = new DiscretePyramidPanel();

		setMainGUI(gui);
	}

	public void process( BufferedImage input ) {
		setInputImage(input);
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
				processedImage = true;
			}});
	}

	@Override
	public synchronized void changeImage(String name, int index) {
		ImageListManager manager = getImageManager();

		BufferedImage image = manager.loadImage(index);
		if( image != null ) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	public static void main( String args[] ) {
		VisualizePyramidDiscreteApp<ImageFloat32> app = new VisualizePyramidDiscreteApp<ImageFloat32>(ImageFloat32.class);

		ImageListManager manager = new ImageListManager();
		manager.add("lena","data/standard/lena512.bmp");
		manager.add("boat","data/standard/boat.png");
		manager.add("fingerprint","data/standard/fingerprint.png");

		app.setImageManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Image Discrete Pyramid");
	}
}
