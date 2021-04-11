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

import boofcv.abst.feature.describe.ConfigBrief;
import boofcv.gui.StandardAlgConfigPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Control panel for {@link ConfigBrief}
 *
 * @author Peter Abeles
 */
public class ControlPanelDescribeBrief extends StandardAlgConfigPanel {
	public final ConfigBrief config;

	private final JSpinner spinnerRadius;
	private final JSpinner spinnerNumPoints;
	private final JSpinner spinnerBlurSigma;
	private final JSpinner spinnerBlurRadius;
	private final JCheckBox checkFixed;

	private final Listener listener;

	public ControlPanelDescribeBrief( @Nullable ConfigBrief config_, Listener listener ) {
		config = config_ == null ? new ConfigBrief() : config_;
		this.listener = listener;

		spinnerRadius = spinner(config.radius, 1, 999, 2);
		spinnerNumPoints = spinner(config.numPoints, 1, 9999, 128);
		spinnerBlurSigma = spinner(config.blurSigma, -1, 100.0, 0.5);
		spinnerBlurRadius = spinner(config.blurRadius, -1, 10000, 1);
		checkFixed = checkbox("Fixed", config.fixed, "true = fixed shape. false = scales and rotates");

		addLabeled(spinnerRadius, "Radius", "Radius of the descriptor's sample region");
		addLabeled(spinnerNumPoints, "Num Points", "Number of points it will sample. Descriptor length.");
		addLabeled(spinnerBlurSigma, "Blur Sigma", "Amount of blur applied to the image before sampling.");
		addLabeled(spinnerBlurRadius, "Blur Radius", "Amount of blur applied to the image before sampling.");
		addAlignLeft(checkFixed);
	}

	@Override
	public void controlChanged( final Object source ) {
		if (source == spinnerRadius) {
			config.radius = ((Number)spinnerRadius.getValue()).intValue();
		} else if (source == spinnerNumPoints) {
			config.numPoints = ((Number)spinnerNumPoints.getValue()).intValue();
		} else if (source == spinnerBlurSigma) {
			config.blurSigma = ((Number)spinnerBlurSigma.getValue()).doubleValue();
		} else if (source == spinnerBlurRadius) {
			config.blurRadius = ((Number)spinnerBlurRadius.getValue()).intValue();
		} else if (source == checkFixed) {
			config.fixed = checkFixed.isSelected();
		}
		listener.handleChangeBrief();
	}

	@FunctionalInterface
	public interface Listener {
		void handleChangeBrief();
	}
}
