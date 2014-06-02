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
import boofcv.alg.feature.detect.ImageCorruptPanel;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.line.ConfigHoughFoot;
import boofcv.factory.feature.detect.line.ConfigHoughFootSubimage;
import boofcv.factory.feature.detect.line.ConfigHoughPolar;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.feature.ImageLinePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Shows detected lines inside of different images.
 *
 * @author Peter Abeles
 */
// todo configure: blur, edge threshold, non-max radius,  min counts
// todo show binary image, transform
public class DetectLineApp<T extends ImageSingleBand, D extends ImageSingleBand>
		extends SelectAlgorithmAndInputPanel implements ImageCorruptPanel.Listener
{
	Class<T> imageType;

	T input;
	T inputCorrupted;
	T blur;

	float edgeThreshold = 25;
	int maxLines = 10;
	int blurRadius = 2;

	ImageLinePanel gui = new ImageLinePanel();
	boolean processedImage = false;

	ImageCorruptPanel corruptPanel;

	public DetectLineApp( Class<T> imageType , Class<D> derivType ) {
		super(1);

		this.imageType = imageType;

		addAlgorithm(0,"Hough Polar", FactoryDetectLineAlgs.houghPolar(
				new ConfigHoughPolar(3, 30, 2, Math.PI / 180, edgeThreshold, maxLines), imageType, derivType));
		addAlgorithm(0,"Hough Foot", FactoryDetectLineAlgs.houghFoot(
				new ConfigHoughFoot(3, 8, 5, edgeThreshold, maxLines), imageType, derivType));
		addAlgorithm(0,"Hough Foot Sub Image", FactoryDetectLineAlgs.houghFootSub(
				new ConfigHoughFootSubimage(3, 8, 5, edgeThreshold, maxLines, 2, 2), imageType, derivType));
		addAlgorithm(0,"Grid Line", FactoryDetectLineAlgs.lineRansac(40, 30, 2.36, true, imageType, derivType));

		input = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		inputCorrupted = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		blur = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		JPanel viewArea = new JPanel(new BorderLayout());
		corruptPanel = new ImageCorruptPanel();
		corruptPanel.setListener(this);

		viewArea.add(corruptPanel,BorderLayout.WEST);
		viewArea.add(gui,BorderLayout.CENTER);
		setMainGUI(viewArea);
	}

	public void process( final BufferedImage image ) {
		input.reshape(image.getWidth(),image.getHeight());
		inputCorrupted.reshape(image.getWidth(),image.getHeight());
		blur.reshape(image.getWidth(),image.getHeight());

		ConvertBufferedImage.convertFromSingle(image, input, imageType);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setBackground(image);
				gui.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
				doRefreshAll();
			}
		});
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0, null, getAlgorithmCookie(0));
	}

	@Override
	public synchronized void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		corruptPanel.corruptImage(input,inputCorrupted);
		GBlurImageOps.gaussian(inputCorrupted, blur, -1,blurRadius, null);

		if( cookie instanceof DetectLine ) {
			final DetectLine<T> detector = (DetectLine<T>) cookie;

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					ConvertBufferedImage.convertTo(inputCorrupted, gui.background, true);
					gui.setLines(detector.detect(blur));
					gui.repaint();
					processedImage = true;
				}
			});
		} else if( cookie instanceof DetectLineSegment) {
			final DetectLineSegment<T> detector = (DetectLineSegment<T>) cookie;

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					ConvertBufferedImage.convertTo(inputCorrupted,gui.background, true);
					gui.setLineSegments(detector.detect(blur));
					gui.repaint();
					processedImage = true;
				}
			});
		}
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		process(image);
	}

	@Override
	public synchronized void corruptImageChange() {
		doRefreshAll();
	}

	public static void main(String args[]) {
		Class imageType = ImageFloat32.class;
		Class derivType = ImageFloat32.class;

		DetectLineApp app = new DetectLineApp(imageType,derivType);

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("Objects","../data/evaluation/simple_objects.jpg"));
		inputs.add(new PathLabel("Indoors","../data/evaluation/lines_indoors.jpg"));
		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Line Detection");
	}

}
