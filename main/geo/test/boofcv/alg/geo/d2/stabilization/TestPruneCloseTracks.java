/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.d2.stabilization;

import boofcv.alg.geo.AssociatedPair;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPruneCloseTracks {
	@Test
	public void negative() {
		PruneCloseTracks alg = new PruneCloseTracks(2,10,20);
		
		List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
		// space them out far enough so that non of them should be dropped
		pairs.add(new AssociatedPair(0,0,0,3,4));
		pairs.add(new AssociatedPair(0,0,0,0,0));
		pairs.add(new AssociatedPair(0,0,0,5,6));

		PruneCloseDummy tracker = new PruneCloseDummy(pairs);
		
		alg.process(tracker);
		
		assertEquals(0,tracker.numDropped);
	}

	@Test
	public void positive() {
		PruneCloseTracks alg = new PruneCloseTracks(2,10,20);

		List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
		pairs.add(new AssociatedPair(0,0,0,3,4));
		pairs.add(new AssociatedPair(0,0,0,0,0));
		pairs.add(new AssociatedPair(0,0,0,2,4));

		PruneCloseDummy tracker = new PruneCloseDummy(pairs);

		alg.process(tracker);

		assertEquals(1,tracker.numDropped);
	}

	private static class PruneCloseDummy extends TestImageMotionPointKey.DummyTracker {

		List<AssociatedPair> pairs;

		private PruneCloseDummy(List<AssociatedPair> pairs) {
			this.pairs = pairs;
		}

		@Override
		public void dropTrack(AssociatedPair track) {
			super.dropTrack(track);
			assertTrue(pairs.remove(track));
		}
		
		@Override
		public List<AssociatedPair> getActiveTracks() {
			return pairs;
		}
	}
}
