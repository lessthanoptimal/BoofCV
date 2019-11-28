/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.feature.disparity;

import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.alg.feature.disparity.sgm.SgmDisparityCost;
import boofcv.factory.feature.disparity.*;
import boofcv.factory.transform.census.CensusType;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ShowImages;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Controls for configuring disparity algorithms
 *
 * @author Peter Abeles
 */
public class DisparityControlPanel extends StandardAlgConfigPanel {

	private static String[] ERRORS_BLOCK = new String[]{"SAD","Census","NCC"};
	private static String[] ERRORS_BGM = new String[]{"SAD","Census","MI"};

	// which algorithm to run
	int selectedMethod = 0;
	public final ConfigureDisparityBMBest5 configBM = new ConfigureDisparityBMBest5();
	public final ConfigureDisparitySGM configSGM = new ConfigureDisparitySGM();

	JComboBox comboMethod = combo(e -> handleMethod(), selectedMethod,"BlockMatch-5","BlockMatch","SGM");
	JComboBox comboError = combo(e -> handleErrorSelected(false),configBM.errorType.ordinal(),ERRORS_BLOCK);

	JTabbedPane tabbedPane = new JTabbedPane();

	// Controls for families of algorithms
	ControlsBlockMatching controlBM = new ControlsBlockMatching();
	ControlsSemiGlobal controlSGM = new ControlsSemiGlobal();

	// Controls for error types
	ControlsSAD controlSad = new ControlsSAD();
	ControlsCensus controlCensus = new ControlsCensus();
	ControlsNCC controlNCC = new ControlsNCC();
	ControlsMutualInfo controlMI = new ControlsMutualInfo();

	Listener listener;
	Class imageType;

	public DisparityControlPanel( int disparityMin , int disparityRange, Class imageType) {
		this.imageType = imageType;
		tabbedPane.addTab("Method",controlBM);
		tabbedPane.addTab("Error",controlSad);

		// SGM is larger than BM, make the initial area larger
		controlBM.setPreferredSize(controlSGM.getPreferredSize());

		addLabeled(comboMethod,"Method");
		addLabeled(comboError,"Error");
		add(tabbedPane);

		// initially set both to have the same values for disparity
		controlBM.spinnerDisparityMin.setValue(disparityMin);
		controlBM.spinnerDisparityRange.setValue(disparityRange);
		controlSGM.spinnerDisparityMin.setValue(disparityMin);
		controlSGM.spinnerDisparityRange.setValue(disparityRange);
	}

	public void broadcastChange() {
		Listener listener = this.listener;
		if( listener == null )
			return;

		listener.handleDisparityChange();
	}

	public StereoDisparity createAlgorithm() {
//		BoofSwingUtil.checkGuiThread(); // TODO lock instead to make this safe?

		boolean block = isBlockSelected();

		if( block ) {
			Class dispType = configBM.subpixel ? GrayF32.class : GrayU8.class;
			if( selectedMethod == 0 )
				return FactoryStereoDisparity.blockMatchBest5(configBM,imageType,dispType);
			else if( selectedMethod == 1 )
				return FactoryStereoDisparity.blockMatch(configBM,imageType,dispType);
			else
				throw new RuntimeException("BUG");
		} else {
			Class dispType = configSGM.subpixel ? GrayF32.class : GrayU8.class;
			return FactoryStereoDisparity.sgm(configSGM,imageType,dispType);
		}
	}

	public int getDisparityMin() {
		if( isBlockSelected() )
			return configBM.minDisparity;
		else
			return configSGM.minDisparity;
	}

	public int getDisparityRange() {
		if( isBlockSelected() )
			return configBM.rangeDisparity;
		else
			return configSGM.rangeDisparity;
	}

	/**
	 * The user changed which method is being used
	 */
	private void handleMethod() {
		if( selectedMethod == comboMethod.getSelectedIndex() )
			return;
		boolean previousBlock = isBlockSelected();
		selectedMethod = comboMethod.getSelectedIndex();
		boolean block = isBlockSelected();

		// swap out the controls and stuff
		if( block != previousBlock ) {
			int activeTab = tabbedPane.getSelectedIndex(); // don't switch out of the current tab
			Component c;
			if( block ) {
				c = controlBM;
				comboError.setModel( new DefaultComboBoxModel( ERRORS_BLOCK ) );
				comboError.setSelectedIndex(configBM.errorType.ordinal());
			} else {
				c = controlSGM;
				comboError.setModel( new DefaultComboBoxModel( ERRORS_BGM ) );
				comboError.setSelectedIndex(configSGM.errorType.ordinal());
			}
			tabbedPane.removeTabAt(0);
			tabbedPane.insertTab("Method",null,c,null,0);
			tabbedPane.setSelectedIndex(activeTab);
			handleErrorSelected(true);
		}
		broadcastChange();
	}

