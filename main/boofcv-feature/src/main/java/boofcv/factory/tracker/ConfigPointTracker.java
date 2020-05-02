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

package boofcv.factory.tracker;

import boofcv.abst.tracker.ConfigTrackerDda;
import boofcv.abst.tracker.ConfigTrackerHybrid;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.factory.feature.associate.ConfigAssociateGreedy;
import boofcv.factory.feature.associate.ConfigAssociateNearestNeighbor;
import boofcv.factory.feature.detdesc.ConfigDetectDescribe;
import boofcv.struct.Configuration;

/**
 * Configuration for creating implementations of {@link boofcv.abst.tracker.PointTracker}
 *
 * @author Peter Abeles
 */
public class ConfigPointTracker implements Configuration {

	/** Specifies the tracking strategy used */
	public TrackerType typeTracker = TrackerType.KLT;
	/** The association algorithm used. Not always used. */
	public AssociationType typeAssociate = AssociationType.GREEDY;

	/** Configuration for KLT. Detector is specified using detectorPoint. */
	public ConfigPKlt klt = new ConfigPKlt();
	/** Special configuration for DDA tracker */
	public ConfigTrackerDda dda = new ConfigTrackerDda();
	/** Special configuration for hybrid tracker */
	public ConfigTrackerHybrid hybrid = new ConfigTrackerHybrid();

	/** Configuration for detectors and descriptors */
	public ConfigDetectDescribe detDesc = new ConfigDetectDescribe();

	public ConfigAssociateGreedy associateGreedy = new ConfigAssociateGreedy();
	public ConfigAssociateNearestNeighbor associateNN = new ConfigAssociateNearestNeighbor();
	

	@Override
	public void checkValidity() {
		klt.checkValidity();
		associateGreedy.checkValidity();
		associateNN.checkValidity();
	}

	public enum TrackerType {
		KLT,DDA,HYBRID
	}

	public enum AssociationType {
		GREEDY, KD_TREE, RANDOM_FOREST,
	}

}
