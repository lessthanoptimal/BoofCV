/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.dots;

import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestRandomDotMarkerGenerator extends BoofStandardJUnit {

	/**
	 * Makes sure the generated marker meets all the post conditions.
	 */
	@Test
	void createRandomMarker() {
		// Generate a marker where it will be easy to find a valid solution for all N points. If r is too big
		// then this won't happen
		int N = 20;
		double w = 20;
		double r = 1;
		var bounds = new Rectangle2D_F64();

		for (int trial = 0; trial < 10; trial++) {
			List<Point2D_F64> marker = RandomDotMarkerGenerator.createRandomMarker(rand,N,w,w,r);
			assertEquals(N,marker.size());
			checkMarkerProperties(w, r, bounds, marker);
		}
	}

	/**
	 * It shouldn't be able to meet the request. Make sure it fails gracefully.
	 */
	@Test
	void createRandomMarker_hard() {
		int N = 2000;
		double w = 24;
		double r = 2;
		Rectangle2D_F64 bounds = new Rectangle2D_F64();

		List<Point2D_F64> marker = RandomDotMarkerGenerator.createRandomMarker(rand,N,w,w,r);
		assertTrue(marker.size() < N);
		assertTrue(marker.size() > 10);
		checkMarkerProperties(w, r, bounds, marker);
	}

	private void checkMarkerProperties(double width, double radius, Rectangle2D_F64 bounds, List<Point2D_F64> marker) {
		UtilPoint2D_F64.bounding(marker, bounds);
		assertTrue(bounds.getWidth() <= width);
		assertTrue(bounds.getHeight() <= width);
		for (int i = 0; i < marker.size(); i++) {
			double closest = Double.MAX_VALUE;
			for (int j = i + 1; j < marker.size(); j++) {
				double d = marker.get(i).distance2(marker.get(j));
				if (d < closest) {
					closest = d;
				}
			}
			closest = Math.sqrt(closest);
			assertTrue(closest >= radius);
		}
	}
}