	private boolean isBlockSelected() {
		return selectedMethod < 2;
	}

	private void handleErrorSelected( boolean force ) {
		boolean block = isBlockSelected();
		int previousIdx = block ? configBM.errorType.ordinal() : configSGM.errorType.ordinal();
		if( !force && previousIdx == comboError.getSelectedIndex() )
			return;
		int selectedIdx = comboError.getSelectedIndex();

		// If not forced that means the user selected a new error type, make that tab active
		// If forced keep the previously active tab active
		int activeTab = !force ? 1 : tabbedPane.getSelectedIndex();
//		System.out.println("error for block="+block+" idx="+selectedIdx);
		Component c;
		if( block ) {
			configBM.errorType = DisparityError.values()[selectedIdx];
			controlCensus.update(configBM.censusVariant);
			switch( selectedIdx ) {
				case 0: c = controlSad; break;
				case 1: c = controlCensus; break;
				case 2: c = controlNCC; break;
				default: throw new IllegalArgumentException("Unknown");
			}
		} else {
			configSGM.errorType = DisparitySgmError.values()[selectedIdx];
			controlCensus.update(configSGM.censusVariant);
			controlMI.update(configSGM.errorHMI);
			switch( selectedIdx ) {
				case 0: c = controlSad; break;
				case 1: c = controlCensus; break;
				case 2: c = controlMI; break;
				default: throw new IllegalArgumentException("Unknown");
			}
		}
		tabbedPane.removeTabAt(1);
		tabbedPane.insertTab("Error",null,c,null,1);
		tabbedPane.setSelectedIndex(activeTab);

		if( !force )
			broadcastChange();
	}

	public class ControlsBlockMatching extends StandardAlgConfigPanel implements ChangeListener, ActionListener {
		JSpinner spinnerDisparityMin = spinner(configBM.minDisparity,0, 1000,5);
		JSpinner spinnerDisparityRange = spinner(configBM.rangeDisparity,1, 254,5);
		JSpinner radiusXSpinner = spinner(configBM.regionRadiusX,1,30,1); // TODO move to error
		JSpinner radiusYSpinner = spinner(configBM.regionRadiusY,1,30,1);
		JSpinner spinnerError = spinner(configBM.maxPerPixelError,-1,80,5);
		JSpinner spinnerReverse = spinner(configBM.validateRtoL,-1,50,1);
		JSpinner spinnerTexture = spinner(configBM.texture,0.0,1.0,0.05,1,3);
		JCheckBox subpixelToggle = checkbox("Subpixel",configBM.subpixel);

