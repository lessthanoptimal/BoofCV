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

package boofcv.gui.controls;

import boofcv.abst.geo.calibration.CalibrateMonoPlanar;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.calib.CameraModelType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Provides a graphical way to select the camera calibration model
 *
 * @author Peter Abeles
 */
public class CalibrationModelPanel extends StandardAlgConfigPanel implements ActionListener {

	JComboBox<CameraModelType> comboType;
	JPanel panelTarget = new JPanel();

	public CameraModelType selected = CameraModelType.BROWN;

	public final ControlPanelPinhole pinhole = new ControlPanelPinhole(this::updateParameters);
	public final UniversalPanel universal = new UniversalPanel(this::updateParameters);
	public final KannalaBrandtPanel kannalaBrandt = new KannalaBrandtPanel();

	// Used to find out when settings have been changed
	public Listener listener = () -> {};

	public CalibrationModelPanel() {
		setBorder(BorderFactory.createEmptyBorder());

		comboType = new JComboBox<>(CameraModelType.values());
		comboType.addActionListener(this);
		comboType.setMaximumSize(comboType.getPreferredSize());

		panelTarget.setLayout(new BorderLayout());
		panelTarget.setPreferredSize(new Dimension(250, 100));
		panelTarget.setMaximumSize(panelTarget.getPreferredSize());

		changeTargetPanel();

		addLabeled(comboType, "Model Type");
		add(Box.createRigidArea(new Dimension(10, 10)));
		addAlignCenter(panelTarget);
	}

	public void setToBrown( boolean skew, int numRadial, boolean tangential ) {
		comboType.setSelectedIndex(CameraModelType.BROWN.ordinal());
		pinhole.skew.check.setSelected(skew);
		pinhole.numRadial.spinner.setValue(numRadial);
		pinhole.tangential.check.setSelected(tangential);
	}

	public void setToUniversal( boolean skew, int numRadial, boolean tangential ) {
		comboType.setSelectedIndex(CameraModelType.UNIVERSAL.ordinal());
		universal.skew.check.setSelected(skew);
		universal.numRadial.spinner.setValue(numRadial);
		universal.tangential.check.setSelected(tangential);
	}

	public void setToKannalaBrandt( boolean skew, int numSymmetric, int numAsymmetric ) {
		comboType.setSelectedIndex(CameraModelType.KANNALA_BRANDT.ordinal());
		kannalaBrandt.skew.check.setSelected(skew);
		kannalaBrandt.numSymmetric.spinner.setValue(numSymmetric);
		kannalaBrandt.numAsymmetric.spinner.setValue(numAsymmetric);
	}

	public void configureCalibrator( CalibrateMonoPlanar calibrator ) {
		switch (selected) {
			case BROWN -> calibrator.configurePinhole(
					pinhole.skew.value, pinhole.numRadial.value.intValue(), pinhole.tangential.value);
			case UNIVERSAL -> calibrator.configureUniversalOmni(
					universal.skew.value, universal.numRadial.value.intValue(), universal.tangential.value);
			case KANNALA_BRANDT -> calibrator.configureKannalaBrandt(kannalaBrandt.skew.value,
					kannalaBrandt.numSymmetric.vint(), kannalaBrandt.numAsymmetric.vint());
		}
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if (e.getSource() == comboType) {
			selected = (CameraModelType)comboType.getSelectedItem();
			changeTargetPanel();
			updateParameters();
		}
	}

	public void updateParameters() {
		changeTargetPanel();
		listener.changed();
	}

	private void changeTargetPanel() {
		JPanel p = switch (selected) {
			case BROWN -> pinhole;
			case UNIVERSAL -> universal;
			case KANNALA_BRANDT -> kannalaBrandt;
			default -> throw new RuntimeException("Unknown");
		};

		panelTarget.removeAll();
		panelTarget.add(BorderLayout.CENTER, p);
		panelTarget.validate();
		panelTarget.repaint();
	}

	private static class UniversalPanel extends StandardAlgConfigPanel {
		public final JSpinnerNumber numRadial = spinnerWrap(2, 0, 10, 1).tt("Number of radial distortion terms");
		public final JCheckBoxValue tangential = checkboxWrap("Tangential", false).tt("Include tangential distortion");
		public final JCheckBoxValue skew = checkboxWrap("Zero Skew", true).tt("Include skew in camera model. Rarely needed.");

		// called after a parameter changes value
		public Runnable parametersUpdated = () -> {};

		public UniversalPanel() {
			setBorder(BorderFactory.createEmptyBorder());
			addLabeled(numRadial.spinner, "Radial");
			addAlignLeft(tangential.check);
			addAlignLeft(skew.check);
		}

		public UniversalPanel( Runnable parametersUpdated ) {
			this();
			this.parametersUpdated = parametersUpdated;
		}

		@Override public void controlChanged( Object source ) {
			parametersUpdated.run();
		}
	}

	public class KannalaBrandtPanel extends StandardAlgConfigPanel {
		public final JSpinnerNumber numSymmetric = spinnerWrap(5, 0, 10, 1).tt("Number of symmetric terms");
		public final JSpinnerNumber numAsymmetric = spinnerWrap(0, 0, 10, 1).tt("Number of asymmetric terms. Typically not used.");
		public final JCheckBoxValue skew = checkboxWrap("Zero Skew", true).tt("Include skew in camera model. Rarely needed.");

		public KannalaBrandtPanel() {
			setBorder(BorderFactory.createEmptyBorder());

			addLabeled(numSymmetric.spinner, "Symmetric");
			addLabeled(numAsymmetric.spinner, "Asymmetric");
			addAlignLeft(skew.check);
		}

		@Override public void controlChanged( final Object source ) {
			updateParameters();
		}
	}

	@FunctionalInterface
	public interface Listener {
		void changed();
	}
}
