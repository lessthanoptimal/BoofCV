/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.app.qrcode;

import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCodeMaskPattern;
import boofcv.app.PaperSize;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.misc.Unit;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

/**
 * Selects various parameters when generating a QR Code document
 *
 * @author Peter Abeles
 */
public class CreateQrCodeControlPanel extends StandardAlgConfigPanel implements ActionListener
{
	JTextArea messageField= new JTextArea();
	JComboBox<String> comboOutputFormat = new JComboBox<>(new String[]{"pdf","png","bmp","jpg","ppm","pgm"});
	JComboBox<String> comboVersion = new JComboBox<>();
	JComboBox<String> comboError = new JComboBox<>();
	JComboBox<String> comboPattern = new JComboBox<>();
	JComboBox<String> comboMode = new JComboBox<>();
	JCheckBox checkFillGrid;
	JCheckBox checkHideInfo;
	JComboBox<PaperSize> comboPaper = new JComboBox<>(PaperSize.values().toArray(new PaperSize[0]));

	JComboBox<Unit> comboUnits = new JComboBox<>(Unit.values());
	JFormattedTextField fieldMarkerWidth = BoofSwingUtil.createTextField(3.0,0.0,Double.NaN);

	int version=-1;
	String message = "Enter Text Here";
	QrCode.ErrorLevel error = null;
	QrCodeMaskPattern mask = null;
	QrCode.Mode mode = null;
	PaperSize paperSize;
	boolean fillGrid=false;
	boolean hideInfo=false;
	String format;

	Unit documentUnits = Unit.CENTIMETER;
	double markerWidth = 3;

	Listener listener;

	public CreateQrCodeControlPanel(final Listener listener ) {
		this.listener = listener;

		format = (String)comboOutputFormat.getSelectedItem();
		comboOutputFormat.addActionListener(this);

		messageField.setText(message);
		messageField.setPreferredSize(new Dimension(200,300));
		messageField.setLineWrap(true);
		messageField.setWrapStyleWord(true);
		messageField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				message = messageField.getText();
				listener.controlsUpdates();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				message = messageField.getText();
				listener.controlsUpdates();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				message = messageField.getText();
				listener.controlsUpdates();
			}
		});

		comboVersion.addItem("AUTOMATIC");
		for (int i = 1; i <= 40; i++) {
			comboVersion.addItem(""+i);
		}
		comboVersion.addActionListener(this);
		comboVersion.setMaximumSize(comboVersion.getPreferredSize());

		for (QrCode.ErrorLevel e : QrCode.ErrorLevel.values() ) {
			comboError.addItem(e.name());
		}
		comboError.setSelectedIndex(QrCode.ErrorLevel.M.ordinal());
		comboError.addActionListener(this);
		comboError.setMaximumSize(comboError.getPreferredSize());

		comboPattern.addItem("AUTOMATIC");
		for ( QrCodeMaskPattern e : QrCodeMaskPattern.values() ) {
			comboPattern.addItem(e.toString().substring(1,4));
		}
		comboPattern.addActionListener(this);
		comboPattern.setMaximumSize(comboPattern.getPreferredSize());

		comboMode.addItem("AUTOMATIC");
		comboMode.addItem(QrCode.Mode.NUMERIC.name());
		comboMode.addItem(QrCode.Mode.ALPHANUMERIC.name());
		comboMode.addItem(QrCode.Mode.BYTE.name());
		comboMode.addItem(QrCode.Mode.KANJI.name());
		comboMode.addActionListener(this);
		comboMode.setMaximumSize(comboMode.getPreferredSize());

		comboPaper.setSelectedItem(PaperSize.LETTER);
		comboPaper.addActionListener(this);
		comboPaper.setMaximumSize(comboPaper.getPreferredSize());
		paperSize = comboPaper.getItemAt(comboPaper.getSelectedIndex());

		checkFillGrid = checkbox("Fill Grid",fillGrid);
		checkHideInfo = checkbox("Hide Info",hideInfo);

		add(new JScrollPane(messageField));
		addLabeled(comboOutputFormat,"Output Format");
		addLabeled(comboPaper,"Paper Size");
		add(createMarkerWidthPanel());
		add(createFlagPanel());
		add(new JSeparator());
		addLabeled(comboVersion,"Version");
		addLabeled(comboError,"Error");
		addLabeled(comboPattern,"Pattern");
		addLabeled(comboMode,"Mode");
	}

	private JPanel createFlagPanel() {
		JPanel panel = new JPanel(new GridLayout(0,2));
		panel.add(checkFillGrid);
		panel.add(checkHideInfo);
		return panel;
	}

	private JPanel createMarkerWidthPanel() {
		fieldMarkerWidth.setPreferredSize(new Dimension(50,20));
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
			public void insertUpdate(DocumentEvent e) {
				try {
					markerWidth = ((Number)fieldMarkerWidth.getFormatter().stringToValue(fieldMarkerWidth.getText())).doubleValue();
				} catch (ParseException ignore) {}
			}

			@Override
			public void removeUpdate(DocumentEvent e) {insertUpdate(e);}

			@Override
			public void changedUpdate(DocumentEvent e) {insertUpdate(e);}
		});
		comboUnits.setSelectedIndex(documentUnits.ordinal());
		comboUnits.setMaximumSize(comboUnits.getPreferredSize());
		comboUnits.addActionListener(this);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.add(new JLabel("Marker Width"));
		panel.add(Box.createRigidArea(new Dimension(10,10)));
		panel.add(fieldMarkerWidth);
		panel.add(comboUnits);
		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == comboVersion ) {
			version = comboVersion.getSelectedIndex();
			listener.controlsUpdates();
		} else if( e.getSource() == comboError ) {
			error = QrCode.ErrorLevel.lookup(comboError.getSelectedIndex());
			listener.controlsUpdates();
		} else if( e.getSource() == comboPattern ) {
			if( comboPattern.getSelectedIndex() == 0 ) {
				mask = null;
			} else {
				mask = QrCodeMaskPattern.lookupMask((String)comboPattern.getSelectedItem());
			}
			listener.controlsUpdates();
		} else if( e.getSource() == comboMode ) {
			if( comboMode.getSelectedIndex() == 0 ) {
				mode = null;
			} else {
				mode = QrCode.Mode.lookup((String)comboPattern.getSelectedItem());
			}
			listener.controlsUpdates();
		} else if( e.getSource() == comboPaper ) {
			paperSize = (PaperSize)comboPaper.getSelectedItem();
		} else if( e.getSource() == checkHideInfo ) {
			hideInfo = checkHideInfo.isSelected();
		} else if( e.getSource() == checkFillGrid ) {
			fillGrid = checkFillGrid.isSelected();
		} else if( e.getSource() == comboUnits ) {
			documentUnits = (Unit) comboUnits.getSelectedItem();
		} else if( e.getSource() == comboOutputFormat ) {
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
