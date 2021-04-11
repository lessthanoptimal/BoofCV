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

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;

/**
 * GUI control panel for {@link ConfigExtract}
 *
 * @author Peter Abeles
 */
public class ControlPanelExtractor extends StandardAlgConfigPanel {
	public final ConfigExtract config;

	private final JSpinner spinnerRadius;
	private final JSpinner spinnerThreshold;

	private final Listener listener;

	public ControlPanelExtractor( ConfigExtract config, Listener listener ) {
		this.config = config;
		this.listener = listener;

		spinnerRadius = spinner(config.radius, 0, 999, 1);
		spinnerThreshold = spinner(config.threshold, 0.0, 100000, 1.0, "0.0E0", 8);

		addLabeled(spinnerRadius, "Radius", "Non-Maximum suppression radius");
		addLabeled(spinnerThreshold, "Threshold", "Minimum detection threshold. Set to 0 to disable");
	}

	@Override
	public void controlChanged( final Object source ) {
		if (source == spinnerRadius) {
			config.radius = ((Number)spinnerRadius.getValue()).intValue();
		} else if (source == spinnerThreshold) {
			config.threshold = ((Number)spinnerThreshold.getValue()).floatValue();
		}
		listener.handleExtractorChange();
	}

	@FunctionalInterface
	public interface Listener {
		void handleExtractorChange();
	}
}