		ControlsBlockMatching() {
			addLabeled(spinnerDisparityMin, "Min Disp.");
			addLabeled(spinnerDisparityRange, "Range Disp.");
			addLabeled(radiusXSpinner,    "Radius X");
			addLabeled(radiusYSpinner,    "Radius Y");
			addLabeled(spinnerError,     "Max Error");
			addLabeled(spinnerTexture,   "Texture");
			addLabeled(spinnerReverse,   "Reverse");
			addAlignLeft(subpixelToggle);
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == spinnerReverse) {
				configBM.validateRtoL = ((Number) spinnerReverse.getValue()).intValue();
			} else if( e.getSource() == spinnerDisparityMin) {
				configBM.minDisparity = ((Number) spinnerDisparityMin.getValue()).intValue();
			} else if( e.getSource() == spinnerDisparityRange) {
				configBM.rangeDisparity = ((Number) spinnerDisparityRange.getValue()).intValue();
			} else if( e.getSource() == spinnerError) {
				configBM.maxPerPixelError = ((Number) spinnerError.getValue()).intValue();
			} else if( e.getSource() == radiusXSpinner) {
				configBM.regionRadiusX = ((Number) radiusXSpinner.getValue()).intValue();
			} else if( e.getSource() == radiusYSpinner) {
				configBM.regionRadiusY = ((Number) radiusYSpinner.getValue()).intValue();
			} else if( e.getSource() == spinnerTexture) {
				configBM.texture = ((Number) spinnerTexture.getValue()).doubleValue();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if( e.getSource() == subpixelToggle) {
				configSGM.subpixel = subpixelToggle.isSelected();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	public class ControlsSemiGlobal extends StandardAlgConfigPanel implements ChangeListener, ActionListener {
		JComboBox comboPaths = combo(configSGM.paths.ordinal(),"1","2","4","8","16");

		JSpinner spinnerPenaltySmall = spinner(configSGM.penaltySmallChange,0, SgmDisparityCost.MAX_COST,10);
		JSpinner spinnerPenaltyLarge = spinner(configSGM.penaltyLargeChange,1, SgmDisparityCost.MAX_COST,10);

		JSpinner spinnerDisparityMin = spinner(configSGM.minDisparity,0, 1000,5);
		JSpinner spinnerDisparityRange = spinner(configSGM.rangeDisparity,1, 254,5);
		JSpinner spinnerError = spinner(configSGM.maxError,-1,SgmDisparityCost.MAX_COST,5);
		JSpinner spinnerReverse = spinner(configSGM.validateRtoL,-1,50,1);
		JSpinner spinnerTexture = spinner(configSGM.texture,0.0,1.0,0.05,1,3);
		JCheckBox subpixelToggle = checkbox("Subpixel",configSGM.subpixel);

		ControlsSemiGlobal() {
			addLabeled(spinnerDisparityMin, "Min Disp.");
			addLabeled(spinnerDisparityRange, "Range Disp.");
			addLabeled(spinnerError,     "Max Error");
			addLabeled(spinnerTexture,   "Texture");
			addLabeled(spinnerReverse,   "Reverse");
			addLabeled(comboPaths, "Paths");
			addLabeled(spinnerPenaltySmall, "Penalty Small");
			addLabeled(spinnerPenaltyLarge, "Penalty Large");
			addAlignLeft(subpixelToggle);
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == spinnerReverse) {
				configSGM.validateRtoL = ((Number) spinnerReverse.getValue()).intValue();
			} else if( e.getSource() == spinnerDisparityMin) {
				configSGM.minDisparity = ((Number) spinnerDisparityMin.getValue()).intValue();
			} else if( e.getSource() == spinnerDisparityRange) {
				configSGM.rangeDisparity = ((Number) spinnerDisparityRange.getValue()).intValue();
			} else if( e.getSource() == spinnerError) {
				configSGM.maxError = ((Number) spinnerError.getValue()).intValue();
			} else if( e.getSource() == spinnerTexture) {
				configSGM.texture = ((Number) spinnerTexture.getValue()).doubleValue();
			} else if( e.getSource() == spinnerPenaltySmall) {
				configSGM.penaltySmallChange = ((Number) spinnerPenaltySmall.getValue()).intValue();
			} else if( e.getSource() == spinnerPenaltyLarge) {
				configSGM.penaltyLargeChange = ((Number) spinnerPenaltyLarge.getValue()).intValue();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if( e.getSource() == comboPaths) {
				configSGM.paths = ConfigureDisparitySGM.Paths.values()[comboPaths.getSelectedIndex()];
			} else if( e.getSource() == subpixelToggle) {
					configSGM.subpixel = subpixelToggle.isSelected();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	class ControlsSAD extends StandardAlgConfigPanel {

	}

	class ControlsCensus extends StandardAlgConfigPanel implements ChangeListener, ActionListener {
		JComboBox comboVariant = combo(0, (Object[]) CensusType.values());

		public ControlsCensus() {
			addLabeled(comboVariant, "Variant");
		}

		public void update( CensusType settings ) {
			comboVariant.setSelectedIndex(settings.ordinal());
		}

		@Override
		public void stateChanged(ChangeEvent e) {

		}

		@Override
		public void actionPerformed(ActionEvent e) {

		}
	}

	class ControlsNCC extends StandardAlgConfigPanel implements ChangeListener, ActionListener {
		JSpinner spinnerEps = spinner(0.0,0, 1.0,0.001);

		ControlsNCC() {
			addLabeled(spinnerEps, "EPS");
		}

		public void update( double eps ) {
			spinnerEps.setValue(eps);
		}

		@Override
		public void stateChanged(ChangeEvent e) {

		}

		@Override
		public void actionPerformed(ActionEvent e) {

		}
	}

	class ControlsMutualInfo extends StandardAlgConfigPanel implements ChangeListener, ActionListener {
		JSpinner spinnerBlur = spinner(1,0, 10,1);
		JSpinner spinnerPyramidWidth = spinner(20,20, 10000,50);
		JSpinner spinnerExtra = spinner(0,0, 5,1);

		ConfigureDisparitySGM.MutualInformation settings;

		ControlsMutualInfo() {
			addLabeled(spinnerBlur, "Blur Radius");
			addLabeled(spinnerPyramidWidth, "Pyr Min W");
			addLabeled(spinnerExtra, "Extra Iter.");
		}

		public void update( ConfigureDisparitySGM.MutualInformation settings ) {
			this.settings = settings;
			spinnerBlur.setValue(settings.smoothingRadius);
			spinnerPyramidWidth.setValue(settings.pyramidLayers.minWidth);
			spinnerExtra.setValue(settings.extraIterations);
		}

		@Override
		public void stateChanged(ChangeEvent e) {

		}

		@Override
		public void actionPerformed(ActionEvent e) {

		}
	}

	public Listener getListener() {
		return listener;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public interface Listener {
		void handleDisparityChange();
	}

	public static void main(String[] args) {
		DisparityControlPanel controls = new DisparityControlPanel(0,150,null);
		ShowImages.showWindow(controls,"Controls");
	}
}
