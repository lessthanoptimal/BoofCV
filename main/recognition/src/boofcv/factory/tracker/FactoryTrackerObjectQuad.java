/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.tracker.Sfot_to_TrackObjectQuad;
import boofcv.abst.tracker.Tld_to_TrackerObjectQuad;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.alg.tracker.sfot.SparseFlowObjectTracker;
import boofcv.alg.tracker.tld.TldConfig;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.struct.image.ImageSingleBand;

/**
 * Factory for implementations of {@link TrackerObjectQuad}, a high level interface for tracking user specified
 * objects inside video sequences.  As usual, the high level interface makes it easier to use these algorithms
 * at the expensive of algorithm specific features.
 *
 * @author Peter Abeles
 */
public class FactoryTrackerObjectQuad {

	/**
	 * Create an instance of {@link TldTracker  Tracking-Learning-Detection (TLD)} tracker for the
	 * {@link TrackerObjectQuad} interface.
	 * @param config Configuration for the tracker
	 * @param <T> Image input type
	 * @param <D> Image derivative type
	 * @return TrackerObjectQuad
	 */
	public static <T extends ImageSingleBand,D extends ImageSingleBand>
	TrackerObjectQuad<T> createTLD( TldConfig<T,D> config ) {
		TldTracker<T,D> tracker = new TldTracker<T,D>(config);

		return new Tld_to_TrackerObjectQuad<T,D>(tracker);
	}

	/**
	 * Create an instance of {@link SparseFlowObjectTracker  Sparse Flow Object Tracker} for the
	 * {@link TrackerObjectQuad} interface.
	 * @param config Configuration for the tracker
	 * @param <T> Image input type
	 * @param <D> Image derivative type
	 * @return TrackerObjectQuad
	 */
	public static <T extends ImageSingleBand,D extends ImageSingleBand>
	TrackerObjectQuad<T> createSparseFlow( SfotConfig<T,D> config ) {
		SparseFlowObjectTracker<T,D> tracker = new SparseFlowObjectTracker<T,D>(config);

		return new Sfot_to_TrackObjectQuad<T,D>(tracker);
	}
}
