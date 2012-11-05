/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.trackers;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestDetectAssociateTracker extends StandardImagePointTracker<ImageFloat32> {


	/**
	 * Make sure drop track is correctly recycling the data
	 */
	@Test
	public void dropTrack_Recycle() {
		fail("Implement");
	}

	/**
	 * Make sure drop all tracks is correctly recycling the data
	 */
	@Test
	public void dropAllTracks_Recycle() {
		fail("Implement");
	}

	/**
	 * Make sure drop tracks dropped during process are correctly recycled
	 */
	@Test
	public void process_drop_Recycle() {
		fail("Implement");
	}

	@Override
	public void trackUpdateDrop(ImagePointTracker<ImageFloat32> tracker) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void trackUpdateChangePosition(ImagePointTracker<ImageFloat32> tracker) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public ImagePointTracker<ImageFloat32> createTracker() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
