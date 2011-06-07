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

package gecv.numerics.fitting.modelset.ransac;

import gecv.numerics.fitting.modelset.DistanceFromModel;
import gecv.numerics.fitting.modelset.ModelFitter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;


/**
 * @author Peter Abeles
 */
public class TestSimpleRansacCommon {

	Random rand = new Random(345);

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
		SimpleRansacCommon.randomDraw(dataSet, 150, initSet, rand);

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
		SimpleRansacCommon.randomDraw(dataSet, 150, initSet, rand);
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
		SimpleRansacCommon.randomDraw(dataSet, 15, initSet, rand);

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
		SimpleRansacCommon.randomDraw(dataSet, 15, initSet, rand);
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
		SimpleRansacCommon<double[],Integer> ransac = new RandsacDebug(stuff, stuff);
		double param[] = new double[]{modelVal};

		ransac.selectMatchSet(dataSet, 4, 5, param);

		assertTrue(ransac.candidatePoints.size() == 7);
	}

	public static class RandsacDebug extends SimpleRansacCommon<double[],Integer> {

		public RandsacDebug(ModelFitter<double[],Integer> integerModelFitter,
							DistanceFromModel<double[],Integer> modelDistance) {
			super(integerModelFitter, modelDistance, 0, 0);
		}

		@Override
		public boolean process(List<Integer> dataSet, double[] paramInital) {
			return false;
		}

		@Override
		public double getError() {
			return 0;
		}
	}

	public static class DebugModelStuff implements
			DistanceFromModel<double[],Integer>, ModelFitter<double[],Integer> {

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
		public double[] declareModel() {
			return new double[1];
		}

		@Override
		public boolean fitModel(List<Integer> dataSet, double []initP, double[] p) {
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