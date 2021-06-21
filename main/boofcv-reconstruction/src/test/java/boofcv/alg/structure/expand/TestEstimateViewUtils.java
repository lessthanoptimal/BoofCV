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

package boofcv.alg.structure.expand;

import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.structure.MetricSanityChecks;
import boofcv.alg.structure.PairwiseGraphUtils;
import boofcv.alg.structure.expand.EstimateViewUtils.RemoveResults;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestEstimateViewUtils extends BoofStandardJUnit {

	/**
	 * Very simple test that just makes sure three views/cameras have been set as constant initially
	 */
	@Test void configureSbaStructure() {
		DogArray<AssociatedTriple> inliers = new DogArray<>(AssociatedTriple::new);
		inliers.resize(95);

		var alg = new EstimateViewUtils();
		// configure internal data structures so that it won't blow up
		alg.camera1 = new BundlePinholeSimplified();
		alg.camera2 = new BundlePinholeSimplified();
		alg.camera3 = new BundlePinholeSimplified();
		alg.normalize1.setK(100, 100, 0, 100, 100).setDistortion(0.1, 0.1);
		alg.normalize2.setK(100, 100, 0, 100, 100).setDistortion(0.1, 0.1);
		alg.normalize3.setK(100, 100, 0, 100, 100).setDistortion(0.1, 0.1);
		alg.usedThreeViewInliers.resize(40);

		alg.configureSbaStructure(inliers.toList());

		// Check the results to make sure stuff is marked as known
		for (int i = 0; i < 3; i++) {
			assertTrue(alg.metricSba.structure.cameras.get(i).known);
			assertTrue(alg.metricSba.structure.motions.get(i).known);
		}
		assertEquals(40, alg.metricSba.structure.points.size);
	}

	@Test void setCamera3() {
		var camera = new BundlePinholeSimplified(50, 1, 2);
		var alg = new EstimateViewUtils();
		alg.setCamera3(camera);

		// Make sure it saved the reference and copied everything else over
		assertSame(camera, alg.camera3);
		assertEquals(50, alg.normalize3.fx, UtilEjml.TEST_F64);
		assertEquals(50, alg.normalize3.fy, UtilEjml.TEST_F64);
		assertEquals(0, alg.normalize3.skew, UtilEjml.TEST_F64);
		assertEquals(0, alg.normalize3.cx, UtilEjml.TEST_F64);
		assertEquals(0, alg.normalize3.cy, UtilEjml.TEST_F64);
		assertEquals(1, alg.normalize3.params.radial[0], UtilEjml.TEST_F64);
		assertEquals(2, alg.normalize3.params.radial[1], UtilEjml.TEST_F64);
	}

	/**
	 * Checks return value and handles a fatal error
	 */
	@Test void removedBadFeatures_FatalError() {
		var pairwise = new PairwiseGraphUtils();


		// This will always have a fatal error
		var checks = new DummySanityChecks();
		checks.success = false;

		var alg = new EstimateViewUtils();
		alg.checks = checks;

		// The fatal error should be detected early on, processing stop, and the function return false
		assertEquals(RemoveResults.FAILED, alg.removedBadFeatures(pairwise, 0.1, null));
	}

	/**
	 * Correctly applies the bad feature threshold that defines an unrecoverable error
	 */
	@Test void removedBadFeatures_BadFeaturesThreshold() {
		var checks = new DummySanityChecks();
		var pairwise = new PairwiseGraphUtils();
		var alg = new EstimateViewUtils();

		// If 10% of the features are bad it will try to fix the situation
		double fractionRecover = 0.1;
		alg.checks = checks;

		// need to make the number of inliers 100 so that internal checks pass
		alg.usedThreeViewInliers.resize(100);

		// There will be too many bad ones it should fail
		checks.numFeatures = 100;
		checks.numBad = 11;
		assertEquals(RemoveResults.FAILED, alg.removedBadFeatures(pairwise, fractionRecover, null));
		assertEquals(100, alg.usedThreeViewInliers.size);

		// There will be just enough to barely get 'AGAIN'
		checks.counter = 0;
		checks.numBad = 10;
		assertEquals(RemoveResults.AGAIN, alg.removedBadFeatures(pairwise, fractionRecover, null));
		assertEquals(90, alg.usedThreeViewInliers.size);

		// No errors, it should be good now. No changes to inliers
		checks.counter = 0;
		checks.numBad = 0;
		alg.usedThreeViewInliers.resize(100);
		assertEquals(RemoveResults.GOOD, alg.removedBadFeatures(pairwise, fractionRecover, null));
		assertEquals(100, alg.usedThreeViewInliers.size);
	}

	private static class DummySanityChecks extends MetricSanityChecks {
		boolean success = true;
		int numFeatures = 100;
		int numBad = 0;

		int counter = 0;

		@Override public boolean checkPhysicalConstraints( SceneStructureMetric structure,
														   SceneObservations observations,
														   List<CameraPinholeBrown> listPriors ) {
			if (counter == 0) {
				badFeatures.resetResize(numFeatures, false);
				for (int i = 0; i < numBad; i++) {
					badFeatures.set(i, true);
				}
			} else {
				// second pass will be good
				badFeatures.fill(false);
			}
			counter++;
			return success;
		}
	}
}
