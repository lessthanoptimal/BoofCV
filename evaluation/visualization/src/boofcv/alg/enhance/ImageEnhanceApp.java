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

package boofcv.alg.enhance;

import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.image.ImageUInt8;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Displays various image enhancement filters
 *
 * @author Peter Abeles
 */
public class ImageEnhanceApp
		extends SelectAlgorithmAndInputPanel implements ChangeListener
{
	// displays intensity image
	ImagePanel gui;

	// converted input image
	ImageUInt8 input;
	ImageUInt8 enhanced = new ImageUInt8(1,1);
	// if it has processed an image or not
	boolean processImage = false;

	BufferedImage output;

	// storage for histogram
	int histogram[] = new int[256];
	int transform[] = new int[256];

	// used to specify size of local region
	JSpinner selectRadius;

	int radius = 50;

	int previousActive=-1;

	public ImageEnhanceApp() {
		super(1);

		addAlgorithm(0, "Histogram Global", 0);
		addAlgorithm(0, "Histogram Local", 1);
		addAlgorithm(0, "Sharpen-4",2);
		addAlgorithm(0, "Sharpen-8",3);

		selectRadius = new JSpinner(new SpinnerNumberModel(radius,10,100,10));
		selectRadius.addChangeListener(this);

		gui = new ImagePanel();
		setMainGUI(gui);
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( input == null )
			return;

		int active = (Integer)cookie;
		if( active == 0 ) {
			ImageStatistics.histogram(input,histogram);
			EnhanceImageOps.equalize(histogram, transform);
			EnhanceImageOps.applyTransform(input, transform, enhanced);
		} else if( active == 1 ) {
			EnhanceImageOps.equalizeLocal(input, radius, enhanced, histogram, transform);
		} else if( active == 2 ) {
			EnhanceImageOps.sharpen4(input, enhanced);
		} else if( active == 3 ) {
			EnhanceImageOps.sharpen8(input, enhanced);
		}

		if( previousActive != active ) {
			if( active == 1 ) {
				addToToolbar(selectRadius);
			} else {
				removeFromToolbar(selectRadius);
			}
			previousActive = active;
		}

		ConvertBufferedImage.convertTo(enhanced, output);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				gui.setBufferedImage(output);
				gui.repaint();
				gui.requestFocusInWindow();
			}
		});
	}

	public void process( final BufferedImage input ) {
		setInputImage(input);
		this.input = ConvertBufferedImage.convertFromSingle(input, this.input, ImageUInt8.class);
		this.enhanced = new ImageUInt8(input.getWidth(),input.getHeight());
		this.output = new BufferedImage( input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);

		// over write input image so that it's gray scale
		ConvertBufferedImage.convertTo(this.input,input);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setPreferredSize(new Dimension(input.getWidth(),input.getHeight()));
				processImage = true;
			}});
		doRefreshAll();
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		if( image != null ) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processImage;
	}

	public static void main( String args[] ) {

		ImageEnhanceApp app = new ImageEnhanceApp();

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();

		inputs.add(new PathLabel("dark","../data/applet/enhance/dark.jpg"));
		inputs.add(new PathLabel("dull","../data/applet/enhance/dull.jpg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Image Enhancement");
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		radius = (Integer)selectRadius.getValue();
		doRefreshAll();
	}
}
