/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestGeoModelEstimatorNto1 extends BoofStandardJUnit {

	List<AssociatedPair> obs = new ArrayList<>();

	DistanceEpipolarConstraint distance = new DistanceEpipolarConstraint();

	DMatrixRMaj found = new DMatrixRMaj(3,3);

	public TestGeoModelEstimatorNto1() {
		for (int i = 0; i < 5; i++) {
			double x0 = rand.nextDouble() * 2 - 1.0;
			double x1 = rand.nextDouble() * 2 - 1.0;
			double y0 = rand.nextDouble() * 2 - 1.0;
			double y1 = rand.nextDouble() * 2 - 1.0;

			obs.add(new AssociatedPair(x0, y0, x1, y1));
		}
	}

	@Test void successButNoSolutions() {
		GeoModelEstimatorNto1<DMatrixRMaj,AssociatedPair> alg =
				new DummyEstimator(new Dummy(0, true),distance,2);

		assertFalse(alg.process(obs,found));
	}

	@Test void checkFailed() {
		GeoModelEstimatorNto1<DMatrixRMaj,AssociatedPair>  alg =
				new DummyEstimator(new Dummy(0, false), distance,2);

		assertFalse(alg.process(obs,found));
	}

	@Test void checkOneSolution() {
		GeoModelEstimatorNto1<DMatrixRMaj,AssociatedPair>  alg =
				new DummyEstimator(new Dummy(1, true), distance,2);

		assertTrue(alg.process(obs,found));
	}

	/**
	 * Generate one matrix which should match the epipolar constraint and a bunch of random
	 * ones. See if it selects the correct matrix
	 */
	@Test void checkSelectBestSolution() {
		DMatrixRMaj correct = createSolution();

		GeoModelEstimatorNto1<DMatrixRMaj,AssociatedPair>  alg =
				new DummyEstimator(new Dummy(correct, 7), distance,2);

		assertTrue(alg.process(obs,found));

		// See if it selected the correct matrix
		assertTrue(MatrixFeatures_DDRM.isIdentical(found, correct, 1e-8));
	}

	private DMatrixRMaj createSolution() {
		EssentialNister5 nister = new EssentialNister5();


		DogArray<DMatrixRMaj> solutions = new QueueMatrix(3, 3);
		assertTrue(nister.processNormalized(obs,solutions));

		return solutions.get(0);
	}

	private static class Dummy implements GeoModelEstimatorN<DMatrixRMaj,AssociatedPair> {
		int numberOfSolutions;
		boolean success;

		DMatrixRMaj correct;

		private Dummy(int numberOfSolutions, boolean success) {
			this.numberOfSolutions = numberOfSolutions;
			this.success = success;
		}

		private Dummy(DMatrixRMaj correct, int numberOfSolutions) {
			this.correct = correct;
			this.numberOfSolutions = numberOfSolutions;
			success = true;
		}

		@Override
		public boolean process(List<AssociatedPair> points , DogArray<DMatrixRMaj> solutions ) {
			assertEquals(3, points.size());

			solutions.reset();
			Random rand = new Random(324);

			for (int i = 0; i < numberOfSolutions; i++) {
				solutions.grow().setTo(RandomMatrices_DDRM.rectangle(3, 3, rand));
			}

			if (correct != null)
				solutions.get(1).setTo(correct);

			return success;
		}

		@Override
		public int getMinimumPoints() {
			return 3;
		}
	}

	private class DummyEstimator extends GeoModelEstimatorNto1<DMatrixRMaj,AssociatedPair> {
		public DummyEstimator(GeoModelEstimatorN<DMatrixRMaj, AssociatedPair> alg,
							  DistanceFromModel<DMatrixRMaj, AssociatedPair> distance,
							  int numTest) {
			super(alg, distance, new QueueMatrix(3,3), numTest);
		}

		@Override
		protected void copy(DMatrixRMaj src, DMatrixRMaj dst) {
			dst.setTo(src);
		}
	}

}
