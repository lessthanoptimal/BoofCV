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
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

/**
 * Control panel for creating Detect-Describe-Associate style trackers
 *
 * @author Peter Abeles
 */
public class ControlPanelHybridTracker extends StandardAlgConfigPanel {
	Listener listener;

	public ControlPanelHybridTracker(Listener listener) {
		this.listener = listener;
	}

	public <T extends ImageBase<T>>
	PointTracker<T> createTracker(ImageType<T> imageType ) {
		Class inputType = imageType.getImageClass();

		ConfigPKlt kltConfig = new ConfigPKlt();
		kltConfig.toleranceFB = 3;
		kltConfig.pruneClose = true;
		kltConfig.templateRadius = 3;
		kltConfig.pyramidLevels = ConfigDiscreteLevels.levels(4);

		PointTracker<T> tracker = FactoryPointTracker.
				combined_ST_SURF_KLT(new ConfigGeneralDetector(600, 3, 0),
						kltConfig, 50, null, null, inputType, null);
		return tracker;
	}

	public interface Listener {
		void changedPointTrackerDda();
	}
}
