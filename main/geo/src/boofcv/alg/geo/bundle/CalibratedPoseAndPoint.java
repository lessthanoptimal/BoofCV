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

package boofcv.alg.geo.bundle;

import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

/**
 * Expanded model for fast computations used by bundle adjustment with calibrated cameras.
 * Designed to minimize unnecessary creating and destroying memory.
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

	public void setViewKnown( int view , boolean known ) {
		viewKnown[view] = known;
	}

	public boolean isViewKnown( int view ) {
		return viewKnown[view];
	}

	public Se3_F64 getWorldToCamera( int view ) {
		return worldToCamera[view];
	}

	public Point3D_F64 getPoint( int index ) {
		return points[index];
	}

	public int getNumPoints() {
		return numPoints;
	}

	public int getNumViews() {
		return numViews;
	}

	public int getNumUnknownViews() {
		int ret = 0;
		for( int i = 0; i < numViews; i++ ) {
			if( !viewKnown[i] )
				ret++;
		}
		return ret;
	}

	public boolean[] getKnownArray() {
		return viewKnown;
	}
}
