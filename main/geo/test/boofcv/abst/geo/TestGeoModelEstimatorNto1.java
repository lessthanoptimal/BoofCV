/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo;

import boofcv.alg.geo.f.DistanceEpipolarConstraint;
import boofcv.alg.geo.f.EssentialNister5;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.GeoModelEstimatorN;
import boofcv.struct.geo.QueueMatrix;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.RowMatrix_F64;
import org.ejml.ops.MatrixFeatures_R64;
import org.ejml.ops.RandomMatrices_R64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestGeoModelEstimatorNto1 {

	Random rand = new Random(234);
	List<AssociatedPair> obs = new ArrayList<>();

	DistanceEpipolarConstraint distance = new DistanceEpipolarConstraint();

	RowMatrix_F64 found = new RowMatrix_F64(3,3);

	public TestGeoModelEstimatorNto1() {
		for (int i = 0; i < 5; i++) {
			double x0 = rand.nextDouble() * 2 - 1.0;
			double x1 = rand.nextDouble() * 2 - 1.0;
			double y0 = rand.nextDouble() * 2 - 1.0;
			double y1 = rand.nextDouble() * 2 - 1.0;

			obs.add(new AssociatedPair(x0, y0, x1, y1));
		}
	}

	@Test
	public void successButNoSolutions() {
		GeoModelEstimatorNto1<RowMatrix_F64,AssociatedPair> alg =
				new DummyEstimator(new Dummy(0, true),distance,2);

		assertFalse(alg.process(obs,found));
	}

	@Test
	public void checkFailed() {
		GeoModelEstimatorNto1<RowMatrix_F64,AssociatedPair>  alg =
				new DummyEstimator(new Dummy(0, false), distance,2);

		assertFalse(alg.process(obs,found));
	}

	@Test
	public void checkOneSolution() {
		GeoModelEstimatorNto1<RowMatrix_F64,AssociatedPair>  alg =
				new DummyEstimator(new Dummy(1, true), distance,2);

		assertTrue(alg.process(obs,found));
	}

	/**
	 * Generate one matrix which should match the epipolar constraint and a bunch of random
	 * ones.  See if it selects the correct matrix
	 */
	@Test
	public void checkSelectBestSolution() {
		RowMatrix_F64 correct = createSolution();

		GeoModelEstimatorNto1<RowMatrix_F64,AssociatedPair>  alg =
				new DummyEstimator(new Dummy(correct, 7), distance,2);

		assertTrue(alg.process(obs,found));

		// See if it selected the correct matrix
		assertTrue(MatrixFeatures_R64.isIdentical(found, correct, 1e-8));
	}

	private RowMatrix_F64 createSolution() {
		EssentialNister5 nister = new EssentialNister5();


		FastQueue<RowMatrix_F64> solutions = new QueueMatrix(3, 3);
		assertTrue(nister.process(obs,solutions));

		return solutions.get(0);
	}

	private static class Dummy implements GeoModelEstimatorN<RowMatrix_F64,AssociatedPair> {
		int numberOfSolutions;
		boolean success;

		RowMatrix_F64 correct;

		private Dummy(int numberOfSolutions, boolean success) {
			this.numberOfSolutions = numberOfSolutions;
			this.success = success;
		}

		private Dummy(RowMatrix_F64 correct, int numberOfSolutions) {
			this.correct = correct;
			this.numberOfSolutions = numberOfSolutions;
			success = true;
		}

		@Override
		public boolean process(List<AssociatedPair> points , FastQueue<RowMatrix_F64> solutions ) {
			assertEquals(3, points.size());

			solutions.reset();
			Random rand = new Random(324);

			for (int i = 0; i < numberOfSolutions; i++) {
				solutions.grow().set(RandomMatrices_R64.createRandom(3, 3, rand));
			}

			if (correct != null)
				solutions.get(1).set(correct);

			return success;
		}

		@Override
		public int getMinimumPoints() {
			return 3;
		}
	}

	private class DummyEstimator extends GeoModelEstimatorNto1<RowMatrix_F64,AssociatedPair> {

		public DummyEstimator(GeoModelEstimatorN<RowMatrix_F64, AssociatedPair> alg,
							  DistanceFromModel<RowMatrix_F64, AssociatedPair> distance,
							  int numTest) {
			super(alg, distance, new QueueMatrix(3,3), numTest);
		}

		@Override
		protected void copy(RowMatrix_F64 src, RowMatrix_F64 dst) {
			dst.set(src);
		}
	}

}
