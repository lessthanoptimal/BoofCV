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

package gecv.gui.image;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a single display which can switch between different images.  A list of images is provided on the right.
 *
 * @author Peter Abeles
 */
public class PanelListPanel extends JPanel implements ListSelectionListener , ComponentListener {
	// todo put image in a JScrollPane?
	List<JPanel> oanels = new ArrayList<JPanel>();

	private JSplitPane splitPane;
	private JList listPanel;

	DefaultListModel listModel = new DefaultListModel();

	public PanelListPanel() {

		int width = 600;
		int height = 600;

		listPanel = new JList(listModel);

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				new JPanel(), listPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(width-200);

		listPanel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listPanel.setSelectedIndex(0);
		listPanel.addListSelectionListener(this);

		splitPane.setPreferredSize(new Dimension(width, height));
//		listPanel.setMinimumSize(new Dimension(200,height));
//		listPanel.setPreferredSize(new Dimension(200,height));
//		imagePanel.setPreferredSize(new Dimension(width-200,height));
//		splitPane.resetToPreferredSizes();

		add(splitPane);
//		add(BorderLayout.CENTER,imagePanel);
//		add(BorderLayout.EAST,listPanel);
		addComponentListener(this);
	}

	public void addImage( JPanel oanel , String name ) {
		oanels.add(oanel);
		listModel.addElement(name);

		if( oanels.size() == 1 ) {
			listPanel.setSelectedIndex(0);
		}
		splitPane.repaint();
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if( e.getValueIsAdjusting() )
			return;

		int index = listPanel.getSelectedIndex();
		JPanel panel = oanels.get(index);
		splitPane.setLeftComponent(panel);
		splitPane.repaint();
	}

	@Override
	public void componentResized(ComponentEvent e) {
		int w = e.getComponent().getWidth();
		int h = e.getComponent().getHeight();

//		System.out.println("w "+w+" h "+h);
//		splitPane.setMinimumSize(new Dimension(w, h));
		splitPane.setPreferredSize(new Dimension(w, h));
//		splitPane.resetToPreferredSizes();
//		if( splitPane.getWidth() >= 200 ) {
//			splitPane.setDividerLocation(splitPane.getWidth()-200);
//		}
		splitPane.repaint();
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void componentShown(ComponentEvent e) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
