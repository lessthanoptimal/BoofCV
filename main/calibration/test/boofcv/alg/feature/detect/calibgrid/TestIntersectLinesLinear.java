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

package boofcv.alg.feature.detect.calibgrid;

import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestIntersectLinesLinear {

	/**
	 * Trivial case with two perfect observations
	 */
	@Test
	public void trivial() {
		IntersectLinesLinear alg = new IntersectLinesLinear();
		
		List<LineParametric2D_F64> lines = new ArrayList<LineParametric2D_F64>();
		lines.add( new LineParametric2D_F64(2,0,1,1));
		lines.add( new LineParametric2D_F64(0,2,1,-1));

		assertTrue(alg.process(lines));

		Point2D_F64 found = alg.getPoint();
		assertEquals(2,found.x,1e-8);
		assertEquals(0,found.y,1e-8);
	}

	/**
	 * Provide two points which should completely dominate the solution with one noise point
	 * which should be virtually ignored
	 */
	@Test
	public void weight() {
		IntersectLinesLinear alg = new IntersectLinesLinear();

		List<LineParametric2D_F64> lines = new ArrayList<LineParametric2D_F64>();
		lines.add(new LineParametric2D_F64(10, 0, 10000, 0));
		lines.add(new LineParametric2D_F64(1, 10, 0, 10000));
		lines.add( new LineParametric2D_F64(2,10,0,0.01));

		assertTrue(alg.process(lines));

		Point2D_F64 found = alg.getPoint();
		assertEquals(1,found.x,0.01);
		assertEquals(0,found.y,0.01);
	}

	/**
	 * Provide redundant but perfect observations
	 */
	@Test
	public void redundant() {
		IntersectLinesLinear alg = new IntersectLinesLinear();

		List<LineParametric2D_F64> lines = new ArrayList<LineParametric2D_F64>();
		lines.add(new LineParametric2D_F64(10, 0, 1, 0));
		lines.add(new LineParametric2D_F64(1, 10, 0, 1));
		lines.add( new LineParametric2D_F64(10,0,-1,0));
		lines.add( new LineParametric2D_F64(1,10,0,-1));
		lines.add( new LineParametric2D_F64(5,0,1,0));
		lines.add( new LineParametric2D_F64(1,6,0,1));

		assertTrue(alg.process(lines));

		Point2D_F64 found = alg.getPoint();
		assertEquals(1,found.x,1e-8);
		assertEquals(0,found.y,1e-8);
	}
}
