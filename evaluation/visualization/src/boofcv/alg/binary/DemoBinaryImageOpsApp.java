/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.binary;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.binary.FactoryBinaryImageOps;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Demonstrates the affects of different binary operations on an image.
 */
// todo clean up appearance
public class DemoBinaryImageOpsApp<T extends ImageSingleBand> extends SelectAlgorithmAndInputPanel
		implements SelectHistogramThresholdPanel.Listener
{
	Class<T> imageType;
	T imageInput;
	ImageUInt8 imageBinary;
	ImageUInt8 imageOutput;
	ImageUInt8 selectedVisualize;

	FilterImageInterface<ImageUInt8, ImageUInt8> filter;
	BufferedImage work;

	boolean processedImage = false;

	JComboBox imagesCombo;
	SelectHistogramThresholdPanel selectThresh;
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

		imageInput = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
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

		selectThresh = new SelectHistogramThresholdPanel(20,true);
		selectThresh.setListener(this);

		left.add(imagesCombo);
		left.add(selectThresh);
		left.add(Box.createHorizontalGlue());

		return left;
	}

	public void process( final BufferedImage image ) {
		imageInput.reshape(image.getWidth(),image.getHeight());
		imageBinary.reshape(image.getWidth(),image.getHeight());
		imageOutput.reshape(image.getWidth(),image.getHeight());

		ConvertBufferedImage.convertFromSingle(image, imageInput, imageType);

		final double threshold = GThresholdImageOps.computeOtsu(imageInput,0,256);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				selectThresh.setThreshold((int) threshold);
				setInputImage(image);
				selectThresh.getHistogramPanel().update(imageInput);
				selectThresh.repaint();
			}});
		doRefreshAll();
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

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
		GThresholdImageOps.threshold(imageInput, imageBinary, selectThresh.getThreshold(), selectThresh.isDown());
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
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());
		if( image != null ) {
			process(image);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == imagesCombo ) {
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

	@Override
	public void histogramThresholdChange() {
		performWork();
	}

	public static void main( String args[] ) {
		DemoBinaryImageOpsApp app = new DemoBinaryImageOpsApp(ImageFloat32.class);

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("particles","../data/evaluation/particles01.jpg"));
		inputs.add(new PathLabel("shapes","../data/evaluation/shapes01.png"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Image Noise Removal");

		System.out.println("Done");
	}
}
