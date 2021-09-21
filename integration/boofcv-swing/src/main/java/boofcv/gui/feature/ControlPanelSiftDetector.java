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

package boofcv.gui.feature;

import boofcv.abst.feature.describe.ConfigSiftScaleSpace;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.factory.feature.detect.selector.SelectLimitTypes;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ShowImages;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Control panel for SIFT. {@link ConfigSiftScaleSpace} and {@link ConfigSiftDetector}
 *
 * @author Peter Abeles
 */
public class ControlPanelSiftDetector extends StandardAlgConfigPanel {
	public final ConfigSiftScaleSpace configSS;
	public final ConfigSiftDetector configDetector;

	private final ControlPanelExtractor controlExtractor;
	private final JSpinner spinnerMaxPerScale;
	private final JSpinner spinnerMaxAll;
	private final JComboBox<String> comboSelector;
	private final JSpinner spinnerEdgeResponse;
	private final ControlPanelSiftScaleSpace controlSS;

	private final Listener listener;

	public ControlPanelSiftDetector( Listener listener ) {
		this(new ConfigSiftScaleSpace(), new ConfigSiftDetector(), listener);
	}

	public ControlPanelSiftDetector( @Nullable ConfigSiftScaleSpace configSS_, @Nullable ConfigSiftDetector configDetector_, Listener listener ) {
		this.configSS = configSS_ == null ? new ConfigSiftScaleSpace() : configSS_;
		this.configDetector = configDetector_ == null ? new ConfigSiftDetector() : configDetector_;
		this.listener = listener;

		this.controlExtractor = new ControlPanelExtractor(configDetector.extract, listener::handleChangeSiftDetector);
		this.controlSS = new ControlPanelSiftScaleSpace(configSS, listener::handleChangeSiftDetector);
		this.spinnerMaxPerScale = spinner(configDetector.maxFeaturesPerScale, 0, 9999, 100);
		this.spinnerMaxAll = spinner(configDetector.maxFeaturesAll, -1, 9999, 100);
		this.comboSelector = combo(configDetector.selector.type.ordinal(), (Object[])SelectLimitTypes.values());
		this.spinnerEdgeResponse = spinner(configDetector.edgeR, 1.0, 1000.0, 1.0);

		var controlDetection = new StandardAlgConfigPanel();
		controlDetection.add(controlExtractor);
		controlDetection.addLabeled(spinnerMaxPerScale, "Max-Per-Scale", "Maximum number of features detected per scale");
		controlDetection.addLabeled(spinnerMaxAll, "Max-All", "Maximum number of features allowed");
		controlDetection.addLabeled(comboSelector, "Type",
				"Method used to select points when more have been detected than the maximum allowed");
		controlDetection.addLabeled(spinnerEdgeResponse, "Max Edge", "Maximum edge response. Larger values are more tolerant");

		// Remove borders and label border
		controlExtractor.setBorder(BorderFactory.createEmptyBorder());
		controlSS.setBorder(BorderFactory.createTitledBorder("Scale-Space"));
		controlDetection.setBorder(BorderFactory.createTitledBorder("Detection"));

		add(controlDetection);
		add(controlSS);
	}

	@Override
	public void controlChanged( final Object source ) {
		if (source == spinnerMaxPerScale) {
			configDetector.maxFeaturesPerScale = ((Number)spinnerMaxPerScale.getValue()).intValue();
		} else if (source == spinnerMaxAll) {
			configDetector.maxFeaturesAll = ((Number)spinnerMaxAll.getValue()).intValue();
		} else if (source == comboSelector) {
			configDetector.selector.type = SelectLimitTypes.values()[comboSelector.getSelectedIndex()];
		} else if (source == spinnerEdgeResponse) {
			configDetector.edgeR = ((Number)spinnerEdgeResponse.getValue()).doubleValue();
		}
		listener.handleChangeSiftDetector();
	}

	@FunctionalInterface
	public interface Listener {
		void handleChangeSiftDetector();
	}

	public static void main( String[] args ) {
		var control = new ControlPanelSiftDetector(() -> {});
		ShowImages.showWindow(control, "Sift Detector", true);
	}
}
