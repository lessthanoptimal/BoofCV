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
import boofcv.gui.ProcessImage;
import boofcv.gui.image.ImagePyramidPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
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
public class VisualizePyramidFloatApp <T extends ImageBase>
	extends JPanel implements ProcessImage
{
	double scales[] = new double[]{1,1.2,2.4,3.6,4.8,6.0,12,20};

	Class<T> imageType;
	InterpolatePixel<T> interp;
	ImagePyramidPanel<T> gui = new ImagePyramidPanel<T>();

	public VisualizePyramidFloatApp( Class<T> imageType ) {
		this.imageType = imageType;
		interp = FactoryInterpolation.bilinearPixel(imageType);

		setLayout(new BorderLayout());
		add(gui,BorderLayout.CENTER);
	}

	@Override
	public void process( final BufferedImage input ) {
		final T gray = ConvertBufferedImage.convertFrom(input,null,imageType);

		PyramidUpdateSubsampleScale<T> updater = new PyramidUpdateSubsampleScale<T>(interp);
		PyramidFloat<T> pyramid = new PyramidFloat<T>(imageType,scales);

		updater.update(gray,pyramid);

		gui.set(pyramid,true);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.render();
				gui.repaint();
				setPreferredSize(new Dimension(gray.width+50,gray.height+20));
			}});
	}

	public static void main( String args[] ) {

		BufferedImage original = UtilImageIO.loadImage("data/standard/boat.png");

//		VisualizePyramidFloatApp<ImageFloat32> app = new VisualizePyramidFloatApp<ImageFloat32>(ImageFloat32.class);
		VisualizePyramidFloatApp<ImageUInt8> app = new VisualizePyramidFloatApp<ImageUInt8>(ImageUInt8.class);

		app.process(original);

		ShowImages.showWindow(app,"Image Float Pyramid");
	}
}
