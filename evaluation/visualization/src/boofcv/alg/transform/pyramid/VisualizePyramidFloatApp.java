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

package boofcv.alg.transform.pyramid;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectImagePanel;
import boofcv.gui.image.ImagePyramidPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.pyramid.PyramidFloat;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays an image pyramid.
 *
 * @author Peter Abeles
 */
public class VisualizePyramidFloatApp <T extends ImageSingleBand>
	extends SelectImagePanel implements ProcessInput
{
	double scales[] = new double[]{1,1.2,2.4,3.6,4.8,6.0,12,20};

	Class<T> imageType;
	InterpolatePixel<T> interp;
	ImagePyramidPanel<T> gui = new ImagePyramidPanel<T>();
	boolean processedImage = false;

	public VisualizePyramidFloatApp( Class<T> imageType ) {
		this.imageType = imageType;
		interp = FactoryInterpolation.bilinearPixel(imageType);

		setMainGUI(gui);
	}

	public void process( final BufferedImage input ) {
		setInputImage(input);
		final T gray = ConvertBufferedImage.convertFromSingle(input, null, imageType);

		PyramidUpdateSubsampleScale<T> updater = new PyramidUpdateSubsampleScale<T>(interp);
		PyramidFloat<T> pyramid = new PyramidFloat<T>(imageType,scales);

		updater.update(gray,pyramid);

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
	public synchronized void changeImage(String name, int index) {
		ImageListManager manager = getInputManager();

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

//		VisualizePyramidFloatApp<ImageFloat32> app = new VisualizePyramidFloatApp<ImageFloat32>(ImageFloat32.class);
		VisualizePyramidFloatApp<ImageUInt8> app = new VisualizePyramidFloatApp<ImageUInt8>(ImageUInt8.class);

		ImageListManager manager = new ImageListManager();
		manager.add("boat","data/standard/boat.png");
		manager.add("shapes","data/shapes01.png");
		manager.add("sunflowers","data/sunflowers.png");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Image Float Pyramid");
	}
}
