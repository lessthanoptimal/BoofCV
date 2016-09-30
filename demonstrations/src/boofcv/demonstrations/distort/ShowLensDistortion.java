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

package boofcv.demonstrations.distort;

import boofcv.alg.distort.AdjustmentType;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.core.image.border.BorderType;
import boofcv.gui.SelectInputPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.ProgressMonitorThread;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
public class ShowLensDistortion<T extends ImageGray>
		extends SelectInputPanel implements ChangeListener, ItemListener
{
	double radial1 = 0;
	double radial2 = 0;
	double tangential1 = 0;
	double tangential2 = 0;
	boolean fullView = false;

	Class<T> imageType;

	JSpinner radialOrder1;
	JSpinner radialOrder2;
	JSpinner tangentialOrder1;
	JSpinner tangentialOrder2;

	JCheckBox showFullImage;

	ImagePanel gui = new ImagePanel();
	boolean processedImage = false;

	Planar<T> input;
	Planar<T> output;
	// rendered in main thread
	BufferedImage renderedImage;
	// rendered is copied to this in a GUI thread
	BufferedImage outputImage;

	// tells progress monitor the current progress
	volatile int progress;

	public ShowLensDistortion(Class<T> imageType) {

		this.imageType = imageType;
		input = new Planar<T>(imageType, 1, 1 , 3);
		output = new Planar<T>(imageType, 1, 1 , 3);
		addToToolbar(createRadialSelect());

		setMainGUI(gui);
	}

	private JPanel createRadialSelect() {
		JPanel ret = new JPanel();
		ret.setLayout(new BoxLayout(ret, BoxLayout.X_AXIS));

		radialOrder1 = new JSpinner(new SpinnerNumberModel(radial1,-1.0,2.0,0.05));
		radialOrder1.addChangeListener(this);
		int h = radialOrder1.getPreferredSize().height;
		radialOrder1.setPreferredSize(new Dimension(50, h));
		radialOrder1.setMaximumSize(radialOrder1.getPreferredSize());

		radialOrder2 = new JSpinner(new SpinnerNumberModel(radial2,-1.0,2.0,0.05));
		radialOrder2.addChangeListener(this);
		radialOrder2.setPreferredSize(new Dimension(50, h));
		radialOrder2.setMaximumSize(radialOrder1.getPreferredSize());

		tangentialOrder1 = new JSpinner(new SpinnerNumberModel(tangential1,-1.0,1.0,0.01));
		tangentialOrder1.addChangeListener(this);
		tangentialOrder1.setPreferredSize(new Dimension(50, h));
		tangentialOrder1.setMaximumSize(tangentialOrder1.getPreferredSize());

		tangentialOrder2 = new JSpinner(new SpinnerNumberModel(tangential2,-1.0,1.0,0.01));
		tangentialOrder2.addChangeListener(this);
		tangentialOrder2.setPreferredSize(new Dimension(50, h));
		tangentialOrder2.setMaximumSize(tangentialOrder2.getPreferredSize());

		showFullImage = new JCheckBox();
		showFullImage.setSelected(fullView);
		showFullImage.addItemListener(this);

		ret.add(Box.createRigidArea(new Dimension(10, 1)));
		ret.add(new JLabel("Radial 1:"));
		ret.add(radialOrder1);
		ret.add(Box.createRigidArea(new Dimension(10,1)));
		ret.add(new JLabel("Radial 2:"));
		ret.add(radialOrder2);
		ret.add(Box.createRigidArea(new Dimension(10,1)));
		ret.add(new JLabel("T1:"));
		ret.add(tangentialOrder1);
		ret.add(Box.createRigidArea(new Dimension(10,1)));
		ret.add(new JLabel("T2:"));
		ret.add(tangentialOrder2);
		ret.add(Box.createRigidArea(new Dimension(10,1)));
		ret.add(new JLabel("Full:"));
		ret.add(showFullImage);

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

		CameraPinholeRadial param = new CameraPinholeRadial().fsetK(input.width * 0.8, input.width * 0.8, 0,
				input.width / 2, input.height / 2, input.width, input.height).fsetRadial(radial1, radial2).
				fsetTangental(tangential1, tangential2);

		AdjustmentType type = fullView ? AdjustmentType.FULL_VIEW : AdjustmentType.NONE;
		ImageDistort distort = LensDistortionOps.imageRemoveDistortion(type, BorderType.ZERO, param, null, input.getImageType());

		distort.apply(input,output);
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
		} else {
			System.err.println("Can't open "+inputRefs.get(index).getPath());
			System.exit(1);
		}
	}

	@Override
	public synchronized void stateChanged(ChangeEvent e) {
		if( e.getSource() == radialOrder1 )
			radial1 = ((Number) radialOrder1.getValue()).doubleValue();
		else if( e.getSource() == radialOrder2 )
			radial2 = ((Number) radialOrder2.getValue()).doubleValue();
		else if( e.getSource() == tangentialOrder1 )
			tangential1 = ((Number) tangentialOrder1.getValue()).doubleValue();
		else if( e.getSource() == tangentialOrder2 )
			tangential2 = ((Number) tangentialOrder2.getValue()).doubleValue();

		performUpdate();
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		fullView = showFullImage.isSelected();
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

//		ShowImageBlurApp<GrayF32> app
//				= new ShowImageBlurApp<GrayF32>(GrayF32.class);
		ShowLensDistortion<GrayU8> app
				= new ShowLensDistortion<GrayU8>(GrayU8.class);

		List<PathLabel> inputs = new ArrayList<PathLabel>();

		inputs.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));
		inputs.add(new PathLabel("beach",UtilIO.pathExample("scale/beach02.jpg")));
		inputs.add(new PathLabel("sunflowers",UtilIO.pathExample("sunflowers.jpg")));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Lens Distortion", true);
	}
}