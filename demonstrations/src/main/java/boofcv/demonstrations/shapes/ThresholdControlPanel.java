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

package boofcv.demonstrations.shapes;

import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ConfigThresholdBlockMinMax;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

/**
 * @author Peter Abeles
 */
public class ThresholdControlPanel extends StandardAlgConfigPanel
		implements ActionListener, ChangeListener
{

	Listener listener;

	JComboBox comboType;
	JSpinner spinnerThreshold;
	JSpinner spinnerRadius;
	JSpinner spinnerScale;
	JButton buttonUpDown;

	boolean thresholdAdaptive;
	boolean thresholdGlobal;

	public ThresholdType type = ThresholdType.GLOBAL_OTSU;
	public double fixedThreshold = 50;
	public double scale = 0.95;
	public boolean down = true;
	public int radius = 10;
	public float savolaK = 0.3f;
	public int minPixelValue = 0;
	public int maxPixelValue = 255;
	public double minimumSpread = 10;

	public ThresholdControlPanel(Listener listener) {
		this.listener = listener;

		comboType = new JComboBox();
		for( ThresholdType type : ThresholdType.values() ) {
			comboType.addItem(type.name());
		}

		comboType.setMaximumSize(comboType.getPreferredSize());
		comboType.setSelectedIndex(type.ordinal());

		spinnerThreshold = new JSpinner(new SpinnerNumberModel(fixedThreshold,0,255,1));
		spinnerThreshold.setMaximumSize(spinnerThreshold.getPreferredSize());

		spinnerRadius = new JSpinner(new SpinnerNumberModel(radius,1,500,1));
		spinnerRadius.setMaximumSize(spinnerRadius.getPreferredSize());

		spinnerScale = new JSpinner(new SpinnerNumberModel(scale,0,2.0,0.05));
		configureSpinnerFloat(spinnerScale);

		buttonUpDown = new JButton();
		buttonUpDown.setPreferredSize(new Dimension(100, 30));
		buttonUpDown.setMaximumSize(buttonUpDown.getPreferredSize());
		buttonUpDown.setMinimumSize(buttonUpDown.getPreferredSize());
		setToggleText(down);

		comboType.addActionListener(this);
		spinnerThreshold.addChangeListener(this);
		spinnerRadius.addChangeListener(this);
		spinnerScale.addChangeListener(this);
		buttonUpDown.addActionListener(this);

		addLabeled(comboType, "Type", this);
		addLabeled(spinnerThreshold, "Threshold", this);
		addLabeled(spinnerRadius, "Radius", this);
		addLabeled(spinnerScale, "Scale", this);
		addAlignCenter(buttonUpDown, this);


		updateEnabledByType();
	}

	private void configureSpinnerFloat( JSpinner spinner ) {
		JSpinner.NumberEditor editor = (JSpinner.NumberEditor)spinner.getEditor();
		DecimalFormat format = editor.getFormat();
		format.setMinimumFractionDigits(3);
		format.setMinimumIntegerDigits(1);
		editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
		Dimension d = spinner.getPreferredSize();
		d.width = 60;
		spinner.setPreferredSize(d);
		spinner.addChangeListener(this);
		spinner.setMaximumSize(d);
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
			type = ThresholdType.values()[comboType.getSelectedIndex()];
			updateEnabledByType();
			listener.imageThresholdUpdated();
		} else if( e.getSource() == buttonUpDown ) {
			down = !down;
			setToggleText(down);
			listener.imageThresholdUpdated();
		}
	}

	private void updateEnabledByType() {
		switch( type ) {
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
				spinnerScale.setEnabled(false);
			} else {
				spinnerRadius.setEnabled(true);
				spinnerScale.setEnabled(true);
			}
		} else {
			spinnerThreshold.setEnabled(true);
			spinnerRadius.setEnabled(false);
			spinnerScale.setEnabled(false);
		}

		spinnerThreshold.repaint();
		spinnerRadius.repaint();
		spinnerScale.repaint();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == spinnerThreshold ) {
			fixedThreshold = ((Number) spinnerThreshold.getValue()).intValue();
			listener.imageThresholdUpdated();
		} else if( e.getSource() == spinnerRadius ) {
			radius = ((Number) spinnerRadius.getValue()).intValue();
			listener.imageThresholdUpdated();
		} else if( e.getSource() == spinnerScale ) {
			scale = ((Number) spinnerScale.getValue()).doubleValue();
			listener.imageThresholdUpdated();
		}
	}

	public interface Listener {
		void imageThresholdUpdated();
	}

	public ConfigThreshold createConfig() {
		ConfigThreshold config;
		if( type == ThresholdType.LOCAL_BLOCK_MIN_MAX) {
			ConfigThresholdBlockMinMax _config = new ConfigThresholdBlockMinMax();

			_config.minimumSpread = minimumSpread;
			config = _config;
		} else {
			config = new ConfigThreshold();
		}
		config.type = type;
		config.fixedThreshold = fixedThreshold;
		config.scale = scale;
		config.down = down;
		config.radius = radius;
		config.savolaK = savolaK;
		config.minPixelValue = minPixelValue;
		config.maxPixelValue = maxPixelValue;

		return config;
	}
}
