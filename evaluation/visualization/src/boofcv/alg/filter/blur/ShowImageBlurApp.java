/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Shows the result of different blur operations
 *
 * @author Peter Abeles
 */
public class ShowImageBlurApp<T extends ImageSingleBand>
	extends SelectAlgorithmImagePanel implements ProcessInput , ChangeListener
{
	int radius = 2;
	int active = 0;

	Class<T> imageType;

	JSpinner radiusSpinner;
	ImagePanel gui = new ImagePanel();
	boolean processedImage = false;

	T input;
	T output;
	T storage;
	BufferedImage renderedImage;

	public ShowImageBlurApp( Class<T> imageType ) {
		super(1);

		addAlgorithm(0,"Gaussian",0);
		addAlgorithm(0,"Mean",1);
		addAlgorithm(0,"Median",2);

		this.imageType = imageType;
		input = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		output = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		storage = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

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

		ConvertBufferedImage.convertFromSingle(image, input, imageType);

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

		switch (active) {
			case 0:
				GBlurImageOps.gaussian(input, output, -1, radius, storage);
				break;

			case 1:
				GBlurImageOps.mean(input, output, radius, storage);
				break;

			case 2:
				GBlurImageOps.median(input, output, radius);
			break;
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ConvertBufferedImage.convertTo(output, renderedImage);
				gui.repaint();
			}
		});
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager manager = getInputManager();

		BufferedImage image = manager.loadImage(index);
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
				new Thread() {
					public void run() {
						performUpdate();
					}
				}.start();

			}});
	}

	public static void main(String args[]) {

//		ShowImageBlurApp<ImageFloat32> app
//				= new ShowImageBlurApp<ImageFloat32>(ImageFloat32.class);
		ShowImageBlurApp<ImageUInt8> app
				= new ShowImageBlurApp<ImageUInt8>(ImageUInt8.class);

		ImageListManager manager = new ImageListManager();
		manager.add("shapes","data/shapes01.png");
		manager.add("sunflowers","data/sunflowers.png");
		manager.add("beach","data/scale/beach02.jpg");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Image Derivative");
	}
}
