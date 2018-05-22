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
 * <p>Detects a hexagonal circle grid. Circles are spaced such that center of each circle is the same distance from
 * each of its five neighbors. Rows and columns are counted in a zig-zag pattern, see example below. When there
 * is symmetric ambiguity the canonical orientation is used.</p>
 *
 * <p>Canonical orientation is defined as having the rows/columns matched, element (0,0) being occupied.
 * If there are multiple solution a solution will be selected which is in counter-clockwise order (image coordinates)
 * and if there is still ambiguity the ellipse closest to the image origin will be selected as (0,0).</p>
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
 * <img src="doc-files/hexagonal_grid.png"/>
 * </center>
 * Example of a 24 by 28 grid; row, column.
 *
 * @see KeyPointsCircleHexagonalGrid
 *
 * @author Peter Abeles
 */
public class DetectCircleHexagonalGrid<T extends ImageGray<T>> extends DetectCircleGrid<T> {

	/**
	 * Creates and configures the detector
	 *
	 * @param numRows number of rows in grid
	 * @param numCols number of columns in grid
	 * @param inputToBinary Converts the input image into a binary image
	 * @param ellipseDetector Detects ellipses inside the image
	 * @param clustering Finds clusters of ellipses
	 */
	public DetectCircleHexagonalGrid(int numRows, int numCols,
									 InputToBinary<T> inputToBinary,
									 BinaryEllipseDetector<T> ellipseDetector,
									 EllipsesIntoClusters clustering) {
		super(numRows,numCols,inputToBinary,ellipseDetector, clustering,
				new EllipseClustersIntoHexagonalGrid());
	}

	@Override
	protected void configureContourDetector(T gray) {
		// overestimate for multiple reasons. Doesn't take in account space and distance between touching circles
		// isn't correct
		int diameter = Math.max(gray.width,gray.height)/(Math.max(numCols,numRows));

		BinaryLabelContourFinder contourFinder = ellipseDetector.getEllipseDetector().getContourFinder();
		contourFinder.setMaxContour((int)(Math.PI*diameter*3)+1);
		contourFinder.setSaveInnerContour(false);
	}

	@Override
	public int totalEllipses( int numRows , int numCols ) {
		return (numRows/2)*(numCols/2) + ((numRows+1)/2)*((numCols+1)/2);
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

		if( g.get(0,0) == null ) {
			reverse(g);
		}

		// select the best corner for canonical
		if( g.columns%2 == 1 && g.rows%2 == 1) {
			// first make sure orientation constraint is maintained
			if( isClockWise(g)) {
				flipHorizontal(g);
			}

			int numRotationsCCW = closestCorner4(g);
			if( g.columns == g.rows ) {
				for (int i = 0; i < numRotationsCCW; i++) {
					rotateGridCCW(g);
				}
			} else if( numRotationsCCW == 2 ){
				// only two valid solutions.  rotate only if the other valid solution is better
				rotateGridCCW(g);
				rotateGridCCW(g);
			}
		} else if( g.columns%2 == 1 ) {
			// only two solutions.  Go with the one which maintains orientation constraint
			if( isClockWise(g)) {
				flipHorizontal(g);
			}
		} else if( g.rows%2 == 1 ) {
			// only two solutions.  Go with the one which maintains orientation constraint
			if( isClockWise(g)) {
				flipVertical(g);
			}
		}
	}

	/**
	 * Uses the cross product to determine if the grid is in clockwise order
	 */
	private static boolean isClockWise( Grid g ) {
		EllipseRotated_F64 v00 = g.get(0,0);
		EllipseRotated_F64 v02 = g.columns<3?g.get(1,1):g.get(0,2);
		EllipseRotated_F64 v20 = g.rows<3?g.get(1,1):g.get(2,0);

		double a_x = v02.center.x - v00.center.x;
		double a_y = v02.center.y - v00.center.y;

		double b_x = v20.center.x - v00.center.x;
		double b_y = v20.center.y - v00.center.y;

		return a_x * b_y - a_y * b_x < 0;
	}
}
