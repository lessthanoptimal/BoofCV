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

package boofcv.gui;

import boofcv.gui.image.ImagePanel;
import boofcv.io.MediaManager;
import boofcv.io.PathLabel;
import boofcv.io.wrapper.DefaultMediaManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;


/**
 * Provides pull a menubar for selecting the input source and which algorithm to use
 *
 * @author Peter Abeles
 */
public abstract class SelectAlgorithmAndInputPanel extends JPanel
		implements ActionListener, VisualizeApp
{
	JToolBar toolbar;
	// each combo box is used to select different algorithms
	JComboBox algBoxes[];
	// used to select the input image
	JComboBox imageBox;
	// when selected it shows the original image
	protected JCheckBox originalCheck;
	List<Object> algCookies[];
	// list of input names and where to get the inputs
	protected List<PathLabel> inputRefs;
	protected String baseDirectory="";

	// components which had been externally added
	List<JComponent> addedComponents = new ArrayList<>();

	// what the original image was before any processing
	protected BufferedImage inputImage;
	// panel used for displaying the original image
	ImagePanel origPanel = new ImagePanel();
	// the main GUI being displayed
	Component gui;
	// should it post algorithm change events yet?
	boolean postAlgorithmEvents = false;

	// abstract way of reading in media
	protected MediaManager media = DefaultMediaManager.INSTANCE;

	public SelectAlgorithmAndInputPanel(int numAlgFamilies) {
		super(new BorderLayout());
		toolbar = new JToolBar();

		imageBox = new JComboBox();
		toolbar.add(imageBox);
		imageBox.addActionListener(this);
		imageBox.setMaximumSize(imageBox.getPreferredSize());

		algBoxes = new JComboBox[numAlgFamilies];
		algCookies = new List[numAlgFamilies];
		for( int i = 0; i < numAlgFamilies; i++ ) {
			JComboBox b = algBoxes[i] = new JComboBox();
			toolbar.add( b);
			b.addActionListener(this);
			b.setMaximumSize(b.getPreferredSize());
			algCookies[i] = new ArrayList<>();
		}

		toolbar.add(Box.createHorizontalGlue());
		
		originalCheck = new JCheckBox("Show Input");
		toolbar.add(originalCheck);
		originalCheck.addActionListener(this);

		originalCheck.setEnabled(false);

		add(toolbar, BorderLayout.PAGE_START);
	}

	/**
	 * Loads a standardized file for input references
	 *
	 * @param fileName path to config file
	 */
	@Override
	public void loadInputData(String fileName) {
		Reader r = media.openFile(fileName);

		List<PathLabel> refs = new ArrayList<>();
		try {
			BufferedReader reader = new BufferedReader(r);

			String line;
			while( (line = reader.readLine()) != null ) {

				String[]z = line.split(":");
				String[] names = new String[z.length-1];
				for( int i = 1; i < z.length; i++ ) {
					names[i-1] = baseDirectory+z[i];
				}

				refs.add(new PathLabel(z[0],names));
			}

			setInputList(refs);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sets the directory that relative references are relative too
	 */
	public void setBaseDirectory(String baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	/**
	 * Adds a new component into the toolbar.
	 *
	 * @param comp The component being added
	 */
	public void addToToolbar( JComponent comp ) {
		toolbar.add(comp,1+algBoxes.length);
		toolbar.revalidate();
		addedComponents.add(comp);
	}

	public void removeFromToolbar( JComponent comp ) {
		toolbar.remove(comp);
		toolbar.revalidate();
		addedComponents.remove(comp);
	}

	/**
	 * Used to add the main GUI to this panel.   Must use this function.
	 * Algorithm change events will not be posted until this function has been set.
	 *
	 * @param gui The main GUI being displayed.
	 */
	public void setMainGUI( final Component gui ) {
		postAlgorithmEvents = true;
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

	/**
	 * Specifies a list of images to use as input and loads them
	 *
	 * @param inputRefs Name of input and where to get it
	 */
	public void setInputList(final List<PathLabel> inputRefs) {
		this.inputRefs = inputRefs;
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for( int i = 0; i < inputRefs.size(); i++ ) {
					imageBox.addItem(inputRefs.get(i).getLabel());
				}
			}});
	}


	public void addAlgorithm(final int indexFamily, final String name, Object cookie) {
		algCookies[indexFamily].add(cookie);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				algBoxes[indexFamily].addItem(name);
			}});
	}

	/**
	 * Grabs the currently selected algorithm, passes information to GUI for updating, toggles GUI
	 * being active/not.  refreshAll() is called in a new thread.
	 *
	 */
	public void doRefreshAll() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// collect the current state inside the GUI thread
				final Object state[] = new Object[ algCookies.length ];
				for( int i = 0; i < state.length; i++ ) {
					state[i] = algCookies[i].get(algBoxes[i].getSelectedIndex());
				}
				// create a new thread to process this change
				new Thread() {
					public void run() {
						setActiveGUI(false);
						refreshAll(state);
						setActiveGUI(true);
					}
				}.start();
			}});
	}

	/**
	 * Enables/disables the ability to interact with the algorithms GUI.
	 */
	private void setActiveGUI( final boolean isEnabled ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				toolbar.setEnabled(isEnabled);
				for( JComboBox b : algBoxes ) {
					b.setEnabled(isEnabled);
				}
				for( JComponent b : addedComponents ) {
					b.setEnabled(isEnabled);
				}
				imageBox.setEnabled(isEnabled);
			}
		});
	}

	/**
	 * Returns the cookie associated with the specified algorithm family.
	 */
	protected <T> T getAlgorithmCookie( int indexFamily ) {
		return (T)algCookies[indexFamily].get( algBoxes[indexFamily].getSelectedIndex() );
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		for( int i = 0; i < algBoxes.length; i++ ) {
			if( algBoxes[i] == e.getSource() ) {
				// see if its ready to start posting these events
				if( !postAlgorithmEvents )
					return;

				// notify the main GUI to change the input algorithm
				final Object cookie = algCookies[i].get(algBoxes[i].getSelectedIndex());
				final String name = (String)algBoxes[i].getSelectedItem();
				final int indexFamily = i;

				new Thread() {
					public void run() {
						performSetAlgorithm(indexFamily,name, cookie);
					}
				}.start();
				return;
			}
		}

		if( e.getSource() == imageBox ) {
			// notify the main GUI to change the input image
			final String name = (String)imageBox.getSelectedItem();
			new Thread() {
				public void run() {
					performChangeInput(name, imageBox.getSelectedIndex());
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

	private void performSetAlgorithm( int indexFamily , String name, Object cookie) {
		setActiveGUI(false);
		setActiveAlgorithm(indexFamily, name , cookie );
		setActiveGUI(true);
	}

	private void performChangeInput(String name, int index) {
		setActiveGUI(false);
		changeInput(name, index);
		setActiveGUI(true);
	}

	@Override
	public void setMediaManager( MediaManager manager) {
		this.media = manager;
	}

	/**
	 * Provides the current state of all selected algorithms.
	 *
	 * @param cookies state of each selected algorithm.
	 */
	public abstract void refreshAll( Object[] cookies );

	/**
	 * A request has been made to change the processing algorithm.  NOT called from a GUI thread.
	 *
	 * @param indexFamily
	 * @param name Display name of the algorithm.
	 * @param cookie Reference to user defined data.
	 */
	public abstract void setActiveAlgorithm(int indexFamily, String name, Object cookie);

	/**
	 * A request to change the input image has been made.  The input image's label and its index in the
	 * manager are returned.
	 *
	 * @param name Display name of the image.
	 * @param index Which image in the list.
	 */
	public abstract void changeInput(String name, int index);
}
