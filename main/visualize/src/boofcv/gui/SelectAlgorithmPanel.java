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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;


/**
 * Provides a pull down list form which the user can select which algorithm to run.  After
 * it has been selected the input should be processed and displayed.
 *
 * @author Peter Abeles
 */
public abstract class SelectAlgorithmPanel extends JPanel
		implements ActionListener
{
	JToolBar toolbar;
	JComboBox algBox;
	List<Object> algCookies = new ArrayList<>();
	Component gui;

	public SelectAlgorithmPanel() {
		super(new BorderLayout());

		toolbar = new JToolBar();
		algBox = new JComboBox();
		algBox.setMaximumSize(algBox.getPreferredSize());
		toolbar.add(algBox);

		algBox.addActionListener(this);

		toolbar.add(Box.createHorizontalGlue());
		add(toolbar, BorderLayout.PAGE_START);
	}

	/**
	 * Used to add the main GUI to this panel.   Must use this function.
	 * Algorithm change events will not be posted until this function has been set.
	 *
	 * @param gui The main GUI being displayed.
	 */
	public void setMainGUI( final Component gui ) {
		this.gui = gui;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				add(gui,BorderLayout.CENTER);
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
			final Object cookie = algCookies.get(algBox.getSelectedIndex());
			final String name = (String)algBox.getSelectedItem();

			new Thread() {
				public void run() {
					performSetAlgorithm(name, cookie);
				}
			}.start();
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

	public abstract void setActiveAlgorithm( String name , Object cookie );

}
