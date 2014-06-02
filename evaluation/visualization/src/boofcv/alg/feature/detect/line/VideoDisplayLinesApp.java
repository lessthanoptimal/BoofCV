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

package boofcv.alg.feature.detect.line;

import boofcv.abst.feature.detect.line.DetectLine;
import boofcv.abst.feature.detect.line.DetectLineSegment;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.line.ConfigHoughFoot;
import boofcv.factory.feature.detect.line.ConfigHoughFootSubimage;
import boofcv.factory.feature.detect.line.ConfigHoughPolar;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.gui.VideoProcessAppBase;
import boofcv.gui.feature.ImageLinePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Runs a KLT tracker through a video sequence
 *
 * @author Peter Abeles
 */
public class VideoDisplayLinesApp<I extends ImageSingleBand, D extends ImageSingleBand>
		extends VideoProcessAppBase<I> implements MouseListener
{
	I blur;

	float edgeThreshold = 20;
	int maxLines = 10;
	int blurRadius = 2;

	ImageLinePanel gui = new ImageLinePanel();
	boolean processedImage = false;

	Object lineDetector;

	public VideoDisplayLinesApp(Class<I> imageType, Class<D> derivType) {
		super(1,imageType);

		addAlgorithm(0,"Hough Foot", FactoryDetectLineAlgs.houghFoot(
				new ConfigHoughFoot(3, 8, 5, edgeThreshold, maxLines), imageType, derivType));
		addAlgorithm(0,"Hough Polar", FactoryDetectLineAlgs.houghPolar(
				new ConfigHoughPolar(3, 30, 2, Math.PI / 180, edgeThreshold, maxLines), imageType, derivType));
		addAlgorithm(0,"Hough Foot Sub Image", FactoryDetectLineAlgs.houghFootSub(
				new ConfigHoughFootSubimage(3, 8, 5, edgeThreshold, maxLines, 2, 2), imageType, derivType));
		addAlgorithm(0,"Grid Line", FactoryDetectLineAlgs.lineRansac(40, 30, 2.36, true, imageType, derivType));


		blur = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		gui.addMouseListener(this);
		gui.requestFocus();
		setMainGUI(gui);
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public void process( SimpleImageSequence<I> sequence ) {
		stopWorker();
		this.sequence = sequence;
		sequence.setLoop(true);
		doRefreshAll();
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( sequence == null )
			return;
		
		stopWorker();

		lineDetector = cookie;

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				sequence.reset();
				I image = sequence.next();
				blur.reshape(image.width,image.height);
				gui.setPreferredSize(new Dimension(image.width, image.height));
				revalidate();
				startWorkerThread();
			}
		});
	}

	@Override
	protected void updateAlg(final I frame, BufferedImage buffImage) {

		if( lineDetector instanceof DetectLine) {
			GBlurImageOps.gaussian(frame, blur, -1, blurRadius, null);
			final DetectLine<I> detector = (DetectLine<I>) lineDetector;

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					gui.setLines(detector.detect(blur));
					gui.repaint();
					processedImage = true;
				}
			});
		} else if( lineDetector instanceof DetectLineSegment) {
			final DetectLineSegment<I> detector = (DetectLineSegment<I>) lineDetector;

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					gui.setLineSegments(detector.detect(frame));
					gui.repaint();
					processedImage = true;
				}
			});
		}
	}

	@Override
	protected void handleRunningStatus(int status) {}

	@Override
	protected void updateAlgGUI(ImageSingleBand frame, BufferedImage imageGUI, double fps) {
		gui.setBackground(imageGUI);
	}

	public static void main( String args[] ) {
		VideoDisplayLinesApp app = new VideoDisplayLinesApp(ImageFloat32.class, ImageFloat32.class);

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("Apartment", "../data/applet/lines_indoors.mjpeg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Feature Tracker");
	}
}
