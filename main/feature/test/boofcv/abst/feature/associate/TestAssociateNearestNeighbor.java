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

package boofcv.abst.feature.associate;

import boofcv.struct.FastQueue;
import boofcv.struct.GrowingArrayInt;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestAssociateNearestNeighbor {

	/**
	 * Several tests combined into one
	 */
	@Test
	public void various() {

		Dummy<Integer> nn = new Dummy<Integer>();
		// multiple matches for 1 and 2
		// no match for 3
		nn.assoc = new int[]{2,0,1,-1,4,-1,-1,2,2,1};

		AssociateNearestNeighbor<TupleDesc_F64> alg = new AssociateNearestNeighbor<TupleDesc_F64>(nn,10);

		FastQueue<TupleDesc_F64> src = new FastQueue<TupleDesc_F64>(10,TupleDesc_F64.class,false);
		FastQueue<TupleDesc_F64> dst = new FastQueue<TupleDesc_F64>(10,TupleDesc_F64.class,false);

		for( int i = 0; i < 5; i++ ) {
			src.add( new TupleDesc_F64(10));
		}

		for( int i = 0; i < 10; i++ ) {
			dst.add( new TupleDesc_F64(10));
		}

		alg.setSource(src);
		alg.setDestination(dst);

		alg.associate();

		FastQueue<AssociatedIndex> matches = alg.getMatches();
		assertTrue(nn.pointDimension == 10);

		assertEquals(2,matches.size);
		assertEquals(0,matches.get(0).src);
		assertEquals(1,matches.get(0).dst);
		assertEquals(4,matches.get(1).src);
		assertEquals(4,matches.get(1).dst);

		GrowingArrayInt unassoc = alg.getUnassociatedSource();
		assertEquals(3, unassoc.size);
		assertEquals(1,unassoc.get(0));
		assertEquals(2,unassoc.get(1));
		assertEquals(3,unassoc.get(2));
	}

	public static class Dummy<D> implements NearestNeighbor<D> {

		public int pointDimension;

		List<double[]> points;
		List<D> data;

		public int assoc[];
		int numCalls = 0;

		@Override
		public void init(int pointDimension) {
			this.pointDimension = pointDimension;
		}

		@Override
		public void setPoints(List<double[]> points, List<D> data) {
			this.points = points;
			this.data = data;
		}

		@Override
		public boolean findNearest(double[] point, double maxDistance, NnData<D> result) {

			int w = assoc[numCalls++];

			if( w >= 0 ) {
				result.data = data.get(w);
				result.point = points.get(w);
				return true;
			}

			return false;
		}
	}

}
