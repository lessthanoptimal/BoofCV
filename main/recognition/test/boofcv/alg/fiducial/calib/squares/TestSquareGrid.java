/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.squares;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSquareGrid {

	@Test
	public void get() {
		SquareGrid grid = TestSquareGridTools.createGrid(3,4);

		assertTrue(grid.nodes.get(0)==grid.get(0,0));
		assertTrue(grid.nodes.get(3)==grid.get(0,3));
		assertTrue(grid.nodes.get(2*4+1)==grid.get(2,1));
		assertTrue(grid.nodes.get(2*4)==grid.get(-1,0));
		assertTrue(grid.nodes.get(2*4+3)==grid.get(-1,-1));

	}

	@Test
	public void getCornerByIndex() {
		SquareGrid grid = TestSquareGridTools.createGrid(3,4);

		assertTrue(grid.get(0,0)==grid.getCornerByIndex(0));
		assertTrue(grid.get(0,3)==grid.getCornerByIndex(1));
		assertTrue(grid.get(2,3)==grid.getCornerByIndex(2));
		assertTrue(grid.get(2,0) == grid.getCornerByIndex(3));
	}

	@Test
	public void getCornerIndex() {
		SquareGrid grid = TestSquareGridTools.createGrid(3,4);

		assertTrue(0==grid.getCornerIndex(grid.get(0, 0)));
		assertTrue(1==grid.getCornerIndex(grid.get(0, 3)));
		assertTrue(2==grid.getCornerIndex(grid.get(2, 3)));
		assertTrue(3==grid.getCornerIndex(grid.get(2, 0)));
	}
}
