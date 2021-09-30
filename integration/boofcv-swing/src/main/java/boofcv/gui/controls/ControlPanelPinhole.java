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

package boofcv.gui.controls;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;

/**
 * Panel for configuring Brown camera model parameters
 *
 * @author Peter Abeles
 */
public class ControlPanelPinhole extends StandardAlgConfigPanel {
	public final JSpinnerNumber numRadial = spinnerWrap(2, 0, 10, 1).tt("Number of radial distortion terms");
	public final JCheckBoxValue tangential = checkboxWrap("Tangential", false).tt("Include tangential distortion");
	public final JCheckBoxValue skew = checkboxWrap("Zero Skew", true).tt("Include skew in camera model. Rarely needed.");

	// called after a parameter changes value
	public Runnable parametersUpdated = ()->{};

	public ControlPanelPinhole() {
		setBorder(BorderFactory.createEmptyBorder());
		addLabeled(numRadial.spinner, "Radial");
		addAlignLeft(tangential.check);
		addAlignLeft(skew.check);
	}

	public ControlPanelPinhole( Runnable parametersUpdated ) {
		this();
		this.parametersUpdated = parametersUpdated;
	}

	@Override public void controlChanged( Object source ) {
		parametersUpdated.run();
	}
}
