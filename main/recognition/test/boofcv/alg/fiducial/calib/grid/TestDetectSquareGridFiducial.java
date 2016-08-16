/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.grid;

import boofcv.alg.fiducial.calib.squares.SquareGrid;
import boofcv.alg.fiducial.calib.squares.SquareGridTools;
import boofcv.alg.fiducial.calib.squares.TestSquareGridTools;
import boofcv.alg.fiducial.calib.squares.TestSquareRegularClustersIntoGrids;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDetectSquareGridFiducial {
	@Test
	public void process() {
		// intentionally blank.  tested in the wrapper class
	}

	@Test
	public void extractCalibrationPoints() {
		SquareGrid grid = TestSquareGridTools.createGrid(3,4);

		DetectSquareGridFiducial alg = new DetectSquareGridFiducial(3,4,1,null,null);

		new SquareGridTools().orderSquareCorners(grid);

		alg.extractCalibrationPoints(grid);
		List<Point2D_F64> list = alg.getCalibrationPoints();

		assertEquals(4 * 3 * 4, list.size());

		double w = TestSquareRegularClustersIntoGrids.DEFAULT_WIDTH;

		double x0 = -w/2;
		double y0 = -w/2;

		for (int row = 0; row < grid.rows * 2; row++) {
			for (int col = 0; col < grid.columns * 2; col++) {
				double x = x0 + col*w;
				double y = y0 + row*w;

				Point2D_F64 p = list.get(row*grid.columns*2+col);

				assertEquals(x,p.x,1e-8);
				assertEquals(y,p.y,1e-8);
			}
		}

	}
}
