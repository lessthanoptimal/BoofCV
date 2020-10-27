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

package boofcv.demonstrations.feature.detect.line;

import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.factory.feature.detect.line.ConfigEdgeThreshold;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;

/**
 * Control panel for {@link boofcv.factory.feature.detect.line.ConfigEdgeThreshold}
 *
 * @author Peter Abeles
 */
public class ControlPanelEdgeThreshold extends StandardAlgConfigPanel {
	public ConfigEdgeThreshold config;

	private final JComboBox<String> comboDerivative;
	private final JSpinner sThreshold;
	private final JCheckBox cNonMax;

	private final Listener listener;

	public ControlPanelEdgeThreshold( ConfigEdgeThreshold config, Listener listener ) {
		this.config = config;
		this.listener = listener;

		comboDerivative = combo(config.gradient.ordinal(), (Object[])DerivativeType.values());
		sThreshold = spinner(config.threshold, 0, 1000, 1.0f);
		cNonMax = checkbox("Non-Max", config.nonMax, "Apply gradient non-maximum suppression");

		addLabeled(comboDerivative, "Derivative");
		addLabeled(sThreshold, "Threshold");
		addAlignLeft(cNonMax);
	}

	@Override
	public void controlChanged( final Object source ) {
		if (source == comboDerivative) {
			config.gradient = DerivativeType.values()[comboDerivative.getSelectedIndex()];
		} else if (source == sThreshold) {
			config.threshold = ((Number)sThreshold.getValue()).floatValue();
		} else if (source == cNonMax) {
			config.nonMax = cNonMax.isSelected();
		}
		listener.handleEdgeThreshold();
	}

	public interface Listener {
		void handleEdgeThreshold();
	}
}
