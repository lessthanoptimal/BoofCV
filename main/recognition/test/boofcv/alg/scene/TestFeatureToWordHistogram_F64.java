/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.scene;

import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.clustering.AssignCluster;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestFeatureToWordHistogram_F64 {

	public static final int NUM_CLUSTERS = 5;

	@Test
	public void getTotalWords() {
		FeatureToWordHistogram_F64 alg = new FeatureToWordHistogram_F64(new Assign(),true);
		assertEquals(NUM_CLUSTERS,alg.getTotalWords());
	}

	@Test
	public void simpleTest_hard() {
		FeatureToWordHistogram_F64 alg = new FeatureToWordHistogram_F64(new Assign(),true);

		for (int i = 0; i < 7; i++) {
			alg.addFeature(new TupleDesc_F64(5));
		}

		alg.process();

		double histogram[] = alg.getHistogram();

		checkSumOne(alg, histogram);

		assertEquals(2.0/7.0,histogram[0],1e-8);
		assertEquals(2.0/7.0,histogram[1],1e-8);
		assertEquals(1.0/7.0,histogram[2],1e-8);
		assertEquals(1.0/7.0,histogram[3],1e-8);
		assertEquals(1.0/7.0,histogram[4],1e-8);
	}

	private void checkSumOne(FeatureToWordHistogram_F64 alg, double[] histogram) {
		double total = 0;
		for (int i = 0; i < alg.getTotalWords(); i++) {
			total += histogram[i];
		}
		assertEquals(1.0,total,1e-8);
	}

	@Test
	public void simpleTest_soft() {
		FeatureToWordHistogram_F64 alg = new FeatureToWordHistogram_F64(new Assign(),false);

		for (int i = 0; i < 7; i++) {
			alg.addFeature(new TupleDesc_F64(5));
		}

		alg.process();

		double histogram[] = alg.getHistogram();

		checkSumOne(alg, histogram);

		assertEquals(0,histogram[0],1e-8);
		assertEquals(0,histogram[1],1e-8);
		assertEquals(0.25,histogram[2],1e-8);
		assertEquals(0.75,histogram[3],1e-8);
		assertEquals(0,histogram[4], 1e-8);
	}

	@Test
	public void reset() {
		FeatureToWordHistogram_F64 alg = new FeatureToWordHistogram_F64(new Assign(),true);

		for (int i = 0; i < 7; i++) {
			alg.addFeature(new TupleDesc_F64(5));
		}

		alg.process();

		// reset should clear previous history
		alg.reset();

		alg.addFeature(new TupleDesc_F64(5));
		alg.process();

		double histogram[] = alg.getHistogram();

		checkSumOne(alg, histogram);

		assertEquals(0,histogram[0],1e-8);
		assertEquals(0,histogram[1],1e-8);
		assertEquals(1,histogram[2],1e-8);
		assertEquals(0,histogram[3],1e-8);
		assertEquals(0,histogram[4], 1e-8);
	}

	private class Assign implements AssignCluster<double[]> {

		int numCalls = 0;

		@Override
		public int assign(double[] point) {
			return numCalls++ % NUM_CLUSTERS;
		}

		@Override
		public void assign(double[] point, double[] fit) {
			Arrays.fill(fit,0);
			fit[2] = 0.25;
			fit[3] = 0.75;
		}

		@Override
		public int getNumberOfClusters() {
			return NUM_CLUSTERS;
		}

		@Override
		public AssignCluster<double[]> copy() {
			return null;
		}
	}
}
