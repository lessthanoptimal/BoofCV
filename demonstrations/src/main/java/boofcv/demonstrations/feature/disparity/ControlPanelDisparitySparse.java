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

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.factory.feature.disparity.ConfigDisparityBM;
import boofcv.factory.feature.disparity.ConfigDisparityError;
import boofcv.factory.feature.disparity.DisparityError;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.transform.census.CensusVariants;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ShowImages;
import boofcv.struct.image.ImageGray;

import javax.swing.*;
import java.awt.*;

/**
 * Controls for configuring sparse disparity algorithms
 *
 * @author Peter Abeles
 */
public class ControlPanelDisparitySparse extends StandardAlgConfigPanel {

	private static String[] ERRORS_BLOCK = new String[]{"SAD","Census","NCC"};

	public final ConfigDisparityBM config;

	JComboBox<String> comboError;

	JTabbedPane tabbedPane = new JTabbedPane();

	// Controls for families of algorithms
	ControlsBlockMatching controlBM;

	// Controls for error types
	ControlsSAD controlSad;
	ControlsCensus controlCensus;
	ControlsNCC controlNCC;
	ControlsMutualInfo controlHMI;

	boolean ignoreChanges=false;

	Listener listener;

	/**
	 * Configures the control panel
	 *
	 * @param config Default configuration
	 * @param listener Listener for when changes are made.
	 */
	public ControlPanelDisparitySparse(ConfigDisparityBM config, Listener listener)
	{
		this.config = config;
		this.listener = listener;

		comboError = combo(e -> handleErrorSelected(false), config.errorType.ordinal(),(Object[])ERRORS_BLOCK);
		controlBM = new ControlsBlockMatching();
		controlSad = new ControlsSAD();
		controlCensus = new ControlsCensus();
		controlNCC = new ControlsNCC();
		controlHMI = new ControlsMutualInfo();

		tabbedPane.addTab("Disparity",controlBM);
		tabbedPane.addTab("Error",getErrorControl(comboError.getSelectedIndex()));

		addLabeled(comboError,"Error");
		add(tabbedPane);
	}

	public void broadcastChange() {
		Listener listener = this.listener;
		if( listener == null )
			return;
		if( ignoreChanges )
			return;

		listener.handleSparseDisparityChange();
	}


	public <T extends ImageGray<T>>
	StereoDisparitySparse<T> createAlgorithm(Class<T> imageType) {
		return FactoryStereoDisparity.sparseBlockMatching(config,imageType);
	}

	public int getDisparityMin() {
		return config.disparityMin;
	}

	public int getDisparityRange() {
		return config.disparityRange;
	}

	private void handleErrorSelected( boolean force ) {
		int previousIdx = config.errorType.ordinal();
		if( !force && previousIdx == comboError.getSelectedIndex() )
			return;
		int selectedIdx = comboError.getSelectedIndex();

		// Avoid multiple calls to broadcastChange()
		if( !force )
			ignoreChanges = true;

		// If forced keep the previously active tab active
		int activeTab = tabbedPane.getSelectedIndex();
//		System.out.println("error for block="+block+" idx="+selectedIdx);

		config.errorType = DisparityError.values()[selectedIdx];
		controlCensus.update(config.configCensus);
		controlNCC.update(config.configNCC);

		Component c = getErrorControl(selectedIdx);
		tabbedPane.removeTabAt(1);
		tabbedPane.insertTab("Error",null,c,null,1);
		tabbedPane.setSelectedIndex(activeTab);

		if( !force )
			SwingUtilities.invokeLater(()->{ignoreChanges=false;broadcastChange();});
	}

	private Component getErrorControl( int selectedIdx ) {
		Component c;
		switch( selectedIdx ) {
			case 0: c = controlSad; break;
			case 1: c = controlCensus; break;
			case 2: c = controlNCC; break;
			default: throw new IllegalArgumentException("Unknown");
		}
		return c;
	}

