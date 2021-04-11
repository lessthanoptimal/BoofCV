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

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.factory.feature.detect.selector.SelectLimitTypes;
import boofcv.gui.StandardAlgConfigPanel;
import lombok.Getter;

import javax.swing.*;

/**
 * Control panel for {@link ConfigGeneralDetector}.
 *
 * @author Peter Abeles
 */
public class ControlPanelGeneralCorner extends StandardAlgConfigPanel {
	public final ConfigGeneralDetector config;

	private @Getter final ControlPanelExtractor controlExtract;
	private @Getter final JSpinner spinnerMaxFeatures;
	private @Getter final JComboBox<String> comboSelector;

	private final Listener listener;

	public ControlPanelGeneralCorner(ConfigGeneralDetector config , Listener listener ) {
		this.config = config;
		this.listener = listener;

		controlExtract = new ControlPanelExtractor(config,listener::handleGeneralCorner);
		comboSelector = combo(config.selector.type.ordinal(), (Object[]) SelectLimitTypes.values());
		spinnerMaxFeatures = spinner(config.maxFeatures,-1,9999,50);

		var panelSelector = new StandardAlgConfigPanel();
		panelSelector.addLabeled(comboSelector,  "Type",
				"Method used to select points when more have been detected than the maximum allowed");
		panelSelector.addLabeled(spinnerMaxFeatures,"Max Feats","Maximum features it will detect. If < 1 then for no limit");

		panelSelector.setBorder(BorderFactory.createTitledBorder("Selector"));
		controlExtract.setBorder(BorderFactory.createTitledBorder("Non-Max"));

		add(controlExtract);
		add(panelSelector);
	}

	@Override
	public void controlChanged(final Object source) {
		if( source == spinnerMaxFeatures ) {
			config.maxFeatures = ((Number) spinnerMaxFeatures.getValue()).intValue();
		} else if( source == comboSelector) {
			config.selector.type = SelectLimitTypes.values()[comboSelector.getSelectedIndex()];
		}
		listener.handleGeneralCorner();
	}

	@FunctionalInterface
	public interface Listener {
		void handleGeneralCorner();
	}
}
