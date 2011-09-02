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

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
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
		setLayout(new BorderLayout());
		listPanel = new JList(listModel);

		listPanel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listPanel.setSelectedIndex(0);
		listPanel.addListSelectionListener(this);

		JScrollPane scroll = new JScrollPane(listPanel);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, new JPanel());
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(100);

		add(splitPane);
		addComponentListener(this);
	}

	public void reset() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					panels.clear();
					listModel.removeAllElements();
				}
			});
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
		}
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
	public void addItem( final JPanel panel , final String name ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panels.add(panel);
				listModel.addElement(name);
				splitPane.setDividerLocation((int)listPanel.getPreferredSize().getWidth()+1);
				if( listModel.size() == 1 ) {
					listPanel.setSelectedIndex(0);
				}
			}
		});
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if( e.getValueIsAdjusting() )
			return;

		final int index = listPanel.getSelectedIndex();
		if( index >= 0 ) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					// the split pane likes to screw up the divider location when the
					// right component is changed and a scroll pane is being used on the left
					int loc = splitPane.getDividerLocation();
					splitPane.setRightComponent(panels.get(index));
					splitPane.setDividerLocation(loc);
					splitPane.repaint();
				}
			});
		}
	}

	@Override
	public void componentResized(ComponentEvent e) {
		final int w = e.getComponent().getWidth();
		final int h = e.getComponent().getHeight();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				splitPane.setPreferredSize(new Dimension(w, h));
				splitPane.setDividerLocation((int)listPanel.getPreferredSize().getWidth()+1);
//				splitPane.repaint();
			}
		});
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
