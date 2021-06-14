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

package boofcv.alg.structure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
class TestEstimateViewUtils {
	@Test void implement() {
		fail("implement");
	}

//	/**
//	 * Checks return value and handles a fatal error
//	 */
//	@Test void removedBadFeatures_FatalError() {
//		// This will always have a fatal error
//		var checks = new DummySanityChecks();
//		checks.success = false;
//
//		var alg = new EstimateViewSelfCalibrate();
//		alg.checks = checks;
//
//		var workGraph = new SceneWorkingGraph();
//
//		// The fatal error should be detected early on, processing stop, and the function return false
//		assertFalse(alg.removedBadFeatures(workGraph));
//	}
//
//	/**
//	 * Correctly applies the bad feature threshold that defines an unrecoverable error
//	 */
//	@Test void removedBadFeatures_BadFeaturesThreshold() {
//		var scene = new SceneWorkingGraph();
//		var checks = new DummySanityChecks();
//		var alg = new EstimateViewSelfCalibrate() {
//			@Override boolean refineWithBundleAdjustment( SceneWorkingGraph workGraph ) {
//				return true; // need this to do nothing
//			}
//		};
//		alg.pairwiseUtils = new PairwiseGraphUtils();
//
//		// If 10% of the features are bad it will try to fix the situation
//		alg.fractionBadFeaturesRecover = 0.1;
//		alg.checks = checks;
//
//		// need to make the number of inliers 100 so that internal checks pass
//		alg.pairwiseUtils.inliersThreeView.resize(100);
//		alg.pairwiseUtils.inlierIdx.resize(100);
//
//		// First test will have too many, above the threshold. Second time will have just the right number to pass
//		checks.numFeatures = 100;
//		checks.numBad = 11;
//		assertFalse(alg.removedBadFeatures(scene));
//		assertEquals(100, alg.pairwiseUtils.inliersThreeView.size);
//
//		checks.counter = 0;
//		checks.numBad = 10;
//		assertTrue(alg.removedBadFeatures(scene));
//		assertEquals(90, alg.pairwiseUtils.inliersThreeView.size);
//		assertEquals(90, alg.pairwiseUtils.inlierIdx.size);
//
//		// The second pass will always be perfect and isn't checked by this function
//	}
//
//	private static class DummySanityChecks extends MetricSanityChecks {
//		boolean success = true;
//		int numFeatures = 100;
//		int numBad = 0;
//
//		int counter = 0;
//
//		@Override
//		public boolean checkPhysicalConstraints( SceneStructureMetric structure,
//												 SceneObservations observations,
//												 List<CameraPinholeBrown> listPriors ) {
//			if (counter == 0) {
//				badFeatures.resetResize(numFeatures, false);
//				for (int i = 0; i < numBad; i++) {
//					badFeatures.set(i, true);
//				}
//			} else {
//				// second pass will be good
//				badFeatures.fill(false);
//			}
//			counter++;
//			return success;
//		}
//	}
}