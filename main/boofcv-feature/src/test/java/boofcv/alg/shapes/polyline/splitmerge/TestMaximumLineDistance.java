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

package boofcv.alg.shapes.polyline.splitmerge;

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I32;
import org.junit.jupiter.api.Test;

import java.util.List;

import static boofcv.alg.shapes.polyline.splitmerge.TestPolylineSplitMerge.rect;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestMaximumLineDistance extends BoofStandardJUnit {

	@Test void selectSplitPoint() {
		List<Point2D_I32> contour = rect(10,12,20,22);

		MaximumLineDistance alg = new MaximumLineDistance();

		PolylineSplitMerge.SplitResults results = new PolylineSplitMerge.SplitResults();
		alg.selectSplitPoint(contour,0,25,results);

		assertEquals(10,results.index);
		assertTrue(results.score > 1);

		// see if it handles wrapping indexes

		alg.selectSplitPoint(contour,31,4,results);

		assertEquals(0,results.index);
		assertTrue(results.score > 1);
	}

	@Test void compareScore() {

		MaximumLineDistance alg = new MaximumLineDistance();

		assertEquals(1,alg.compareScore(5,1));
		assertEquals(0,alg.compareScore(5,5));
		assertEquals(-1,alg.compareScore(1,5));

	}
}
