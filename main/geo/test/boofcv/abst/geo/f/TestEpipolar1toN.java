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

import boofcv.abst.geo.EpipolarMatrixEstimator;
import boofcv.alg.geo.AssociatedPair;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestEpipolar1toN {

	List<AssociatedPair> points = new ArrayList<AssociatedPair>();

	@Test
	public void basicTest() {
		Epipolar1toN alg = new Epipolar1toN(new Dummy(true));

		assertTrue(alg.process(points));

		List<DenseMatrix64F> l = alg.getSolutions();
		assertEquals(1, l.size());

		alg = new Epipolar1toN(new Dummy(false));

		assertFalse(alg.process(points));
	}

	/**
	 * Makes sure everything is reset properly on multiple calls
	 */
	@Test
	public void multipleCalls() {
		Epipolar1toN alg = new Epipolar1toN(new Dummy(true));

		assertTrue(alg.process(points));
		assertEquals(1, alg.getSolutions().size());
		assertTrue(alg.process(points));
		assertEquals(1, alg.getSolutions().size());

		alg = new Epipolar1toN(new Dummy(false));
		assertFalse(alg.process(points));
		assertFalse(alg.process(points));
	}

	private static class Dummy implements EpipolarMatrixEstimator {
		boolean hasSolution;

		private Dummy(boolean hasSolution) {
			this.hasSolution = hasSolution;
		}

		@Override
		public boolean process(List<AssociatedPair> points) {
			return hasSolution;
		}

		@Override
		public DenseMatrix64F getEpipolarMatrix() {
			return new DenseMatrix64F(3, 3);
		}

		@Override
		public int getMinimumPoints() {
			return 3;
		}
	}

}
