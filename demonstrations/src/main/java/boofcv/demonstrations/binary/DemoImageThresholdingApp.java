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

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.demonstrations.shapes.ThresholdControlPanel;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Demonstrates different ways to threshold an image
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DemoImageThresholdingApp<T extends ImageGray<T>>
		extends DemonstrationBase
		implements ThresholdControlPanel.Listener {
	GrayU8 imageBinary = new GrayU8(1, 1);
	BufferedImage visualizedBinary;
	BufferedImage inputCopy;

	ShapeVisualizePanel gui = new ShapeVisualizePanel();
	ControlPanel controlPanel = new ControlPanel();

	public DemoImageThresholdingApp( List<String> examples, Class<T> imageType ) {
		super(examples, ImageType.single(imageType));

		gui.setPreferredSize(new Dimension(800, 600));
//		gui.setCentering(true);

		add(controlPanel, BorderLayout.WEST);
		add(gui, BorderLayout.CENTER);
	}

	@Override
	protected void configureVideo( int which, SimpleImageSequence sequence ) {
		super.configureVideo(which, sequence);
		sequence.setLoop(true);
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		imageBinary.reshape(width, height);
		visualizedBinary = ConvertBufferedImage.checkDeclare(width, height, visualizedBinary, BufferedImage.TYPE_INT_BGR);

		controlPanel.setImageSize(width, height);
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {
		inputCopy = ConvertBufferedImage.checkCopy(buffered, inputCopy);
		ConfigThreshold config = controlPanel.threshold.createConfig();
		InputToBinary inputToBinary = FactoryThresholdBinary.threshold(config, input.imageType.getImageClass());

		long time0 = System.nanoTime();
		inputToBinary.process(input, imageBinary);
		long time1 = System.nanoTime();
		double processingTimeMS = (time1 - time0)*1e-6;

		VisualizeBinaryData.renderBinary(imageBinary, false, visualizedBinary);

		SwingUtilities.invokeLater(() -> {
			controlPanel.setProcessingTimeMS(processingTimeMS);
			controlPanel.threshold.updateHistogram((ImageGray)input);
			changeView();
		});
	}

	public void changeView() {
		switch (controlPanel.view) {
			case 0 -> gui.setImage(inputCopy);
			case 1 -> gui.setImage(visualizedBinary);
		}
		gui.repaint();
	}

	@Override
	public void imageThresholdUpdated() {
		reprocessInput();
	}

	class ControlPanel extends DetectBlackShapePanel implements ActionListener, ChangeListener {
		// selects which image to view
		JComboBox imageView;
		ThresholdControlPanel threshold = new ThresholdControlPanel(DemoImageThresholdingApp.this);

		int view = 1;

		public ControlPanel() {
			selectZoom = spinner(1, MIN_ZOOM, MAX_ZOOM, 1);
			imageView = combo(view, "Input", "Binary");

			threshold.setBorder(BorderFactory.createEmptyBorder());
			threshold.addHistogramGraph();

			addLabeled(processingTimeLabel, "Time (ms)");
			addLabeled(imageSizeLabel, "Image Size");
			addLabeled(imageView, "View");
			addLabeled(selectZoom, "Zoom");
			add(threshold);
		}

		@Override
		public void actionPerformed( ActionEvent e ) {
			view = imageView.getSelectedIndex();
			changeView();
		}

		@Override
		public void stateChanged( ChangeEvent e ) {
			if (e.getSource() == selectZoom) {
				zoom = ((Number)selectZoom.getValue()).doubleValue();
				gui.setScale(zoom);
			}
		}
	}

	public static void main( String[] args ) {
		java.util.List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("particles", UtilIO.pathExample("particles01.jpg")));
		examples.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));
		examples.add(new PathLabel("stained", UtilIO.pathExample("segment/stained_handwriting.jpg")));
		examples.add(new PathLabel("Chessboard Movie", UtilIO.pathExample("fiducial/chessboard/movie.mjpeg")));

		SwingUtilities.invokeLater(() -> {
			var app = new DemoImageThresholdingApp(examples, GrayF32.class);

			app.openExample(examples.get(0));
			app.display("Thresholding Demo");
		});
	}
}
