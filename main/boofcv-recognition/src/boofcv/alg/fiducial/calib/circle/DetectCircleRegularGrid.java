/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.circle;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.Grid;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.struct.image.ImageGray;

/**
 * TODO UPDATE
 *
 * <p>Detects asymmetric grids of circles.  The grid is composed of two regular grids which are offset by half a period.
 * See image below for an example.  Rows and columns are counted by counting every row even if they are offset
 * from each other.  The returned grid will be put into canonical orientation, see below.</p>
 *
 * <p>
 * For each circle there is one control point.  The control point is first found by detecting all the ellipses, which
 * is what a circle appears to be under perspective distortion.  The center the ellipse might not match the physical
 * center of the circle.  The intersection of lines does not change under perspective distortion.  The outer common
 * tangent lines between neighboring ellipses are found.  Then the intersection of two such lines is found.  This
 * intersection will be the physical center of the circle.
 * </p>
 *
 * <center>
 * <img src="doc-files/asymcirclegrid.jpg"/>
 * </center>
 * Example of a 8 by 5 grid; row, column.
 *
 * <p>Canonical orientation is defined as having the rows/columns matched, element (0,0) being occupied.
 * If there are multiple solution a solution will be selected which is in counter-clockwise order (image coordinates)
 * and if there is still ambiguity the ellipse closest to the image origin will be selected as (0,0).</p>
 *
 * @author Peter Abeles
 */
public class DetectCircleRegularGrid<T extends ImageGray<T>> extends DetectCircleGrid<T> {


	/**
	 * Creates and configures the detector
	 *
	 * @param numRows number of rows in grid
	 * @param numCols number of columns in grid
	 * @param inputToBinary Converts the input image into a binary image
	 * @param ellipseDetector Detects ellipses inside the image
	 * @param clustering Finds clusters of ellipses
	 */
	public DetectCircleRegularGrid(int numRows, int numCols,
								   InputToBinary<T> inputToBinary,
								   BinaryEllipseDetector<T> ellipseDetector,
								   EllipsesIntoClusters clustering) {
		super(numRows,numCols,inputToBinary,ellipseDetector,clustering,
				new EllipseClustersIntoRegularGrid());
	}

	/**
	 * Puts the grid into a canonical orientation
	 */
	@Override
	protected void putGridIntoCanonical(Grid g ) {
		// first put it into a plausible solution
		if( g.columns != numCols ) {
			rotateGridCCW(g);
		}

		if( isClockWise(g)) {
			flipHorizontal(g);
		}
	}
}
