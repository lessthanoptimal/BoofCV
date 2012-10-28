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

package boofcv.numerics.fitting.modelset.ransac;

import boofcv.numerics.fitting.modelset.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;


/**
 * @author Peter Abeles
 */
public class TestRansac extends GenericModelSetTests {

	Random rand = new Random(234);


	public TestRansac() {
		configure(0.9, 0.05, true);
	}

	@Override
	public ModelMatcher<double[],Double> createModelMatcher(DistanceFromModel<double[],Double> distance,
															ModelGenerator<double[],Double> generator,
															ModelFitter<double[],Double> fitter,
															int minPoints,
															double fitThreshold) {
		Ransac<double[],Double> ret = new Ransac<double[],Double>(344, generator, distance, 200, fitThreshold);
		ret.setSampleSize(minPoints);

		return ret;
	}

	/**
	 * See if it correctly randomly selects points when the initial set size is
	 * similar to the data set size
	 */
	@SuppressWarnings({"NumberEquality"})
	@Test
	public void randomDraw_large() {
		List<Integer> dataSet = new ArrayList<Integer>();

		for (int i = 0; i < 200; i++) {
			dataSet.add(i);
		}

		List<Integer> initSet = new ArrayList<Integer>();
		Ransac.randomDraw(dataSet, 150, initSet, rand);

		assertEquals(150, initSet.size());

		// make sure the item is in the original data set and that it is only contained once
		int numTheSame = 0;
		for (int i = 0; i < initSet.size(); i++) {
			Integer o = initSet.get(i);
			// make sure it is in the original data set
			assertTrue(dataSet.contains(o));

			// make sure the order has been changed
			if (o == i)
				numTheSame++;

			// make sure only one copy is in the init set
			for (int j = i + 1; j < initSet.size(); j++) {
				if (o == initSet.get(j)) {
					fail("Multiple copies in initSet");
				}
			}
		}

		// if the order has been randomized then very few should be in the original order
		assertTrue(numTheSame < initSet.size() * 0.9);

		// call get init set once more and see if it was cleared
		Ransac.randomDraw(dataSet, 150, initSet, rand);
		assertEquals(150, initSet.size());
	}

	/**
	 * See if it correctly randomly selects points when the initial set size is
	 * much smaller than the data set size
	 */
	@SuppressWarnings({"NumberEquality"})
	@Test
	public void randomDraw_small() {
		List<Integer> dataSet = new ArrayList<Integer>();

		for (int i = 0; i < 200; i++) {
			dataSet.add(i);
		}

		List<Integer> initSet = new ArrayList<Integer>();
		Ransac.randomDraw(dataSet, 15, initSet, rand);

		assertEquals(15, initSet.size());

		// make sure the item is in the original data set and that it is only contained once
		for (int i = 0; i < initSet.size(); i++) {
			Integer o = initSet.get(i);
			// make sure it is in the original dataset
			assertTrue(dataSet.contains(o));

			// make sure the order has been changed
			assertTrue(dataSet.get(i) != o);

			// make sure only one copy is in the init set
			for (int j = i + 1; j < initSet.size(); j++) {
				if (o == initSet.get(j)) {
					fail("Multiple copies in initSet");
				}
			}
		}

		// call get init set once more and see if it was cleared
		Ransac.randomDraw(dataSet, 15, initSet, rand);
		assertEquals(15, initSet.size());
	}

	/**
	 * See if it will select models with more of the correct points in it
	 */
	@Test
	public void selectMatchSet() {
		double modelVal = 50;

		List<Integer> dataSet = new ArrayList<Integer>();

		for (int i = 0; i < 200; i++) {
			dataSet.add(i);
		}

		DebugModelStuff stuff = new DebugModelStuff((int) modelVal);
		Ransac<double[],Integer> ransac = new Ransac<double[],Integer>(234,stuff,stuff,20,1);
		ransac.setSampleSize(5);
		// declare the array so it doesn't blow up when accessed
		ransac.matchToInput = new int[ dataSet.size()];
		double param[] = new double[]{modelVal};

		ransac.selectMatchSet(dataSet, 4, param);

		assertTrue(ransac.candidatePoints.size() == 7);
	}

	public static class DebugModelStuff implements
			DistanceFromModel<double[],Integer>, ModelGenerator<double[],Integer> {

		int threshold;

		double error;

		double[] param;

		public DebugModelStuff(int threshold) {
			this.threshold = threshold;
		}

		@Override
		public void setModel(double[] param) {
			this.param = param;
		}

		@Override
		public double computeDistance(Integer pt) {
			return Math.abs(pt - param[0]);
		}

		@Override
		public void computeDistance(List<Integer> points, double[] distance) {
			throw new RuntimeException("Why was this called?");
		}

		@Override
		public double[] createModelInstance() {
			return new double[1];
		}

		@Override
		public boolean generate(List<Integer> dataSet, double[] p) {

			error = 0;

			int offset = (int) p[0];

			for (Integer a : dataSet) {
				if (a + offset >= threshold) {
					error++;
				}
			}

			error += offset;
			p[0] = error;

			return true;
		}

		@Override
		public int getMinimumPoints() {
			return 1;
		}
	}
}
