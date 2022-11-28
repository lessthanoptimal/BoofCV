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

package boofcv.app.aztec;

import boofcv.alg.fiducial.aztec.AztecCode;
import boofcv.generate.PaperSize;
import boofcv.generate.Unit;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

/**
 * Selects various parameters when generating a Micro QR Code document
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAble.Init"})
public class CreateAztecCodeControlPanel extends StandardAlgConfigPanel implements ActionListener {
	JTextArea messageField = new JTextArea();
	JComboBox<String> comboOutputFormat = new JComboBox<>(new String[]{"pdf", "png", "bmp", "jpg", "ppm", "pgm"});
	JComboBox<String> comboError = new JComboBox<>();
	JComboBox<String> comboLayers = new JComboBox<>();
	JComboBox<String> comboStructure = combo(1, (Object[])AztecCode.Structure.values());
	JCheckBox checkFillGrid;
	JCheckBox checkDrawGrid;
	JCheckBox checkHideInfo;
	JComboBox<PaperSize> comboPaper = new JComboBox<>(PaperSize.values().toArray(new PaperSize[0]));

	JComboBox<Unit> comboUnits = new JComboBox<>(Unit.values());
	JFormattedTextField fieldMarkerWidth = BoofSwingUtil.createTextField(3.0, 0.0, Double.NaN);

	String message = "Enter Text Here";
	int errorFraction = -1;
	int numLayers = 0;
	AztecCode.Structure structure = AztecCode.Structure.FULL;
	PaperSize paperSize;
	boolean fillGrid = false;
	public boolean drawGrid = false;
	boolean hideInfo = false;
	String format;

	Unit documentUnits = Unit.CENTIMETER;
	double markerWidth = 3;

	Listener listener;

	public CreateAztecCodeControlPanel( final Listener listener ) {
		this.listener = listener;

		format = (String)comboOutputFormat.getSelectedItem();
		comboOutputFormat.addActionListener(this);

		messageField.setText(message);
		messageField.setPreferredSize(new Dimension(200, 300));
		messageField.setLineWrap(true);
		messageField.setWrapStyleWord(true);
		messageField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate( DocumentEvent e ) {
				message = messageField.getText();
				listener.controlsUpdates();
			}

			@Override
			public void removeUpdate( DocumentEvent e ) {
				message = messageField.getText();
				listener.controlsUpdates();
			}

			@Override
			public void changedUpdate( DocumentEvent e ) {
				message = messageField.getText();
				listener.controlsUpdates();
			}
		});

		updateComboLayers();
		comboLayers.addActionListener(this);
		comboLayers.setMaximumSize(comboLayers.getPreferredSize());

		comboError.addItem("AUTOMATIC");
		for (int i = 0; i <= 10; i++) {
			comboError.addItem("" + i);
		}
		comboError.setSelectedIndex(0);
		comboError.addActionListener(this);
		comboError.setMaximumSize(comboError.getPreferredSize());

		comboPaper.setSelectedItem(PaperSize.LETTER);
		comboPaper.addActionListener(this);
		comboPaper.setMaximumSize(comboPaper.getPreferredSize());
		paperSize = comboPaper.getItemAt(comboPaper.getSelectedIndex());

		checkFillGrid = checkbox("Fill Grid", fillGrid);
		checkDrawGrid = checkbox("Draw Grid", drawGrid);
		checkHideInfo = checkbox("Hide Info", hideInfo);

		add(new JScrollPane(messageField));
		addLabeled(comboOutputFormat, "Output Format");
		addLabeled(comboPaper, "Paper Size");
		add(createMarkerWidthPanel());
		add(createFlagPanel());
		add(new JSeparator());
		addLabeled(comboError, "Error");
		addLabeled(comboLayers, "Layers");
		addLabeled(comboStructure, "Structure");
	}

	private void updateComboLayers() {
		comboLayers.removeAllItems();
		comboLayers.addItem("AUTOMATIC");
		for (int i = 1; i <= structure.getMaxDataLayers(); i++) {
			comboLayers.addItem("" + i);
		}
	}

	private JPanel createFlagPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(checkFillGrid);
		panel.add(checkDrawGrid);
		panel.add(checkHideInfo);
		return panel;
	}

	private JPanel createMarkerWidthPanel() {
		fieldMarkerWidth.setPreferredSize(new Dimension(50, 20));
		fieldMarkerWidth.setMaximumSize(fieldMarkerWidth.getPreferredSize());
		fieldMarkerWidth.setValue(markerWidth);
//		fieldMarkerWidth.addPropertyChangeListener(new PropertyChangeListener() {
//			@Override
//			public void propertyChange(PropertyChangeEvent evt) {
//				markerWidth = (Double)fieldMarkerWidth.getValue();
//				System.out.println("Marker width "+markerWidth);
//			}
//		});
		// save the numeric value whenever it's valid. This way when the user selects print it will use the size
		// that the user has typed in even if they didn't hit enter
		fieldMarkerWidth.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate( DocumentEvent e ) {
				try {
					markerWidth = ((Number)fieldMarkerWidth.getFormatter().stringToValue(fieldMarkerWidth.getText())).doubleValue();
				} catch (ParseException ignore) {
				}
			}

			@Override
			public void removeUpdate( DocumentEvent e ) {insertUpdate(e);}

			@Override
			public void changedUpdate( DocumentEvent e ) {insertUpdate(e);}
		});
		comboUnits.setSelectedIndex(documentUnits.ordinal());
		comboUnits.setMaximumSize(comboUnits.getPreferredSize());
		comboUnits.addActionListener(this);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(new JLabel("Marker Width"));
		panel.add(Box.createRigidArea(new Dimension(10, 10)));
		panel.add(fieldMarkerWidth);
		panel.add(comboUnits);
		return panel;
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if (e.getSource() == comboLayers) {
			numLayers = comboLayers.getSelectedIndex();
			listener.controlsUpdates();
		} else if (e.getSource() == comboError) {
			if (comboError.getSelectedIndex() == 0) {
				errorFraction = -1;
			} else {
				errorFraction = comboError.getSelectedIndex() - 1;
			}
			listener.controlsUpdates();
		} else if (e.getSource() == comboStructure) {
			structure = AztecCode.Structure.values()[comboStructure.getSelectedIndex()];
			updateComboLayers();
			comboLayers.validate();
			listener.controlsUpdates();
		} else if (e.getSource() == comboPaper) {
			paperSize = (PaperSize)comboPaper.getSelectedItem();
		} else if (e.getSource() == checkHideInfo) {
			hideInfo = checkHideInfo.isSelected();
		} else if (e.getSource() == checkFillGrid) {
			fillGrid = checkFillGrid.isSelected();
		} else if (e.getSource() == checkDrawGrid) {
			drawGrid = checkDrawGrid.isSelected();
		} else if (e.getSource() == comboUnits) {
			documentUnits = (Unit)comboUnits.getSelectedItem();
		} else if (e.getSource() == comboOutputFormat) {
			format = (String)comboOutputFormat.getSelectedItem();
			// toggle controls depending on type of output format
			boolean enable = comboOutputFormat.getSelectedIndex() == 0;
			comboPaper.setEnabled(enable);
			checkHideInfo.setEnabled(enable);
			checkFillGrid.setEnabled(enable);
			comboUnits.setEnabled(enable);
			fieldMarkerWidth.setEnabled(enable);
		}
	}

	public interface Listener {
		void controlsUpdates();
	}
}
