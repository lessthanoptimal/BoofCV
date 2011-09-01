/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.geo.trackers;

import gecv.abst.feature.tracker.PointSequentialTracker;
import gecv.abst.feature.tracker.PstWrapperKltPyramid;
import gecv.alg.tracker.pklt.PkltManager;
import gecv.alg.tracker.pklt.PkltManagerConfig;
import gecv.alg.tracker.pklt.PyramidKltFeature;
import gecv.struct.image.ImageFloat32;
import org.junit.Before;


/**
 * @author Peter Abeles
 */
public class TestPstWrapperKltPyramid extends StandardPointSequentialTrackerTests{

	PkltManagerConfig<ImageFloat32,ImageFloat32> config;
	PkltManager<ImageFloat32,ImageFloat32> manager;
	PstWrapperKltPyramid<ImageFloat32,ImageFloat32> pointTracker;

	@Before
	public void init() {
		config = PkltManagerConfig.createDefault(ImageFloat32.class,ImageFloat32.class,width,height);
		config.minFeatures = 0;
		manager = new PkltManager<ImageFloat32,ImageFloat32>(config);
		pointTracker = new PstWrapperKltPyramid<ImageFloat32,ImageFloat32>(manager);
	}

	@Override
	public void trackUpdateDrop(PointSequentialTracker tracker) {
		PkltManager<?,?> m = ((PstWrapperKltPyramid)tracker).getTrackManager();

		// this will force it to drop the tracks
		for( PyramidKltFeature f : m.getTracks() ) {
			f.desc[0].Gxx = 0;
			f.desc[0].Gyy = 0;
			f.desc[0].Gxy = 0;
		}
		((PstWrapperKltPyramid)tracker).process(image);
	}

	@Override
	public void trackUpdateChangePosition(PointSequentialTracker tracker) {
		((PstWrapperKltPyramid)tracker).process(image);
	}

	@Override
	public PointSequentialTracker createTracker() {
		pointTracker.process(image);
		return pointTracker;
	}
}
