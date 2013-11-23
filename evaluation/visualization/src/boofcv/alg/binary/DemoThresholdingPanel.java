/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.binary;

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
public class DemoThresholdingPanel extends StandardAlgConfigPanel implements ActionListener, ChangeListener {

	JComboBox comboSelect;

	JButton directionButton;
	JSlider thresholdLevel;
	JSpinner spinnerRadius;
	JSpinner spinnerBias;

	Listener listener;

	int valueThreshold;
	boolean directionDown;
	int threshRadius;
	double bias;

	public DemoThresholdingPanel(int threshold ,
								 boolean directionDown,
								 int radius, double bias,
								 Listener listener) {
		this.listener = listener;
		this.valueThreshold = threshold;
		this.directionDown = directionDown;
		this.threshRadius = radius;
		this.bias = bias;

		String[] algStrings = { "Fixed", "Adaptive Square", "Adaptive Gaussian"};

		comboSelect = new JComboBox(algStrings);
		comboSelect.addActionListener(this);
		comboSelect.setMaximumSize(comboSelect.getPreferredSize());

		directionButton = new JButton();
		directionButton.setPreferredSize(new Dimension(100,30));
		directionButton.setMaximumSize(directionButton.getPreferredSize());
		directionButton.setMinimumSize(directionButton.getPreferredSize());
		setToggleText(directionDown);
		directionButton.addActionListener(this);

		thresholdLevel = new JSlider(JSlider.HORIZONTAL,0,255,valueThreshold);
		thresholdLevel.setMajorTickSpacing(20);
		thresholdLevel.setPaintTicks(true);
		thresholdLevel.addChangeListener(this);
		thresholdLevel.setValue(threshold);

		spinnerRadius = new JSpinner(new SpinnerNumberModel(threshRadius,5,200,10));
		spinnerRadius.addChangeListener(this);
		spinnerRadius.setMaximumSize(spinnerRadius.getPreferredSize());

		spinnerBias = new JSpinner(new SpinnerNumberModel(bias,-50,50,10));
		spinnerBias.addChangeListener(this);
		spinnerBias.setMaximumSize(spinnerBias.getPreferredSize());

		updateActive(comboSelect.getSelectedIndex());

		add(comboSelect);
		addSeparator(100);
		addAlignLeft(directionButton, this);
		addAlignLeft(thresholdLevel, this);
		addLabeled(spinnerRadius, "Radius", this);
		addLabeled(spinnerBias, "Bias", this);
		add(Box.createVerticalGlue());
	}

	private void setToggleText( boolean direction ) {
		if(direction)
			directionButton.setText("down");
		else
			directionButton.setText("Up");
	}

	protected void updateActive( int which ) {
		if( which == 0 ) {
			thresholdLevel.setEnabled(true);
			spinnerRadius.setEnabled(false);
			spinnerBias.setEnabled(false);
		} else {
			thresholdLevel.setEnabled(false);
			spinnerRadius.setEnabled(true);
			spinnerBias.setEnabled(true);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == comboSelect ) {
			updateActive(comboSelect.getSelectedIndex());
			listener.changeSelected( comboSelect.getSelectedIndex() );
		} else if( e.getSource() == directionButton ) {
			directionDown = !directionDown;
			setToggleText(directionDown);
			listener.settingChanged();
		}
	}

	public boolean getDirection() {
		return directionDown;
	}

	public int getValueThreshold() {
		return valueThreshold;
	}

	public int getThreshRadius() {
		return threshRadius;
	}

	public double getBias() {
		return bias;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == thresholdLevel ) {
			valueThreshold = thresholdLevel.getValue();
			listener.settingChanged();
		} else if( e.getSource() == spinnerRadius) {
			threshRadius = ((Number) spinnerRadius.getValue()).intValue();
			listener.settingChanged();
		} else if( e.getSource() == spinnerBias) {
			bias = ((Number) spinnerBias.getValue()).doubleValue();
			listener.settingChanged();
		}
	}

	public static interface Listener {
		public void changeSelected( int which );

		public void settingChanged();
	}
}
