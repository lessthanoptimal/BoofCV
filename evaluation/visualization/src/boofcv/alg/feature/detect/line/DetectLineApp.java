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

package boofcv.alg.feature.detect.line;


import boofcv.abst.feature.detect.line.DetectLine;
import boofcv.abst.feature.detect.line.DetectLineSegment;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.line.FactoryDetectLine;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Shows detected lines inside of different images.
 *
 * @author Peter Abeles
 */
// todo configure: blur, edge threshold, non-max radius,  min counts
// todo show binary image, transform
public class DetectLineApp<T extends ImageBase, D extends ImageBase>
		extends SelectAlgorithmImagePanel implements ProcessInput
{
	Class<T> imageType;

	T input;
	T blur;

	float edgeThreshold = 25;
	int maxLines = 10;
	int blurRadius = 2;

	ImageLinePanel gui = new ImageLinePanel();
	boolean processedImage = false;

	public DetectLineApp( Class<T> imageType , Class<D> derivType ) {
		super(1);

		this.imageType = imageType;

		addAlgorithm(0,"Hough Foot",FactoryDetectLine.houghFoot(3, 8, 5, edgeThreshold, maxLines, imageType, derivType));
		addAlgorithm(0,"Hough Polar",FactoryDetectLine.houghPolar(3, 30, 2, Math.PI/180, edgeThreshold, maxLines, imageType, derivType));
		addAlgorithm(0,"Hough Foot Sub Image",FactoryDetectLine.houghFootSub(3, 8, 5, edgeThreshold, maxLines, 2, 2, imageType, derivType));
		addAlgorithm(0,"Grid Line", FactoryDetectLine.lineRansac(40, 30, 2.36, true, imageType, derivType));

		input = GeneralizedImageOps.createImage(imageType,1,1);
		blur = GeneralizedImageOps.createImage(imageType,1,1);

		setMainGUI(gui);
	}

	public void process( final BufferedImage image ) {
		input.reshape(image.getWidth(),image.getHeight());
		blur.reshape(image.getWidth(),image.getHeight());

		ConvertBufferedImage.convertFrom(image,input,imageType);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setBackground(image);
				gui.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
				doRefreshAll();
			}
		});
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0, null, getAlgorithmCookie(0));
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		GBlurImageOps.gaussian(input, blur, -1,blurRadius, null);

		if( cookie instanceof DetectLine ) {
			final DetectLine<T> detector = (DetectLine<T>) cookie;

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					gui.setLines(detector.detect(blur));
					gui.repaint();
					processedImage = true;
				}
			});
		} else if( cookie instanceof DetectLineSegment) {
			final DetectLineSegment<T> detector = (DetectLineSegment<T>) cookie;

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					gui.setLineSegments(detector.detect(blur));
					gui.repaint();
					processedImage = true;
				}
			});
		}
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager m = getInputManager();
		BufferedImage image = m.loadImage(index,0);

		process(image);
	}

	public static void main(String args[]) {
		Class imageType = ImageFloat32.class;
		Class derivType = ImageFloat32.class;

		DetectLineApp app = new DetectLineApp(imageType,derivType);

		ImageListManager manager = new ImageListManager();
		manager.add("Objects","data/simple_objects.jpg");
		manager.add("Indoors","data/lines_indoors.jpg");
		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Line Detection");
	}

}
