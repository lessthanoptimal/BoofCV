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

package boofcv.abst.geo.f;

import boofcv.abst.geo.EpipolarMatrixEstimatorN;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.f.EssentialNister5;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.RandomMatrices;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestFundamentalNto1 {

	Random rand = new Random(234);
	List<AssociatedPair> obs = new ArrayList<AssociatedPair>();

	public TestFundamentalNto1() {
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
		FundamentalNto1 alg = new FundamentalNto1(new Dummy(0, true), 2);

		assertFalse(alg.process(obs));
	}

	@Test
	public void checkFailed() {
		FundamentalNto1 alg = new FundamentalNto1(new Dummy(0, false), 2);

		assertFalse(alg.process(obs));
	}

	@Test
	public void checkOneSolution() {
		FundamentalNto1 alg = new FundamentalNto1(new Dummy(1, true), 2);

		assertTrue(alg.process(obs));

		assertTrue(null != alg.getEpipolarMatrix());
	}

	/**
	 * Generate one matrix which should match the epipolar constraint and a bunch of random
	 * ones.  See if it selects the correct matrix
	 */
	@Test
	public void checkSelectBestSolution() {
		DenseMatrix64F correct = createSolution();

		FundamentalNto1 alg = new FundamentalNto1(new Dummy(correct, 7), 2);

		assertTrue(alg.process(obs));

		// See if it selected the correct matrix
		assertTrue(correct == alg.getEpipolarMatrix());
	}

	private DenseMatrix64F createSolution() {
		EssentialNister5 nister = new EssentialNister5();

		assertTrue(nister.process(obs));

		return nister.getSolutions().get(0);
	}

	private static class Dummy implements EpipolarMatrixEstimatorN {
		int numberOfSolutions;
		boolean success;

		DenseMatrix64F correct;

		private Dummy(int numberOfSolutions, boolean success) {
			this.numberOfSolutions = numberOfSolutions;
			this.success = success;
		}

		private Dummy(DenseMatrix64F correct, int numberOfSolutions) {
			this.correct = correct;
			this.numberOfSolutions = numberOfSolutions;
			success = true;
		}

		@Override
		public boolean process(List<AssociatedPair> points) {
			assertEquals(3, points.size());
			return success;
		}

		@Override
		public List<DenseMatrix64F> getSolutions() {
			Random rand = new Random(324);

			List<DenseMatrix64F> ret = new ArrayList<DenseMatrix64F>();
			for (int i = 0; i < numberOfSolutions; i++) {
				ret.add(RandomMatrices.createRandom(3, 3, rand));
			}

			if (correct != null)
				ret.set(1, correct);

			return ret;
		}

		@Override
		public int getMinimumPoints() {
			return 3;
		}
	}
}
