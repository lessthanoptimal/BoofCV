/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import boofcv.factory.transform.census.CensusVariants;
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
	private static String[] ERRORS_SGM = new String[]{"Absolute Diff","Census","HMI"};

	// which algorithm to run
	int selectedMethod = 0;
	public final ConfigDisparityBMBest5 configBM;
	public final ConfigDisparitySGM configSGM;

	JComboBox<String> comboMethod, comboError;

	JTabbedPane tabbedPane = new JTabbedPane();

	// Controls for families of algorithms
	ControlsBlockMatching controlBM;
	ControlsSemiGlobal controlSGM;

	// Controls for error types
	ControlsSAD controlSad;
	ControlsCensus controlCensus;
	ControlsNCC controlNCC;
	ControlsMutualInfo controlHMI;

	boolean ignoreChanges=false;

	Listener listener;
	Class imageType;

	public DisparityControlPanel( ConfigDisparityBMBest5 configBM,
								  ConfigDisparitySGM configSGM,
								  Class imageType)
	{
		this.configBM = configBM;
		this.configSGM = configSGM;
		this.imageType = imageType;

		comboMethod = combo(e -> handleMethod(), selectedMethod,"BlockMatch-5","BlockMatch","SGM");
		if( isBlockSelected() )
			comboError = combo(e -> handleErrorSelected(false),configBM.errorType.ordinal(),(Object[])ERRORS_BLOCK);
		else
			comboError = combo(e -> handleErrorSelected(false),configSGM.errorType.ordinal(), (Object[])ERRORS_SGM);
		controlBM = new ControlsBlockMatching();
		controlSGM = new ControlsSemiGlobal();
		controlSad = new ControlsSAD();
		controlCensus = new ControlsCensus();
		controlNCC = new ControlsNCC();
		controlHMI = new ControlsMutualInfo();

		tabbedPane.addTab("Method",getModelControl(isBlockSelected()));
		tabbedPane.addTab("Error",getErrorControl(comboError.getSelectedIndex()));

		// SGM is larger than BM, make the initial area larger
		controlBM.setPreferredSize(controlSGM.getPreferredSize());

		addLabeled(comboMethod,"Method");
		addLabeled(comboError,"Error");
		add(tabbedPane);
	}

	public static DisparityControlPanel createRange( int disparityMin , int disparityRange, Class imageType) {
		ConfigDisparityBMBest5 configBM = new ConfigDisparityBMBest5();
		ConfigDisparitySGM configSGM = new ConfigDisparitySGM();

		configBM.disparityMin = disparityMin;
		configBM.disparityRange = disparityRange;
		configSGM.disparityMin = disparityMin;
		configSGM.disparityRange = disparityRange;

		return new DisparityControlPanel(configBM,configSGM,imageType);
	}

	public void broadcastChange() {
		Listener listener = this.listener;
		if( listener == null )
			return;
		if( ignoreChanges )
			return;

		listener.handleDisparityChange();
	}

	public void updateControlEnabled() {
		if( !isBlockSelected() ) {
			controlSGM.updateControlsEnabled();
		}
	}

	@SuppressWarnings("unchecked")
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
			return configBM.disparityMin;
		else
			return configSGM.disparityMin;
	}

	public int getDisparityRange() {
		if( isBlockSelected() )
			return configBM.disparityRange;
		else
			return configSGM.disparityRange;
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

		// All the code above can cause multiple calls to broadcastChange() as listeners are triggered
		ignoreChanges = true;

		// swap out the controls and stuff
		if( block != previousBlock ) {
			int activeTab = tabbedPane.getSelectedIndex(); // don't switch out of the current tab
			if( block ) {
				comboError.setModel( new DefaultComboBoxModel<>( ERRORS_BLOCK ) );
				comboError.setSelectedIndex(configBM.errorType.ordinal());
			} else {
				comboError.setModel( new DefaultComboBoxModel<>( ERRORS_SGM ) );
				comboError.setSelectedIndex(configSGM.errorType.ordinal());
			}
			Component c = getModelControl(block);
			if( !block )
				controlSGM.updateControlsEnabled();
			tabbedPane.removeTabAt(0);
			tabbedPane.insertTab("Method",null,c,null,0);
			tabbedPane.setSelectedIndex(activeTab);
			handleErrorSelected(true);
		}

		// This will ignore all changes until after they have been processed
		SwingUtilities.invokeLater(()->{ignoreChanges=false;broadcastChange();});
	}

	private Component getModelControl( boolean block ) {
		Component c;
		if( block ) {
			c = controlBM;
		} else {
			c = controlSGM;
		}
		return c;
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

		// Avoid multiple calls to broadcastChange()
		if( !force )
			ignoreChanges = true;

		// If forced keep the previously active tab active
		int activeTab = tabbedPane.getSelectedIndex();
//		System.out.println("error for block="+block+" idx="+selectedIdx);

		if( block ) {
			configBM.errorType = DisparityError.values()[selectedIdx];
			controlCensus.update(configBM.configCensus);
			controlNCC.update(configBM.configNCC);
		} else {
			configSGM.errorType = DisparitySgmError.values()[selectedIdx];
			controlCensus.update(configSGM.configCensus);
			controlHMI.update(configSGM.configHMI);
		}
		Component c = getErrorControl(selectedIdx);
		tabbedPane.removeTabAt(1);
		tabbedPane.insertTab("Error",null,c,null,1);
		tabbedPane.setSelectedIndex(activeTab);

		if( !force )
			SwingUtilities.invokeLater(()->{ignoreChanges=false;broadcastChange();});
	}

	private Component getErrorControl( int selectedIdx ) {
		Component c;
		if( isBlockSelected() ) {
			switch( selectedIdx ) {
				case 0: c = controlSad; break;
				case 1: c = controlCensus; break;
				case 2: c = controlNCC; break;
				default: throw new IllegalArgumentException("Unknown");
			}
		} else {
			switch( selectedIdx ) {
				case 0: c = controlSad; break;
				case 1: c = controlCensus; break;
				case 2: c = controlHMI; break;
				default: throw new IllegalArgumentException("Unknown");
			}
		}
		return c;
	}

	public class ControlsBlockMatching extends StandardAlgConfigPanel implements ChangeListener, ActionListener {
		JSpinner spinnerDisparityMin = spinner(configBM.disparityMin,0, 1000,5);
		JSpinner spinnerDisparityRange = spinner(configBM.disparityRange,1, 254,5);
		JSpinner radiusXSpinner = spinner(configBM.regionRadiusX,0,50,1); // TODO move to error
		JSpinner radiusYSpinner = spinner(configBM.regionRadiusY,0,50,1);
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
				configBM.disparityMin = ((Number) spinnerDisparityMin.getValue()).intValue();
			} else if( e.getSource() == spinnerDisparityRange) {
				configBM.disparityRange = ((Number) spinnerDisparityRange.getValue()).intValue();
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
				configBM.subpixel = subpixelToggle.isSelected();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	public class ControlsSemiGlobal extends StandardAlgConfigPanel implements ChangeListener, ActionListener {
		JComboBox<String> comboPaths = combo(configSGM.paths.ordinal(),"1","2","4","8","16");

		JSpinner spinnerPenaltySmall = spinner(configSGM.penaltySmallChange,0, SgmDisparityCost.MAX_COST,10);
		JSpinner spinnerPenaltyLarge = spinner(configSGM.penaltyLargeChange,1, SgmDisparityCost.MAX_COST,10);

		JSpinner spinnerDisparityMin = spinner(configSGM.disparityMin,0, 1000,5);
		JSpinner spinnerDisparityRange = spinner(configSGM.disparityRange,1, 254,5);
		JSpinner spinnerError = spinner(configSGM.maxError,-1,Short.MAX_VALUE,200);
		JSpinner spinnerReverse = spinner(configSGM.validateRtoL,-1,50,1);
		JSpinner spinnerTexture = spinner(configSGM.texture,0.0,1.0,0.05,1,3);
		JCheckBox subpixelToggle = checkbox("Subpixel",configSGM.subpixel);
		JCheckBox useBlocks = checkbox("Use Blocks",configSGM.useBlocks);
		JComboBox<String> comboBlockApproach = combo(configSGM.configBlockMatch.approach.ordinal(),(Object[])BlockMatchingApproach.values());
		JSpinner radiusXSpinner = spinner(configSGM.configBlockMatch.radiusX,0,50,1); // TODO move to error
		JSpinner radiusYSpinner = spinner(configSGM.configBlockMatch.radiusY,0,50,1);

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
			addAlignLeft(useBlocks);
			addLabeled(comboBlockApproach,"Approach");
			addLabeled(radiusXSpinner,    "Radius X");
			addLabeled(radiusYSpinner,    "Radius Y");
			updateControlsEnabled();
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == spinnerReverse) {
				configSGM.validateRtoL = ((Number) spinnerReverse.getValue()).intValue();
			} else if( e.getSource() == spinnerDisparityMin) {
				configSGM.disparityMin = ((Number) spinnerDisparityMin.getValue()).intValue();
			} else if( e.getSource() == spinnerDisparityRange) {
				configSGM.disparityRange = ((Number) spinnerDisparityRange.getValue()).intValue();
			} else if( e.getSource() == spinnerError) {
				configSGM.maxError = ((Number) spinnerError.getValue()).intValue();
			} else if( e.getSource() == spinnerTexture) {
				configSGM.texture = ((Number) spinnerTexture.getValue()).doubleValue();
			} else if( e.getSource() == spinnerPenaltySmall) {
				configSGM.penaltySmallChange = ((Number) spinnerPenaltySmall.getValue()).intValue();
			} else if( e.getSource() == spinnerPenaltyLarge) {
				configSGM.penaltyLargeChange = ((Number) spinnerPenaltyLarge.getValue()).intValue();
			} else if( e.getSource() == radiusXSpinner) {
				configSGM.configBlockMatch.radiusX = ((Number) radiusXSpinner.getValue()).intValue();
			} else if( e.getSource() == radiusYSpinner) {
				configSGM.configBlockMatch.radiusY = ((Number) radiusYSpinner.getValue()).intValue();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if( e.getSource() == comboPaths) {
				configSGM.paths = ConfigDisparitySGM.Paths.values()[comboPaths.getSelectedIndex()];
			} else if( e.getSource() == subpixelToggle) {
				configSGM.subpixel = subpixelToggle.isSelected();
			} else if( e.getSource() == useBlocks) {
				configSGM.useBlocks = useBlocks.isSelected();
				updateControlsEnabled();
			} else if( e.getSource() == comboBlockApproach) {
				configSGM.configBlockMatch.approach = BlockMatchingApproach.values()[comboBlockApproach.getSelectedIndex()];
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}

		void updateControlsEnabled() {
			final boolean e = configSGM.useBlocks;
			comboBlockApproach.setEnabled(e);
			radiusXSpinner.setEnabled(e);
			radiusYSpinner.setEnabled(e);
		}
	}

	class ControlsSAD extends StandardAlgConfigPanel {

	}

	class ControlsCensus extends StandardAlgConfigPanel implements ChangeListener, ActionListener {
		JComboBox<String> comboVariant = combo(0, (Object[]) CensusVariants.values());
		ConfigDisparityError.Census settings;

		public ControlsCensus() {
			addLabeled(comboVariant, "Variant");
		}

		public void update( ConfigDisparityError.Census settings ) {
			this.settings = settings;
			comboVariant.setSelectedIndex(settings.variant.ordinal());
		}

		@Override
		public void stateChanged(ChangeEvent e) {

		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if( e.getSource() == comboVariant) {
				settings.variant = CensusVariants.values()[comboVariant.getSelectedIndex()];
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	class ControlsNCC extends StandardAlgConfigPanel implements ChangeListener, ActionListener {
		JSpinner spinnerEps = spinner(0.0,0, 1.0,0.001,"0.0E0",10);
		ConfigDisparityError.NCC settings;

		ControlsNCC() {
			addLabeled(spinnerEps, "EPS");
		}

		public void update( ConfigDisparityError.NCC settings) {
			this.settings = settings;
			spinnerEps.setValue(settings.eps);
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == spinnerEps) {
				settings.eps = ((Number) spinnerEps.getValue()).doubleValue();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}

		@Override
		public void actionPerformed(ActionEvent e) {

		}
	}

	class ControlsMutualInfo extends StandardAlgConfigPanel implements ChangeListener, ActionListener {
		JSpinner spinnerBlur = spinner(1,0, 10,1);
		JSpinner spinnerPyramidWidth = spinner(20,20, 10000,50);
		JSpinner spinnerExtra = spinner(0,0, 5,1);

		ConfigDisparityError.HMI settings;

		ControlsMutualInfo() {
			addLabeled(spinnerBlur, "Blur Radius");
			addLabeled(spinnerPyramidWidth, "Pyr Min W");
			addLabeled(spinnerExtra, "Extra Iter.");
		}

		public void update( ConfigDisparityError.HMI settings ) {
			this.settings = settings;
			spinnerBlur.setValue(settings.smoothingRadius);
			spinnerPyramidWidth.setValue(settings.pyramidLayers.minWidth);
			spinnerExtra.setValue(settings.extraIterations);
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == spinnerBlur) {
				settings.smoothingRadius = ((Number) spinnerBlur.getValue()).intValue();
			} else if( e.getSource() == spinnerPyramidWidth) {
				settings.pyramidLayers.minWidth = ((Number) spinnerPyramidWidth.getValue()).intValue();
				settings.pyramidLayers.minHeight = ((Number) spinnerPyramidWidth.getValue()).intValue();
			} else if( e.getSource() == spinnerExtra) {
				settings.extraIterations = ((Number) spinnerExtra.getValue()).intValue();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
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
		DisparityControlPanel controls = DisparityControlPanel.createRange(0,150,null);
		ShowImages.showWindow(controls,"Controls");
	}
}
