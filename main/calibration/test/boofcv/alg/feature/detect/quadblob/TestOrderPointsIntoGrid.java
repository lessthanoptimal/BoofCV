/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.quadblob;

import boofcv.alg.feature.detect.grid.UtilCalibrationGrid;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestOrderPointsIntoGrid {

	Random rand = new Random(234);

	/**
	 * Give it a grid of points which has been shuffled.  Rotate the output until it is in the correct orientation
	 * then inspect the points to see if they are in the correct order.
	 */
	@Test
	public void basicTest() {

		int numCols = 3;
		int numRows = 4;

		List<Point2D_F64> unordered = new ArrayList<Point2D_F64>();

		for( int i = 0; i < numRows; i++ ) {
			for( int j = 0; j < numCols; j++ ) {
				unordered.add( new Point2D_F64(j,i));
			}
		}

		// randomize the list
		Collections.shuffle(unordered,rand);

		// reorganize the list
		OrderPointsIntoGrid alg = new OrderPointsIntoGrid();
		alg.process(unordered);

		List<Point2D_F64> found = alg.getOrdered();

		// put into the original order
		if( numCols == alg.getNumCols() && numRows == alg.getNumRows() ) {
			if( found.get(0).x != 0 ) {
				found = UtilCalibrationGrid.rotatePoints(found,numCols,numRows);
				found = UtilCalibrationGrid.rotatePoints(found,numRows,numCols);
			}

		} else if( numCols == alg.getNumCols() && numRows == alg.getNumRows() ) {
			found = UtilCalibrationGrid.rotatePoints(found,numRows,numCols);
			if( found.get(0).x != 0 ) {
				found = UtilCalibrationGrid.rotatePoints(found,numCols,numRows);
				found = UtilCalibrationGrid.rotatePoints(found,numRows,numCols);
			}
		} else {
			fail("Unexpected number of rows/columns");
		}

		// check results
		int index = 0;
		for( int i = 0; i < numRows; i++ ) {
			for( int j = 0; j < numCols; j++ ) {
				Point2D_F64 p = found.get(index++);
				assertEquals(j,p.x,1e-8);
				assertEquals(i,p.y,1e-8);
			}
		}
	}
}
