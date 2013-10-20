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

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.SelectInputPanel;
import boofcv.gui.image.ImagePyramidPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
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
public class VisualizePyramidFloatApp <T extends ImageSingleBand>
	extends SelectInputPanel
{
	double scales[] = new double[]{1,1.2,2.4,3.6,4.8,6.0,12,20};

	Class<T> imageType;
	InterpolatePixelS<T> interp;
	ImagePyramidPanel<T> gui = new ImagePyramidPanel<T>();
	boolean processedImage = false;

	public VisualizePyramidFloatApp( Class<T> imageType ) {
		this.imageType = imageType;
		interp = FactoryInterpolation.bilinearPixelS(imageType);

		setMainGUI(gui);
	}

	public void process( final BufferedImage input ) {
		setInputImage(input);
		final T gray = ConvertBufferedImage.convertFromSingle(input, null, imageType);

		PyramidFloat<T> pyramid = new PyramidFloatScale<T>(interp,scales,imageType);

		pyramid.process(gray);

		gui.set(pyramid,true);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.render();
				gui.repaint();
				setPreferredSize(new Dimension(gray.width+50,gray.height+20));
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

//		VisualizePyramidFloatApp<ImageFloat32> app = new VisualizePyramidFloatApp<ImageFloat32>(ImageFloat32.class);
		VisualizePyramidFloatApp<ImageUInt8> app = new VisualizePyramidFloatApp<ImageUInt8>(ImageUInt8.class);

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("boat","../data/evaluation/standard/boat.png"));
		inputs.add(new PathLabel("shapes","../data/evaluation/shapes01.png"));
		inputs.add(new PathLabel("sunflowers","../data/evaluation/sunflowers.png"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Image Float Pyramid");
	}
}
