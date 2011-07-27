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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of items and their respective data.
 *
 * @author Peter Abeles
 */
public class ListDisplayPanel extends JPanel implements ListSelectionListener , ComponentListener {

	List<JPanel> panels = new ArrayList<JPanel>();
	private JSplitPane splitPane;
	private JList listPanel;

	DefaultListModel listModel = new DefaultListModel();

	public ListDisplayPanel() {
		listPanel = new JList(listModel);

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, new JPanel());
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(100);

		listPanel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listPanel.setSelectedIndex(0);
		listPanel.addListSelectionListener(this);


		add(splitPane);
		addComponentListener(this);
	}

	/**
	 * Displays a new image in the list.
	 *
	 * @param image
	 * @param name
	 */
	public void addImage( BufferedImage image , String name ) {
		addItem(new ImagePanel(image), name );
	}

	/**
	 * Displays a new JPanel in the list.
	 *
	 * @param panel
	 * @param name
	 */
	public void addItem( JPanel panel , String name ) {
		panels.add(panel);
		listModel.addElement(name);

		if( listModel.size() == 1 ) {
			listPanel.setSelectedIndex(0);
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		System.out.println("Enter value change");
		if( e.getValueIsAdjusting() )
			return;
		System.out.println("changed "+listPanel.getWidth()+" "+listPanel.getPreferredSize().getWidth());

		int index = listPanel.getSelectedIndex();
		splitPane.setRightComponent(panels.get(index));
		splitPane.repaint();
	}

	@Override
	public void componentResized(ComponentEvent e) {
		int w = e.getComponent().getWidth();
		int h = e.getComponent().getHeight();

		splitPane.setPreferredSize(new Dimension(w, h));
		splitPane.repaint();
	}

	@Override
	public void componentMoved(ComponentEvent e) {
	}

	@Override
	public void componentShown(ComponentEvent e) {

	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}

	/**
	 * Adjust the display size for a panel with the specified dimensions.  Useful for when
	 * the data being displayed is different sizes.
	 * 
	 * @param width
	 * @param height
	 */
	public void setDataDisplaySize(int width, int height) {

		int w = width + splitPane.getDividerLocation();
		int h = (int)Math.max(height,listPanel.getPreferredSize().getHeight());


		splitPane.setPreferredSize(new Dimension(w,h));
	}
}
