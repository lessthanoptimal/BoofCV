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
import georegression.geometry.UtilLine2D_F64;
import georegression.geometry.UtilVector2D_F64;
import georegression.geometry.algs.TangentLinesTwoEllipses_F64;
import georegression.metric.Intersection2D_F64;
import georegression.misc.GrlConstants;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import org.ddogleg.struct.FastQueue;

import static boofcv.alg.fiducial.calib.circle.DetectAsymmetricCircleGrid.totalEllipses;

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
public class AsymmetricGridKeyPointDetections {

	// tangent points on each ellipse
	FastQueue<Tangents> tangents = new FastQueue<>(Tangents.class,true);

	// detected location
	FastQueue<Point2D_F64> keypoints = new FastQueue<>(Point2D_F64.class,true);

	// used to compute tangent lines between two ellipses
	private TangentLinesTwoEllipses_F64 tangentFinder = new TangentLinesTwoEllipses_F64(GrlConstants.DOUBLE_TEST_TOL,10);

	// storage for tangent points on ellipses
	private Point2D_F64 A0 = new Point2D_F64(); private Point2D_F64 A1 = new Point2D_F64();
	private Point2D_F64 A2 = new Point2D_F64(); private Point2D_F64 A3 = new Point2D_F64();
	private Point2D_F64 B0 = new Point2D_F64(); private Point2D_F64 B1 = new Point2D_F64();
	private Point2D_F64 B2 = new Point2D_F64(); private Point2D_F64 B3 = new Point2D_F64();

	// local work space for center of intersections
	private LineGeneral2D_F64 lineA = new LineGeneral2D_F64();
	private LineGeneral2D_F64 lineB = new LineGeneral2D_F64();
	private Point2D_F64 location = new Point2D_F64();

	/**
	 * Computes key points from the grid of ellipses
	 * @param grid Grid of ellipses
	 * @return true if successful or false if it failed
	 */
	public boolean process(Grid grid ) {
		// reset and initialize data structures
		init(grid);

		// add tangent points from adjacent ellipses
		if( !horizontal(grid) )
			return false;
		if( !vertical(grid) )
			return false;
		if( !diagonalLR(grid) )
			return false;
		if( !diagonalRL(grid) )
			return false;

		return computeEllipseCenters();
	}

	void init(Grid grid) {
		tangents.resize(totalEllipses(grid.rows,grid.columns));
		for (int i = 0; i < tangents.size(); i++) {
			tangents.get(i).reset();
		}
	}

	boolean horizontal(Grid grid) {
		for (int i = 0; i < grid.rows; i++) {
			for (int j = 0; j < grid.columns-2; j++) {
				if( i%2==0 && j%2==1 ) continue;
				if( i%2==1 && j%2==0 ) continue;

				if (!addTangents(grid, i, j, i, j+2))
					return false;
			}
		}

		return true;
	}

	boolean vertical(Grid grid) {
		for (int i = 0; i < grid.rows-2; i++) {
			for (int j = 0; j < grid.columns; j++) {
				if( i%2==0 && j%2==1 ) continue;
				if( i%2==1 && j%2==0 ) continue;

				if (!addTangents(grid, i, j, i+2, j))
					return false;
			}
		}

		return true;
	}

	boolean diagonalLR(Grid grid) {
		for (int i = 0; i < grid.rows-1; i++) {
			for (int j = 0; j < grid.columns-1; j++) {
				if( i%2==0 && j%2==1 ) continue;
				if( i%2==1 && j%2==0 ) continue;

				if (!addTangents(grid, i, j, i+1, j+1))
					return false;
			}
		}

		return true;
	}

	boolean diagonalRL(Grid grid) {
		for (int i = 0; i < grid.rows-1; i++) {
			for (int j = 1; j < grid.columns; j++) {
				if( i%2==0 && j%2==1 ) continue;
				if( i%2==1 && j%2==0 ) continue;

				if (!addTangents(grid, i, j, i+1, j-1))
					return false;
			}
		}

		return true;
	}

	/**
	 * Computes tangent points to the two ellipses specified by the grid coordinates
	 */
	private boolean addTangents(Grid grid, int rowA, int colA, int rowB, int colB) {
		EllipseRotated_F64 a = grid.get(rowA,colA);
		EllipseRotated_F64 b = grid.get(rowB,colB);

		if( !tangentFinder.process(a,b, A0, A1, A2, A3, B0, B1, B2, B3) ) {
			return false;
		}
		Tangents ta = tangents.get(grid.getIndexOfEllipse(rowA,colA));
		Tangents tb = tangents.get(grid.getIndexOfEllipse(rowB,colB));

		// add tangent points from the two lines which do not cross the center line
		ta.grow().set(A0);
		ta.grow().set(A3);

		tb.grow().set(B0);
		tb.grow().set(B3);
		return true;
	}

	/**
	 * Finds the intersection of all the tangent lines with each other the computes the average of those points.
	 * That location is where the center is set to.  Each intersection of lines is weighted by the acute angle.
	 * lines which are 90 degrees to each other are less sensitive to noise
	 */
	boolean computeEllipseCenters() {
		keypoints.reset();

		for (int tangentIdx = 0; tangentIdx < tangents.size(); tangentIdx++) {
//			System.out.println("tangent id "+tangentIdx);
			Tangents t = tangents.get(tangentIdx);
			Point2D_F64 center = keypoints.grow();
			center.set(0,0);
			double totalWeight = 0;

			for (int i = 0; i < t.size(); i += 2) {
				UtilLine2D_F64.convert(t.get(i),t.get(i+1),lineA);

				for (int j = i+2; j < t.size(); j += 2) {
					UtilLine2D_F64.convert(t.get(j),t.get(j+1),lineB);

					// way each intersection based on the acute angle.  lines which are nearly parallel will
					// be unstable estimates
					double w = UtilVector2D_F64.acute(lineA.A,lineA.B,lineB.A,lineB.B);
					if( w > Math.PI/2.0 )
						w = Math.PI-w;

					// If there is perfect data and no noise there will be duplicated lines.  With noise there will
					// be very similar lines
					if( w <= 0.02 )
						continue;

					if( null == Intersection2D_F64.intersection(lineA,lineB, location) ) {
						return false;
					}

//					System.out.printf("   %4.2f loc %6.2f %6.2f\n",w,location.x,location.y);
					center.x += location.x*w;
					center.y += location.y*w;

					totalWeight += w;
				}
			}

			if( totalWeight == 0 )
				return false;

			center.x /= totalWeight;
			center.y /= totalWeight;
		}

		return true;
	}

	/**
	 * Returns the location of each key point in the image from the most recently processed grid.
	 * @return detected image location
	 */
	public FastQueue<Point2D_F64> getKeyPoints() {
		return keypoints;
	}

	public FastQueue<Tangents> getTangents() {
		return tangents;
	}

	public static class Tangents extends FastQueue<Point2D_F64> {
		public Tangents() {
			super(8, Point2D_F64.class, true);
		}
	}
}
