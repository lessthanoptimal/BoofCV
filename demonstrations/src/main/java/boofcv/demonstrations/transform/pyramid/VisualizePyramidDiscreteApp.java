/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.transform.pyramid;

import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.gui.SelectInputPanel;
import boofcv.gui.VisualizeApp;
import boofcv.gui.image.DiscretePyramidPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
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
public class VisualizePyramidDiscreteApp<T extends ImageGray<T>>
		extends SelectInputPanel implements VisualizeApp {
	Class<T> imageType;
	DiscretePyramidPanel<T> gui;
	PyramidDiscrete<T> pyramid;

	boolean processedImage = false;

	public VisualizePyramidDiscreteApp( Class<T> imageType ) {
		this.imageType = imageType;
		ConfigDiscreteLevels configLevels = ConfigDiscreteLevels.levels(5);

		pyramid = FactoryPyramid.discreteGaussian(configLevels, -1, 2, true, ImageType.single(imageType));
		gui = new DiscretePyramidPanel<>();

		setMainGUI(gui);
	}

	public void process( BufferedImage input ) {
		setInputImage(input);
		final T gray = ConvertBufferedImage.convertFromSingle(input, null, imageType);

		// update the pyramid
		pyramid.process(gray);

		// render the pyramid
		SwingUtilities.invokeLater(() -> {
			gui.setPyramid(pyramid);
			gui.render();
			gui.repaint();
//				setPreferredSize(new Dimension(gray.width,gray.height));
			processedImage = true;
		});
	}

	@Override
	public void loadConfigurationFile( String fileName ) {}

	@Override
	public synchronized void changeInput( String name, int index ) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		if (image != null) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	public static void main( String[] args ) {
		VisualizePyramidDiscreteApp<GrayF32> app = new VisualizePyramidDiscreteApp<>(GrayF32.class);

		List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("Human Statue", UtilIO.pathExample("standard/kodim17.jpg")));
		inputs.add(new PathLabel("boat", UtilIO.pathExample("standard/boat.jpg")));
		inputs.add(new PathLabel("fingerprint", UtilIO.pathExample("standard/fingerprint.jpg")));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while (!app.getHasProcessedImage()) {
			BoofMiscOps.sleep(10);
		}

		ShowImages.showWindow(app, "Image Discrete Pyramid", true);
	}
}
