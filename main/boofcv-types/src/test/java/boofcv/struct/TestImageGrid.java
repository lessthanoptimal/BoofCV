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

package boofcv.struct;

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Peter Abeles
 */
public class TestImageGrid extends BoofStandardJUnit {
	@Test void initialize() {
		ImageGrid<List<Point2D_F64>> grid = new ImageGrid<>(ArrayList::new, List::clear);

		// easy case
		grid.initialize(10, 40, 50);
		assertEquals(4, grid.cols);
		assertEquals(5, grid.rows);
		assertEquals(10, grid.lengthX);
		assertEquals(10, grid.lengthY);

		// check to see how it rounds
		grid.initialize(12, 40, 55);
		assertEquals(4, grid.cols);
		assertEquals(5, grid.rows);
		assertEquals(10, grid.lengthX);
		assertEquals(11, grid.lengthY);

		// Does it clear the lists?
		grid.get(0, 1).add(new Point2D_F64());
		grid.initialize(12, 40, 55);
		assertEquals(0, grid.get(0, 1).size());
	}

	@Test void getCellAtPixel() {
		ImageGrid<List<Point2D_F64>> grid = new ImageGrid<>(ArrayList::new, List::clear);
		grid.initialize(10, 40, 50);
		assertSame(grid.get(0, 0), grid.getCellAtPixel(3, 4));
		assertSame(grid.get(0, 0), grid.getCellAtPixel(9, 9));
		assertSame(grid.get(1, 0), grid.getCellAtPixel(9, 10));
		assertSame(grid.get(1, 1), grid.getCellAtPixel(10, 10));
	}

	@Test void processCells() {
		ImageGrid<List<Point2D_F64>> grid = new ImageGrid<>(ArrayList::new, List::clear);
		grid.initialize(10, 40, 50);
		grid.processCells(( row, col, list ) -> list.add(new Point2D_F64(col, row)));
		for (int row = 0; row < grid.rows; row++) {
			for (int col = 0; col < grid.cols; col++) {
				assertEquals(0.0, grid.get(row, col).get(0).distance(col, row), UtilEjml.TEST_F64);
			}
		}
	}

	@Test void processCellsThreads() {
		ImageGrid<List<Point2D_F64>> grid = new ImageGrid<>(ArrayList::new, List::clear);
		grid.initialize(10, 40, 50);
		grid.processCellsThreads(( row, col, list ) -> list.add(new Point2D_F64(col, row)));
		for (int row = 0; row < grid.rows; row++) {
			for (int col = 0; col < grid.cols; col++) {
				assertEquals(0.0, grid.get(row, col).get(0).distance(col, row), UtilEjml.TEST_F64);
			}
		}
	}
}
