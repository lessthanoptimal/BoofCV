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

package boofcv.alg.fiducial.calib.circle;

import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoAsymmetricGrid.Grid;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Computes key points from an observed asymmetric circular grid.  Each key point is defined as the center's true
 * geometric center.  The center is found by detecting tangent points between two neighboring circles (red dots) and
 * then finding the closest point (green circle) to their lines (yellow).  Tangent points are the same under perspective
 * distortion and the same can be said for the intersection of their lines.</p>
 *
 * <center>
 * <img src="doc-files/asymcircle_tangent_intersections.png"/>
 * </center>
 *
 * @author Peter Abeles
 */
public class AsymmetricGridKeyPoints {

	// fiducial reference frame locaiton
	List<Point2D_F64> location = new ArrayList<>();
	// detected location
	List<Point2D_F64> detected = new ArrayList<>();

	public AsymmetricGridKeyPoints( int numRows , int numCols ,
									double radius , double separation ) {

		// compute the number of nodes in the grid
		int outRows = numRows - numRows/2;
		int outCols = numCols - numCols/2;

		int inRows = numRows - outRows;
		int inCols = numCols - outCols;

		int N = outRows*outCols + inRows*inCols;
		// TODO this number is wrong.
		for (int i = 0; i < N; i++) {
			location.add( new Point2D_F64());
			detected.add( new Point2D_F64());
		}

		// create circles at the
		computePoints(createFiducialGrid(numRows,numCols,radius,separation), location);
	}

	private Grid createFiducialGrid(int numRows , int numCols , double radius , double separation) {
		return null;
	}

	public void process(Grid grid ) {
		computePoints(grid, detected);
	}

	public void computePoints( Grid g , List<Point2D_F64> locations ) {
		// horizontal

		// vertical

		// diagonal
	}

	/**
	 * Returns the location of each key point in the fiducial reference frame
	 * @return locations in fiducial frame
	 */
	public List<Point2D_F64> getLocation() {
		return location;
	}

	/**
	 * Returns the location of each key point in the image from the most recently processed grid.
	 * @return detected image location
	 */
	public List<Point2D_F64> getDetected() {
		return detected;
	}
}
