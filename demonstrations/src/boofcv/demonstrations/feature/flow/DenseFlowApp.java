/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.gui.PanelGridPanel;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.feature.VisualizeOpticalFlow;
import boofcv.gui.image.AnimatePanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.ProgressMonitorThread;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.GrayF32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Displays dense optical flow
 *
 * @author Peter Abeles
 */
public class DenseFlowApp
		extends SelectAlgorithmAndInputPanel
{
	// displays intensity image
	PanelGridPanel gui;
	ImagePanel flowPanel;
	AnimatePanel animationPanel;

	// converted input image
	GrayF32 unscaled = new GrayF32(1,1);
	GrayF32 input0 = new GrayF32(1,1);
	GrayF32 input1 = new GrayF32(1,1);
	ImageFlow flow = new ImageFlow(1,1);

	// if it has processed an image or not
	boolean processImage = false;
	boolean hasInputImage = false;

	DenseOpticalFlow<GrayF32> denseFlow;

	BufferedImage converted0,converted1,visualized;

	public DenseFlowApp() {
		super(1);

		Class T = GrayF32.class;

		addAlgorithm(0, "KLT", FactoryDenseOpticalFlow.flowKlt(null,6,T,T));
		addAlgorithm(0, "Region", FactoryDenseOpticalFlow.region(null,T));
		addAlgorithm(0, "Horn-Schunck-Pyramid",FactoryDenseOpticalFlow.hornSchunckPyramid(null,GrayF32.class));
		addAlgorithm(0, "Brox",FactoryDenseOpticalFlow.broxWarping(null, GrayF32.class));
		addAlgorithm(0, "Horn-Schunck",FactoryDenseOpticalFlow.hornSchunck(null, GrayF32.class));

		animationPanel = new AnimatePanel(200,null);
		flowPanel = new ImagePanel();
		gui = new PanelGridPanel(2,animationPanel,flowPanel);
		animationPanel.start();

		setMainGUI(gui);
	}

	@Override
	public synchronized void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		denseFlow = (DenseOpticalFlow<GrayF32>)cookie;
		process();
	}

	@Override
	public synchronized void changeInput(String name, int index) {
		BufferedImage image0 = media.openImage(inputRefs.get(index).getPath(0));
		BufferedImage image1 = media.openImage(inputRefs.get(index).getPath(1) );

		// process at 1/2 resolution to make it faster
		unscaled.reshape(image0.getWidth(),image1.getHeight());
		input0.reshape(unscaled.width/2,unscaled.height/2);
		input1.reshape(unscaled.width/2,unscaled.height/2);
		flow.reshape(unscaled.width/2,unscaled.height/2);

		ConvertBufferedImage.convertFrom(image0, unscaled, false);
		new FDistort(unscaled,input0).scaleExt().apply();
		ConvertBufferedImage.convertFrom(image1, unscaled, false);
		new FDistort(unscaled,input1).scaleExt().apply();

		converted0 = new BufferedImage(input0.width,input0.height,BufferedImage.TYPE_INT_RGB);
		converted1 = new BufferedImage(input0.width,input0.height,BufferedImage.TYPE_INT_RGB);
		visualized = new BufferedImage(input0.width,input0.height,BufferedImage.TYPE_INT_RGB);

		ConvertBufferedImage.convertTo(input0, converted0, true);
		ConvertBufferedImage.convertTo(input1, converted1, true);

		hasInputImage = true;

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				flowPanel.setPreferredSize(new Dimension(input0.width,input0.height));
				flowPanel.setBufferedImage(visualized);
				animationPanel.setPreferredSize(new Dimension(input0.width,input0.height));
				animationPanel.setAnimation(converted0,converted1);
				gui.revalidate();
			}
		});

		doRefreshAll();
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	private synchronized void process() {
		if( hasInputImage && denseFlow != null ) {
			ProcessThread progress = new ProcessThread(this);
			progress.start();
			denseFlow.process(input0, input1, flow);
			progress.stopThread();

			VisualizeOpticalFlow.colorized(flow, 10, visualized);
			gui.repaint();

			processImage = true;
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processImage;
	}

	/**
	 * Displays a progress monitor and updates its state periodically
	 */
	public class ProcessThread extends ProgressMonitorThread
	{
		public ProcessThread( JComponent owner ) {
			super(new ProgressMonitor(owner, "Computing Flow", "", 0, 100));
		}

		@Override
		public void doRun() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					monitor.setProgress(0);

				}});
		}
	}

	public static void main( String args[] ) {

		DenseFlowApp app = new DenseFlowApp();

//		app.setBaseDirectory(UtilIO.pathExample("denseflow/");
//		app.loadInputData(UtilIO.pathExample("denseflow/denseflow.txt");

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();

		inputs.add(new PathLabel("urban", UtilIO.pathExample("denseflow/Urban2_07.png"), UtilIO.pathExample("denseflow/Urban2_08.png")));
		inputs.add(new PathLabel("dog",UtilIO.pathExample("denseflow/dogdance07.png"),UtilIO.pathExample("denseflow/dogdance08.png")));
		inputs.add(new PathLabel("grove",UtilIO.pathExample("denseflow/Grove2_07.png"),UtilIO.pathExample("denseflow/Grove2_08.png")));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Dense Optical Flow",true);
	}

}
