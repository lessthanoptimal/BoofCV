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

package boofcv.alg.fiducial.calib.circle;

import boofcv.abst.filter.binary.BinaryLabelContourFinder;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.Grid;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.struct.image.ImageGray;
import georegression.struct.curve.EllipseRotated_F64;

/**
 * <p>Detects regular grids of circles, see below.  A valid grid is in counter-clockwise order and if there are
 * multiple possible solution the solution with corner (0,0) closest to the pixel coordinate (0,0) is selected</p>
 *
 * <p>
 * For each circle there is are four control points.  Each control point corresponds to the tangent line connecting
 * the vertical and horizontal neigbors.  Tangent points are used since they are invariant under
 * perspective distortion.
 * </p>
 *
 * <center>
 * <img src="doc-files/regularcirclegrid.jpg"/>
 * </center>
 * Example of a 6 by 4 grid; row, column.
 *
 * @author Peter Abeles
 */
public class DetectCircleRegularGrid<T extends ImageGray<T>> extends DetectCircleGrid<T> {

	// storage for the distance of each corner
	private double closestCorner[] = new double[4];

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

	@Override
	protected void configureContourDetector(T gray) {
		// overestimate the max diameter by not taking in account space between the circles
		int diameter = Math.max(gray.width,gray.height)/Math.max(numCols,numRows);

		BinaryLabelContourFinder contourFinder = ellipseDetector.getEllipseDetector().getContourFinder();
		contourFinder.setMaxContour((int)(Math.PI*diameter)*2);
		contourFinder.setSaveInnerContour(false);
	}

	@Override
	public int totalEllipses( int numRows , int numCols ) {
		return numRows*numCols;
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

		// pick the solutin which puts (0,0) coordinate the closest to the top left corner to resolve ambiguity
		if( g.columns == g.rows ) {
			closestCorner[0] = g.get(0,0).center.normSq();
			closestCorner[1] = g.get(numRows-1,0).center.normSq();
			closestCorner[2] = g.get(numRows-1,numCols-1).center.normSq();
			closestCorner[3] = g.get(0,numCols-1).center.normSq();

			int best = 0;
			for (int i = 0; i < 4; i++) {
				if( closestCorner[i] < closestCorner[best]) {
					best = i;
				}
			}

			for (int i = 0; i < best; i++) {
				rotateGridCCW(g);
			}
		} else {
			double d00 = g.get(0,0).center.normSq();
			double d11 = g.get(numRows-1,numCols-1).center.normSq();

			if( d11 < d00 ) {
				rotateGridCCW(g);
				rotateGridCCW(g);
			}
		}
	}

	/**
	 * Uses the cross product to determine if the grid is in clockwise order
	 */
	private static boolean isClockWise( Grid g ) {
		EllipseRotated_F64 v00 = g.get(0,0);
		EllipseRotated_F64 v02 = g.get(0,1);
		EllipseRotated_F64 v20 = g.get(1,0);

		double a_x = v02.center.x - v00.center.x;
		double a_y = v02.center.y - v00.center.y;

		double b_x = v20.center.x - v00.center.x;
		double b_y = v20.center.y - v00.center.y;

		return a_x * b_y - a_y * b_x < 0;
	}
}
