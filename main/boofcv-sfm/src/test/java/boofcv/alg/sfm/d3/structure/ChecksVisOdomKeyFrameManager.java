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

package boofcv.alg.sfm.d3.structure;

import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.factory.geo.FactoryMultiView;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.jupiter.api.Test;

import static boofcv.alg.sfm.d3.structure.TestMaxGeoKeyFrameManager.connectFrames;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class ChecksVisOdomKeyFrameManager {
	int width = 100;
	int height = 200;
	BundleAdjustment<SceneStructureMetric> sba = FactoryMultiView.bundleSparseMetric(null);

	public abstract VisOdomKeyFrameManager createFrameManager();

	@Test
	void discardMultipleFramesWithNothing() {
		var scene = new VisOdomBundleAdjustment<>(sba, VisOdomBundleAdjustment.BTrack::new);
		var tracker = new TestMaxGeoKeyFrameManager.DummyTracker();
		tracker.activeTracks = 50;
		tracker.maxSpawn = 60;
		VisOdomKeyFrameManager alg = createFrameManager();
		alg.initialize(width,height);

		for (int i = 0; i < 5; i++) {
			scene.addFrame(i);
			GrowQueue_I32 discard = alg.selectFramesToDiscard(tracker,5,scene);
			assertEquals(0,discard.size);
			alg.handleSpawnedTracks(tracker);
		}
		// connect the two most recent frames
		scene.addFrame(5);
		connectFrames(4,5,10,scene);

		// tell it to only keep 3
		GrowQueue_I32 discard = alg.selectFramesToDiscard(tracker,3,scene);
		// there should only be 3 dropped in this order
		assertEquals(3,discard.size);
		for (int i = 0; i < 3; i++) {
			assertEquals(i,discard.get(i));
		}
	}
}
