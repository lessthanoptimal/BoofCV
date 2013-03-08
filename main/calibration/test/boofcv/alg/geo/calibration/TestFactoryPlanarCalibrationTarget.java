/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestFactoryPlanarCalibrationTarget {

	@Test
	public void gridSquare() {
		PlanarCalibrationTarget config = FactoryPlanarCalibrationTarget.gridSquare(3, 5, 0.1, 0.2);
		List<Point2D_F64> l = config.points;

		assertEquals(4*6,l.size());
		
		double w = l.get(1).x - l.get(0).x;
		double h = l.get(4).y - l.get(0).y;

		assertEquals(0.1,w,1e-8);
		assertEquals(0.1,h,1e-8);
		
		double s = l.get(2).x - l.get(1).x;
		
		assertEquals(0.2,s,1e-8);
	}
}
