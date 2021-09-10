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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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

	public int pinholeRadial = 2;
	public boolean pinholeTangential = true;
	public boolean pinholeSkew = true;

	public int universalRadial = 2;
	public boolean universalTangential = true;
	public boolean universalSkew = true;

	public final PinholePanel pinhole = new PinholePanel();
	public final UniversalPanel universal = new UniversalPanel();
	public final KannalaBrandtPanel kannalaBrandt = new KannalaBrandtPanel();

	// Used to find out when settings have been changed
	public Listener listener = ()->{};

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

	public void configureCalibrator(CalibrateMonoPlanar calibrator) {
		switch (selected) {
			case BROWN -> calibrator.configurePinhole(pinholeSkew, pinholeRadial, pinholeTangential);
			case UNIVERSAL -> calibrator.configureUniversalOmni(universalSkew, universalRadial, universalTangential);
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

	private class PinholePanel extends StandardAlgConfigPanel
			implements ChangeListener, ActionListener {

		JSpinner numRadial;
		JCheckBox tangential;
		JCheckBox skew;

		public PinholePanel() {
			setBorder(BorderFactory.createEmptyBorder());

			numRadial = spinner(pinholeRadial, 0, 5, 1);
			tangential = checkbox("Tangential", pinholeTangential);
			skew = checkbox("Zero Skew", pinholeSkew);

			addLabeled(numRadial, "Radial");
			addAlignLeft(tangential);
			addAlignLeft(skew);
		}

		@Override
		public void stateChanged( ChangeEvent e ) {
			if (e.getSource() == numRadial) {
				pinholeRadial = ((Number)numRadial.getValue()).intValue();
			}
			updateParameters();
		}

		@Override
		public void actionPerformed( ActionEvent e ) {
			if (e.getSource() == tangential) {
				pinholeTangential = tangential.isSelected();
			} else if (e.getSource() == skew) {
				pinholeSkew = skew.isSelected();
			}
			updateParameters();
		}
	}

	private class UniversalPanel extends StandardAlgConfigPanel
			implements ChangeListener, ActionListener {

		JSpinner numRadial;
		JCheckBox tangential;
		JCheckBox skew;

		public UniversalPanel() {
			setBorder(BorderFactory.createEmptyBorder());

			numRadial = spinner(universalRadial, 0, 5, 1);
			tangential = checkbox("Tangential", universalTangential);
			skew = checkbox("Zero Skew", universalSkew);

			addLabeled(numRadial, "Radial");
			addAlignLeft(tangential);
			addAlignLeft(skew);
		}

		@Override
		public void stateChanged( ChangeEvent e ) {
			if (e.getSource() == numRadial) {
				universalRadial = ((Number)numRadial.getValue()).intValue();
			}
			updateParameters();
		}

		@Override
		public void actionPerformed( ActionEvent e ) {
			if (e.getSource() == tangential) {
				universalTangential = tangential.isSelected();
			} else if (e.getSource() == skew) {
				universalSkew = skew.isSelected();
			}
			updateParameters();
		}
	}

	public class KannalaBrandtPanel extends StandardAlgConfigPanel {
		public final JSpinnerNumber numSymmetric = spinnerWrap(5, 0, 10, 1);
		public final JSpinnerNumber numAsymmetric = spinnerWrap(0, 0, 10, 1);
		public final JCheckBoxValue skew = checkboxWrap("Zero Skew", universalSkew);

		public KannalaBrandtPanel() {
			setBorder(BorderFactory.createEmptyBorder());

			addLabeled(numSymmetric.spinner, "Symmetric");
			addLabeled(numAsymmetric.spinner, "Asymmetric");
			addAlignLeft(skew.check);
		}

		@Override public void controlChanged( final Object source ) {
			if (source == numSymmetric.spinner) {
				numSymmetric.updateValue();
			} else if (source == numAsymmetric.spinner) {
				numAsymmetric.updateValue();
			} else if (source == skew.check) {
				skew.updateValue();
			}
			updateParameters();
		}
	}

	@FunctionalInterface
	public interface Listener {
		void changed();
	}
}
