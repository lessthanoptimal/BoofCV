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
public abstract class SelectAlgorithmPanel extends JPanel implements ActionListener {
	JToolBar toolbar;
	JComboBox algBox;

	List<Object> cookies = new ArrayList<Object>();

	public SelectAlgorithmPanel() {
		toolbar = new JToolBar();
		algBox = new JComboBox();
		toolbar.add(algBox);

		algBox.addActionListener(this);

		setLayout(new BorderLayout());
		add(toolbar, BorderLayout.PAGE_START);
	}

	public void addAlgorithm( final String name , Object cookie ) {
		cookies.add(cookie);
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
		Object cookie = cookies.get(algBox.getSelectedIndex());
		String name = (String)algBox.getSelectedItem();
		performSetAlgorithm(cookie, name);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JComboBox cb = (JComboBox)e.getSource();

		final Object cookie = cookies.get(cb.getSelectedIndex());
		final String name = (String)cb.getSelectedItem();
		
		new Thread() {
			public void run() {
				performSetAlgorithm(cookie, name);
			}
		}.start();
	}

	private void performSetAlgorithm(Object cookie, String name) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				algBox.setEnabled(false);
				}});
		setActiveAlgorithm( name , cookie );
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				algBox.setEnabled(true);
			}});
	}

	public abstract void setActiveAlgorithm( String name , Object cookie );
}
