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
import boofcv.factory.filter.binary.ConfigThresholdLocalOtsu;
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

	boolean isAdaptive;
	boolean isThresholdGlobal;

	public ThresholdType type;

	public double scale = -1;
	public boolean down = true;
	public int radius = 10;
	public float savolaK = -1;
	public int otsuTuning = -1;
	public int minPixelValue = 0;
	public int maxPixelValue = 255;

	// toggle value of threshold
	public double minimumSpread = new ConfigThresholdBlockMinMax().minimumSpread;
	public int globalThreshold = 50;

	public ThresholdControlPanel(Listener listener) {
		this(listener,ConfigThreshold.global(ThresholdType.GLOBAL_OTSU));
	}

	public ThresholdControlPanel(Listener listener, ConfigThreshold configThreshold) {
		this.listener = listener;
		this.type = configThreshold.type;
		this.scale = configThreshold.scale;
		this.down = configThreshold.down;
		this.radius = configThreshold.radius;
		this.savolaK = configThreshold.savolaK;
		this.minPixelValue = configThreshold.minPixelValue;
		this.maxPixelValue = configThreshold.maxPixelValue;

		if( configThreshold instanceof ConfigThresholdLocalOtsu ) {
			otsuTuning = (int)((ConfigThresholdLocalOtsu)configThreshold).tuning;
		}

		comboType = new JComboBox();
		for( ThresholdType type : ThresholdType.values() ) {
			comboType.addItem(type.name());
		}

		comboType.setMaximumSize(comboType.getPreferredSize());
		comboType.setSelectedIndex(this.type.ordinal());

		spinnerThreshold = new JSpinner(new SpinnerNumberModel(globalThreshold,0,1000,1));
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
//		spinnerScale.addChangeListener(this);
		buttonUpDown.addActionListener(this);

		addLabeled(comboType, "Type", this);
		addLabeled(spinnerThreshold, "Threshold", this);
		addLabeled(spinnerRadius, "Radius", this);
		addLabeled(spinnerScale, "Scale", this);
		addAlignCenter(buttonUpDown, this);


		updateEnabledByType();
	}

	public void setOtsuTuning(int otsuTuning) {
		this.otsuTuning = otsuTuning;
		updateThresholdValue();
	}

	public void setRadius(int value ) {
		spinnerRadius.removeChangeListener(this);
		spinnerRadius.setValue(value);
		this.radius = value;
		spinnerRadius.addChangeListener(this);
	}

	private void updateThresholdValue() {
		spinnerThreshold.removeChangeListener(this);
		if( type == ThresholdType.FIXED ) {
			spinnerThreshold.setValue(globalThreshold);
		} else if( type == ThresholdType.BLOCK_MIN_MAX) {
			spinnerThreshold.setValue((int)minimumSpread);
		} else if( type == ThresholdType.BLOCK_OTSU ||
				type == ThresholdType.LOCAL_OTSU  ) {
			spinnerThreshold.setValue(otsuTuning);
		}
		spinnerThreshold.addChangeListener(this);
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
				isAdaptive = false;
				break;

			case GLOBAL_ENTROPY:
			case GLOBAL_OTSU:
				isAdaptive = true;
				isThresholdGlobal = true;
				break;

			default:
				isAdaptive = true;
				isThresholdGlobal = false;
				break;
		}

		if(isAdaptive) {
			spinnerThreshold.setEnabled(false);
			if(isThresholdGlobal) {
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

		if( type == ThresholdType.BLOCK_MIN_MAX) {
			spinnerThreshold.setEnabled(true);
			isAdaptive = false;
		}
		if( type == ThresholdType.BLOCK_OTSU ||
				type == ThresholdType.LOCAL_OTSU  ) {
			spinnerThreshold.setEnabled(true);
			isAdaptive = false;
		}
		updateThresholdValue();

		spinnerThreshold.repaint();
		spinnerRadius.repaint();
		spinnerScale.repaint();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == spinnerThreshold ) {
			int value = ((Number) spinnerThreshold.getValue()).intValue();
			if( type == ThresholdType.BLOCK_MIN_MAX) {
				minimumSpread = value;
			} else if( type == ThresholdType.BLOCK_OTSU ||
					type == ThresholdType.LOCAL_OTSU ) {
				otsuTuning = value;
			} else {
				globalThreshold = value;
			}
			updateThresholdValue();
			listener.imageThresholdUpdated();
		} else if( e.getSource() == spinnerRadius ) {
			radius = ((Number) spinnerRadius.getValue()).intValue();
			listener.imageThresholdUpdated();
		} else if( e.getSource() == spinnerScale ) {
			scale = ((Number) spinnerScale.getValue()).doubleValue();
			listener.imageThresholdUpdated();
		}
	}

	public void setConfiguration(ConfigThreshold configuration) {
		comboType.removeActionListener(this);
		spinnerRadius.removeChangeListener(this);
		spinnerScale.removeChangeListener(this);
		buttonUpDown.removeActionListener(this);


		comboType.setSelectedIndex(configuration.type.ordinal());
		spinnerRadius.setValue(configuration.radius);
		spinnerScale.setValue(configuration.scale);
		buttonUpDown.setSelected(configuration.down);

		type = configuration.type;
		radius = configuration.radius;
		scale = configuration.scale;
		down = configuration.down;
		if( type == ThresholdType.FIXED ) {
			globalThreshold = (int)configuration.fixedThreshold;
		} else if( type == ThresholdType.BLOCK_MIN_MAX) {
			minimumSpread = ((ConfigThresholdBlockMinMax)configuration).minimumSpread;
		} else if( type == ThresholdType.BLOCK_OTSU ||
				type == ThresholdType.LOCAL_OTSU  ) {
			otsuTuning = (int)((ConfigThresholdLocalOtsu)configuration).tuning;
		}

		comboType.addActionListener(this);
		spinnerRadius.addChangeListener(this);
		spinnerScale.addChangeListener(this);
		buttonUpDown.addActionListener(this);

		updateThresholdValue();
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public interface Listener {
		void imageThresholdUpdated();
	}

	public ConfigThreshold createConfig() {
		ConfigThreshold config;
		if( type == ThresholdType.BLOCK_MIN_MAX) {
			ConfigThresholdBlockMinMax _config = new ConfigThresholdBlockMinMax();
			_config.minimumSpread = minimumSpread;
			config = _config;
		} else if( type == ThresholdType.BLOCK_OTSU ||
				type == ThresholdType.LOCAL_OTSU ) {
			ConfigThresholdLocalOtsu _config = new ConfigThresholdLocalOtsu();
			_config.tuning = otsuTuning;
			config = _config;
		} else {
			config = new ConfigThreshold();
			config.fixedThreshold = globalThreshold;
		}

		config.type = type;
		config.scale = scale;
		config.down = down;
		config.radius = radius;
		config.savolaK = savolaK;
		config.minPixelValue = minPixelValue;
		config.maxPixelValue = maxPixelValue;

		return config;
	}
}
