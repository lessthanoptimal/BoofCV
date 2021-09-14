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

package boofcv.demonstrations.calibration;

import boofcv.gui.StandardAlgConfigPanel;
import boofcv.misc.BoofLambdas;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray_B;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of images and provides buttons for modifying points and images.
 *
 * @author Peter Abeles
 */
public class CalibrationListPanel extends StandardAlgConfigPanel implements ListSelectionListener {
	public final JButton bRemovePoint = button("Remove Point", true, e -> {});
	public final JButton bRemoveImage = button("Remove Image", true, e -> {});
	public final JButton bReset = button("Reset", true, e -> {});

	/** Called when the selected image has been changed */
	@Getter @Setter public BoofLambdas.ProcessI selectionChanged = ( index ) -> {};

	JList<String> imageList;
	List<String> imageNames = new ArrayList<>();
	DogArray_B imageSuccess = new DogArray_B();
	int selectedImage = -1;

	public CalibrationListPanel() {
		imageList = new JList<>();
		imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		imageList.addListSelectionListener(this);

		// Highlight images where it failed
		imageList.setCellRenderer(new DefaultListCellRenderer() {
			@Override public Component getListCellRendererComponent( JList list, Object value, int index,
																	 boolean isSelected, boolean cellHasFocus ) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (!imageSuccess.get(index))
					setBackground(Color.RED);
				return c;
			}
		});

		var scroll = new JScrollPane(imageList);

		// use a GridLayout so all buttons are the same size
		var editPanel = new JPanel(new GridLayout(0, 1));
		editPanel.add(bRemovePoint);
		editPanel.add(bRemoveImage);
		editPanel.add(bReset);
		editPanel.setMaximumSize(editPanel.getPreferredSize());

		add(editPanel);
		add(scroll);
	}

	public void addImage( String imageName, boolean success ) {
		imageNames.add(imageName);
		this.imageSuccess.add(success);
		String[] names = imageNames.toArray(new String[0]);
		imageList.removeListSelectionListener(this);
		imageList.setListData(names);
		if (names.length == 1) {
			selectedImage = 0;
			imageList.addListSelectionListener(this);
			imageList.setSelectedIndex(selectedImage);
			validate();
		} else {
			// each time an image is added it resets the selected value
			imageList.setSelectedIndex(selectedImage);
			imageList.addListSelectionListener(this);
		}
	}

	public void clearImages() {
		imageNames.clear();
		imageSuccess.reset();
		imageList.removeListSelectionListener(this);
		imageList.setListData(new String[0]);
		selectedImage = -1;
	}

	public void setSelected( int index ) {
		if (imageList.getSelectedIndex() == index)
			return;
		imageList.setSelectedIndex(index);
	}

	@Override public void valueChanged( ListSelectionEvent e ) {
		if (e.getValueIsAdjusting() || e.getFirstIndex() == -1)
			return;

		int selected = imageList.getSelectedIndex();

		// See if there's no change
		if (selected == selectedImage)
			return;

		selectedImage = selected;
		selectionChanged.process(selected);
	}
}
