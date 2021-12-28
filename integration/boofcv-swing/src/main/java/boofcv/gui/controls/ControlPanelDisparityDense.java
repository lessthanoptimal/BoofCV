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

import boofcv.abst.disparity.ConfigSpeckleFilter;
import boofcv.abst.disparity.DisparitySmoother;
import boofcv.abst.disparity.StereoDisparity;
import boofcv.alg.disparity.sgm.SgmDisparityCost;
import boofcv.factory.disparity.*;
import boofcv.factory.transform.census.CensusVariants;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ShowImages;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;

import javax.swing.*;
import java.awt.*;

/**
 * Controls for configuring disparity algorithms
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ControlPanelDisparityDense extends StandardAlgConfigPanel {

	private final static String[] ERRORS_BLOCK = new String[]{"SAD", "Census", "NCC"};
	private final static String[] ERRORS_SGM = new String[]{"Absolute Diff", "Census", "HMI"};

	// which algorithm to run
	int selectedMethod = 0;
	public final ConfigDisparityBMBest5 configBM;
	public final ConfigDisparitySGM configSGM;
	public final ConfigSpeckleFilter configSpeckle;

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

	// Disparity smoothing
	ControlsSpeckleConnComp controlSpeckle;

	boolean ignoreChanges = false;

	Listener listener;
	Class imageType;

	public ControlPanelDisparityDense( ConfigDisparityBMBest5 configBM,
									   ConfigDisparitySGM configSGM,
									   ConfigSpeckleFilter configSpeckle,
									   Class imageType ) {
		setBorder(BorderFactory.createEmptyBorder());
		this.configBM = configBM;
		this.configSGM = configSGM;
		this.configSpeckle = configSpeckle;
		this.imageType = imageType;

		comboMethod = combo(e -> handleMethod(), selectedMethod, "BlockMatch-5", "BlockMatch", "SGM");
		if (isBlockSelected())
			comboError = combo(e -> handleErrorSelected(false), configBM.errorType.ordinal(), (Object[])ERRORS_BLOCK);
		else
			comboError = combo(e -> handleErrorSelected(false), configSGM.errorType.ordinal(), (Object[])ERRORS_SGM);
		controlBM = new ControlsBlockMatching();
		controlSGM = new ControlsSemiGlobal();
		controlSad = new ControlsSAD();
		controlCensus = new ControlsCensus();
		controlNCC = new ControlsNCC();
		controlHMI = new ControlsMutualInfo();
		controlSpeckle = new ControlsSpeckleConnComp();

		tabbedPane.addTab("Method", getModelControl(isBlockSelected()));
		tabbedPane.addTab("Error", getErrorControl(comboError.getSelectedIndex()));
		tabbedPane.addTab("Smooth", controlSpeckle);

		// SGM is larger than BM, make the initial area larger
		controlBM.setPreferredSize(controlSGM.getPreferredSize());

		addLabeled(comboMethod, "Method");
		addLabeled(comboError, "Error");
		add(tabbedPane);

		// Make sure the GUI is updated with the latest selection
		handleErrorSelected(true);
	}

	public static ControlPanelDisparityDense createRange( int disparityMin, int disparityRange,
														  Class imageType ) {
		ConfigDisparityBMBest5 configBM = new ConfigDisparityBMBest5();
		ConfigDisparitySGM configSGM = new ConfigDisparitySGM();
		ConfigSpeckleFilter configSpeckle = new ConfigSpeckleFilter();

		configBM.disparityMin = disparityMin;
		configBM.disparityRange = disparityRange;
		configSGM.disparityMin = disparityMin;
		configSGM.disparityRange = disparityRange;

		return new ControlPanelDisparityDense(configBM, configSGM, configSpeckle, imageType);
	}

	public void broadcastChange() {
		Listener listener = this.listener;
		if (listener == null)
			return;
		if (ignoreChanges)
			return;

		listener.handleDisparityChange();
	}

	public void updateControlEnabled() {
		if (!isBlockSelected()) {
			controlSGM.updateControlsEnabled();
		}
	}

	@SuppressWarnings("unchecked")
	public StereoDisparity createAlgorithm() {
//		BoofSwingUtil.checkGuiThread(); // TODO lock instead to make this safe?

		boolean block = isBlockSelected();

		if (block) {
			Class dispType = configBM.subpixel ? GrayF32.class : GrayU8.class;
			if (selectedMethod == 0)
				return FactoryStereoDisparity.blockMatchBest5(configBM, imageType, dispType);
			else if (selectedMethod == 1)
				return FactoryStereoDisparity.blockMatch(configBM, imageType, dispType);
			else
				throw new RuntimeException("BUG");
		} else {
			Class dispType = configSGM.subpixel ? GrayF32.class : GrayU8.class;
			return FactoryStereoDisparity.sgm(configSGM, imageType, dispType);
		}
	}

	public DisparitySmoother createSmoother() {
		boolean block = isBlockSelected();

		Class dispType;
		if (block) {
			dispType = configBM.subpixel ? GrayF32.class : GrayU8.class;
		} else {
			dispType = configSGM.subpixel ? GrayF32.class : GrayU8.class;
		}

		return FactoryStereoDisparity.removeSpeckle(configSpeckle, dispType);
	}

	public int getDisparityMin() {
		if (isBlockSelected())
			return configBM.disparityMin;
		else
			return configSGM.disparityMin;
	}

	public int getDisparityRange() {
		if (isBlockSelected())
			return configBM.disparityRange;
		else
			return configSGM.disparityRange;
	}

	/**
	 * The user changed which method is being used
	 */
	private void handleMethod() {
		if (selectedMethod == comboMethod.getSelectedIndex())
			return;
		boolean previousBlock = isBlockSelected();
		selectedMethod = comboMethod.getSelectedIndex();
		boolean block = isBlockSelected();

		// All the code above can cause multiple calls to broadcastChange() as listeners are triggered
		ignoreChanges = true;

		// swap out the controls and stuff
		if (block != previousBlock) {
			int activeTab = tabbedPane.getSelectedIndex(); // don't switch out of the current tab
			if (block) {
				comboError.setModel(new DefaultComboBoxModel<>(ERRORS_BLOCK));
				comboError.setSelectedIndex(configBM.errorType.ordinal());
			} else {
				comboError.setModel(new DefaultComboBoxModel<>(ERRORS_SGM));
				comboError.setSelectedIndex(configSGM.errorType.ordinal());
			}
			Component c = getModelControl(block);
			if (!block)
				controlSGM.updateControlsEnabled();
			tabbedPane.removeTabAt(0);
			tabbedPane.insertTab("Method", null, c, null, 0);
			tabbedPane.setSelectedIndex(activeTab);
			handleErrorSelected(true);
		}

		// This will ignore all changes until after they have been processed
		SwingUtilities.invokeLater(() -> {
			ignoreChanges = false;
			broadcastChange();
		});
	}

	private Component getModelControl( boolean block ) {
		Component c;
		if (block) {
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
		if (!force && previousIdx == comboError.getSelectedIndex())
			return;
		int selectedIdx = comboError.getSelectedIndex();

		// Avoid multiple calls to broadcastChange()
		if (!force)
			ignoreChanges = true;

		// If forced keep the previously active tab active
		int activeTab = tabbedPane.getSelectedIndex();
//		System.out.println("error for block="+block+" idx="+selectedIdx);

		if (block) {
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
		tabbedPane.insertTab("Error", null, c, null, 1);
		tabbedPane.setSelectedIndex(activeTab);

		if (!force)
			SwingUtilities.invokeLater(() -> {
				ignoreChanges = false;
				broadcastChange();
			});
	}

	private Component getErrorControl( int selectedIdx ) {
		Component c;
		if (isBlockSelected()) {
			c = switch (selectedIdx) {
				case 0 -> controlSad;
				case 1 -> controlCensus;
				case 2 -> controlNCC;
				default -> throw new IllegalArgumentException("Unknown");
			};
		} else {
			c = switch (selectedIdx) {
				case 0 -> controlSad;
				case 1 -> controlCensus;
				case 2 -> controlHMI;
				default -> throw new IllegalArgumentException("Unknown");
			};
		}
		return c;
	}

	public class ControlsBlockMatching extends StandardAlgConfigPanel {
		JSpinner spinnerDisparityMin = spinner(configBM.disparityMin, 0, 1000, 5);
		JSpinner spinnerDisparityRange = spinner(configBM.disparityRange, 1, 254, 5);
		JSpinner radiusXSpinner = spinner(configBM.regionRadiusX, 0, 50, 1);
		JSpinner radiusYSpinner = spinner(configBM.regionRadiusY, 0, 50, 1);
		JSpinner spinnerError = spinner(configBM.maxPerPixelError, -1, 80, 5);
		JSpinner spinnerReverse = spinner(configBM.validateRtoL, -1, 50, 1);
		JSpinner spinnerTexture = spinner(configBM.texture, 0.0, 1.0, 0.05, 1, 3);
		JCheckBox subpixelToggle = checkbox("Subpixel", configBM.subpixel, "Subpixel Disparity Estimate");

		ControlsBlockMatching() {
			setBorder(BorderFactory.createEmptyBorder());
			addLabeled(spinnerDisparityMin, "Min Disp.", "Minimum disparity value considered. (Pixels)");
			addLabeled(spinnerDisparityRange, "Range Disp.", "Range of disparity values searched. (Pixels)");
			addLabeled(radiusXSpinner, "Radius X", "Block Width. (Pixels)");
			addLabeled(radiusYSpinner, "Radius Y", "Block Height. (Pixels)");
			addLabeled(spinnerError, "Max Error", "Maximum allowed matching error");
			addLabeled(spinnerTexture, "Texture", "Texture validation. 0 = disabled. 1 = most strict.");
			addLabeled(spinnerReverse, "Reverse", "Reverse Validation Tolerance. -1 = disable. (Pixels)");
			addAlignLeft(subpixelToggle);
		}

		@Override
		public void controlChanged( final Object source ) {
			if (source == spinnerReverse) {
				configBM.validateRtoL = ((Number)spinnerReverse.getValue()).intValue();
			} else if (source == spinnerDisparityMin) {
				configBM.disparityMin = ((Number)spinnerDisparityMin.getValue()).intValue();
			} else if (source == spinnerDisparityRange) {
				configBM.disparityRange = ((Number)spinnerDisparityRange.getValue()).intValue();
			} else if (source == spinnerError) {
				configBM.maxPerPixelError = ((Number)spinnerError.getValue()).intValue();
			} else if (source == radiusXSpinner) {
				configBM.regionRadiusX = ((Number)radiusXSpinner.getValue()).intValue();
			} else if (source == radiusYSpinner) {
				configBM.regionRadiusY = ((Number)radiusYSpinner.getValue()).intValue();
			} else if (source == spinnerTexture) {
				configBM.texture = ((Number)spinnerTexture.getValue()).doubleValue();
			} else if (source == subpixelToggle) {
				configBM.subpixel = subpixelToggle.isSelected();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	public class ControlsSemiGlobal extends StandardAlgConfigPanel {
		JComboBox<String> comboPaths = combo(configSGM.paths.ordinal(), "1", "2", "4", "8", "16");

		JSpinner spinnerPenaltySmall = spinner(configSGM.penaltySmallChange, 0, SgmDisparityCost.MAX_COST, 10);
		JSpinner spinnerPenaltyLarge = spinner(configSGM.penaltyLargeChange, 1, SgmDisparityCost.MAX_COST, 10);

		JSpinner spinnerDisparityMin = spinner(configSGM.disparityMin, 0, 1000, 5);
		JSpinner spinnerDisparityRange = spinner(configSGM.disparityRange, 1, 254, 5);
		JSpinner spinnerError = spinner(configSGM.maxError, -1, Short.MAX_VALUE, 200);
		JSpinner spinnerReverse = spinner(configSGM.validateRtoL, -1, 50, 1);
		JSpinner spinnerTexture = spinner(configSGM.texture, 0.0, 1.0, 0.05, 1, 3);
		JCheckBox subpixelToggle = checkbox("Subpixel", configSGM.subpixel);
		JCheckBox useBlocks = checkbox("Use Blocks", configSGM.useBlocks);
		JComboBox<String> comboBlockApproach = combo(configSGM.configBlockMatch.approach.ordinal(), (Object[])BlockMatchingApproach.values());
		JSpinner radiusXSpinner = spinner(configSGM.configBlockMatch.radiusX, 0, 50, 1); // TODO move to error
		JSpinner radiusYSpinner = spinner(configSGM.configBlockMatch.radiusY, 0, 50, 1);

		ControlsSemiGlobal() {
			setBorder(BorderFactory.createEmptyBorder());
			addLabeled(spinnerDisparityMin, "Min Disp.");
			addLabeled(spinnerDisparityRange, "Range Disp.");
			addLabeled(spinnerError, "Max Error");
			addLabeled(spinnerTexture, "Texture");
			addLabeled(spinnerReverse, "Reverse");
			addLabeled(comboPaths, "Paths");
			addLabeled(spinnerPenaltySmall, "Penalty Small");
			addLabeled(spinnerPenaltyLarge, "Penalty Large");
			addAlignLeft(subpixelToggle);
			addAlignLeft(useBlocks);
			addLabeled(comboBlockApproach, "Approach");
			addLabeled(radiusXSpinner, "Radius X");
			addLabeled(radiusYSpinner, "Radius Y");
			updateControlsEnabled();
		}

		@Override
		public void controlChanged( final Object source ) {
			if (source == spinnerReverse) {
				configSGM.validateRtoL = ((Number)spinnerReverse.getValue()).intValue();
			} else if (source == spinnerDisparityMin) {
				configSGM.disparityMin = ((Number)spinnerDisparityMin.getValue()).intValue();
			} else if (source == spinnerDisparityRange) {
				configSGM.disparityRange = ((Number)spinnerDisparityRange.getValue()).intValue();
			} else if (source == spinnerError) {
				configSGM.maxError = ((Number)spinnerError.getValue()).intValue();
			} else if (source == spinnerTexture) {
				configSGM.texture = ((Number)spinnerTexture.getValue()).doubleValue();
			} else if (source == spinnerPenaltySmall) {
				configSGM.penaltySmallChange = ((Number)spinnerPenaltySmall.getValue()).intValue();
			} else if (source == spinnerPenaltyLarge) {
				configSGM.penaltyLargeChange = ((Number)spinnerPenaltyLarge.getValue()).intValue();
			} else if (source == radiusXSpinner) {
				configSGM.configBlockMatch.radiusX = ((Number)radiusXSpinner.getValue()).intValue();
			} else if (source == radiusYSpinner) {
				configSGM.configBlockMatch.radiusY = ((Number)radiusYSpinner.getValue()).intValue();
			} else if (source == comboPaths) {
				configSGM.paths = ConfigDisparitySGM.Paths.values()[comboPaths.getSelectedIndex()];
			} else if (source == subpixelToggle) {
				configSGM.subpixel = subpixelToggle.isSelected();
			} else if (source == useBlocks) {
				configSGM.useBlocks = useBlocks.isSelected();
				updateControlsEnabled();
			} else if (source == comboBlockApproach) {
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

	static class ControlsSAD extends StandardAlgConfigPanel {}

	@SuppressWarnings({"NullAway.Init"})
	class ControlsCensus extends StandardAlgConfigPanel {
		JComboBox<String> comboVariant = combo(0, (Object[])CensusVariants.values());
		ConfigDisparityError.Census settings;

		public ControlsCensus() {
			setBorder(BorderFactory.createEmptyBorder());
			addLabeled(comboVariant, "Variant");
		}

		public void update( ConfigDisparityError.Census settings ) {
			this.settings = settings;
			comboVariant.setSelectedIndex(settings.variant.ordinal());
		}

		@Override
		public void controlChanged( final Object source ) {
			if (source == comboVariant) {
				settings.variant = CensusVariants.values()[comboVariant.getSelectedIndex()];
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	@SuppressWarnings({"NullAway.Init"})
	class ControlsNCC extends StandardAlgConfigPanel {
		JSpinner spinnerEps = spinner(0.0, 0, 1.0, 0.001, "0.0E0", 10);
		ConfigDisparityError.NCC settings;

		ControlsNCC() {
			setBorder(BorderFactory.createEmptyBorder());
			addLabeled(spinnerEps, "EPS");
		}

		public void update( ConfigDisparityError.NCC settings ) {
			this.settings = settings;
			spinnerEps.setValue(settings.eps);
		}

		@Override
		public void controlChanged( final Object source ) {
			if (source == spinnerEps) {
				settings.eps = ((Number)spinnerEps.getValue()).doubleValue();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	@SuppressWarnings({"NullAway.Init"})
	class ControlsMutualInfo extends StandardAlgConfigPanel {
		JSpinner spinnerBlur = spinner(1, 0, 10, 1);
		JSpinner spinnerPyramidWidth = spinner(20, 20, 10000, 50);
		JSpinner spinnerExtra = spinner(0, 0, 5, 1);

		ConfigDisparityError.HMI settings;

		ControlsMutualInfo() {
			setBorder(BorderFactory.createEmptyBorder());
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
		public void controlChanged( final Object source ) {
			if (source == spinnerBlur) {
				settings.smoothingRadius = ((Number)spinnerBlur.getValue()).intValue();
			} else if (source == spinnerPyramidWidth) {
				settings.pyramidLayers.minWidth = ((Number)spinnerPyramidWidth.getValue()).intValue();
				settings.pyramidLayers.minHeight = ((Number)spinnerPyramidWidth.getValue()).intValue();
			} else if (source == spinnerExtra) {
				settings.extraIterations = ((Number)spinnerExtra.getValue()).intValue();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	class ControlsSpeckleConnComp extends StandardAlgConfigPanel {
		JSpinner spinnerSimilar = spinner(configSpeckle.similarTol, 0.0, 100.0, 0.5);
		JConfigLength lengthRegion = configLength(configSpeckle.maximumArea, 0.0, 1e6);

		public ControlsSpeckleConnComp() {
			setBorder(BorderFactory.createEmptyBorder());
			addLabeled(spinnerSimilar, "Simularity",
					"How similar two pixel values need to be considered connected.");
			addLabeled(lengthRegion, "Region", "Maximum region size for removal");
		}

		@Override
		public void controlChanged( final Object source ) {
			if (source == spinnerSimilar) {
				configSpeckle.similarTol = ((Number)spinnerSimilar.getValue()).floatValue();
			} else if (source == lengthRegion) {
				configSpeckle.maximumArea.setTo(lengthRegion.getValue());
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	public Listener getListener() {
		return listener;
	}

	public void setListener( Listener listener ) {
		this.listener = listener;
	}

	public interface Listener {
		void handleDisparityChange();
	}

	public static void main( String[] args ) {
		ControlPanelDisparityDense controls = ControlPanelDisparityDense.createRange(0, 150, GrayU8.class);
		ShowImages.showWindow(controls, "Controls");
	}
}
