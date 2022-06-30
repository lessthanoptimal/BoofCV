/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

	// Disparity configuration. NOT publicly accessible because BM and BM5 are mirrored. use accessor
	private final ConfigDisparity configDisparity;
	// Configuration for speckle filtering
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

	public ControlPanelDisparityDense( ConfigDisparity configDisparity,
									   ConfigSpeckleFilter configSpeckle,
									   Class imageType ) {
		setBorder(BorderFactory.createEmptyBorder());
		this.configDisparity = configDisparity;
		this.configSpeckle = configSpeckle;
		this.imageType = imageType;

		comboMethod = combo(e -> handleMethod(), configDisparity.approach.ordinal(), "BlockMatch", "BlockMatch-5", "SGM");
		if (isBlockSelected())
			comboError = combo(e -> handleErrorSelected(false), configDisparity.approachBM.errorType.ordinal(), (Object[])ERRORS_BLOCK);
		else
			comboError = combo(e -> handleErrorSelected(false), configDisparity.approachSGM.errorType.ordinal(), (Object[])ERRORS_SGM);
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
		var c = new ConfigDisparity();
		c.approachBM.disparityMin = disparityMin;
		c.approachBM.disparityRange = disparityRange;
		c.approachSGM.disparityMin = disparityMin;
		c.approachSGM.disparityRange = disparityRange;

		return new ControlPanelDisparityDense(c, new ConfigSpeckleFilter(), imageType);
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

	/**
	 * Accessor function to copy over the disparity configuration. This is needed because BM and BM5 are mirrored
	 */
	public void getDisparityConfig( ConfigDisparity dst ) {
		configDisparity.approachBM5.setTo(configDisparity.approachBM);
		dst.setTo(configDisparity);
	}

	@SuppressWarnings("unchecked")
	public StereoDisparity createAlgorithm() {
//		BoofSwingUtil.checkGuiThread(); // TODO lock instead to make this safe?

		configDisparity.approachBM5.setTo(configDisparity.approachBM);
		return FactoryStereoDisparity.generic(configDisparity, imageType);
	}

	public DisparitySmoother createSmoother() {
		configDisparity.approachBM5.setTo(configDisparity.approachBM);
		Class dispType = configDisparity.isSubpixel() ? GrayF32.class : GrayU8.class;
		return FactoryStereoDisparity.removeSpeckle(configSpeckle, dispType);
	}

	public int getDisparityMin() {
		if (isBlockSelected())
			return configDisparity.approachBM.disparityMin;
		else
			return configDisparity.approachSGM.disparityMin;
	}

	public int getDisparityRange() {
		if (isBlockSelected())
			return configDisparity.approachBM.disparityRange;
		else
			return configDisparity.approachSGM.disparityRange;
	}

	/**
	 * The user changed which method is being used
	 */
	private void handleMethod() {
		if (configDisparity.approach.ordinal() == comboMethod.getSelectedIndex())
			return;
		boolean previousBlock = isBlockSelected();
		configDisparity.approach = ConfigDisparity.Approach.values()[comboMethod.getSelectedIndex()];
		boolean block = isBlockSelected();

		// All the code above can cause multiple calls to broadcastChange() as listeners are triggered
		ignoreChanges = true;

		// swap out the controls and stuff
		if (block != previousBlock) {
			int activeTab = tabbedPane.getSelectedIndex(); // don't switch out of the current tab
			if (block) {
				comboError.setModel(new DefaultComboBoxModel<>(ERRORS_BLOCK));
				comboError.setSelectedIndex(configDisparity.approachBM.errorType.ordinal());
			} else {
				comboError.setModel(new DefaultComboBoxModel<>(ERRORS_SGM));
				comboError.setSelectedIndex(configDisparity.approachSGM.errorType.ordinal());
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
		return configDisparity.approach != ConfigDisparity.Approach.SGM;
	}

	private void handleErrorSelected( boolean force ) {
		boolean block = isBlockSelected();
		int previousIdx = block ? configDisparity.approachBM.errorType.ordinal() : configDisparity.approachSGM.errorType.ordinal();
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
			configDisparity.approachBM.errorType = DisparityError.values()[selectedIdx];
			controlCensus.update(configDisparity.approachBM.configCensus);
			controlNCC.update(configDisparity.approachBM.configNCC);
		} else {
			configDisparity.approachSGM.errorType = DisparitySgmError.values()[selectedIdx];
			controlCensus.update(configDisparity.approachSGM.configCensus);
			controlHMI.update(configDisparity.approachSGM.configHMI);
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
		JSpinner spinnerDisparityMin = spinner(configDisparity.approachBM.disparityMin, 0, 1000, 5);
		JSpinner spinnerDisparityRange = spinner(configDisparity.approachBM.disparityRange, 1, 254, 5);
		JSpinner radiusXSpinner = spinner(configDisparity.approachBM.regionRadiusX, 0, 50, 1);
		JSpinner radiusYSpinner = spinner(configDisparity.approachBM.regionRadiusY, 0, 50, 1);
		JSpinner spinnerError = spinner(configDisparity.approachBM.maxPerPixelError, -1, 80, 5);
		JSpinner spinnerReverse = spinner(configDisparity.approachBM.validateRtoL, -1, 50, 1);
		JSpinner spinnerTexture = spinner(configDisparity.approachBM.texture, 0.0, 1.0, 0.05, 1, 3);
		JCheckBox subpixelToggle = checkbox("Subpixel", configDisparity.approachBM.subpixel, "Subpixel Disparity Estimate");

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
			ConfigDisparityBM c = configDisparity.approachBM;

			if (source == spinnerReverse) {
				c.validateRtoL = ((Number)spinnerReverse.getValue()).intValue();
			} else if (source == spinnerDisparityMin) {
				c.disparityMin = ((Number)spinnerDisparityMin.getValue()).intValue();
			} else if (source == spinnerDisparityRange) {
				c.disparityRange = ((Number)spinnerDisparityRange.getValue()).intValue();
			} else if (source == spinnerError) {
				c.maxPerPixelError = ((Number)spinnerError.getValue()).intValue();
			} else if (source == radiusXSpinner) {
				c.regionRadiusX = ((Number)radiusXSpinner.getValue()).intValue();
			} else if (source == radiusYSpinner) {
				c.regionRadiusY = ((Number)radiusYSpinner.getValue()).intValue();
			} else if (source == spinnerTexture) {
				c.texture = ((Number)spinnerTexture.getValue()).doubleValue();
			} else if (source == subpixelToggle) {
				c.subpixel = subpixelToggle.isSelected();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	public class ControlsSemiGlobal extends StandardAlgConfigPanel {
		JComboBox<String> comboPaths = combo(configDisparity.approachSGM.paths.ordinal(), "1", "2", "4", "8", "16");

		JSpinner spinnerPenaltySmall = spinner(configDisparity.approachSGM.penaltySmallChange, 0, SgmDisparityCost.MAX_COST, 10);
		JSpinner spinnerPenaltyLarge = spinner(configDisparity.approachSGM.penaltyLargeChange, 1, SgmDisparityCost.MAX_COST, 10);

		JSpinner spinnerDisparityMin = spinner(configDisparity.approachSGM.disparityMin, 0, 1000, 5);
		JSpinner spinnerDisparityRange = spinner(configDisparity.approachSGM.disparityRange, 1, 254, 5);
		JSpinner spinnerError = spinner(configDisparity.approachSGM.maxError, -1, Short.MAX_VALUE, 200);
		JSpinner spinnerReverse = spinner(configDisparity.approachSGM.validateRtoL, -1, 50, 1);
		JSpinner spinnerTexture = spinner(configDisparity.approachSGM.texture, 0.0, 1.0, 0.05, 1, 3);
		JCheckBox subpixelToggle = checkbox("Subpixel", configDisparity.approachSGM.subpixel);
		JCheckBox useBlocks = checkbox("Use Blocks", configDisparity.approachSGM.useBlocks);
		JComboBox<String> comboBlockApproach = combo(configDisparity.approachSGM.configBlockMatch.approach.ordinal(), (Object[])BlockMatchingApproach.values());
		JSpinner radiusXSpinner = spinner(configDisparity.approachSGM.configBlockMatch.radiusX, 0, 50, 1); // TODO move to error
		JSpinner radiusYSpinner = spinner(configDisparity.approachSGM.configBlockMatch.radiusY, 0, 50, 1);

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
			ConfigDisparitySGM c = configDisparity.approachSGM;

			if (source == spinnerReverse) {
				c.validateRtoL = ((Number)spinnerReverse.getValue()).intValue();
			} else if (source == spinnerDisparityMin) {
				c.disparityMin = ((Number)spinnerDisparityMin.getValue()).intValue();
			} else if (source == spinnerDisparityRange) {
				c.disparityRange = ((Number)spinnerDisparityRange.getValue()).intValue();
			} else if (source == spinnerError) {
				c.maxError = ((Number)spinnerError.getValue()).intValue();
			} else if (source == spinnerTexture) {
				c.texture = ((Number)spinnerTexture.getValue()).doubleValue();
			} else if (source == spinnerPenaltySmall) {
				c.penaltySmallChange = ((Number)spinnerPenaltySmall.getValue()).intValue();
			} else if (source == spinnerPenaltyLarge) {
				c.penaltyLargeChange = ((Number)spinnerPenaltyLarge.getValue()).intValue();
			} else if (source == radiusXSpinner) {
				c.configBlockMatch.radiusX = ((Number)radiusXSpinner.getValue()).intValue();
			} else if (source == radiusYSpinner) {
				c.configBlockMatch.radiusY = ((Number)radiusYSpinner.getValue()).intValue();
			} else if (source == comboPaths) {
				c.paths = ConfigDisparitySGM.Paths.values()[comboPaths.getSelectedIndex()];
			} else if (source == subpixelToggle) {
				c.subpixel = subpixelToggle.isSelected();
			} else if (source == useBlocks) {
				c.useBlocks = useBlocks.isSelected();
				updateControlsEnabled();
			} else if (source == comboBlockApproach) {
				c.configBlockMatch.approach = BlockMatchingApproach.values()[comboBlockApproach.getSelectedIndex()];
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}

		void updateControlsEnabled() {
			final boolean e = configDisparity.approachSGM.useBlocks;
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
