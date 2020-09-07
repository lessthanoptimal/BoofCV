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

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.transform.pyramid.PyramidFloatScale;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.SelectInputPanel;
import boofcv.gui.image.ImagePyramidPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.PyramidFloat;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Displays an image pyramid.
 *
 * @author Peter Abeles
 */
public class VisualizePyramidFloatApp<T extends ImageGray<T>>
		extends SelectInputPanel {
	double[] scales = new double[]{1, 1.2, 2.4, 3.6, 4.8, 6.0, 12, 20};

	Class<T> imageType;
	InterpolatePixelS<T> interp;
	ImagePyramidPanel<T> gui = new ImagePyramidPanel<>();
	boolean processedImage = false;

	public VisualizePyramidFloatApp( Class<T> imageType ) {
		this.imageType = imageType;
		interp = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);

		setMainGUI(gui);
	}

	public void process( final BufferedImage input ) {
		setInputImage(input);
		final T gray = ConvertBufferedImage.convertFromSingle(input, null, imageType);

		PyramidFloat<T> pyramid = new PyramidFloatScale<>(interp, scales, imageType);

		pyramid.process(gray);

		gui.set(pyramid, true);

		SwingUtilities.invokeLater(() -> {
			gui.render();
			gui.repaint();
			setPreferredSize(new Dimension(gray.width + 50, gray.height + 20));
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

//		VisualizePyramidFloatApp<GrayF32> app = new VisualizePyramidFloatApp<>(GrayF32.class);
		VisualizePyramidFloatApp<GrayU8> app = new VisualizePyramidFloatApp<>(GrayU8.class);

		java.util.List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("boat", UtilIO.pathExample("standard/boat.jpg")));
		inputs.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));
		inputs.add(new PathLabel("sunflowers", UtilIO.pathExample("sunflowers.jpg")));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while (!app.getHasProcessedImage()) {
			BoofMiscOps.sleep(10);
		}

		ShowImages.showWindow(app, "Image Float Pyramid", true);
	}
}
