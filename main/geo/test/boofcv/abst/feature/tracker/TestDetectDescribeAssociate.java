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

package boofcv.abst.feature.tracker;

import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDetectDescribeAssociate {

	/**
	 * Make sure drop track is correctly recycling the data
	 */
	@Test
	public void dropTrack_Recycle() {
		Helper dat = new Helper();

		dat.tracksAll.add(dat.getUnused());
		dat.tracksAll.add(dat.getUnused());
		dat.tracksAll.add(dat.getUnused());

		PointTrack a = dat.tracksAll.get(1);

		assertEquals(0,dat.unused.size());
		dat.dropTrack(a);
		assertEquals(1,dat.unused.size());

		assertEquals(2,dat.tracksAll.size());
	}

	/**
	 * Make sure drop all tracks is correctly recycling the data
	 */
	@Test
	public void dropAllTracks_Recycle() {
		Helper dat = new Helper();

		dat.tracksAll.add(dat.getUnused());
		dat.tracksAll.add(dat.getUnused());
		dat.tracksAll.add(dat.getUnused());

		dat.dropAllTracks();
		assertEquals(3,dat.unused.size());
		assertEquals(0, dat.tracksAll.size());
	}

	@Test
	public void addNewTrack() {
		Helper dat = new Helper();

		TupleDesc_F64 desc = dat.manager.createDescription();
		desc.value[0] = 5;

		PointTrack found0 = dat.addNewTrack(5,10,desc);
		PointTrack found1 = dat.addNewTrack(8,23,desc);

		// unique featureId should be assigned
		assertTrue(found0.featureId != found1.featureId);
		// make sure a copy is made
		assertTrue(found0.getDescription() != desc);
		assertTrue(((TupleDesc_F64)found0.getDescription()).value[0] == 5);
		// should check to see if the feature is valid
		assertTrue(dat.validCalled);
	}

	private static class Helper extends DetectDescribeAssociate<GrayF32,TupleDesc_F64> {
		boolean validCalled = false;

		private Helper() {
			manager = new HelpManager();
		}

		protected boolean checkValidSpawn( PointTrack p ) {
			validCalled = true;
			return true;
		}
	}

	private static class HelpManager implements DdaFeatureManager<GrayF32,TupleDesc_F64> {

		@Override
		public void detectFeatures(GrayF32 input,
								   FastQueue<Point2D_F64> locDst,
								   FastQueue<TupleDesc_F64> featDst) {

		}

		@Override
		public TupleDesc_F64 createDescription() {
			return new TupleDesc_F64(10);
		}

		@Override
		public Class<TupleDesc_F64> getDescriptionType() {
			return TupleDesc_F64.class;
		}
	}
}
