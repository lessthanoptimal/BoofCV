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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.struct.feature.MatchScoreType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestBaseAssociateLocation2DFilter {

	FastQueue<Point2D_F64> locationSrc = new FastQueue<>(Point2D_F64.class, false);
	FastQueue<Integer> descSrc = new FastQueue<>(Integer.class, false);
	FastQueue<Point2D_F64> locationDst = new FastQueue<>(Point2D_F64.class, false);
	FastQueue<Integer> descDst = new FastQueue<>(Integer.class, false);

	@Before
	public void init() {
		locationSrc.reset();
		descSrc.reset();
		locationDst.reset();
		descDst.reset();
	}

	@Test
	public void checkDistanceFilter() {
		locationSrc.add( new Point2D_F64(10,10));
		descSrc.add(20);
		locationDst.add( new Point2D_F64(10,100));
		descDst.add(20);

		// first test a positive case where the two are (barely) within tolerance
		BaseAssociateLocation2DFilter<Integer> alg = new Helper(false,1000);
		alg.setMaxDistance(90);
		alg.setSource(locationSrc,descSrc);
		alg.setDestination(locationDst, descDst);

		alg.associate();
		assertEquals(1,alg.getMatches().size);

		// now make it outside of the max distance
		alg.setMaxDistance(80);
		alg.associate();
		assertEquals(0,alg.getMatches().size);
	}

	@Test
	public void checkMaxError() {
		locationSrc.add( new Point2D_F64(10,10));
		descSrc.add(20);
		locationDst.add( new Point2D_F64(10,10));
		descDst.add(50);

		// first test a positive case where the two are (barely) within tolerance
		BaseAssociateLocation2DFilter<Integer> alg = new Helper(false,31);
		alg.setSource(locationSrc,descSrc);
		alg.setDestination(locationDst, descDst);

		alg.associate();
		assertEquals(1,alg.getMatches().size);

		// now make it outside of the max error
		alg = new Helper(false,20);
		alg.setSource(locationSrc,descSrc);
		alg.setDestination(locationDst, descDst);
		alg.associate();
		assertEquals(0,alg.getMatches().size);
	}

	@Test
	public void checkBackwards() {
		locationSrc.add( new Point2D_F64(10,10));
		descSrc.add(20);
		locationSrc.add( new Point2D_F64(10,20));
		descSrc.add(60);
		locationDst.add( new Point2D_F64(10,10));
		descDst.add(50);
		locationDst.add( new Point2D_F64(15,10));
		descDst.add(60);

		// with no backwards validation it should accept the match
		BaseAssociateLocation2DFilter<Integer> alg = new Helper(false,1000);
		alg.setSource(locationSrc,descSrc);
		alg.setDestination(locationDst, descDst);

		alg.associate();
		assertEquals(2,alg.getMatches().size);

		// with backwards validation it should be rejected
		alg = new Helper(true,1000);
		alg.setSource(locationSrc,descSrc);
		alg.setDestination(locationDst, descDst);
		alg.associate();
		assertEquals(1,alg.getMatches().size);
	}

	private class Helper extends BaseAssociateLocation2DFilter<Integer> {

		Point2D_F64 src;

		protected Helper(boolean backwardsValidation, double maxError) {
			super(new Score(), backwardsValidation, maxError);
		}

		@Override
		protected void setActiveSource(Point2D_F64 p) {
			this.src = p;
		}

		@Override
		protected double computeDistanceToSource(Point2D_F64 p) {
			return src.distance(p);
		}
	}

	private static class Score implements ScoreAssociation<Integer> {

		@Override
		public double score(Integer a, Integer b) {
			return Math.abs(a-b);
		}

		@Override
		public MatchScoreType getScoreType() {
			return MatchScoreType.NORM_ERROR;
		}
	}
}
