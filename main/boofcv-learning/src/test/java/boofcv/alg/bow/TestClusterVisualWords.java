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

package boofcv.alg.bow;

import boofcv.struct.feature.TupleDesc_F64;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.clustering.AssignCluster;
import org.ddogleg.clustering.ComputeClusters;
import org.ddogleg.struct.LArrayAccessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestClusterVisualWords extends BoofStandardJUnit {

	long SEED = 123;
	int NUM_CLUSTERS = 12;
	double DISTANCE = 394.5;

	@Test public void process() {
		DummyClusters clusters = new DummyClusters();
		ClusterVisualWords alg = new ClusterVisualWords(clusters,SEED);

		alg.addReference(new TupleDesc_F64(2));
		alg.addReference(new TupleDesc_F64(2));
		alg.addReference(new TupleDesc_F64(2));
		alg.process(NUM_CLUSTERS);

		assertEquals(1,clusters.numInit);
		assertEquals(1,clusters.numProcess);
		assertEquals(3,clusters.numInputPoints);
		assertEquals(DISTANCE,clusters.getDistanceMeasure(),1e-8);
	}

	protected class DummyClusters implements ComputeClusters<double[]> {
		int numInit = 0;
		int numProcess = 0;
		int numInputPoints = 0;

		@Override public void initialize(long randomSeed) {
			numInit++;

			assertEquals(SEED,randomSeed);
		}

		@Override public void process( LArrayAccessor<double[]> points, int numCluster) {
			numProcess++;
			numInputPoints = points.size();
			assertEquals(NUM_CLUSTERS, numCluster);
		}

		@Override public AssignCluster<double[]> getAssignment() {
			return null;
		}

		@Override public double getDistanceMeasure() {
			return DISTANCE;
		}

		@Override public void setVerbose(boolean verbose) {}

		@Override public ComputeClusters<double[]> newInstanceThread() {
			return null;
		}
	}
}
