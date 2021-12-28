/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.scene.ImageClassifier;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a set of images and what their assigned labels are
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ImageClassificationPanel extends JPanel implements ListSelectionListener {
	JTextArea textArea = new JTextArea();

	JScrollPane listScroll;
	private JList listPanel;
	JLayeredPane centerPanel;
	private ImagePanel centerImage = new ImagePanel();
	DefaultListModel listModel = new DefaultListModel();

	final List<Image> results = new ArrayList<>();

	public ImageClassificationPanel() {
		super(new BorderLayout());

		listPanel = new JList(listModel);

		listPanel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listPanel.setSelectedIndex(0);
		listPanel.addListSelectionListener(this);

		listScroll = new JScrollPane(listPanel);
		listScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		centerPanel = new JLayeredPane();
		centerPanel.setPreferredSize(new Dimension(600, 600));
		centerPanel.add(textArea, 0);
		centerPanel.add(centerImage, 1);

		centerImage.setScaling(ScaleOptions.DOWN);
		centerImage.setOpaque(true);


		textArea.setFont(new Font("Courier New", Font.BOLD, 16));
		textArea.setLineWrap(false);
		textArea.setOpaque(true);
		textArea.setForeground(Color.BLACK);
		textArea.setBackground(new Color(255, 255, 255, 125));

		add(centerPanel, BorderLayout.CENTER);
		add(listScroll, BorderLayout.WEST);
	}

	public void addImage( BufferedImage image, String name,
						  List<ImageClassifier.Score> scores, List<String> categories ) {
		synchronized (results) {
			final Image a = new Image();
			a.image = image;
			a.name = name;
			for (ImageClassifier.Score score : scores) {
				a.results.add(String.format("%6.2f %s", score.score, categories.get(score.category)));
			}
			results.add(a);

			SwingUtilities.invokeLater(() -> {
				listModel.addElement(a.name);
				if (listModel.size() == 1) {
					listPanel.setSelectedIndex(0);
				}
				// update the list's size
				Dimension d = listPanel.getMinimumSize();
				listPanel.setPreferredSize(new Dimension(d.width + listScroll.getVerticalScrollBar().getWidth(), d.height));
				validate();
			});
		}
		repaint();
	}

	@Override
	public void valueChanged( ListSelectionEvent e ) {
		if (e.getValueIsAdjusting())
			return;

		final int index = listPanel.getSelectedIndex();
		if (index >= 0) {
			final Image selected;
			synchronized (results) {
				selected = results.get(index);
			}

			SwingUtilities.invokeLater(() -> {
				String text = "";
				int N = Math.min(5, selected.results.size());
				for (String s : selected.results.subList(0, N)) {
					text += s + "\n";
				}
				textArea.setText(text);

				Dimension tp = textArea.getPreferredSize();
				textArea.setBounds(0, 0, tp.width, tp.height);

				int w = Math.min(selected.image.getWidth(), centerPanel.getWidth());
				int h = Math.min(selected.image.getHeight(), centerPanel.getHeight());

				centerImage.setBounds(0, 0, w, h);
				centerImage.setImageRepaint(selected.image);
			});
		}
	}

	@SuppressWarnings({"NullAway.Init"})
	private static class Image {
		BufferedImage image;
		String name;

		List<String> results = new ArrayList<>();
	}
}
