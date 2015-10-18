/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.shapes;

import boofcv.factory.filter.binary.ThresholdType;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Peter Abeles
 */
class ThresholdControlPanel extends StandardAlgConfigPanel
		implements ActionListener, ChangeListener
{

	Listener listener;

	JComboBox comboType;
	JSpinner spinnerThreshold;
	JSpinner spinnerRadius;
	JButton buttonUpDown;

	boolean thresholdAdaptive;
	boolean thresholdGlobal;

	ThresholdType vType = ThresholdType.GLOBAL_OTSU;
	int vThreshold = 50;
	boolean vDirectionDown = true;
	int vRadius = 10;

	public ThresholdControlPanel(Listener listener) {
		this.listener = listener;

		comboType = new JComboBox();
		for( ThresholdType type : ThresholdType.values() ) {
			comboType.addItem(type.name());
		}

		comboType.setMaximumSize(comboType.getPreferredSize());
		comboType.setSelectedIndex(vType.ordinal());

		spinnerThreshold = new JSpinner(new SpinnerNumberModel(vThreshold,0,255,1));
		spinnerThreshold.setMaximumSize(spinnerThreshold.getPreferredSize());

		spinnerRadius = new JSpinner(new SpinnerNumberModel(vRadius,1,50,1));
		spinnerRadius.setMaximumSize(spinnerRadius.getPreferredSize());

		buttonUpDown = new JButton();
		buttonUpDown.setPreferredSize(new Dimension(100, 30));
		buttonUpDown.setMaximumSize(buttonUpDown.getPreferredSize());
		buttonUpDown.setMinimumSize(buttonUpDown.getPreferredSize());
		setToggleText(vDirectionDown);

		comboType.addActionListener(this);
		spinnerThreshold.addChangeListener(this);
		spinnerRadius.addChangeListener(this);
		buttonUpDown.addActionListener(this);

		addLabeled(comboType, "Type", this);
		addLabeled(spinnerThreshold, "Threshold", this);
		addLabeled(spinnerRadius, "Radius", this);
		addAlignCenter(buttonUpDown, this);


		updateEnabledByType();
	}

	private void setToggleText( boolean direction ) {
		if(direction)
			buttonUpDown.setText("down");
		else
			buttonUpDown.setText("Up");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == comboType ) {
			vType = ThresholdType.values()[comboType.getSelectedIndex()];
			updateEnabledByType();
			listener.imageThresholdUpdated();
		} else if( e.getSource() == buttonUpDown ) {
			vDirectionDown = !vDirectionDown;
			setToggleText(vDirectionDown);
			listener.imageThresholdUpdated();
		}
	}

	private void updateEnabledByType() {
		switch( vType ) {
			case FIXED:
				thresholdAdaptive = false;
				break;

			case GLOBAL_ENTROPY:
			case GLOBAL_OTSU:
				thresholdAdaptive = true;
				thresholdGlobal = true;
				break;

			default:
				thresholdAdaptive = true;
				thresholdGlobal = false;
				break;
		}

		if( thresholdAdaptive ) {
			spinnerThreshold.setEnabled(false);
			if( thresholdGlobal ) {
				spinnerRadius.setEnabled(false);
			} else {
				spinnerRadius.setEnabled(true);
			}
		} else {
			spinnerThreshold.setEnabled(true);
			spinnerRadius.setEnabled(false);
		}

		spinnerThreshold.repaint();
		spinnerRadius.repaint();

	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == spinnerThreshold ) {
			vThreshold = ((Number) spinnerThreshold.getValue()).intValue();
			listener.imageThresholdUpdated();
		} else if( e.getSource() == spinnerRadius ) {
			vRadius = ((Number) spinnerRadius.getValue()).intValue();
			listener.imageThresholdUpdated();
		}
	}

	public interface Listener {
		void imageThresholdUpdated();
	}
}
