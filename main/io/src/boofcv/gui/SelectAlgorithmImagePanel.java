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

package boofcv.gui;

import boofcv.gui.image.ImagePanel;
import boofcv.io.image.ImageListManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


/**
 * Provides pull a menubar for selecting the input image and which algorithm to use
 *
 * @author Peter Abeles
 */
public abstract class SelectAlgorithmImagePanel extends JPanel
		implements ActionListener
{
	JToolBar toolbar;
	// used to select the algorithm
	JComboBox algBox;
	// used to select the input image
	JComboBox imageBox;
	// when selected it shows the original image
	JCheckBox originalCheck;
	List<Object> algCookies = new ArrayList<Object>();
	ImageListManager imageManager;

	// what the original image was before any processing
	BufferedImage inputImage;
	// panel used for displaying the original image
	ImagePanel origPanel = new ImagePanel();
	// the main GUI being displayed
	Component gui;

	public SelectAlgorithmImagePanel() {
		super(new BorderLayout());
		toolbar = new JToolBar();

		imageBox = new JComboBox();
		toolbar.add(imageBox);
		imageBox.addActionListener(this);

		algBox = new JComboBox();
		toolbar.add(algBox);
		algBox.addActionListener(this);

		originalCheck = new JCheckBox("Show Input");
		toolbar.add(originalCheck);
		originalCheck.addActionListener(this);

		originalCheck.setEnabled(false);

		add(toolbar, BorderLayout.PAGE_START);
	}

	/**
	 * Used to add the main GUI to this panel.   Must use this function.
	 *
	 * @param gui The main GUI being displayed.
	 */
	public void addMainGUI( final Component gui ) {
		this.gui = gui;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				add(gui,BorderLayout.CENTER);
			}});
	}

	/**
	 * Specifies an image which contains the original input image.  After this has been called the
	 * view input image widget is activated and when selected this image will be displayed instead
	 * of the main GUI.  This functionality is optional.
	 *
	 * @param image Original input image.
	 */
	public void setInputImage( BufferedImage image ) {
		inputImage = image;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if( inputImage == null ) {
					originalCheck.setEnabled(false);
				} else {
					originalCheck.setEnabled(true);
					origPanel.setBufferedImage(inputImage);
					origPanel.setPreferredSize(new Dimension(inputImage.getWidth(),inputImage.getHeight()));
					origPanel.repaint();
				}
			}});
	}

	public void setImageManager( final ImageListManager manager ) {
		this.imageManager = manager;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for( int i = 0; i < manager.size(); i++ ) {
					imageBox.addItem(manager.getLabel(i));
				}
			}});
	}

	public void addAlgorithm( final String name , Object cookie ) {
		algCookies.add(cookie);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				algBox.addItem(name);
			}});
	}

	/**
	 * Tells it to switch again to the current algorithm.  Useful if the input has changed and information
	 * needs to be rendered again.
	 */
	public void refreshAlgorithm() {
		Object cookie = algCookies.get(algBox.getSelectedIndex());
		String name = (String)algBox.getSelectedItem();
		performSetAlgorithm(name, cookie);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == algBox ) {
			// notify the main GUI to change the input algorithm
			final Object cookie = algCookies.get(algBox.getSelectedIndex());
			final String name = (String)algBox.getSelectedItem();

			new Thread() {
				public void run() {
					performSetAlgorithm(name, cookie);
				}
			}.start();
		} else if( e.getSource() == imageBox ) {
			// notify the main GUI to change the input image
			final String name = (String)imageBox.getSelectedItem();
			new Thread() {
				public void run() {
					performChangeImage(name, imageBox.getSelectedIndex());
				}
			}.start();
		} else if( e.getSource() == originalCheck ) {
			origPanel.setSize(gui.getWidth(),gui.getHeight());
			// swap the main GUI with a picture of the original input image
			if( originalCheck.isSelected() ) {
				remove(gui);
				add(origPanel);
			} else {
				remove(origPanel);
				add(gui);
			}
			validate();
			repaint();
		}
	}

	private void performSetAlgorithm(String name, Object cookie) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				toolbar.setEnabled(false);
				}});
		setActiveAlgorithm( name , cookie );
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				toolbar.setEnabled(true);
			}});
	}

	private void performChangeImage(String name , int index ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				toolbar.setEnabled(false);
				}});
		changeImage( name , index );
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				toolbar.setEnabled(true);
			}});
	}

	public ImageListManager getImageManager() {
		return imageManager;
	}

	/**
	 * A request has been made to change the processing algorithm.
	 *
	 * @param name Display name of the algorithm.
	 * @param cookie Reference to user defined data.
	 */
	public abstract void setActiveAlgorithm( String name , Object cookie );

	/**
	 * A request to change the input image has been made.  The input image's label and its index in the
	 * manager are returned.
	 *
	 * @param name Display name of the image.
	 * @param index Which image in the list.
	 */
	public abstract void changeImage( String name , int index );
}
