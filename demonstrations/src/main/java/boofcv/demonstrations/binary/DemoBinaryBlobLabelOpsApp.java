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
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
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
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Demonstrates the affects of different binary operations on an image.
 */
@SuppressWarnings({"NullAway.Init"})
public class DemoBinaryBlobLabelOpsApp<T extends ImageGray<T>> extends SelectAlgorithmAndInputPanel
		implements SelectHistogramThresholdPanel.Listener {

	Random rand = new Random(234234);

	Class<T> imageType;
	T imageInput;
	GrayU8 imageBinary;
	GrayU8 imageOutput1;
	GrayU8 imageOutput2;
	GrayS32 imageLabeled;
	ImageGray selectedVisualize;

	FilterImageInterface<GrayU8, GrayU8> filter1;
	FilterImageInterface<GrayU8, GrayU8> filter2;
	ConnectRule connectRule;
	BufferedImage work;
	int colors[];

	boolean processedImage = false;

	JComboBox imagesCombo;
	SelectHistogramThresholdPanel selectThresh;
	ImagePanel gui = new ImagePanel();

	public DemoBinaryBlobLabelOpsApp( Class<T> imageType ) {
		super(3);

		this.imageType = imageType;

		addAlgorithm(0, "Erode-4", FactoryBinaryImageOps.erode4(1));
		addAlgorithm(0, "Erode-8", FactoryBinaryImageOps.erode8(1));
		addAlgorithm(0, "Dilate-4", FactoryBinaryImageOps.dilate4(1));
		addAlgorithm(0, "Dilate-8", FactoryBinaryImageOps.dilate8(1));
		addAlgorithm(0, "Remove Noise", FactoryBinaryImageOps.removePointNoise());

		addAlgorithm(1, "Erode-4", FactoryBinaryImageOps.erode4(1));
		addAlgorithm(1, "Erode-8", FactoryBinaryImageOps.erode8(1));
		addAlgorithm(1, "Dilate-4", FactoryBinaryImageOps.dilate4(1));
		addAlgorithm(1, "Dilate-8", FactoryBinaryImageOps.dilate8(1));
		addAlgorithm(1, "Remove Noise", FactoryBinaryImageOps.removePointNoise());

		addAlgorithm(2, "Label-4", 4);
		addAlgorithm(2, "Label-8", 8);

		JPanel body = new JPanel();
		body.setLayout(new BorderLayout());

		body.add(createControlPanel(), BorderLayout.NORTH);
		body.add(gui, BorderLayout.CENTER);

		imageInput = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		imageBinary = new GrayU8(1, 1);
		imageOutput1 = new GrayU8(1, 1);
		imageOutput2 = new GrayU8(1, 1);
		imageLabeled = new GrayS32(1, 1);

		selectedVisualize = imageLabeled;

		setMainGUI(body);
	}

	private JPanel createControlPanel() {
		JPanel left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));

		imagesCombo = new JComboBox();
		imagesCombo.addItem("Thresholded");
		imagesCombo.addItem("Filtered 1");
		imagesCombo.addItem("Filtered 2");
		imagesCombo.addItem("Labeled");
		imagesCombo.addActionListener(this);
		imagesCombo.setSelectedIndex(3);
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
		imageOutput1.reshape(image.getWidth(), image.getHeight());
		imageOutput2.reshape(image.getWidth(), image.getHeight());
		imageLabeled.reshape(image.getWidth(), image.getHeight());

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
		filter1 = (FilterImageInterface<GrayU8, GrayU8>)cookies[0];
		filter2 = (FilterImageInterface<GrayU8, GrayU8>)cookies[1];
		connectRule = (Integer)cookies[2] == 4 ? ConnectRule.FOUR : ConnectRule.EIGHT;
		performWork();
	}

	@Override
	public void setActiveAlgorithm( int indexFamily, String name, Object cookie ) {
		switch (indexFamily) {
			case 0:
				filter1 = (FilterImageInterface<GrayU8, GrayU8>)cookie;
				break;
			case 1:
				filter2 = (FilterImageInterface<GrayU8, GrayU8>)cookie;
				break;
			case 2:
				connectRule = (Integer)cookie == 4 ? ConnectRule.FOUR : ConnectRule.EIGHT;
				break;
		}

		performWork();
	}

	private synchronized void performWork() {
		if (filter1 == null || filter2 == null)
			return;
		GThresholdImageOps.threshold(imageInput, imageBinary, selectThresh.getThreshold(), selectThresh.isDown());
		filter1.process(imageBinary, imageOutput1);
		filter2.process(imageOutput1, imageOutput2);
		List<Contour> found = BinaryImageOps.contour(imageOutput2, connectRule, imageLabeled);
		if (colors == null || colors.length <= found.size())
			colors = BinaryImageOps.selectRandomColors(found.size(), rand);

		SwingUtilities.invokeLater(() -> {
			if (work == null || work.getWidth() != imageInput.width || work.getHeight() != imageInput.height) {
				work = new BufferedImage(imageInput.width, imageInput.height, BufferedImage.TYPE_INT_BGR);
			}
			renderVisualizeImage();
			gui.setImage(work);
			gui.setPreferredSize(new Dimension(imageInput.width, imageInput.height));
			processedImage = true;
			gui.repaint();
		});
	}

	private synchronized void renderVisualizeImage() {
		if (selectedVisualize instanceof GrayU8)
			VisualizeBinaryData.renderBinary((GrayU8)selectedVisualize, false, work);
		else {
			VisualizeBinaryData.renderLabeled((GrayS32)selectedVisualize, colors, work);
		}
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
			} else if (index == 1) {
				selectedVisualize = imageOutput1;
			} else if (index == 2) {
				selectedVisualize = imageOutput2;
			} else if (index == 3) {
				selectedVisualize = imageLabeled;
			}
			if (work != null) {
				renderVisualizeImage();
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
		DemoBinaryBlobLabelOpsApp app = new DemoBinaryBlobLabelOpsApp(GrayF32.class);

		List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("particles", UtilIO.pathExample("particles01.jpg")));
		inputs.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while (!app.getHasProcessedImage()) {
			BoofMiscOps.sleep(10);
		}

		ShowImages.showWindow(app, "Label Binary Blobs", true);

		System.out.println("Done");
	}
}
