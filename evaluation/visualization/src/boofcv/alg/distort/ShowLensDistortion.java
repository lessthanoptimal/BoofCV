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

package boofcv.alg.distort;

import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.SelectInputPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.ProgressMonitorThread;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.distort.PointTransform_F32;
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
import java.util.List;

/**
 * Shows lens distortion with different amounts of radial distortion
 *
 * @author Peter Abeles
 */
public class ShowLensDistortion<T extends ImageSingleBand>
		extends SelectInputPanel implements ChangeListener
{
	double radial1 = -0.65;
	double radial2 = -0.1;

	Class<T> imageType;

	JSpinner radialOrder1;
	JSpinner radialOrder2;
	ImagePanel gui = new ImagePanel();
	boolean processedImage = false;

	MultiSpectral<T> input;
	MultiSpectral<T> output;
	// rendered in main thread
	BufferedImage renderedImage;
	// rendered is copied to this in a GUI thread
	BufferedImage outputImage;

	// tells progress monitor the current progress
	volatile int progress;

	public ShowLensDistortion(Class<T> imageType) {

		this.imageType = imageType;
		input = new MultiSpectral<T>(imageType, 1, 1 , 3);
		output = new MultiSpectral<T>(imageType, 1, 1 , 3);
		addToToolbar(createRadialSelect());

		setMainGUI(gui);
	}

	private JPanel createRadialSelect() {
		JPanel ret = new JPanel();
		ret.setLayout(new BoxLayout(ret, BoxLayout.X_AXIS));

		radialOrder1 = new JSpinner(new SpinnerNumberModel(radial1,-1.0,2.0,0.05));
		radialOrder1.addChangeListener(this);
		int h = radialOrder1.getPreferredSize().height;
		radialOrder1.setPreferredSize(new Dimension(50,h));
		radialOrder1.setMaximumSize(radialOrder1.getPreferredSize());

		radialOrder2 = new JSpinner(new SpinnerNumberModel(radial2,-1.0,2.0,0.05));
		radialOrder2.addChangeListener(this);
		radialOrder2.setPreferredSize(new Dimension(50,h));
		radialOrder2.setMaximumSize(radialOrder1.getPreferredSize());

		ret.add(Box.createRigidArea(new Dimension(10,1)));
		ret.add(new JLabel("Radial 1:"));
		ret.add(radialOrder1);
		ret.add(Box.createRigidArea(new Dimension(10,1)));
		ret.add(new JLabel("Radial 2:"));
		ret.add(radialOrder2);

		// change the enabled status of the spinner
		ret.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if( evt.getPropertyName().equals("enabled")) {
					JPanel src = (JPanel)evt.getSource();
					boolean value = (Boolean)evt.getNewValue();
					for( int i = 0; i < src.getComponentCount(); i++ ) {
						src.getComponent(i).setEnabled(value);
					}
				}
			}
		});

		return ret;
	}

	public void process( final BufferedImage image ) {
		input.reshape(image.getWidth(),image.getHeight());
		output.reshape(image.getWidth(),image.getHeight());

		ConvertBufferedImage.convertFromMulti(image, input, true, imageType);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setInputImage(image);
				outputImage = new BufferedImage(input.width, input.height,BufferedImage.TYPE_INT_BGR);
				renderedImage = new BufferedImage(input.width, input.height,BufferedImage.TYPE_INT_BGR);
				gui.setBufferedImage(outputImage);
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
		performUpdate();
	}

	private synchronized void performUpdate() {
		if( input == null || output == null )
			return;

		progress = 0;

		ProgressMonitorThread thread = new MyMonitorThread(this);
		thread.start();

		PointTransform_F32 ptran =
				new AddRadialPtoP_F32(input.width*0.8,input.width*0.8,0,
						input.width/2,input.height/2,radial1,radial2);
		PixelTransform_F32 tran=new PointToPixelTransform_F32(ptran);

		for( int i = 0; i < input.getNumBands(); i++ , progress++ ) {
			T bandIn = input.getBand(i);
			T bandOut = output.getBand(i);

			DistortImageOps.distortSingle(bandIn,bandOut,tran,false, TypeInterpolate.BILINEAR);
		}
		thread.stopThread();
		ConvertBufferedImage.convertTo(output, renderedImage, true);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				outputImage.createGraphics().drawImage(renderedImage,0,0,null);
				gui.repaint();
			}
		});
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
		if( e.getSource() == radialOrder1 )
			radial1 = ((Number) radialOrder1.getValue()).doubleValue();
		if( e.getSource() == radialOrder2 )
			radial2 = ((Number) radialOrder2.getValue()).doubleValue();

		performUpdate();
	}
	
	private class MyMonitorThread extends ProgressMonitorThread {

		protected MyMonitorThread(Component comp) {
			super(new ProgressMonitor(comp,
					"Applying Distortion",
					"", 0, input.getNumBands()));
		}

		@Override
		public void doRun() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					monitor.setProgress(progress);
				}});
		}
	}

	public static void main(String args[]) {

//		ShowImageBlurApp<ImageFloat32> app
//				= new ShowImageBlurApp<ImageFloat32>(ImageFloat32.class);
		ShowLensDistortion<ImageUInt8> app
				= new ShowLensDistortion<ImageUInt8>(ImageUInt8.class);

		List<PathLabel> inputs = new ArrayList<PathLabel>();

		inputs.add(new PathLabel("shapes","../data/evaluation/shapes01.png"));
		inputs.add(new PathLabel("beach","../data/evaluation/scale/beach02.jpg"));
		inputs.add(new PathLabel("sunflowers","../data/evaluation/sunflowers.png"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Lens Distortion");
	}
}