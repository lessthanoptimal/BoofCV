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

package boofcv.alg.filter.blur;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.ProgressMonitorThread;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

/**
 * Shows the result of different blur operations
 *
 * @author Peter Abeles
 */
public class ShowImageBlurApp<T extends ImageSingleBand>
	extends SelectAlgorithmAndInputPanel implements ChangeListener
{
	int radius = 2;
	int active = 0;

	Class<T> imageType;

	JSpinner radiusSpinner;
	ImagePanel gui = new ImagePanel();
	boolean processedImage = false;

	MultiSpectral<T> input;
	MultiSpectral<T> output;
	MultiSpectral<T> storage;
	BufferedImage renderedImage;
	
	// tells progress monitor the current progress
	volatile int progress;

	public ShowImageBlurApp( Class<T> imageType ) {
		super(1);

		addAlgorithm(0,"Gaussian",0);
		addAlgorithm(0,"Mean",1);
		addAlgorithm(0,"Median",2);

		this.imageType = imageType;
		input = new MultiSpectral<T>(imageType, 1, 1 , 3);
		output = new MultiSpectral<T>(imageType, 1, 1 , 3);
		storage = new MultiSpectral<T>(imageType, 1, 1 , 3);

		addToToolbar(createRadialSelect());

		setMainGUI(gui);
	}

	private JPanel createRadialSelect() {
		JPanel ret = new JPanel();
		ret.setLayout(new BoxLayout(ret,BoxLayout.X_AXIS));

		JLabel desc = new JLabel("Radius:");
		radiusSpinner = new JSpinner(new SpinnerNumberModel(radius,1,30,1));
		radiusSpinner.addChangeListener(this);
		radiusSpinner.setMaximumSize(radiusSpinner.getPreferredSize());

		ret.add(Box.createRigidArea(new Dimension(10,1)));
		ret.add(desc);
		ret.add(radiusSpinner);

		// change the enabled status of the spinner
		ret.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if( evt.getPropertyName().equals("enabled")) {
					JPanel src = (JPanel)evt.getSource();
					boolean value = (Boolean)evt.getNewValue();
					src.getComponent(1).setEnabled(value);
					src.getComponent(2).setEnabled(value);
				}
			}
		});

		return ret;
	}

	public void process( final BufferedImage image ) {
		input.reshape(image.getWidth(),image.getHeight());
		output.reshape(image.getWidth(),image.getHeight());
		storage.reshape(image.getWidth(),image.getHeight());

		ConvertBufferedImage.convertFromMulti(image, input, true, imageType);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setInputImage(image);
				renderedImage = new BufferedImage(input.width, input.height,BufferedImage.TYPE_INT_BGR);
				gui.setBufferedImage(renderedImage);
				gui.setPreferredSize(new Dimension(input.width,input.height));
				gui.repaint();
				processedImage = true;
				doRefreshAll();
			}});
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0, null, cookies[0]);
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		active = (Integer) cookie;

		performUpdate();
	}

	private synchronized void performUpdate() {
		if( input == null || output == null )
			return;

		String message = active == 2 ? "Median is slow" : "";

		progress = 0;

		ProgressMonitorThread monitor = new MyMonitor(this,message);
		monitor.start();
		
		for( int i = 0; i < input.getNumBands(); i++ , progress++ ) {
			T bandIn = input.getBand(i);
			T bandOut = output.getBand(i);
			T bandStorage = storage.getBand(i);

			switch (active) {
				case 0:
					GBlurImageOps.gaussian(bandIn, bandOut, -1, radius, bandStorage);
					break;

				case 1:
					GBlurImageOps.mean(bandIn, bandOut, radius, bandStorage);
					break;

				case 2:
					GBlurImageOps.median(bandIn, bandOut, radius);
					break;
			}
		}
		monitor.stopThread();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ConvertBufferedImage.convertTo(output, renderedImage, true);
				gui.repaint();
			}
		});
	}
	
	private class MyMonitor extends ProgressMonitorThread {

		protected MyMonitor( Component comp , String message ) {
			super( new ProgressMonitor(comp,
					"Blurring the Image",
					message, 0, input.getNumBands()));
		}

		@Override
		public void doRun() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					monitor.setProgress(progress);
				}});
		}
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		if (image != null) {
			process(image);
		}
	}

	@Override
	public synchronized void stateChanged(ChangeEvent e) {
		final int lradius = ((Number)radiusSpinner.getValue()).intValue();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				radius = lradius;
				performUpdate();
			}});
	}

	public static void main(String args[]) {

//		ShowImageBlurApp<ImageFloat32> app
//				= new ShowImageBlurApp<ImageFloat32>(ImageFloat32.class);
		ShowImageBlurApp<ImageUInt8> app
				= new ShowImageBlurApp<ImageUInt8>(ImageUInt8.class);

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("shapes","../data/evaluation/shapes01.png"));
		inputs.add(new PathLabel("sunflowers","../data/evaluation/sunflowers.png"));
		inputs.add(new PathLabel("beach","../data/evaluation/scale/beach02.jpg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Image Blur");
	}
}
