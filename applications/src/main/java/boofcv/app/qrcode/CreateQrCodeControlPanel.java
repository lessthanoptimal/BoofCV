/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Selects various parameters when generating a QR Code document
 *
 * @author Peter Abeles
 */
public class CreateQrCodeControlPanel extends StandardAlgConfigPanel implements ActionListener
{
	JTextArea messageField= new JTextArea();
	JComboBox<String> comboVersion = new JComboBox<>();
	JComboBox<String> comboError = new JComboBox<>();
	JComboBox<String> comboPattern = new JComboBox<>();
	JComboBox<String> comboMode = new JComboBox<>();
	JCheckBox checkFillGrid;
	JCheckBox checkHideInfo;
	JComboBox<PaperSize> comboPaper = new JComboBox<>(PaperSize.values().toArray(new PaperSize[0]));

	int version=-1;
	String message = "";
	QrCode.ErrorLevel error = null;
	QrCodeMaskPattern mask = null;
	QrCode.Mode mode = null;
	PaperSize paperSize;
	boolean fillGrid=false;
	boolean hideInfo=false;


	Listener listener;

	public CreateQrCodeControlPanel(final Listener listener ) {
		this.listener = listener;

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
		addLabeled(comboVersion,"Version");
		addLabeled(comboError,"Error");
		addLabeled(comboPattern,"Pattern");
		addLabeled(comboMode,"Mode");
		addAlignLeft(checkFillGrid);
		addAlignLeft(checkHideInfo);
		addLabeled(comboPaper,"Paper Size");
		add(checkFillGrid);
		add(checkHideInfo);
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
		}
	}

	public interface Listener {
		void controlsUpdates();
	}

}
