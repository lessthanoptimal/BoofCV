/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.bundle.BundleAdjustmentSceneStructure.Point;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Peter Abeles
 */
public class TestBundleAdjustmentSceneStructure {
	@Test
	public void removePoints() {

		BundleAdjustmentSceneStructure structure = new BundleAdjustmentSceneStructure(false);

		Point original[] = structure.points = new Point[20];
		for (int i = 0; i < structure.points.length; i++) {
			structure.points[i] = new Point(3);
		}

		GrowQueue_I32 which = new GrowQueue_I32();
		which.add(2);
		which.add(5);

		structure.removePoints(which);

		assertEquals(18,structure.points.length);

		assertSame(original[0], structure.points[0]);
		assertSame(original[3], structure.points[2]);
		assertSame(original[4], structure.points[3]);
		assertSame(original[7], structure.points[5]);
		assertSame(original[19], structure.points[17]);
	}

	@Test
	public void Point_removeView() {
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
}