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

package boofcv.alg.mvs;

import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestPointIndexToPointIterator extends BoofStandardJUnit {
	@Test void basic() {
		List<Point2D_F64> list = new ArrayList<>();
		list.add(new Point2D_F64(1.0, 2.0));
		list.add(new Point2D_F64(2.0, 2.0));
		list.add(new Point2D_F64(3.0, 2.0));

		var alg = new PointToIndexIterator<>(list, 1, 3, new PointIndex2D_F64());

		for (int i = 1; i < list.size(); i++) {
			assertTrue(alg.hasNext());
			PointIndex2D_F64 found = alg.next();
			assertEquals(i, found.index);
			assertEquals(0.0, list.get(i).distance(found.p), UtilEjml.TEST_F64);
		}

		assertFalse(alg.hasNext());
	}
}
