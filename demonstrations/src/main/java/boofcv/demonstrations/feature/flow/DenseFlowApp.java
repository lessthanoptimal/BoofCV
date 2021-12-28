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

package boofcv.demonstrations.feature.flow;

import boofcv.abst.distort.FDistort;
import boofcv.abst.flow.DenseOpticalFlow;
import boofcv.factory.flow.FactoryDenseOpticalFlow;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.PanelGridPanel;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.dialogs.OpenImageSetDialog;
import boofcv.gui.feature.VisualizeOpticalFlow;
import boofcv.gui.image.AnimatePanel;
import boofcv.gui.image.ImagePanel;
import boofcv.io.PathLabel;
import boofcv.io.ProgressMonitorThread;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Displays dense optical flow
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DenseFlowApp extends DemonstrationBase {
	// TODO add control for input scale factor

	// displays intensity image
	PanelGridPanel gui;
	ImagePanel flowPanel;
	AnimatePanel animationPanel;

	final FLowControls controls = new FLowControls();

	// converted input image
	GrayF32 temp0 = new GrayF32(1, 1);
	GrayF32 input0 = new GrayF32(1, 1);
	GrayF32 input1 = new GrayF32(1, 1);
	ImageFlow flow = new ImageFlow(1, 1);

	final Object lock = new Object();
	DenseOpticalFlow<GrayF32> denseFlow;

	BufferedImage converted0, converted1, visualized;

	public DenseFlowApp( java.util.List<PathLabel> examples ) {
		super(true, false, examples, ImageType.single(GrayF32.class));

		animationPanel = new AnimatePanel(200);
		flowPanel = new ImagePanel();
		gui = new PanelGridPanel(2, animationPanel, flowPanel);
		animationPanel.start();

		declareAlgorithm();

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, gui);
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, final int width, final int height ) {
		switch (source) {
			case 0:
				input0.reshape(width, height);
				flow.reshape(width, height);
				gui.setPreferredSize(new Dimension(input0.width*2, input0.height));
				flowPanel.setPreferredSize(new Dimension(input0.width, input0.height));
				animationPanel.setPreferredSize(new Dimension(input0.width, input0.height));
				break;
			case 1:
				input1.reshape(width, height);
				break;
		}
		SwingUtilities.invokeLater(() -> {
			controls.setImageSize(input0.width, input0.height);
		});
	}

	@Override
	protected void handleInputFailure( int source, String error ) {
	}

	@Override
	protected void openFileMenuBar() {
		String[] files = BoofSwingUtil.openImageSetChooser(window, OpenImageSetDialog.Mode.EXACTLY, 2);
		if (files == null)
			return;
		BoofSwingUtil.invokeNowOrLater(() -> openImageSet(false, files));
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage bufferedIn, ImageBase input ) {
		switch (sourceID) {
			case 0:
				temp0.setTo((GrayF32)input);
				break;

			case 1:
				if (controls.maxWidth < input.width) {
					int width = controls.maxWidth;
					int height = input.height*width/input.width;
					input0.reshape(width, height);
					input1.reshape(width, height);
				} else {
					input0.reshape(input.width, input.height);
					input1.reshape(input.width, input.height);
				}

				new FDistort(temp0, input0).scaleExt().apply();
				new FDistort(input, input1).scaleExt().apply();

				converted0 = ConvertBufferedImage.checkDeclare(input0.width, input0.height, converted0, BufferedImage.TYPE_INT_RGB);
				converted1 = ConvertBufferedImage.checkDeclare(input0.width, input0.height, converted1, BufferedImage.TYPE_INT_RGB);
				visualized = ConvertBufferedImage.checkDeclare(input0.width, input0.height, visualized, BufferedImage.TYPE_INT_RGB);
				flow.reshape(input0.width, input0.height);

				ConvertBufferedImage.convertTo(input0, converted0, true);
				ConvertBufferedImage.convertTo(input1, converted1, true);

				SwingUtilities.invokeLater(() -> {
					flowPanel.setImage(visualized);
					animationPanel.setAnimation(converted0, converted1);
					gui.revalidate();
				});
				process();
				break;
		}
	}

	private void process() {
		synchronized (lock) {
			if (denseFlow == null)
				return;
			ProcessThread progress = new ProcessThread(this);
			progress.start();
			long time0 = System.currentTimeMillis();
			denseFlow.process(input0, input1, flow);
			long time1 = System.currentTimeMillis();
			progress.stopThread();

			VisualizeOpticalFlow.colorized(flow, 10, visualized);
			gui.repaint();
			SwingUtilities.invokeLater(() -> controls.setTime(time1 - time0));
		}
	}

	/**
	 * Displays a progress monitor and updates its state periodically
	 */
	public static class ProcessThread extends ProgressMonitorThread {
		ProcessThread( JComponent owner ) {
			super(new ProgressMonitor(owner, "Computing Flow", "", 0, 100));
		}

		@Override
		public void doRun() {
			SwingUtilities.invokeLater(() -> monitor.setProgress(0));
		}
	}

	void declareAlgorithm() {
		Class T = GrayF32.class;

		synchronized (lock) {
			switch (controls.selectedAlg) {
				case 0:
					denseFlow = FactoryDenseOpticalFlow.flowKlt(null, 6, T, T);
					break;
				case 1:
					denseFlow = FactoryDenseOpticalFlow.region(null, T);
					break;
				case 2:
					denseFlow = FactoryDenseOpticalFlow.hornSchunckPyramid(null, GrayF32.class);
					break;
				case 3:
					denseFlow = FactoryDenseOpticalFlow.broxWarping(null, GrayF32.class);
					break;
				case 4:
					denseFlow = FactoryDenseOpticalFlow.hornSchunck(null, GrayF32.class);
					break;
			}
		}
	}

	class FLowControls extends StandardAlgConfigPanel implements ActionListener, ChangeListener {
		JLabel labelTime = new JLabel();
		JLabel labelSize = new JLabel();
		JComboBox<String> comboFlowAlg;
		JSpinner spinnerWidth;
		int maxWidth = 800;

		private int selectedAlg;

		public FLowControls() {


			comboFlowAlg = combo(selectedAlg, "KLT", "Region", "Horn-Schunck-Pyramid", "Brox", "Horn-Schunck");
			spinnerWidth = spinner(maxWidth, 100, 20000, 100);

			labelTime.setPreferredSize(new Dimension(70, 26));
			labelTime.setHorizontalAlignment(SwingConstants.RIGHT);

			addLabeled(labelTime, "Time (ms)");
			add(labelSize);
			addLabeled(spinnerWidth, "Max Width");
			addAlignLeft(comboFlowAlg);
		}

		public void setTime( double milliseconds ) {
			labelTime.setText(String.format("%.1f", milliseconds));
		}

		public void setImageSize( int width, int height ) {
			labelSize.setText(width + " x " + height);
		}

		@Override
		public void actionPerformed( ActionEvent e ) {
			if (comboFlowAlg == e.getSource()) {
				selectedAlg = comboFlowAlg.getSelectedIndex();
			}
			declareAlgorithm();
			reprocessInput();
		}

		@Override
		public void stateChanged( ChangeEvent e ) {
			if (spinnerWidth == e.getSource()) {
				maxWidth = ((Integer)spinnerWidth.getValue()).intValue();
				reprocessInput();
			}
		}
	}

	public static void main( String[] args ) {

		java.util.List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("urban", UtilIO.pathExample("denseflow/Urban2_07.png"), UtilIO.pathExample("denseflow/Urban2_08.png")));
		examples.add(new PathLabel("dog", UtilIO.pathExample("denseflow/dogdance07.png"), UtilIO.pathExample("denseflow/dogdance08.png")));
		examples.add(new PathLabel("grove", UtilIO.pathExample("denseflow/Grove2_07.png"), UtilIO.pathExample("denseflow/Grove2_08.png")));

		SwingUtilities.invokeLater(() -> {
			DenseFlowApp app = new DenseFlowApp(examples);

			// Processing time takes a bit so don't open right away
			app.openExample(examples.get(0));
			app.display("Dense Optical Flow");
		});
	}
}
