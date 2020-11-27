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

package boofcv.abst.geo.bundle;

import boofcv.abst.geo.bundle.SceneStructureCommon.Point;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Peter Abeles
 */
class TestSceneStructureCommon extends BoofStandardJUnit {
	@Test
	void removePoints() {

		MockSceneStructureCommon structure = new MockSceneStructureCommon(false);
		structure.homogenous = false;
		structure.pointSize = 3;

		List<Point> original = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			original.add(structure.points.grow());
		}

		DogArray_I32 which = new DogArray_I32();
		which.add(2);
		which.add(5);

		structure.removePoints(which);

		assertEquals(18,structure.points.size);

		assertSame(original.get(0), structure.points.data[0]);
		assertSame(original.get(3), structure.points.data[2]);
		assertSame(original.get(4), structure.points.data[3]);
		assertSame(original.get(7), structure.points.data[5]);
		assertSame(original.get(19), structure.points.data[17]);
	}

	@Test
	void Point_removeView() {
		Point p = new Point(3);

		p.views.add(1);
		p.views.add(6);
		p.views.add(3);
		p.views.add(9);
		p.views.add(4);

		p.removeView(9);

		assertEquals(4,p.views.size);
		assertEquals(1,p.views.get(0));
		assertEquals(6,p.views.get(1));
		assertEquals(3,p.views.get(2));
		assertEquals(4,p.views.get(3));

	}

	private static class MockSceneStructureCommon extends SceneStructureCommon {

		MockSceneStructureCommon(boolean homogenous) {
			super(homogenous);
		}

		@Override
		public int getParameterCount() {
			return 0;
		}
	}
}
