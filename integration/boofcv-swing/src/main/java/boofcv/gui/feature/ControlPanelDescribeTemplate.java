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

import boofcv.abst.feature.describe.ConfigTemplateDescribe;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;

/**
 * Control panel for {@link ConfigTemplateDescribe}
 *
 * @author Peter Abeles
 */
public class ControlPanelDescribeTemplate extends StandardAlgConfigPanel {
	public final ConfigTemplateDescribe config;

	private final JComboBox<String> comboType;
	private final JSpinner spinnerWidth;
	private final JSpinner spinnerHeight;

	private final Listener listener;

	public ControlPanelDescribeTemplate( ConfigTemplateDescribe config_, Listener listener ) {
		config = config_ == null ? new ConfigTemplateDescribe() : config_;
		this.listener = listener;

		comboType = combo(config.type.ordinal(), (Object[])ConfigTemplateDescribe.Type.values());
		spinnerWidth = spinner(config.width, 1, 999, 1);
		spinnerHeight = spinner(config.height, 1, 999, 1);

		addLabeled(comboType, "Type", "Type of template descriptor. How the pixels are encoded");
		addLabeled(spinnerWidth, "Width", "Region Width");
		addLabeled(spinnerHeight, "Height", "Region Height");
	}

	@Override
	public void controlChanged( final Object source ) {
		if (source == comboType) {
			config.type = ConfigTemplateDescribe.Type.values()[comboType.getSelectedIndex()];
		} else if (source == spinnerWidth) {
			config.width = ((Number)spinnerWidth.getValue()).intValue();
		} else if (source == spinnerHeight) {
			config.height = ((Number)spinnerHeight.getValue()).intValue();
		} else {
			throw new IllegalArgumentException("Unknown control");
		}
		listener.handleChangeTemplate();
	}

	@FunctionalInterface
	public interface Listener {
		void handleChangeTemplate();
	}
}
