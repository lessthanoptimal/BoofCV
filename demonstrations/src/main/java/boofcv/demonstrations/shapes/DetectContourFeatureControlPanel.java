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

import boofcv.struct.ConfigLength;
import boofcv.struct.ConnectRule;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * @author Peter Abeles
 */
public class DetectContourFeatureControlPanel extends DetectBlackShapePanel
		implements ActionListener, ChangeListener
{
	DetectContourFeaturesApp owner;

	// selects which image to view
	JComboBox imageView;

	JCheckBox showFeatures;
	JCheckBox showLines;
	JCheckBox showContour;

	ThresholdControlPanel threshold;

	JSpinner spinnerPeriodLength;
	JSpinner spinnerPeriodFraction;
	JSpinner spinnerFeatureThreshold;

	// connection rule
	JComboBox connectCombo;

	public ConfigLength period = ConfigLength.relative(0.02,3);
	public double featureThreshold = 0.1;

	boolean bShowFeatures = true;
	boolean bShowLines = true;
	boolean bShowContour = false;

	ConnectRule connectRule = ConnectRule.FOUR;

	public DetectContourFeatureControlPanel(DetectContourFeaturesApp owner) {
		this.owner = owner;

		imageView = new JComboBox();
		imageView.addItem("Input");
		imageView.addItem("Binary");
		imageView.addItem("Black");
		imageView.addActionListener(this);
		imageView.setMaximumSize(imageView.getPreferredSize());

		selectZoom = new JSpinner(new SpinnerNumberModel(1,MIN_ZOOM,MAX_ZOOM,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		showFeatures = new JCheckBox("Features");
		showFeatures.addActionListener(this);
		showFeatures.setSelected(bShowFeatures);
		showLines = new JCheckBox("Lines");
		showLines.setSelected(bShowLines);
		showLines.addActionListener(this);
		showContour = new JCheckBox("Contour");
		showContour.addActionListener(this);
		showContour.setSelected(bShowContour);

		threshold = new ThresholdControlPanel(owner);

		spinnerPeriodLength = new JSpinner(new SpinnerNumberModel(period.length,
				0,10000,1));
		spinnerPeriodLength.setMaximumSize(spinnerPeriodLength.getPreferredSize());
		spinnerPeriodLength.addChangeListener(this);
		spinnerPeriodFraction = new JSpinner(new SpinnerNumberModel(period.fraction, -1, 1.0, 0.01));
		spinnerPeriodFraction.setMaximumSize(spinnerPeriodFraction.getPreferredSize());
		spinnerPeriodFraction.addChangeListener(this);
		spinnerFeatureThreshold = new JSpinner(new SpinnerNumberModel(featureThreshold, 0, 100, 0.1));
		spinnerFeatureThreshold.setMaximumSize(spinnerFeatureThreshold.getPreferredSize());
		spinnerFeatureThreshold.addChangeListener(this);

		configureSpinnerFloat(spinnerPeriodFraction);
		configureSpinnerFloat(spinnerFeatureThreshold);

		connectCombo = new JComboBox();
		connectCombo.addItem("4-Connect");
		connectCombo.addItem("8-Connect");
		connectCombo.addActionListener(this);
		connectCombo.setMaximumSize(connectCombo.getPreferredSize());

		addLabeled(processingTimeLabel,"Time (ms)", this);
		addLabeled(imageSizeLabel,"Size", this);
		addLabeled(imageView, "View: ", this);
		addLabeled(selectZoom,"Zoom",this);
		addAlignLeft(showFeatures, this);
		addAlignLeft(showLines, this);
		addAlignLeft(showContour, this);
		add(threshold);
		addLabeled(spinnerPeriodLength, "Period Lenght: ", this);
		addLabeled(spinnerPeriodFraction, "Period Fraction: ", this);
		addLabeled(spinnerFeatureThreshold, "Feature Threshold: ", this);
		addLabeled(connectCombo,"Rule",this);
		addVerticalGlue(this);
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

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == imageView ) {
			selectedView = imageView.getSelectedIndex();
		} else if( e.getSource() == showFeatures ) {
			bShowFeatures = showFeatures.isSelected();
		} else if( e.getSource() == showLines ) {
			bShowLines = showLines.isSelected();
		} else if( e.getSource() == showContour ) {
			bShowContour = showContour.isSelected();
		} else if( e.getSource() == showContour ) {
			bShowContour = showContour.isSelected();
		} else {
			if( e.getSource() == connectCombo ) {
				connectRule = ConnectRule.values()[connectCombo.getSelectedIndex()];
			}
			owner.configUpdate();
			return;
		}
		owner.viewUpdated();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == spinnerPeriodLength) {
			period.length = ((Number) spinnerPeriodLength.getValue()).intValue();
		} else if( e.getSource() == spinnerPeriodFraction) {
			period.fraction = ((Number) spinnerPeriodFraction.getValue()).doubleValue();
		} else if( e.getSource() == spinnerFeatureThreshold ) {
			featureThreshold = ((Number) spinnerFeatureThreshold.getValue()).doubleValue();
		} else {
			owner.viewUpdated();
			return;
		}
		owner.configUpdate();
	}

	public ThresholdControlPanel getThreshold() {
		return threshold;
	}

}
