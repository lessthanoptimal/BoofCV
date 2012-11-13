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

package boofcv.abst.feature.tracker;

import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import static org.junit.Assert.fail;


/**
 * @author Peter Abeles
 */
public class TestPointTrackerKltPyramid extends StandardImagePointTracker<ImageFloat32> {

	PkltConfig<ImageFloat32,ImageFloat32> config;

	@Override
	public ImagePointTracker<ImageFloat32> createTracker() {
		config = PkltConfig.createDefault(ImageFloat32.class, ImageFloat32.class);
		return FactoryPointSequentialTracker.klt(config,1,1);
	}

	@Test
	public void checkRecycleProcess() {
		fail("implement");
	}

	@Test
	public void checkRecycleSpawn() {
		fail("implement");
	}

	@Test
	public void checkRecycleDropAll() {
		fail("implement");
	}

	@Test
	public void checkRecycleDropTrack() {
		fail("implement");
	}
}
