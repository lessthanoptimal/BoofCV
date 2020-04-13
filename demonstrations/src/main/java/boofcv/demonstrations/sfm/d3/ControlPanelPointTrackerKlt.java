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

import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.feature.ControlPanelPointDetector;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import javax.annotation.Nullable;
import javax.swing.*;

/**
 * Configuration for Point KLT Tracker
 *
 * @author Peter Abeles
 */
public class ControlPanelPointTrackerKlt extends StandardAlgConfigPanel {
	public final ConfigPKlt configKlt;
	public final ConfigPointDetector configDetect;

	private final Listener listener;

	private final JSpinner spinnerLevels;
	private final JCheckBox checkPruneClose;
	private final JSpinner spinnerIterations;
	private final JSpinner spinnerMaxError;
	private final JSpinner spinnerDescRadius;
	private final JSpinner spinnerForwardsBackwards;
	private final ControlPanelPointDetector controlDetector;

	/**
	 * Constructor that uses default settings
	 */
	public ControlPanelPointTrackerKlt(Listener listener) {
		this(listener, new ConfigPointDetector(), new ConfigPKlt());
	}

	/**
	 * Constructor with initial configurations
	 *
	 * @param listener Listener for changes
	 * @param configDetect configuration for the detector
	 * @param configKlt Initial configuration for tracker
	 */
	public ControlPanelPointTrackerKlt(Listener listener, @Nullable ConfigPointDetector configDetect, ConfigPKlt configKlt ) {
		this.listener = listener;
		this.configDetect = configDetect;
		this.configKlt = configKlt;

		spinnerLevels = spinner(configKlt.pyramidLevels.numLevelsRequested,1,20,1);
		checkPruneClose = checkbox("Prune Close", configKlt.pruneClose,"If true then tracks which are clustered close to each other are pruned");
		spinnerIterations = spinner(configKlt.config.maxIterations,1,500,1);
		spinnerMaxError = spinner(configKlt.config.maxPerPixelError,0.0,255.0,5.0);
		spinnerDescRadius = spinner(configKlt.templateRadius,1,100,1);
		spinnerForwardsBackwards = spinner(configKlt.toleranceFB,-1,100.0,1.0);
		if( configDetect != null ) {
			controlDetector = new ControlPanelPointDetector(configDetect, listener::changedPointTrackerKlt);
			controlDetector.setBorder(BorderFactory.createTitledBorder("Detect"));
		} else {
			controlDetector = null;
		}

		addLabeled(spinnerLevels,"Pyr. Levels","Number of layers in image pyramid");
		addAlignLeft(checkPruneClose);
		addLabeled(spinnerDescRadius,"Template Radius","Radius of square template that is tracked");
		addLabeled(spinnerIterations,"Max Iterations","KLT iterations when tracking");
		addLabeled(spinnerMaxError,"Max Error","Drop tracks with an error larger than this value");
		addLabeled(spinnerForwardsBackwards,"F-to-B Tol.","Forwards-Backwards tolerance. 0 = disable (Pixels)");
		if( isConfigureDetector() ) {
			add(controlDetector);
		}
	}

	public boolean isConfigureDetector() {
		return controlDetector != null;
	}

	public <T extends ImageBase<T>>
	PointTracker<T> createTracker(ImageType<T> imageType ) {
		return FactoryPointTracker.klt(configKlt, configDetect, imageType.getImageClass(), null);
	}

	@Override
	public void controlChanged(final Object source) {
		if( source == spinnerLevels) {
			configKlt.pyramidLevels.numLevelsRequested = ((Number) spinnerLevels.getValue()).intValue();
		} else if( source == spinnerDescRadius) {
			configKlt.templateRadius = ((Number) spinnerDescRadius.getValue()).intValue();
		} else if( source == spinnerMaxError) {
			configKlt.config.maxPerPixelError = ((Number) spinnerMaxError.getValue()).floatValue();
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
