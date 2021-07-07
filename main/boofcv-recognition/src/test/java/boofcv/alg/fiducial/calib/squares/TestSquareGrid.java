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

package boofcv.alg.fiducial.calib.squares;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Peter Abeles
 */
public class TestSquareGrid  extends BoofStandardJUnit {

	@Test void get() {
		SquareGrid grid = TestSquareGridTools.createGrid(3,4);

		assertSame(grid.nodes.get(0), grid.get(0, 0));
		assertSame(grid.nodes.get(3), grid.get(0, 3));
		assertSame(grid.nodes.get(2*4 + 1), grid.get(2, 1));
		assertSame(grid.nodes.get(2*4), grid.get(-1, 0));
		assertSame(grid.nodes.get(2*4 + 3), grid.get(-1, -1));

	}

	@Test void getCornerByIndex() {
		SquareGrid grid = TestSquareGridTools.createGrid(3,4);

		assertSame(grid.get(0, 0), grid.getCornerByIndex(0));
		assertSame(grid.get(0, 3), grid.getCornerByIndex(1));
		assertSame(grid.get(2, 3), grid.getCornerByIndex(2));
		assertSame(grid.get(2, 0), grid.getCornerByIndex(3));
	}

	@Test void getCornerIndex() {
		SquareGrid grid = TestSquareGridTools.createGrid(3,4);

		assertEquals(grid.getCornerIndex(grid.get(0, 0)), 0);
		assertEquals(grid.getCornerIndex(grid.get(0, 3)), 1);
		assertEquals(grid.getCornerIndex(grid.get(2, 3)), 2);
		assertEquals(grid.getCornerIndex(grid.get(2, 0)), 3);
	}
}
