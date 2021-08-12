/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.StandardAssociateDescriptionChecks;
import boofcv.alg.descriptor.KdTreeTuple_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastArray;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestAssociateNearestNeighbor_ST extends StandardAssociateDescriptionChecks<TupleDesc_F64> {

	public TestAssociateNearestNeighbor_ST() {
		super(TupleDesc_F64.class);
	}

	@Override
	public AssociateDescription<TupleDesc_F64> createAssociate() {
		// exhaustive algorithm will produce perfect results
		NearestNeighbor<TupleDesc_F64> exhaustive = FactoryNearestNeighbor.exhaustive(new KdTreeTuple_F64(1));
		return new AssociateNearestNeighbor_ST<>(exhaustive, TupleDesc_F64.class);
	}

	@Override
	protected TupleDesc_F64 c( double value ) {
		TupleDesc_F64 s = new TupleDesc_F64(1);
		s.data[0] = value;
		return s;
	}

	/**
	 * See if associations are skipped if the ratio is too low
	 */
	@Test void scoreRatio() {
		Dummy<TupleDesc_F64> nn = new Dummy<>();
		// src = assoc[i] where src is the index of the source feature and i is the index of the dst feature
		nn.assoc = new int[]{2, 0, 1, -1, 4, -1, -1, 2, 2, 1};
		nn.distanceScale = 2.0;

		AssociateNearestNeighbor_ST<TupleDesc_F64> alg = new AssociateNearestNeighbor_ST<>(nn, TupleDesc_F64.class);

		FastArray<TupleDesc_F64> src = new FastArray<>(TupleDesc_F64.class);
		FastArray<TupleDesc_F64> dst = new FastArray<>(TupleDesc_F64.class);

		for (int i = 0; i < 5; i++) {
			src.add(new TupleDesc_F64(10));
		}

		for (int i = 0; i < 10; i++) {
			dst.add(new TupleDesc_F64(10));
		}

		alg.setSource(src);
		alg.setDestination(dst);

		alg.setRatioUsesSqrt(false);
		alg.setScoreRatioThreshold(0.49); // threshold should reject everything
		alg.associate();
		assertEquals(0, alg.getMatches().size);

		alg.setScoreRatioThreshold(0.51); // everything should be accepted
		alg.associate();
		assertEquals(10, alg.getMatches().size);

		alg.setRatioUsesSqrt(true);
		alg.setScoreRatioThreshold(1.0/Math.sqrt(2.0) - 0.001);
		alg.associate();
		assertEquals(0, alg.getMatches().size);

		alg.setScoreRatioThreshold(1.0/Math.sqrt(2.0) + 0.001);
		alg.associate();
		assertEquals(10, alg.getMatches().size);
	}

	/**
	 * Several tests combined into one
	 */
	@Test void various() {

		Dummy<TupleDesc_F64> nn = new Dummy<>();
		// src = assoc[i] where src is the index of the source feature and i is the index of the dst feature
		nn.assoc = new int[]{2, 0, 1, -1, 4, -1, -1, 2, 2, 1};

		AssociateNearestNeighbor_ST<TupleDesc_F64> alg = new AssociateNearestNeighbor_ST<>(nn, TupleDesc_F64.class);

		FastArray<TupleDesc_F64> src = new FastArray<>(TupleDesc_F64.class);
		FastArray<TupleDesc_F64> dst = new FastArray<>(TupleDesc_F64.class);

		for (int i = 0; i < 5; i++) {
			src.add(new TupleDesc_F64(10));
		}

		for (int i = 0; i < 10; i++) {
			dst.add(new TupleDesc_F64(10));
		}

		alg.setSource(src);
		alg.setDestination(dst);

		alg.associate();

		DogArray<AssociatedIndex> matches = alg.getMatches();

		assertEquals(7, matches.size);
		for (int i = 0, count = 0; i < nn.assoc.length; i++) {
			if (nn.assoc[i] != -1) {
				int source = nn.assoc[i];
				assertEquals(source, matches.get(count).src);
				assertEquals(i, matches.get(count).dst);
				count++;
			}
		}

		DogArray_I32 unassoc = alg.getUnassociatedSource();
		assertEquals(1, unassoc.size);
		assertEquals(3, unassoc.get(0));
		unassoc = alg.getUnassociatedDestination();
		assertEquals(3, unassoc.size);
		assertEquals(3, unassoc.get(0));
		assertEquals(5, unassoc.get(1));
		assertEquals(6, unassoc.get(2));
	}

	public static class Dummy<D> implements NearestNeighbor<D> {
		List<D> points;

		public int[] assoc;
		int numCalls = 0;

		double distanceScale;

		@Override
		public void setPoints( List<D> points, boolean assad ) {
			this.points = points;
		}

		@Override
		public Search<D> createSearch() {
			return new InternalSearch();
		}

		private class InternalSearch implements NearestNeighbor.Search<D> {

			@Override
			public boolean findNearest( D point, double maxDistance, NnData<D> result ) {

				int w = assoc[numCalls++];

				if (w >= 0) {
					result.index = w;
					result.point = points.get(w);
					return true;
				}

				return false;
			}

			@Override
			public void findNearest( D point, double maxDistance, int numNeighbors, DogArray<NnData<D>> result ) {
				result.reset();
				int w = assoc[numCalls];

				if (w >= 0) {
					NnData r1 = result.grow();
					r1.index = w;
					r1.point = points.get(w);
					r1.distance = 2.0;
					NnData r2 = result.grow();
					r2.index = w;
					r2.point = points.get(w);
					r2.distance = 2.0*distanceScale;
				}
			}
		}
	}
}
