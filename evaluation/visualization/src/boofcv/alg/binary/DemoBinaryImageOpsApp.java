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

package boofcv.alg.binary;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.binary.FactoryBinaryImageOps;
import boofcv.gui.ImageHistogramPanel;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

/**
 * Demonstrates the affects of different binary operations on an image.
 */
// TODO visually show the threshold in histogram
	// or show behind the scroller

// todo clean up appearance
public class DemoBinaryImageOpsApp<T extends ImageBase> extends SelectAlgorithmImagePanel
		implements ProcessInput , ChangeListener {

	double threshold = 0;
	boolean down = true;

	Class<T> imageType;
	T imageInput;
	ImageUInt8 imageBinary;
	ImageUInt8 imageOutput;
	ImageUInt8 selectedVisualize;

	FilterImageInterface<ImageUInt8, ImageUInt8> filter;
	BufferedImage work;

	boolean processedImage = false;

	JComboBox imagesCombo;
	JSlider thresholdLevel;
	ImageHistogramPanel histogramPanel;
	JButton toggleButton;
	ImagePanel gui = new ImagePanel();

	public DemoBinaryImageOpsApp( Class<T> imageType ) {
		super(1);

		this.imageType = imageType;

		addAlgorithm(0,"Erode-4", FactoryBinaryImageOps.erode4());
		addAlgorithm(0,"Erode-8", FactoryBinaryImageOps.erode8());
		addAlgorithm(0,"Dilate-4", FactoryBinaryImageOps.dilate4());
		addAlgorithm(0,"Dilate-8", FactoryBinaryImageOps.dilate8());
		addAlgorithm(0,"Edge-4", FactoryBinaryImageOps.edge4());
		addAlgorithm(0,"Edge-8", FactoryBinaryImageOps.edge8());
		addAlgorithm(0,"Remove Noise", FactoryBinaryImageOps.removePointNoise());

		JPanel body = new JPanel();
		body.setLayout(new BorderLayout());

		body.add(createLeftPanel(),BorderLayout.NORTH);
		body.add(gui,BorderLayout.CENTER);

		imageInput = GeneralizedImageOps.createImage(imageType,1,1);
		imageBinary = new ImageUInt8(1,1);
		imageOutput = new ImageUInt8(1,1);

		selectedVisualize = imageOutput;

		setMainGUI(body);
	}

	private JPanel createLeftPanel() {
		JPanel left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));

		imagesCombo = new JComboBox();
		imagesCombo.addItem("Thresholded");
		imagesCombo.addItem("Filtered");
		imagesCombo.addActionListener(this);
		imagesCombo.setSelectedIndex(1);
		imagesCombo.setMaximumSize(imagesCombo.getPreferredSize());

		histogramPanel = new ImageHistogramPanel(200,256);
		histogramPanel.setPreferredSize(new Dimension(120,60));
		histogramPanel.setMaximumSize(histogramPanel.getPreferredSize());

		thresholdLevel = new JSlider(JSlider.HORIZONTAL,0,255,20);
		thresholdLevel.setMajorTickSpacing(20);
		thresholdLevel.setPaintTicks(true);
		thresholdLevel.addChangeListener(this);

		toggleButton = new JButton();
		toggleButton.setPreferredSize(new Dimension(100,30));
		toggleButton.setMaximumSize(toggleButton.getPreferredSize());
		toggleButton.setMinimumSize(toggleButton.getPreferredSize());
		setToggleText();
		toggleButton.addActionListener(this);

		left.add(imagesCombo);
		left.add(histogramPanel);
		left.add(Box.createRigidArea(new Dimension(8,8)));
		left.add(thresholdLevel);
		left.add(toggleButton);
		left.add(Box.createHorizontalGlue());

		return left;
	}

	private void setToggleText() {
		if( down )
			toggleButton.setText("down");
		else
			toggleButton.setText("Up");
	}

	public void process( final BufferedImage image ) {
		imageInput.reshape(image.getWidth(),image.getHeight());
		imageBinary.reshape(image.getWidth(),image.getHeight());
		imageOutput.reshape(image.getWidth(),image.getHeight());

		ConvertBufferedImage.convertFrom(image,imageInput,imageType);

		// average pixel intensity should be a reasonable threshold
		threshold = GPixelMath.sum(imageInput)/(imageInput.width*imageInput.height);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				thresholdLevel.setValue((int)threshold);
				setInputImage(image);
				histogramPanel.update(imageInput);
				histogramPanel.repaint();
			}});
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
		filter = (FilterImageInterface<ImageUInt8, ImageUInt8>)cookie;

		performWork();
	}

	private synchronized void performWork() {
		if( filter == null )
			return;
		GThresholdImageOps.threshold(imageInput, imageBinary, threshold, down);
		filter.process(imageBinary,imageOutput);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (work == null || work.getWidth() != imageInput.width || work.getHeight() != imageInput.height) {
					work = new BufferedImage(imageInput.width, imageInput.height, BufferedImage.TYPE_INT_BGR);
				}
				VisualizeBinaryData.renderBinary(selectedVisualize, work);
				gui.setBufferedImage(work);
				gui.setPreferredSize(new Dimension(imageInput.width, imageInput.height));
				processedImage = true;
				gui.repaint();
			}
		});
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager manager = getInputManager();

		BufferedImage image = manager.loadImage(index);
		if( image != null ) {
			process(image);
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == thresholdLevel )
			threshold = ((Number)thresholdLevel.getValue()).doubleValue();

		performWork();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == toggleButton ) {
			down = !down;
			setToggleText();
			performWork();
		}else if( e.getSource() == imagesCombo ) {
			int index = imagesCombo.getSelectedIndex();
			if( index == 0 ) {
				selectedVisualize = imageBinary;
			} else {
				selectedVisualize = imageOutput;
			}
			if( work != null ) {
				VisualizeBinaryData.renderBinary(selectedVisualize, work);
				gui.repaint();
			}
		} else {
			super.actionPerformed(e);
		}
	}

	public static void main( String args[] ) {
		DemoBinaryImageOpsApp app = new DemoBinaryImageOpsApp(ImageFloat32.class);

		ImageListManager manager = new ImageListManager();
		manager.add("lena","data/particles01.jpg");
		manager.add("barbara","data/shapes01.png");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Image Noise Removal");

		System.out.println("Done");
	}
}
