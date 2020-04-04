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

import boofcv.factory.feature.associate.ConfigAssociateGreedy;
import boofcv.gui.StandardAlgConfigPanel;

import javax.annotation.Nullable;
import javax.swing.*;

/**
 * Control Panel for {@link ConfigAssociateGreedy}.
 *
 * @author Peter Abeles
 */
public class ControlPanelAssociateGreedy extends StandardAlgConfigPanel {
	public final ConfigAssociateGreedy config;

	private final JCheckBox checkForwardsBackwards;
	private final JSpinner spinnerRatio;
	private final JSpinner spinnerMaxError;

	private final Listener listener;

	public ControlPanelAssociateGreedy(@Nullable ConfigAssociateGreedy config_, Listener listener) {
		if( config_ == null ) {
			this.config = new ConfigAssociateGreedy();
		} else {
			this.config = config_;
		}
		this.listener = listener;

		checkForwardsBackwards = checkbox("Validate FB",config.forwardsBackwards,"Forwards-Backwards validation");
		spinnerRatio = spinner(config.scoreRatioThreshold,0.0,1.0,0.05,1,4);
		spinnerMaxError = spinner(config.maxErrorThreshold,-1.0,9999,20.0);

		addAlignLeft(checkForwardsBackwards);
		addLabeled(spinnerRatio,"Score Ratio","Score ratio test. 0.0 = strict 1.0 = turned off.");
		addLabeled(spinnerMaxError,"Max Error","Max allowed error. Disable with <= 0");
	}

	@Override
	public void controlChanged(final Object source) {
		if (source == spinnerRatio) {
			config.scoreRatioThreshold = ((Number) spinnerRatio.getValue()).doubleValue();
		} else if (source == spinnerMaxError) {
			config.maxErrorThreshold = ((Number) spinnerMaxError.getValue()).doubleValue();
		} else if (source == checkForwardsBackwards) {
			config.forwardsBackwards = checkForwardsBackwards.isSelected();
		}
		listener.handleChangeAssociateGreedy();
	}

	public interface Listener {
		void handleChangeAssociateGreedy();
	}
}
