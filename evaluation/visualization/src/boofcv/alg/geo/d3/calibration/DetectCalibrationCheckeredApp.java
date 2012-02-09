/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.d3.calibration;

import boofcv.alg.feature.detect.checker.DetectCheckeredGrid;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectImagePanel;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DetectCalibrationCheckeredApp <T extends ImageSingleBand, D extends ImageSingleBand>
		extends SelectImagePanel implements ProcessInput
		
{
	Class<T> imageType;
	DetectCheckeredGrid<T,D> alg;

	ImagePanel gui;
	
	BufferedImage input;
	// gray scale image that targets are detected inside of
	T gray;
	
	boolean processed = false;
	
	public DetectCalibrationCheckeredApp( Class<T> imageType ) {
		this.imageType = imageType;
		alg = new DetectCheckeredGrid<T,D>(8,6,5,imageType);
		gray = GeneralizedImageOps.createSingleBand(imageType,1,1);
		gui = new ImagePanel();
		
		setMainGUI(gui);
	}

	public synchronized void process( final BufferedImage input ) {
		this.input = input;

		gray.reshape(input.getWidth(),input.getHeight());
		ConvertBufferedImage.convertFrom(input, gray);
		
		alg.process(gray);


		Graphics2D g2 = input.createGraphics();

		List<Point2D_I32> candidates =  alg.getCornerCandidates();
		List<Point2D_I32> all =  alg.getAllPoints();

		for( Point2D_I32 c : all ) {
			VisualizeFeatures.drawPoint(g2, c.x, c.y, 2, Color.GREEN);
		}
		for( Point2D_I32 c : candidates ) {
			VisualizeFeatures.drawPoint(g2, c.x, c.y, 2, Color.RED);
		}

		ImageFloat32 intensity = alg.getIntensity();
		BufferedImage out = new BufferedImage(intensity.width,intensity.height,BufferedImage.TYPE_INT_RGB);
		VisualizeImageData.standard(intensity,out);
		ShowImages.showWindow(out,"Feature Intensity");
		
		processed = true;

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setPreferredSize(new Dimension(input.getWidth(), input.getHeight()));
				gui.setBufferedImage(input);
			}
		});
	}

	@Override
	public boolean getHasProcessedImage() {
		return processed;
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager manager = getInputManager();

		BufferedImage image = manager.loadImage(index);
		if (image != null) {
			process(image);
		}
	}

	public static void main(String args[]) {

		DetectCalibrationCheckeredApp app = new DetectCalibrationCheckeredApp(ImageFloat32.class);

		String prefix = "/home/pja/saved2/a/";

		ImageListManager manager = new ImageListManager();
		manager.add("View 01",prefix+"frame01.jpg");
		manager.add("View 02",prefix+"frame02.jpg");
		manager.add("View 03",prefix+"frame03.jpg");
		manager.add("View 04",prefix+"frame04.jpg");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Calibration Target Detection");
	}
}