	public class ControlsBlockMatching extends StandardAlgConfigPanel {
		JSpinner spinnerDisparityMin = spinner(config.disparityMin,0, 1000,5);
		JSpinner spinnerDisparityRange = spinner(config.disparityRange,1, 254,5);
		JSpinner radiusXSpinner = spinner(config.regionRadiusX,0,50,1);
		JSpinner radiusYSpinner = spinner(config.regionRadiusY,0,50,1);
		JSpinner spinnerError = spinner(config.maxPerPixelError,-1,80,5);
		JSpinner spinnerReverse = spinner(config.validateRtoL,-1,50,1);
		JSpinner spinnerTexture = spinner(config.texture,0.0,1.0,0.05,1,3);
		JCheckBox subpixelToggle = checkbox("Subpixel", config.subpixel,"Subpixel Disparity Estimate");

		ControlsBlockMatching() {
			addLabeled(spinnerDisparityMin, "Min Disp.","Minimum disparity value considered. (Pixels)");
			addLabeled(spinnerDisparityRange, "Range Disp.","Range of disparity values searched. (Pixels)");
			addLabeled(radiusXSpinner,    "Radius X","Block Width. (Pixels)");
			addLabeled(radiusYSpinner,    "Radius Y", "Block Height. (Pixels)");
			addLabeled(spinnerError,     "Max Error","Maximum allowed matching error");
			addLabeled(spinnerTexture,   "Texture","Texture validation. 0 = disabled. 1 = most strict.");
			addLabeled(spinnerReverse,   "Reverse","Reverse Validation Tolerance. -1 = disable. (Pixels)");
			addAlignLeft(subpixelToggle);
		}

		@Override
		public void controlChanged(final Object source) {
			if( source == spinnerReverse) {
				config.validateRtoL = ((Number) spinnerReverse.getValue()).intValue();
			} else if( source == spinnerDisparityMin) {
				config.disparityMin = ((Number) spinnerDisparityMin.getValue()).intValue();
			} else if( source == spinnerDisparityRange) {
				config.disparityRange = ((Number) spinnerDisparityRange.getValue()).intValue();
			} else if( source == spinnerError) {
				config.maxPerPixelError = ((Number) spinnerError.getValue()).intValue();
			} else if( source == radiusXSpinner) {
				config.regionRadiusX = ((Number) radiusXSpinner.getValue()).intValue();
			} else if( source == radiusYSpinner) {
				config.regionRadiusY = ((Number) radiusYSpinner.getValue()).intValue();
			} else if( source == spinnerTexture) {
				config.texture = ((Number) spinnerTexture.getValue()).doubleValue();
			} else if( source == subpixelToggle) {
				config.subpixel = subpixelToggle.isSelected();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	class ControlsSAD extends StandardAlgConfigPanel {

	}

	class ControlsCensus extends StandardAlgConfigPanel {
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
		public void controlChanged(final Object source) {
			if( source == comboVariant) {
				settings.variant = CensusVariants.values()[comboVariant.getSelectedIndex()];
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	class ControlsNCC extends StandardAlgConfigPanel {
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
		public void controlChanged(final Object source) {
			if( source == spinnerEps) {
				settings.eps = ((Number) spinnerEps.getValue()).doubleValue();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	class ControlsMutualInfo extends StandardAlgConfigPanel {
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
		public void controlChanged(final Object source) {
			if( source == spinnerBlur) {
				settings.smoothingRadius = ((Number) spinnerBlur.getValue()).intValue();
			} else if( source == spinnerPyramidWidth) {
				settings.pyramidLayers.minWidth = ((Number) spinnerPyramidWidth.getValue()).intValue();
				settings.pyramidLayers.minHeight = ((Number) spinnerPyramidWidth.getValue()).intValue();
			} else if( source == spinnerExtra) {
				settings.extraIterations = ((Number) spinnerExtra.getValue()).intValue();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	public Listener getListener() {
		return listener;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public interface Listener {
		void handleSparseDisparityChange();
	}

	public static void main(String[] args) {
		var controls = new ControlPanelDisparitySparse(new ConfigDisparityBM(),()->{});
		ShowImages.showWindow(controls,"Controls");
	}
}
