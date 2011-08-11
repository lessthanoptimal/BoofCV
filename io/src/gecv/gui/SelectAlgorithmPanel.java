/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;


/**
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

	public void addAlgorithm( String name , Object cookie ) {
		cookies.add(cookie);
		algBox.addItem(name);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JComboBox cb = (JComboBox)e.getSource();
		Object cookie = cookies.get(cb.getSelectedIndex());
		setActiveAlgorithm( (String)cb.getSelectedItem() , cookie );
	}

	public abstract void setActiveAlgorithm( String name , Object cookie );
}
