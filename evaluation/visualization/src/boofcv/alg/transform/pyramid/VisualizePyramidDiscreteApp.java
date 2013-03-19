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

package boofcv.alg.transform.pyramid;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.gui.SelectInputPanel;
import boofcv.gui.VisualizeApp;
import boofcv.gui.image.DiscretePyramidPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidDiscrete;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays an image pyramid.
 *
 * @author Peter Abeles
 */
public class VisualizePyramidDiscreteApp <T extends ImageSingleBand>
	extends SelectInputPanel implements VisualizeApp
{
	int scales[] = new int[]{1,2,4,8,16};

	Class<T> imageType;
	DiscretePyramidPanel gui = new DiscretePyramidPanel();
	PyramidDiscrete<T> pyramid;

	boolean processedImage = false;

	public VisualizePyramidDiscreteApp(Class<T> imageType) {
		this.imageType = imageType;

		pyramid = FactoryPyramid.discreteGaussian(scales,-1,2,true,imageType);
		gui = new DiscretePyramidPanel();

		setMainGUI(gui);
	}

	public void process( BufferedImage input ) {
		setInputImage(input);
		final T gray = ConvertBufferedImage.convertFromSingle(input, null, imageType);

		// update the pyramid
		pyramid.process(gray);

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
	public void loadConfigurationFile(String fileName) {}

	@Override
	public synchronized void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

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

		List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("lena","../data/evaluation/standard/lena512.bmp"));
		inputs.add(new PathLabel("boat","../data/evaluation/standard/boat.png"));
		inputs.add(new PathLabel("fingerprint","../data/evaluation/standard/fingerprint.png"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Image Discrete Pyramid");
	}
}
