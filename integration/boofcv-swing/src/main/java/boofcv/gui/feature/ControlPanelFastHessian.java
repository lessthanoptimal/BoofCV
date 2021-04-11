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

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.factory.feature.detect.selector.SelectLimitTypes;
import boofcv.gui.StandardAlgConfigPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Controls for configuring {@link ConfigFastHessian}.
 *
 * @author Peter Abeles
 */
public class ControlPanelFastHessian extends StandardAlgConfigPanel {
	public final ConfigFastHessian config;

	private final ControlPanelExtractor controlExtractor;
	private final JSpinner spinnerMaxPerScale;
	private final JSpinner spinnerMaxAll;
	private final JComboBox<String> comboSelector;
	private final JSpinner spinnerInitialSample;
	private final JSpinner spinnerInitialSize;
	private final JSpinner spinnerScalesPerOctave;
	private final JSpinner spinnerOctaves;
	private final JSpinner spinnerScaleStep;

	private final Listener listener;

	public ControlPanelFastHessian( @Nullable ConfigFastHessian config_, Listener listener ) {
		this.config = config_ == null ? new ConfigFastHessian() : config_;
		this.listener = listener;

		this.controlExtractor = new ControlPanelExtractor(config.extract, listener::handleChangeFastHessian);
		spinnerMaxPerScale = spinner(config.maxFeaturesPerScale, -1, 9999, 100);
		this.spinnerMaxAll = spinner(config.maxFeaturesAll, -1, 9999, 100);
		this.comboSelector = combo(config.selector.type.ordinal(), (Object[])SelectLimitTypes.values());
		spinnerInitialSample = spinner(config.initialSampleStep, 1, 10, 1);
		spinnerInitialSize = spinner(config.initialSize, 3, 999, 2);
		spinnerScalesPerOctave = spinner(config.numberScalesPerOctave, 1, 100, 1);
		spinnerOctaves = spinner(config.numberOfOctaves, 1, 20, 1);
		spinnerScaleStep = spinner(config.scaleStepSize, 1, 100, 1);

		controlExtractor.setBorder(BorderFactory.createEmptyBorder());

		add(controlExtractor);
		addLabeled(spinnerMaxPerScale, "Max-Per-Scale", "Maximum number of features detected per scale");
		addLabeled(spinnerMaxAll, "Max-All", "Maximum number of features allowed");
		addLabeled(comboSelector, "Type", "Method used to select points when more have been detected than the maximum allowed");
		addLabeled(spinnerInitialSample, "Sample Step", "How often pixels are sampled in first octave");
		addLabeled(spinnerInitialSize, "Kernel Size", "Size of sampling kernel");
		addLabeled(spinnerScalesPerOctave, "Scales", "Number of scales per octave");
		addLabeled(spinnerOctaves, "Octaves", "Number of octaves in the scale space");
		addLabeled(spinnerScaleStep, "Scale Step", "Increment between kernel sizes at each scale");
	}

	@Override
	public void controlChanged( final Object source ) {
		if (source == spinnerMaxPerScale) {
			config.maxFeaturesPerScale = ((Number)spinnerMaxPerScale.getValue()).intValue();
		} else if (source == spinnerMaxAll) {
			config.maxFeaturesAll = ((Number)spinnerMaxAll.getValue()).intValue();
		} else if (source == comboSelector) {
			config.selector.type = SelectLimitTypes.values()[comboSelector.getSelectedIndex()];
		} else if (source == spinnerInitialSample) {
			config.initialSampleStep = ((Number)spinnerInitialSample.getValue()).intValue();
		} else if (source == spinnerInitialSize) {
			config.initialSize = ((Number)spinnerInitialSize.getValue()).intValue();
		} else if (source == spinnerScalesPerOctave) {
			config.numberScalesPerOctave = ((Number)spinnerScalesPerOctave.getValue()).intValue();
		} else if (source == spinnerOctaves) {
			config.numberOfOctaves = ((Number)spinnerOctaves.getValue()).intValue();
		} else if (source == spinnerScaleStep) {
			config.scaleStepSize = ((Number)spinnerScaleStep.getValue()).intValue();
		}
		listener.handleChangeFastHessian();
	}

	@FunctionalInterface
	public interface Listener {
		void handleChangeFastHessian();
	}
}
