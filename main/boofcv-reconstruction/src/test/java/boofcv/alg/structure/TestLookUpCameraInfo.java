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

import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.ImageDimension;
import boofcv.testing.BoofStandardJUnit;
import georegression.metric.UtilAngle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestLookUpCameraInfo extends BoofStandardJUnit {
	/**
	 * adds a view then retrieves various information and makes sure everything is working correctly
	 */
	@Test void addView_lookupVarious() {
		var alg = new LookUpCameraInfo();

		alg.addCameraCanonical(1001, 200, 45);
		alg.addCameraCanonical(1002, 200, 45);
		alg.addCameraCanonical(1003, 200, 45);

		alg.addView("foo", 1);

		assertEquals(1, alg.viewToCamera("foo"));

		ImageDimension shape = new ImageDimension();
		alg.lookupViewShape("foo", shape);
		assertEquals(1002, shape.width);

		CameraPinholeBrown foundCamera = new CameraPinholeBrown();
		alg.lookupCalibration("foo", foundCamera);
		assertEquals(1002, foundCamera.width);
	}

	@Test void addCameraCanonical() {
		var alg = new LookUpCameraInfo();
		alg.addCameraCanonical(1000, 200, 45);

		CameraPinholeBrown found = alg.listCalibration.get(0);
		assertEquals(1000, found.width);
		assertEquals(200, found.height);
		assertEquals(500, found.cx);
		assertEquals(100, found.cy);

		double f = 500/Math.tan(UtilAngle.degreeToRadian(45)/2.0);
		assertEquals(1.0, f/found.fx, 1e-7);
		assertEquals(1.0, f/found.fy, 1e-7);
	}
}
