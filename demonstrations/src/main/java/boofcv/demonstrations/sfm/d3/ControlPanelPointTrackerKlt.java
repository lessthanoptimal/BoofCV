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

package boofcv.demonstrations.sfm.d3;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;

/**
 * Configuration for Point KLT Tracker
 *
 * @author Peter Abeles
 */
public class ControlPanelPointTrackerKlt extends StandardAlgConfigPanel {
	public final ConfigGeneralDetector configDetector;
	public final ConfigPKlt configKlt;

	private final Listener listener;

	private final JSpinner spinnerLevels;
	private final JCheckBox checkPruneClose;
	private final JSpinner spinnerIterations;
	private final JSpinner spinnerMaxError;
	private final JSpinner spinnerDescRadius;
	private final JSpinner spinnerDetectThresh;
	private final JSpinner spinnerDetectRadius;
	private final JSpinner spinnerForwardsBackwards;

	public ControlPanelPointTrackerKlt(Listener listener) {
		this(listener, new ConfigGeneralDetector(), new ConfigPKlt());
	}
	
	public ControlPanelPointTrackerKlt(Listener listener, ConfigGeneralDetector configDetector, ConfigPKlt configKlt ) {
		this.listener = listener;
		this.configDetector = configDetector;
		this.configKlt = configKlt;

		spinnerLevels = spinner(configKlt.pyramidLevels.numLevelsRequested,1,20,1);
		checkPruneClose = checkbox("Prune Close", configKlt.pruneClose,"If true then tracks which are clustered close to each other are pruned");
		spinnerIterations = spinner(configKlt.config.maxIterations,1,500,1);
		spinnerMaxError = spinner(configKlt.config.maxPerPixelError,0.0,255.0,5.0);
		spinnerDescRadius = spinner(configKlt.templateRadius,1,100,1);
		spinnerDetectThresh = spinner(configDetector.threshold,0.0,100.0,1.0);
		spinnerDetectRadius = spinner(configDetector.radius,1,500,1);
		spinnerForwardsBackwards = spinner(configKlt.toleranceFB,-1,100.0,1.0);
		
		setBorder(BorderFactory.createEmptyBorder());

		addLabeled(spinnerLevels,"Pyr. Levels","Number of layers in image pyramid");
		addAlignLeft(checkPruneClose);
		addLabeled(spinnerDescRadius,"Template Radius","Radius of square template that is tracked");
		addLabeled(spinnerIterations,"Max Iterations","KLT iterations when tracking");
		addLabeled(spinnerMaxError,"Max Error","Drop tracks with an error larger than this value");
		addLabeled(spinnerDetectThresh,"Detect Threshold","Shi-Tomasi corner detection threshold");
		addLabeled(spinnerDetectRadius,"Detect Radius","Non-maximum detection radius");
		addLabeled(spinnerForwardsBackwards,"F-to-B Tol.","Forwards-Backwards tolerance. 0 = disable (Pixels)");
	}

	@Override
	public void controlChanged(final Object source) {
		if( source == spinnerLevels) {
			configKlt.pyramidLevels.numLevelsRequested = ((Number) spinnerLevels.getValue()).intValue();
		} else if( source == spinnerDescRadius) {
			configKlt.templateRadius = ((Number) spinnerDescRadius.getValue()).intValue();
		} else if( source == spinnerMaxError) {
			configKlt.config.maxPerPixelError = ((Number) spinnerMaxError.getValue()).floatValue();
		} else if( source == spinnerDetectThresh) {
			configDetector.threshold = ((Number) spinnerDetectThresh.getValue()).floatValue();
		} else if( source == spinnerDetectRadius) {
			configDetector.radius = ((Number) spinnerDetectRadius.getValue()).intValue();
		} else if( source == spinnerForwardsBackwards ) {
			configKlt.toleranceFB = ((Number) spinnerForwardsBackwards.getValue()).doubleValue();
		} else if( source == spinnerIterations ) {
			configKlt.config.maxIterations = ((Number) spinnerIterations.getValue()).intValue();
		} else if( source == checkPruneClose ) {
			configKlt.pruneClose = checkPruneClose.isSelected();
		}
		listener.changedPointTrackerKlt();
	}

	public interface Listener {
		void changedPointTrackerKlt();
	}
}
