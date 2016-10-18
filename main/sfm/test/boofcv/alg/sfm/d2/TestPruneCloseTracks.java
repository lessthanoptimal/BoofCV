/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.d2;

import boofcv.abst.feature.tracker.PointTrack;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPruneCloseTracks {
	@Test
	public void negative() {
		PruneCloseTracks alg = new PruneCloseTracks(2,10,20);
		
		List<PointTrack> tracks = new ArrayList<>();
		// space them out far enough so that non of them should be dropped
		tracks.add(new PointTrack(0,3,4));
		tracks.add(new PointTrack(0, 0, 0));
		tracks.add(new PointTrack(0, 5, 6));

		List<PointTrack> dropped = new ArrayList<>();
		alg.process(tracks,dropped);
		
		assertEquals(0, dropped.size());
	}

	@Test
	public void positive() {
		PruneCloseTracks alg = new PruneCloseTracks(2,10,20);

		List<PointTrack> tracks = new ArrayList<>();
		tracks.add(new PointTrack(0,3,4));
		tracks.add(new PointTrack(0, 0, 0));
		tracks.add(new PointTrack(0, 2, 4));

		List<PointTrack> dropped = new ArrayList<>();
		alg.process(tracks,dropped);

		assertEquals(1, dropped.size());
	}
}
