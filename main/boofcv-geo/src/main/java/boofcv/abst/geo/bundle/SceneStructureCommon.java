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

package boofcv.abst.geo.bundle;

import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Base class for implementations of {@link SceneStructure}. Contains data structures
 * common to all implementations
 *
 * @author Peter Abeles
 */
public abstract class SceneStructureCommon implements SceneStructure {
	public Point[] points;
	/**
	 * True if homogenous coordinates are being used
	 */
	public boolean homogenous;
	// number of elements in a point. Will be 3 or 4
	protected int pointSize;

	public SceneStructureCommon(boolean homogenous) {
		this.homogenous = homogenous;
		pointSize = homogenous ? 4 : 3;
	}

	/**
	 * Specifies the location of a point in 3D space
	 * @param which Which point is being specified
	 * @param x coordinate along x-axis
	 * @param y coordinate along y-axis
	 * @param z coordinate along z-axis
	 */
	public void setPoint( int which , double x , double y , double z ) {
		points[which].set(x,y,z);
	}

	/**
	 * Specifies the location of a point as a 3D homogenous coordinate
	 * @param which Which point is being specified
	 * @param x coordinate along x-axis
	 * @param y coordinate along y-axis
	 * @param z coordinate along z-axis
	 * @param w w-coordinate
	 */
	public void setPoint( int which , double x , double y , double z , double w) {
		points[which].set(x,y,z,w);
	}

	/**
	 * Specifies that the point was observed in this view.
	 * @param pointIndex index of point
	 * @param viewIndex index of view
	 */
	public void connectPointToView( int pointIndex , int viewIndex ) {
		SceneStructureMetric.Point p = points[pointIndex];

		for (int i = 0; i < p.views.size; i++) {
			if( p.views.data[i] == viewIndex )
				throw new IllegalArgumentException("Tried to add the same view twice");
		}
		p.views.add(viewIndex);
	}

	public SceneStructureMetric.Point[] getPoints() {
		return points;
	}

	/**
	 * Removes the points specified in 'which' from the list of points. 'which' must be ordered
	 * from lowest to highest index.
	 *
	 * @param which Ordered list of point indexes to remove
	 */
	public void removePoints( GrowQueue_I32 which ) {
		SceneStructureMetric.Point results[] = new SceneStructureMetric.Point[points.length-which.size];

		int indexWhich = 0;
		for (int i = 0; i < points.length ; i++) {
			if( indexWhich < which.size && which.data[indexWhich] == i ) {
				indexWhich++;
			} else {
				results[i-indexWhich] = points[i];
			}
		}

		points = results;
	}

	public static class Point {

		/**
		 * Where the point is in 3D or 4D space
		 */
		public double coordinate[];

		/**
		 * Indexes of the views that this point appears in
		 */
		public GrowQueue_I32 views = new GrowQueue_I32();

		public Point( int dof ) {
			coordinate = new double[dof];
		}

		/**
		 * Removes the specified view from the list of views. If it's not contained in the list
		 * an exception is thrown
		 * @param which Index of the view which is to be removed
		 */
		public void removeView( int which ) {
			int index = views.indexOf(which);
			if( index == -1 )
				throw new RuntimeException("BUG. Could not find in list of views. which="+which);
			views.remove(index);
		}

		public void set( double x , double y , double z ) {
			coordinate[0] = x;
			coordinate[1] = y;
			coordinate[2] = z;
		}

		public void set( double x , double y , double z , double w ) {
			coordinate[0] = x;
			coordinate[1] = y;
			coordinate[2] = z;
			coordinate[3] = w;
		}

		public void get( Point3D_F64 p ) {
			p.x = coordinate[0];
			p.y = coordinate[1];
			p.z = coordinate[2];
		}

		public void get(Point4D_F64 p ) {
			p.x = coordinate[0];
			p.y = coordinate[1];
			p.z = coordinate[2];
			p.w = coordinate[3];
		}

		public double getX() {
			return coordinate[0];
		}
		public double getY() {
			return coordinate[1];
		}
		public double getZ() {
			return coordinate[2];
		}
		public double getW() {
			return coordinate[3];
		}

		public double distanceSq(Point3D_F64 p) {
			double dx = coordinate[0] - p.x;
			double dy = coordinate[1] - p.y;
			double dz = coordinate[2] - p.z;

			return dx*dx + dy*dy + dz*dz;
		}

		public double distance(Point3D_F64 p) {
			return Math.sqrt(distance(p));
		}

		public double distance(Point p) {
			double dx = coordinate[0] - p.coordinate[0];
			double dy = coordinate[1] - p.coordinate[1];
			double dz = coordinate[2] - p.coordinate[2];

			return Math.sqrt(dx*dx + dy*dy + dz*dz);
		}
	}

	@Override
	public boolean isHomogenous() {
		return homogenous;
	}
}
