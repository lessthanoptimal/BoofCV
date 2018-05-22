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

import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.Grid;
import georegression.geometry.algs.TangentLinesTwoEllipses_F64;
import georegression.misc.GrlConstants;
import georegression.struct.curve.EllipseRotated_F64;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

/**
 * <p>Computes key points from an observed regular circular grid.  Each circle has 4 key points at the grid aligned
 * top, bottom, left, and right side of the circle.  These key points are found using tangent points between
 * adjacent circles.  Tangent points are the same under perspective
 * distortion and the same can be said for the intersection of their lines.  When more than one intersection
 * is at the same location the average is found</p>
 *
 * <center>
 * <img src="doc-files/regcircle_tangent_intersections.png"/>
 * </center>
 *
 * @author Peter Abeles
 */
public class KeyPointsCircleRegularGrid {

	// tangent points on each ellipse
	FastQueue<Tangents> tangents = new FastQueue<>(Tangents.class,true);

	// detected location
	FastQueue<Point2D_F64> keypoints = new FastQueue<>(Point2D_F64.class,true);

	// used to compute tangent lines between two ellipses
	private TangentLinesTwoEllipses_F64 tangentFinder = new TangentLinesTwoEllipses_F64(GrlConstants.TEST_F64,10);

	// storage for tangent points on ellipses
	private Point2D_F64 A0 = new Point2D_F64(); private Point2D_F64 A1 = new Point2D_F64();
	private Point2D_F64 A2 = new Point2D_F64(); private Point2D_F64 A3 = new Point2D_F64();
	private Point2D_F64 B0 = new Point2D_F64(); private Point2D_F64 B1 = new Point2D_F64();
	private Point2D_F64 B2 = new Point2D_F64(); private Point2D_F64 B3 = new Point2D_F64();

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

		keypoints.reset();
		for (int i = 0; i < tangents.size(); i++) {
			tangents.get(i).getTop(keypoints.grow());
			tangents.get(i).getRight(keypoints.grow());
			tangents.get(i).getBottom(keypoints.grow());
			tangents.get(i).getLeft(keypoints.grow());
		}

		return true;
	}

	void init(Grid grid) {
		tangents.resize(grid.ellipses.size());
		for (int i = 0; i < tangents.size(); i++) {
			tangents.get(i).reset();
		}
	}

	boolean horizontal(Grid grid) {
		for (int i = 0; i < grid.rows; i++) {
			for (int j = 0; j < grid.columns-1; j++) {

				if (!addTangents(grid, i, j, i, j+1))
					return false;
			}
		}

		return true;
	}

	boolean vertical(Grid grid) {
		for (int i = 0; i < grid.rows-1; i++) {
			for (int j = 0; j < grid.columns; j++) {
				if (!addTangents(grid, i, j, i+1, j))
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
		Tangents ta = tangents.get(grid.getIndexOfRegEllipse(rowA,colA));
		Tangents tb = tangents.get(grid.getIndexOfRegEllipse(rowB,colB));

		// Which point is 0 or 3 is not defined and can swap arbitrarily.  To fix this problem
		// 0 will be defined as on the 'positive side' of the line connecting the ellipse centers

		double slopeX = b.center.x-a.center.x;
		double slopeY = b.center.y-a.center.y;

		double dx0 = A0.x-a.center.x;
		double dy0 = A0.y-a.center.y;

		double z = slopeX*dy0 - slopeY*dx0;
		if( z < 0 == (rowA == rowB)) {
			Point2D_F64 tmp = A0; A0 = A3; A3 = tmp;
			tmp = B0; B0 = B3; B3 = tmp;
		}

		// add tangent points from the two lines which do not cross the center line
		if( rowA == rowB ) {
			ta.t[ta.countT++].set(A0);
			ta.b[ta.countB++].set(A3);
			tb.t[tb.countT++].set(B0);
			tb.b[tb.countB++].set(B3);
		} else {
			ta.r[ta.countL++].set(A0);
			ta.l[ta.countR++].set(A3);
			tb.r[tb.countL++].set(B0);
			tb.l[tb.countR++].set(B3);
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

	public static class Tangents {

		// top bottom left right
		public Point2D_F64 t[] = new Point2D_F64[2];
		public Point2D_F64 b[] = new Point2D_F64[2];
		public Point2D_F64 l[] = new Point2D_F64[2];
		public Point2D_F64 r[] = new Point2D_F64[2];

		int countT = 0;
		int countB = 0;
		int countL = 0;
		int countR = 0;

		public Tangents() {
			for (int i = 0; i < 2; i++) {
				t[i] = new Point2D_F64();
				b[i] = new Point2D_F64();
				l[i] = new Point2D_F64();
				r[i] = new Point2D_F64();
			}
		}

		public void reset() {
			countT = countB = countL = countR = 0;
		}

		public void getTop( Point2D_F64 top ) {assign(t,countT,top);}

		public void getBottom( Point2D_F64 p ) {
			assign(b,countB,p);
		}

		public void getLeft( Point2D_F64 p ) {
			assign(l,countL,p);
		}

		public void getRight( Point2D_F64 p ) {
			assign(r,countR,p);
		}


		private void assign( Point2D_F64 array[] , int length , Point2D_F64 output ) {
			if( length == 1 ) {
				output.set(array[0]);
			} else {
				output.x = (array[0].x + array[1].x)/2.0;
				output.y = (array[0].y + array[1].y)/2.0;
			}
		}
	}
}
