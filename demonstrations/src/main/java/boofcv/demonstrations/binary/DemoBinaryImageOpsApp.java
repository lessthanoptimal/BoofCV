/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.binary;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.binary.FactoryBinaryImageOps;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Demonstrates the affects of different binary operations on an image.
 */
// todo clean up appearance
@SuppressWarnings({"NullAway.Init"})
public class DemoBinaryImageOpsApp<T extends ImageGray<T>> extends SelectAlgorithmAndInputPanel
		implements SelectHistogramThresholdPanel.Listener {
	Class<T> imageType;
	T imageInput;
	GrayU8 imageBinary;
	GrayU8 imageOutput;
	GrayU8 selectedVisualize;

	FilterImageInterface<GrayU8, GrayU8> filter;
	BufferedImage work;

	boolean processedImage = false;

	JComboBox imagesCombo;
	SelectHistogramThresholdPanel selectThresh;
	ImagePanel gui = new ImagePanel();

	public DemoBinaryImageOpsApp( Class<T> imageType ) {
		super(1);

		this.imageType = imageType;

		addAlgorithm(0, "Erode-4", FactoryBinaryImageOps.erode4(1));
		addAlgorithm(0, "Erode-8", FactoryBinaryImageOps.erode8(1));
		addAlgorithm(0, "Dilate-4", FactoryBinaryImageOps.dilate4(1));
		addAlgorithm(0, "Dilate-8", FactoryBinaryImageOps.dilate8(1));
		addAlgorithm(0, "Edge-4", FactoryBinaryImageOps.edge4(true));
		addAlgorithm(0, "Edge-8", FactoryBinaryImageOps.edge8(true));
		addAlgorithm(0, "Remove Noise", FactoryBinaryImageOps.removePointNoise());

		JPanel body = new JPanel();
		body.setLayout(new BorderLayout());

		body.add(createLeftPanel(), BorderLayout.NORTH);
		body.add(gui, BorderLayout.CENTER);

		imageInput = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		imageBinary = new GrayU8(1, 1);
		imageOutput = new GrayU8(1, 1);

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

		selectThresh = new SelectHistogramThresholdPanel(20, true);
		selectThresh.setListener(this);

		left.add(imagesCombo);
		left.add(selectThresh);
		left.add(Box.createHorizontalGlue());

		return left;
	}

	public void process( final BufferedImage image ) {
		imageInput.reshape(image.getWidth(), image.getHeight());
		imageBinary.reshape(image.getWidth(), image.getHeight());
		imageOutput.reshape(image.getWidth(), image.getHeight());

		ConvertBufferedImage.convertFromSingle(image, imageInput, imageType);

		final double threshold = GThresholdImageOps.computeOtsu(imageInput, 0, 255);
		SwingUtilities.invokeLater(() -> {
			selectThresh.setThreshold((int)threshold);
			setInputImage(image);
			selectThresh.getHistogramPanel().update(imageInput);
			selectThresh.repaint();
		});
		doRefreshAll();
	}

	@Override
	public void loadConfigurationFile( String fileName ) {}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public void refreshAll( Object[] cookies ) {
		setActiveAlgorithm(0, "", cookies[0]);
	}

	@Override
	public void setActiveAlgorithm( int indexFamily, String name, Object cookie ) {
		filter = (FilterImageInterface<GrayU8, GrayU8>)cookie;

		performWork();
	}

	private synchronized void performWork() {
		if (filter == null)
			return;
		GThresholdImageOps.threshold(imageInput, imageBinary, selectThresh.getThreshold(), selectThresh.isDown());
		filter.process(imageBinary, imageOutput);

		SwingUtilities.invokeLater(() -> {
			if (work == null || work.getWidth() != imageInput.width || work.getHeight() != imageInput.height) {
				work = new BufferedImage(imageInput.width, imageInput.height, BufferedImage.TYPE_INT_BGR);
			}
			VisualizeBinaryData.renderBinary(selectedVisualize, false, work);
			gui.setImage(work);
			gui.setPreferredSize(new Dimension(imageInput.width, imageInput.height));
			processedImage = true;
			gui.repaint();
		});
	}

	@Override
	public void changeInput( String name, int index ) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());
		if (image != null) {
			process(image);
		}
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if (e.getSource() == imagesCombo) {
			int index = imagesCombo.getSelectedIndex();
			if (index == 0) {
				selectedVisualize = imageBinary;
			} else {
				selectedVisualize = imageOutput;
			}
			if (work != null) {
				VisualizeBinaryData.renderBinary(selectedVisualize, false, work);
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

	public static void main( String[] args ) {
		DemoBinaryImageOpsApp app = new DemoBinaryImageOpsApp(GrayF32.class);

		java.util.List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("particles", UtilIO.pathExample("particles01.jpg")));
		inputs.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while (!app.getHasProcessedImage()) {
			BoofMiscOps.sleep(10);
		}

		ShowImages.showWindow(app, "Binary Image Ops", true);

		System.out.println("Done");
	}
}
