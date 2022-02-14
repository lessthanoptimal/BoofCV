/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.fiducial;

import boofcv.alg.fiducial.aztec.AztecCode;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays encoded Aztec Code messages.
 *
 * @author Peter Abeles
 */
public class DetectAztecCodeMessagePanel extends StandardAlgConfigPanel
		implements ListSelectionListener {

	Listener listener;
	JList listDetected;
	JTextArea textArea = new JTextArea();

	List<AztecCode> detected = new ArrayList<>();
	List<AztecCode> failures = new ArrayList<>();

	public DetectAztecCodeMessagePanel( Listener listener ) {
		this.listener = listener;

		listDetected = new JList();
		listDetected.setModel(new DefaultListModel());
		listDetected.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		listDetected.setLayoutOrientation(JList.VERTICAL);
		listDetected.setVisibleRowCount(-1);
		listDetected.addListSelectionListener(this);

		textArea.setEditable(false);
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);

		// ensures that the split pane can be dragged
		Dimension minimumSize = new Dimension(0, 0);
		listDetected.setMinimumSize(minimumSize);
		textArea.setMinimumSize(minimumSize);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(listDetected), textArea);
		splitPane.setDividerLocation(150);
		splitPane.setPreferredSize(new Dimension(200, 0));

		addAlignCenter(new JLabel("Aztec Codes"));
		addAlignCenter(splitPane);
	}

	public void updateList( List<AztecCode> detected, List<AztecCode> failures ) {
		BoofSwingUtil.checkGuiThread();

		this.listDetected.removeListSelectionListener(this);
		DefaultListModel<String> model = (DefaultListModel)listDetected.getModel();
		model.clear();

		this.detected.clear();
		for (int i = 0; i < detected.size(); i++) {
			AztecCode marker = detected.get(i);
			String shortName = marker.structure.toString().substring(0, 4);
			model.addElement(String.format("%4s L=%02d %.10s", shortName, marker.dataLayers, marker.message));
			this.detected.add(marker.copy());
		}
		this.failures.clear();
		for (int i = 0; i < failures.size(); i++) {
			AztecCode marker = failures.get(i);
			String shortName = marker.structure.toString().substring(0, 4);
			model.addElement(String.format("%4s L=%02d %s", shortName, marker.dataLayers, marker.failure));
			this.failures.add(marker.copy());
		}
		listDetected.invalidate();
		listDetected.repaint();

		textArea.setText("");

		this.listDetected.addListSelectionListener(this);
	}

	@Override
	public void valueChanged( ListSelectionEvent e ) {
		if (e.getValueIsAdjusting())
			return;
		if (e.getSource() == listDetected) {
			int selected = listDetected.getSelectedIndex();
			if (selected == -1)
				return;

			boolean failed = selected >= detected.size();

			if (failed) {
				selected -= detected.size();
				AztecCode marker = failures.get(selected);
				listener.selectedMarkerInList(selected, true);
				setMarkerMessageText(marker, true);
			} else {
				listener.selectedMarkerInList(selected, false);
				AztecCode marker = detected.get(selected);
				setMarkerMessageText(marker, false);
			}
			textArea.invalidate();
		}
	}

	private void setMarkerMessageText( AztecCode marker, boolean failure ) {
		double errorLevel = marker.getCorrectionLevel();

		if (failure) {
			textArea.setText(String.format("%5s Layers %2d  ECC %.1f\nMessage Words %d\nCause: %s",
					marker.structure, marker.dataLayers, errorLevel, marker.messageWordCount, marker.failure));
		} else {
			textArea.setText(String.format("%5s Layers %2d  ECC %.1f\nMessage Words %d\nBit Errors %d\n\n'%s'",
					marker.structure, marker.dataLayers, errorLevel, marker.messageWordCount, marker.totalBitErrors, marker.message));
		}
	}

	public void setSelectedMarker( int index, boolean failure ) {
		BoofSwingUtil.checkGuiThread();
		this.listDetected.removeListSelectionListener(this);

		if (failure) {
			if (index < failures.size()) {
				listDetected.setSelectedIndex(index + detected.size());
				setMarkerMessageText(failures.get(index), true);
			}
		} else {
			if (index < detected.size()) {
				listDetected.setSelectedIndex(index);
				setMarkerMessageText(detected.get(index), false);
			}
		}

		this.listDetected.addListSelectionListener(this);
	}

	public interface Listener {
		void selectedMarkerInList( int index, boolean failure );
	}
}
