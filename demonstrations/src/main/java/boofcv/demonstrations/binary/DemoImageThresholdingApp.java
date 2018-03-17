/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import boofcv.demonstrations.shapes.ThresholdControlPanel;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ImagePanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates different ways to threshold an image
 *
 * @author Peter Abeles
 */
public class DemoImageThresholdingApp<T extends ImageGray<T>>
		extends DemonstrationBase
		implements ThresholdControlPanel.Listener
{
	GrayU8 imageBinary = new GrayU8(1,1);
	BufferedImage visualizedBinary;
	BufferedImage inputCopy;

	ImagePanel gui = new ImagePanel();
	ControlPanel controlPanel = new ControlPanel();

	public DemoImageThresholdingApp(List<String> examples, Class<T> imageType) {
		super(examples, ImageType.single(imageType));

		gui.setPreferredSize(new Dimension(800,600));
		gui.setCentering(true);

		add(controlPanel,BorderLayout.WEST);
		add(gui,BorderLayout.CENTER);


	}

	@Override
	protected void configureVideo(int which, SimpleImageSequence sequence) {
		super.configureVideo(which, sequence);
		sequence.setLoop(true);
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height)
	{
		imageBinary.reshape(width, height);
		visualizedBinary = ConvertBufferedImage.checkDeclare(width, height, visualizedBinary,BufferedImage.TYPE_INT_BGR);
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input)
	{
		inputCopy = ConvertBufferedImage.checkCopy(buffered,inputCopy);
		ConfigThreshold config = controlPanel.threshold.createConfig();
		InputToBinary inputToBinary = FactoryThresholdBinary.threshold(config,input.imageType.getImageClass());

		inputToBinary.process(input,imageBinary);
		VisualizeBinaryData.renderBinary(imageBinary, false, visualizedBinary);

		SwingUtilities.invokeLater(() -> {
			controlPanel.threshold.updateHistogram((ImageGray)input);
			changeView();
		});
	}

	public void changeView() {
		switch( controlPanel.view ) {
			case 0: gui.setImage(inputCopy); break;
			case 1: gui.setImage(visualizedBinary); break;
		}
		gui.repaint();
	}

	@Override
	public void imageThresholdUpdated() {
		reprocessInput();
	}

	class ControlPanel extends StandardAlgConfigPanel implements ActionListener {
		// selects which image to view
		JComboBox imageView;
		ThresholdControlPanel threshold = new ThresholdControlPanel(DemoImageThresholdingApp.this);

		int view = 1;

		public ControlPanel() {
			imageView = combo(view,"Input","Binary");

			threshold.setBorder(BorderFactory.createEmptyBorder());
			threshold.addHistogramGraph();

			addLabeled(imageView,"View");
			add(threshold);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			view = imageView.getSelectedIndex();
			changeView();
		}
	}

	public static void main( String args[] )
	{
		java.util.List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("particles", UtilIO.pathExample("particles01.jpg")));
		examples.add(new PathLabel("shapes",UtilIO.pathExample("shapes/shapes01.png")));
		examples.add(new PathLabel("stained",UtilIO.pathExample("segment/stained_handwriting.jpg")));
		examples.add(new PathLabel("Chessboard Movie",UtilIO.pathExample("fiducial/chessboard/movie.mjpeg")));

		DemoImageThresholdingApp app = new DemoImageThresholdingApp(examples,GrayF32.class);

		app.openExample(examples.get(0));
		app.display("Thresholding Demo");
	}


}
