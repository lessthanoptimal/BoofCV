/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle;

import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

/**
 * Expanded model for fast computations used by bundle adjustment with calibrated cameras.
 * Designed to minimize unnecessary creating and destroying memory.  Each view can be marked as known or not.
 * If a view is known then initial pose is fixed and will not be optimized.  The list of points is a list
 * of all points which have been observed across all views.  Both views and points are referenced by
 * their index.  Before the class is used you must call {@link #configure(int, int)} to specif the number
 * of views and points.
 *
 * @author Peter Abeles
 */
public class CalibratedPoseAndPoint {

	// number of camera views
	private int numViews;
	// total number of 3D points being viewed
	private int numPoints;

	// if true the worldToCamera is assumed to be known
	private boolean[] viewKnown = new boolean[0];
	// transformation from world to camera frame
	private Se3_F64[] worldToCamera = new Se3_F64[0];
	// location of each point in 3D space, world coordinate
	private Point3D_F64[] points = new Point3D_F64[0];

	/**
	 * Specifies the number of views and 3D points being estimated
	 *
	 * @param numViews Number of camera views observing the points.
	 * @param numPoints Number of points observed
	 */
	public void configure(int numViews, int numPoints) {
		if( worldToCamera.length < numViews ) {
			Se3_F64 temp[] = new Se3_F64[numViews];
			System.arraycopy(worldToCamera, 0, temp, 0, worldToCamera.length);
			for( int i = worldToCamera.length; i < temp.length; i++ ) {
				temp[i] = new Se3_F64();
			}
			worldToCamera = temp;

			viewKnown = new boolean[numViews];
		}

		if( points.length < numPoints ) {
			Point3D_F64 temp[] = new Point3D_F64[numPoints];
			System.arraycopy(points, 0, temp, 0, points.length);
			for( int i = points.length; i < temp.length; i++ ) {
				temp[i] = new Point3D_F64();
			}
			points = temp;
		}

		this.numPoints = numPoints;
		this.numViews = numViews;

		for( int i = 0; i < numViews; i++ ) {
			viewKnown[i] = false;
		}
	}

	/**
	 * Specify if a view is assumed to have a known view or not.
	 * @param view Index of the view
	 * @param known true of known or false if not.
	 */
	public void setViewKnown( int view , boolean known ) {
		viewKnown[view] = known;
	}

	/**
	 * Used to see if a particular view is marked as known or not
	 *
	 * @param view The view's index
	 * @return if true then the view's pose is assumed to be known and is not optimized
	 */
	public boolean isViewKnown( int view ) {
		return viewKnown[view];
	}

	/**
	 * Transform from world to camera view
	 * @param view The view's index
	 * @return The transform
	 */
	public Se3_F64 getWorldToCamera( int view ) {
		return worldToCamera[view];
	}

	/**
	 * Returns the location of a specific point/feature
	 * @param index Index of the point
	 * @return The point's location
	 */
	public Point3D_F64 getPoint( int index ) {
		return points[index];
	}

	/**
	 * The number of points (or features) whose location is being optimized.
	 *
	 * @return number of points
	 */
	public int getNumPoints() {
		return numPoints;
	}

	/**
	 * The number of camera views which observed the points/features
	 * @return Number of views
	 */
	public int getNumViews() {
		return numViews;
	}

	/**
	 * Returns the number of camera views which do not have a 'known' or fixed pose.
	 * @return Number of views whose pose is to be optimized.
	 */
	public int getNumUnknownViews() {
		int ret = 0;
		for( int i = 0; i < numViews; i++ ) {
			if( !viewKnown[i] )
				ret++;
		}
		return ret;
	}

	/**
	 * An array that indicates which views are known and which views are not
	 */
	public boolean[] getKnownArray() {
		return viewKnown;
	}
}
