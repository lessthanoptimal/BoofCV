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

package boofcv.app.markers;

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
 * Base class for configuring square fiducials
 *
 * @author Peter Abeles
 */
public class CreateSquareMarkerControlPanel extends StandardAlgConfigPanel implements ActionListener
{

	JComboBox<String> comboOutputFormat = new JComboBox<>(new String[]{"pdf","png","bmp","jpg","ppm","pgm"});
	JCheckBox checkFillGrid;
	JCheckBox checkHideInfo;
	JComboBox<PaperSize> comboPaper = new JComboBox<>(PaperSize.values().toArray(new PaperSize[0]));

	JComboBox<Unit> comboUnits = new JComboBox<>(Unit.values());
	JFormattedTextField fieldBorderFraction = BoofSwingUtil.createTextField(0.25,0.0,1.0);
	JFormattedTextField fieldMarkerWidth = BoofSwingUtil.createTextField(10,0.0,Double.NaN);


	PaperSize paperSize;
	boolean fillGrid=false;
	boolean hideInfo=false;
	String format;

	Unit documentUnits = Unit.CENTIMETER;
	double markerWidth = 10;
	double borderFraction = 0.25;

	Listener listener;

	public CreateSquareMarkerControlPanel(final Listener listener ) {
		this.listener = listener;
	}

	protected void layoutComponents() {
		configureTextField(fieldBorderFraction,borderFraction,a->borderFraction=a);
		configureTextField(fieldMarkerWidth,markerWidth,a->markerWidth=a);

		format = (String)comboOutputFormat.getSelectedItem();
		comboOutputFormat.addActionListener(this);


		comboPaper.setSelectedItem(PaperSize.LETTER);
		comboPaper.addActionListener(this);
		comboPaper.setMaximumSize(comboPaper.getPreferredSize());
		paperSize = comboPaper.getItemAt(comboPaper.getSelectedIndex());

		checkFillGrid = checkbox("Fill Grid",fillGrid);
		checkHideInfo = checkbox("Hide Info",hideInfo);


		add(new JSeparator());
		addLabeled(comboOutputFormat,"Output Format");
		addLabeled(comboPaper,"Paper Size");
		addLabeled(fieldBorderFraction,"Border");
		add(createMarkerWidthPanel());
		add(createFlagPanel());
	}

	private void configureTextField(JFormattedTextField field , double value , FieldTextChange action ) {
		field.setPreferredSize(new Dimension(50,20));
		field.setMaximumSize(field.getPreferredSize());
		field.setValue(value);

		// save the numeric value whenever it's valid. This way when the user selects print it will use the size
		// that the user has typed in even if they didn't hit enter
		field.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				try {
					double value = ((Number)field.getFormatter().stringToValue(field.getText())).doubleValue();
					action.changed(value);
				} catch (ParseException ignore) {}

			}

			@Override
			public void removeUpdate(DocumentEvent e) {insertUpdate(e);}

			@Override
			public void changedUpdate(DocumentEvent e) {insertUpdate(e);}
		});
	}

	private JPanel createFlagPanel() {
		JPanel panel = new JPanel(new GridLayout(0,2));
		panel.add(checkFillGrid);
		panel.add(checkHideInfo);
		return panel;
	}

	private JPanel createMarkerWidthPanel() {

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
		if( e.getSource() == comboPaper ) {
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

	public interface FieldTextChange {
		void changed( double value );
	}

	public interface Listener {
		void controlsUpdates();
	}

}
