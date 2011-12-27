/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.calibration;

import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCalibrationGridConfig {

	@Test
	public void computeGridPoints() {
		CalibrationGridConfig config = new CalibrationGridConfig(2,3,0.1);

		List<Point2D_F64> l = config.computeGridPoints();

		assertEquals(6,l.size());
		assertTrue(l.get(0).distance(new Point2D_F64(0,0))<=1e-8);
		assertTrue(l.get(1).distance(new Point2D_F64(0.1,0))<=1e-8);
		assertTrue(l.get(2).distance(new Point2D_F64(0,0.1))<=1e-8);
		assertTrue(l.get(3).distance(new Point2D_F64(0.1,0.1))<=1e-8);
		assertTrue(l.get(4).distance(new Point2D_F64(0,0.2))<=1e-8);
		assertTrue(l.get(5).distance(new Point2D_F64(0.1,0.2))<=1e-8);
	}
}
