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

package boofcv.gui.feature;

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.gui.StandardAlgConfigPanel;

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
	private final JSpinner spinnerInitialSample;
	private final JSpinner spinnerInitialSize;
	private final JSpinner spinnerScalesPerOctave;
	private final JSpinner spinnerOctaves;
	private final JSpinner spinnerScaleStep;

	private final Listener listener;

	public ControlPanelFastHessian(ConfigFastHessian config, Listener listener) {
		this.config = config;
		this.listener = listener;

		this.controlExtractor = new ControlPanelExtractor(config.extract,listener::handleChangeFastHessian);
		spinnerMaxPerScale = spinner(config.maxFeaturesPerScale,-1,9999,100);
		spinnerInitialSample = spinner(config.initialSampleStep,1,10,1);
		spinnerInitialSize = spinner(config.initialSize,3,999,2);
		spinnerScalesPerOctave = spinner(config.numberScalesPerOctave,1,100,1);
		spinnerOctaves = spinner(config.numberOfOctaves,1,20,1);
		spinnerScaleStep = spinner(config.scaleStepSize,1,100,1);

		controlExtractor.setBorder(BorderFactory.createEmptyBorder());

		add(controlExtractor);
		addLabeled(spinnerMaxPerScale,"Max-Per-Scale","Maximum number of features detected per scale");
		addLabeled(spinnerInitialSample,"Sample Step","How often pixels are sampled in first octave");
		addLabeled(spinnerInitialSize,"Kernel Size","Size of sampling kernel");
		addLabeled(spinnerScalesPerOctave,"Scales","Number of scales per octave");
		addLabeled(spinnerOctaves,"Octaves","Number of octaves in the scale space");
		addLabeled(spinnerScaleStep,"Scale Step","Increment between kernel sizes at each scale");
	}

	@Override
	public void controlChanged(final Object source) {
		if (source == spinnerMaxPerScale) {
			config.maxFeaturesPerScale = ((Number)spinnerMaxPerScale.getValue()).intValue();
		} else if (source == spinnerInitialSample) {
			config.initialSampleStep = ((Number) spinnerInitialSample.getValue()).intValue();
		} else if (source == spinnerInitialSize) {
			config.initialSize = ((Number) spinnerInitialSize.getValue()).intValue();
		} else if (source == spinnerScalesPerOctave) {
			config.numberScalesPerOctave = ((Number) spinnerScalesPerOctave.getValue()).intValue();
		} else if (source == spinnerOctaves) {
			config.numberOfOctaves = ((Number) spinnerOctaves.getValue()).intValue();
		} else if (source == spinnerScaleStep) {
			config.scaleStepSize = ((Number) spinnerScaleStep.getValue()).intValue();
		}
		listener.handleChangeFastHessian();
	}

	public interface Listener {
		void handleChangeFastHessian();
	}
}
