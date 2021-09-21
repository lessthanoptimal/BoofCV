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
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;

/**
 * Control panel for {@link ConfigSiftScaleSpace}
 *
 * @author Peter Abeles
 */
public class ControlPanelSiftScaleSpace extends StandardAlgConfigPanel {
	public final ConfigSiftScaleSpace config;

	private final JSpinner spinnerSigma0;
	private final JSpinner spinnerNumScales;
	private final JSpinner spinnerNumOctaves;

	private final Listener listener;

	public ControlPanelSiftScaleSpace(ConfigSiftScaleSpace config, Listener listener) {
		this.config = config;
		this.listener = listener;

		spinnerSigma0 = spinner(config.sigma0,0.0, 1000,0.5,"0.0E0",8);
		spinnerNumScales = spinner(config.numScales,1, 100,1);
		spinnerNumOctaves = spinner(config.lastOctave-config.firstOctave,1,100,1);

		addLabeled(spinnerSigma0,"Sigma0","Sigma of first level in image pyramid");
		addLabeled(spinnerNumScales,"Scales","Number of scales in each octave. Number of images will be scales + 2");
		addLabeled(spinnerNumOctaves,"Octaves","Number of octaves");
	}

	@Override
	public void controlChanged(final Object source) {
		if( source == spinnerSigma0 ) {
			config.sigma0 = ((Number)spinnerSigma0.getValue()).floatValue();
		} else if( source == spinnerNumScales ) {
			config.numScales = ((Number)spinnerNumScales.getValue()).intValue();
		} else if( source == spinnerNumOctaves ) {
			int offset = ((Number)spinnerNumOctaves.getValue()).intValue();
			config.lastOctave = config.firstOctave+offset;
		}
		listener.handleScaleSpaceChange();
	}

	@FunctionalInterface
	public interface Listener {
		void handleScaleSpaceChange();
	}
}
