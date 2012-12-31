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

package boofcv.alg.feature.detect.interest;


import boofcv.gui.feature.ScaleSpacePointPanel;
import boofcv.struct.gss.GaussianScaleSpace;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Shows the selected scale space and allows the user to change the selected scale space
 *
 * @author Peter Abeles
 */
public class SelectScaleSpacePanel extends JPanel implements ListSelectionListener, MouseListener {
	// shows the current scale space
	private JList listPanel;
	private DefaultListModel listModel = new DefaultListModel();
	ScaleSpacePointPanel gui;

	public SelectScaleSpacePanel(  GaussianScaleSpace ss , ScaleSpacePointPanel gui )
	{
		super(new BorderLayout());

		this.gui = gui;
		gui.addMouseListener(this);

		listPanel = new JList(listModel);

		listPanel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listPanel.setSelectedIndex(0);
		listModel.addElement("All");
		for( int i = 0; i < ss.getTotalScales(); i++ ) {
			listModel.addElement(String.format("%4.1f",ss.getScale(i)));
		}
		listPanel.addListSelectionListener(this);

		add(listPanel, BorderLayout.CENTER);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// rotate through the levels
		int selected = listPanel.getSelectedIndex()+1;
		if( selected >= listModel.size())
		    selected = 0;
		listPanel.setSelectedIndex(selected);
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		gui.setLevel(listPanel.getSelectedIndex());
		gui.repaint();
		gui.requestFocusInWindow();
	}

	public void reset() {
		if( listPanel.getSelectedIndex() != 0 )
			listPanel.setSelectedIndex(0);
		else
			gui.repaint();
	}
}
