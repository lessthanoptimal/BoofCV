/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort.division;

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestAddDivisionNtoN_F64 extends BoofStandardJUnit {
	@Test void compareManual() {
		double radial = 0.00002;

		double x_dist = 40;
		double y_dist = 52;
		double r = x_dist*x_dist + y_dist*y_dist;

		double x_undist = x_dist/(1.0 + radial*r);
		double y_undist = y_dist/(1.0 + radial*r);

		var found = new Point2D_F64();
		new AddDivisionNtoN_F64().setRadial(radial).compute(x_undist, y_undist, found);

		assertEquals(0.0, found.distance(x_dist, y_dist), UtilEjml.TEST_F64);
	}
}
