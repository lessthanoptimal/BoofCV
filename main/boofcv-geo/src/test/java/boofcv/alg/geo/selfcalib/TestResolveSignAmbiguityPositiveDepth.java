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

package boofcv.alg.geo.selfcalib;

import boofcv.BoofTesting;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
class TestResolveSignAmbiguityPositiveDepth extends CommonThreeViewSelfCalibration {
	@Test
	void simple() {
		standardScene();
		simulateScene(0);

		var alg = new ResolveSignAmbiguityPositiveDepth();

		var results = new MetricCameraTriple();
		results.view_1_to_2.setTo(super.truthView_1_to_i(1));
		results.view_1_to_3.setTo(super.truthView_1_to_i(2));
		results.view1.setTo(super.cameraA);
		results.view2.setTo(super.cameraB);
		results.view3.setTo(super.cameraC);

		alg.process(observations3, results);
		// there should be no change
		BoofTesting.assertEquals(super.truthView_1_to_i(1), results.view_1_to_2, 1e-8, 1e-8);
		BoofTesting.assertEquals(super.truthView_1_to_i(2), results.view_1_to_3, 1e-8, 1e-8);

		// flip the sign now
		results.view_1_to_2.T.scale(-1);
		results.view_1_to_3.T.scale(-1);
		alg.process(observations3, results);
		// the change should be reverted
		BoofTesting.assertEquals(super.truthView_1_to_i(1), results.view_1_to_2, 1e-8, 1e-8);
		BoofTesting.assertEquals(super.truthView_1_to_i(2), results.view_1_to_3, 1e-8, 1e-8);
	}
}
