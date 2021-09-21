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

package boofcv.demonstrations.shapes;

import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ConfigThresholdBlockMinMax;
import boofcv.factory.filter.binary.ConfigThresholdLocalOtsu;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.gui.ImageHistogramPanel;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.controls.JConfigLength;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.ImageGray;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;

/**
 * Controls for adjusting binary thresholding
 *
 * @author Peter Abeles
 */
public class ThresholdControlPanel extends StandardAlgConfigPanel
		implements ActionListener, ChangeListener, JConfigLength.Listener
{

	Listener listener;

	JComboBox comboType;
	JSpinner spinnerThreshold;
	JConfigLength controlWidth = new JConfigLength(this,true);
	JSpinner spinnerScale;
	JButton buttonUpDown;
	JCheckBox checkLocalBlock;
	JCheckBox checkOtsu2;

	boolean isAdaptive;
	boolean isThresholdGlobal;

	public ThresholdType type;

	// Displays a histogram and let's the use manually select a threshold
	// empty panel that the histogram is inserted into
	public JPanel histogramHolder = new JPanel(new BorderLayout());
	public ImageHistogramPanel histogramPanel;

	public double scale = -1;
	public boolean down = true;
	public ConfigLength regionWidth = ConfigLength.fixed(21);
	public float savolaK = -1;
	public float nickK = -1;
	public int otsuTuning = -1;
	public boolean useOtsu2 = true;
	public int minPixelValue = 0;
	public int maxGrayLevels = 255;

	// toggle value of threshold
	public double minimumSpread = new ConfigThresholdBlockMinMax().minimumSpread;
	public int fixedThreshold = 50;

	public boolean thresholdLocalBlocks;

	public ThresholdControlPanel(Listener listener) {
		this(listener,ConfigThreshold.global(ThresholdType.GLOBAL_OTSU));
	}

	public ThresholdControlPanel(Listener listener, ConfigThreshold configThreshold) {
		this.listener = listener;
		this.type = configThreshold.type;
		this.scale = configThreshold.scale;
		this.down = configThreshold.down;
		this.regionWidth = configThreshold.width.copy();
		this.savolaK = configThreshold.niblackK;
		this.nickK = configThreshold.nickK;
		this.minPixelValue = configThreshold.minPixelValue;
		this.maxGrayLevels = configThreshold.maxPixelValue;
		this.thresholdLocalBlocks = configThreshold.thresholdFromLocalBlocks;

		// hide the histogram if it's not added
		histogramHolder.setMaximumSize(new Dimension(1,1));

		if( configThreshold instanceof ConfigThresholdLocalOtsu ) {
			otsuTuning = (int)((ConfigThresholdLocalOtsu)configThreshold).tuning;
			useOtsu2 = ((ConfigThresholdLocalOtsu)configThreshold).useOtsu2;
		} else {
			otsuTuning = (int)new ConfigThresholdLocalOtsu().tuning;
		}

		comboType = new JComboBox();
		for( ThresholdType type : ThresholdType.values() ) {
			comboType.addItem(type.name());
		}

		comboType.setMaximumSize(comboType.getPreferredSize());
		comboType.setSelectedIndex(this.type.ordinal());

		spinnerThreshold = spinner(fixedThreshold,0,1000,1);
		controlWidth.setValue(regionWidth);
		controlWidth.setMaximumSize(controlWidth.getPreferredSize());
		controlWidth.setLengthBounds(5,10000);

		checkOtsu2 = checkbox("Otsu2",useOtsu2);
		checkLocalBlock = checkbox("Local Blocks",thresholdLocalBlocks);

		spinnerScale = new JSpinner(new SpinnerNumberModel(scale,0,2.0,0.05));
		configureSpinnerFloat(spinnerScale);

		buttonUpDown = new JButton();
		buttonUpDown.setPreferredSize(new Dimension(100, 30));
		buttonUpDown.setMaximumSize(buttonUpDown.getPreferredSize());
		buttonUpDown.setMinimumSize(buttonUpDown.getPreferredSize());
		setToggleText(down);

		comboType.addActionListener(this);
//		spinnerScale.addChangeListener(this);
		buttonUpDown.addActionListener(this);

		JPanel togglePanels = new JPanel();
		togglePanels.setLayout(new GridLayout(0,2));
		addLabeled(spinnerScale,"Scale", null, togglePanels);
		togglePanels.add(checkLocalBlock);
		togglePanels.add(checkOtsu2);
		togglePanels.setMaximumSize(togglePanels.getPreferredSize());

		add(histogramHolder);
		addLabeled(comboType, "Type");
		addLabeled(spinnerThreshold, "Threshold");
		addLabeled(buttonUpDown,"Direction");
		addLabeled(controlWidth,"Reg. Width");
		addAlignCenter(togglePanels);

		updateEnabledByType();
	}

	public void updateHistogram(ImageGray gray ) {
		if( histogramPanel == null ) {
			throw new IllegalArgumentException("Must call addHistogramGraph first");
		}
		histogramPanel.updateSafe(gray);
		histogramPanel.repaint();
	}

	public void addHistogramGraph() {
		if( histogramPanel != null )
			throw new IllegalArgumentException("Already called");

		histogramPanel = new ImageHistogramPanel(maxGrayLevels +1, maxGrayLevels);
		histogramHolder.add( BorderLayout.CENTER,histogramPanel );
		histogramHolder.setPreferredSize(new Dimension(0,60));
		histogramHolder.setMaximumSize(new Dimension(100000,60));
		histogramHolder.validate();

		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if( type != ThresholdType.FIXED ) {
					return;
				}

				int where = 255*e.getX()/(histogramPanel.getWidth()-1);
				spinnerThreshold.setValue(where);
			}
			@Override
			public void mouseDragged(MouseEvent e) {
				mousePressed(e);
			}
		};

		histogramPanel.addMouseListener(mouse);
		histogramPanel.addMouseMotionListener(mouse);
	}

	public void setOtsuTuning(int otsuTuning) {
		this.otsuTuning = otsuTuning;
		updateThresholdValue();
	}

	private void updateThresholdValue() {
		spinnerThreshold.removeChangeListener(this);
		if( type == ThresholdType.FIXED ) {
			spinnerThreshold.setValue(fixedThreshold);
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
		} else if( e.getSource() == checkOtsu2 ) {
			useOtsu2 = checkOtsu2.isSelected();
			listener.imageThresholdUpdated();
		} else if( e.getSource() == checkLocalBlock ) {
			thresholdLocalBlocks = checkLocalBlock.isSelected();
			listener.imageThresholdUpdated();
		}
	}

	private void updateEnabledByType() {
		if( histogramPanel != null ) {
			if( type == ThresholdType.FIXED ) {
				histogramPanel.setMarker(fixedThreshold);
			} else {
				histogramPanel.setMarker(-1);
			}
		}

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
				controlWidth.setEnabled(false);
			} else {
				controlWidth.setEnabled(true);
			}
		} else {
			spinnerThreshold.setEnabled(true);
			controlWidth.setEnabled(false);
			spinnerScale.setEnabled(false);
		}

		switch( type ) {
			case BLOCK_OTSU:
			case BLOCK_MEAN:
			case BLOCK_MIN_MAX:
				checkLocalBlock.setEnabled(true);
				break;

			default:
				checkLocalBlock.setEnabled(false);
		}

		if( type == ThresholdType.BLOCK_MIN_MAX) {
			spinnerThreshold.setEnabled(true);
			isAdaptive = false;
		}
		if( type == ThresholdType.BLOCK_OTSU ||
				type == ThresholdType.LOCAL_OTSU  ) {
			checkOtsu2.setEnabled(true);
			spinnerThreshold.setEnabled(true);
			isAdaptive = false;
		} else {
			checkOtsu2.setEnabled(false);
		}


		updateThresholdValue();

		spinnerThreshold.repaint();
		controlWidth.repaint();
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
				fixedThreshold = value;
				if( histogramPanel != null ) {
					histogramPanel.setMarker(fixedThreshold);
				}
			}
			updateThresholdValue();
			listener.imageThresholdUpdated();
		} else if( e.getSource() == spinnerScale ) {
			scale = ((Number) spinnerScale.getValue()).doubleValue();
			listener.imageThresholdUpdated();
		}
	}

	public void setConfiguration(ConfigThreshold configuration) {
		comboType.removeActionListener(this);
		spinnerScale.removeChangeListener(this);
		buttonUpDown.removeActionListener(this);
		checkOtsu2.removeActionListener(this);


		comboType.setSelectedIndex(configuration.type.ordinal());
		controlWidth.setValue(configuration.width);
		spinnerScale.setValue(configuration.scale);
		buttonUpDown.setSelected(configuration.down);

		type = configuration.type;
		regionWidth = configuration.width.copy();
		scale = configuration.scale;
		down = configuration.down;
		if( type == ThresholdType.FIXED ) {
			fixedThreshold = (int)configuration.fixedThreshold;
		} else if( type == ThresholdType.BLOCK_MIN_MAX) {
			minimumSpread = ((ConfigThresholdBlockMinMax)configuration).minimumSpread;
		} else if( type == ThresholdType.BLOCK_OTSU ||
				type == ThresholdType.LOCAL_OTSU  ) {
			otsuTuning = (int)((ConfigThresholdLocalOtsu)configuration).tuning;
			useOtsu2 = ((ConfigThresholdLocalOtsu)configuration).useOtsu2;
		}

		comboType.addActionListener(this);
		spinnerScale.addChangeListener(this);
		buttonUpDown.addActionListener(this);
		checkOtsu2.addActionListener(this);

		updateThresholdValue();
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	@Override
	public void changeConfigLength(JConfigLength source, double fraction, double length) {
		if( source == controlWidth ) {
			regionWidth.length = length;
			regionWidth.fraction = fraction;
			listener.imageThresholdUpdated();
		}
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
			_config.useOtsu2 = useOtsu2;
			config = _config;
		} else {
			config = new ConfigThreshold();
			config.fixedThreshold = fixedThreshold;
		}

		config.type = type;
		config.scale = scale;
		config.down = down;
		config.width = regionWidth.copy();
		config.niblackK = savolaK;
		config.nickK = nickK;
		config.minPixelValue = minPixelValue;
		config.maxPixelValue = maxGrayLevels;
		config.thresholdFromLocalBlocks = thresholdLocalBlocks;

		return config;
	}
}
