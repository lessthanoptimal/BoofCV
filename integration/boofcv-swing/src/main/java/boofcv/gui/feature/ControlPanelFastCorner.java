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

import boofcv.abst.feature.detect.interest.ConfigFastCorner;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.controls.JConfigLength;

import javax.swing.*;

/**
 * Control panel for {@link ConfigFastCorner}
 *
 * @author Peter Abeles
 */
public class ControlPanelFastCorner extends StandardAlgConfigPanel {
	public final ConfigFastCorner config;

	private final JSpinner sPixelTol;
	private final JSpinner sContinuous;
	private final JConfigLength lengthMaxFeatures;

	private final Listener listener;

	public ControlPanelFastCorner( ConfigFastCorner config , Listener listener )
	{
		this.config = config;
		this.listener = listener;

		sPixelTol = spinner(config.pixelTol,0,255,1);
		sContinuous = spinner(config.minContinuous,9,12,1);
		lengthMaxFeatures = configLength(config.maxFeatures,0,4000,listener::handleFastCorner);

		addLabeled(sPixelTol,"Tolerance","Tolerance for deciding if two pixels are significantly different");
		addLabeled(sContinuous,"Continuous","How many continuous pixels are required for it to be a corner");
		addLabeled(lengthMaxFeatures,"Stop","Stops detecting features if this is exceeded. Relative to number of pixels.");
	}

	@Override
	public void controlChanged(final Object source) {
		if( source == sPixelTol ) {
			config.pixelTol = ((Number) sPixelTol.getValue()).intValue();
		} else if( source == sContinuous) {
			config.minContinuous = ((Number) sContinuous.getValue()).intValue();
		}
		listener.handleFastCorner();
	}

	public interface Listener {
		void handleFastCorner();
	}
}
