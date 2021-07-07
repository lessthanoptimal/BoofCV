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

package boofcv.abst.geo.bundle;

import boofcv.abst.geo.bundle.SceneObservations.View;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSceneObservations extends BoofStandardJUnit {
	@Test void View_remove() {
		View v = new View();

		v.add(5,1,2);
		v.add(1,2,3);
		v.add(8,3,3);
		v.add(3,4,2);

		assertEquals(4,v.size());

		v.remove(2);

		assertEquals(3,v.size());
		assertEquals(5,v.point.get(0));
		assertEquals(1,v.point.get(1));
		assertEquals(3,v.point.get(2));

		Point2D_F64 p = new Point2D_F64();
		v.getPixel(0,p);
		assertTrue(p.distance2(1,2) < 1e-7);
		v.getPixel(2,p);
		assertTrue(p.distance2(4,2) < 1e-7);
	}

	@Test void View_set() {
		View v = new View();

		v.add(5,1,2);
		v.add(1,2,3);
		v.add(8,3,3);
		v.add(3,4,2);

		v.set(2,4,-1,-2);
		assertEquals(4,v.point.get(2));
		Point2D_F64 p = new Point2D_F64();
		v.getPixel(2,p);
		assertTrue(p.distance2(-1,-2) < 1e-7);
	}

	@Test void View_setPixel() {
		View v = new View();

		v.add(5,1,2);
		v.add(1,2,3);
		v.add(8,3,3);
		v.add(3,4,2);

		v.setPixel(2,-1,-2);
		assertEquals(8,v.point.get(2));
		Point2D_F64 p = new Point2D_F64();
		v.getPixel(2,p);
		assertTrue(p.distance2(-1,-2) < 1e-7);
	}
}
