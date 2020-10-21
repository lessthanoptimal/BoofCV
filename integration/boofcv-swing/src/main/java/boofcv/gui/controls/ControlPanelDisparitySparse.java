/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.disparity.StereoDisparitySparse;
import boofcv.factory.disparity.ConfigDisparityBM;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.disparity.FactoryStereoDisparity;
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

	boolean ignoreChanges=false;

	Listener listener;

	/**
	 * Configures the control panel
	 *
	 * @param listener Listener for when changes are made.
	 * @param config Default configuration
	 */
	public ControlPanelDisparitySparse(Listener listener, ConfigDisparityBM config)
	{
		setBorder(BorderFactory.createEmptyBorder());
		this.config = config;
		this.listener = listener;

		comboError = combo(e -> handleErrorSelected(false), config.errorType.ordinal(),(Object[])ERRORS_BLOCK);
		controlBM = new ControlsBlockMatching();
		controlSad = new ControlsSAD();
		controlCensus = new ControlsCensus();
		controlNCC = new ControlsNCC();

		tabbedPane.addTab("Disparity",controlBM);
		tabbedPane.addTab("Error",getErrorControl(comboError.getSelectedIndex()));
		tabbedPane.setBorder(BorderFactory.createEmptyBorder());

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
		return FactoryStereoDisparity.sparseRectifiedBM(config,imageType);
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
		controlCensus.update();
		controlNCC.update();

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
			setBorder(BorderFactory.createEmptyBorder());
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
			if( source == spinnerDisparityMin) {
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
			} else if( source == spinnerReverse) {
				config.validateRtoL = ((Number) spinnerReverse.getValue()).intValue();
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	static class ControlsSAD extends StandardAlgConfigPanel {

	}

	class ControlsCensus extends StandardAlgConfigPanel {
		JComboBox<String> comboVariant = combo(config.configCensus.variant.ordinal(), (Object[]) CensusVariants.values());

		public ControlsCensus() {
			setBorder(BorderFactory.createEmptyBorder());
			addLabeled(comboVariant, "Variant");
		}

		public void update() {
			comboVariant.setSelectedIndex(config.configCensus.variant.ordinal());
		}

		@Override
		public void controlChanged(final Object source) {
			if( source == comboVariant) {
				config.configCensus.variant = CensusVariants.values()[comboVariant.getSelectedIndex()];
			} else {
				throw new RuntimeException("Unknown");
			}
			broadcastChange();
		}
	}

	class ControlsNCC extends StandardAlgConfigPanel {
		JSpinner spinnerEps = spinner(config.configNCC.eps,0, 1.0,0.001,"0.0E0",10);

		ControlsNCC() {
			setBorder(BorderFactory.createEmptyBorder());
			addLabeled(spinnerEps, "EPS");
		}

		public void update( ) {
			spinnerEps.setValue(config.configNCC.eps);
		}

		@Override
		public void controlChanged(final Object source) {
			if( source == spinnerEps) {
				config.configNCC.eps = ((Number) spinnerEps.getValue()).doubleValue();
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
		var controls = new ControlPanelDisparitySparse(()->{}, new ConfigDisparityBM());
		ShowImages.showWindow(controls,"Controls");
	}
}
