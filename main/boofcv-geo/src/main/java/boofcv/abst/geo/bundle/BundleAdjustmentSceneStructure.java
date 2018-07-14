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

import boofcv.alg.geo.bundle.cameras.BundleAdjustmentPinhole;
import boofcv.alg.geo.bundle.cameras.BundleAdjustmentPinholeRadial;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Specifies the scene which will be optimized using Bundle Adjustment. Specifically, the initial parameters of
 * and the relationships between cameras, views, and points. A camera projects a 3D point onto the image plane.
 * Examples of specific models include pinhole and fisheye. A view is a set of observed features from a specific
 * camera image. Views have an associated camera and specify the pose of the camera when the scene was viewed. Points
 * describe the scene's 3D structure.
 *
 *
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentSceneStructure {

	public Camera[] cameras;
	public View[] views;
	public Point[] points;
	/**
	 * True if homogenous coordinates are being used
	 */
	public boolean homogenous;
	// number of elements in a point. Will be 3 or 4
	private int pointSize;

	/**
	 * Configure bundle adjustment
	 * @param homogenous if true then homogeneous coordinates are used
	 */
	public BundleAdjustmentSceneStructure(boolean homogenous) {
		this.homogenous = homogenous;
		pointSize = homogenous ? 4 : 3;
	}

	/**
	 * Call this funciton first. Specifies number of each type of data which is available.
	 *
	 * @param totalCameras Number of cameras
	 * @param totalViews Number of views
	 * @param totalPoints Number of points
	 */
	public void initialize( int totalCameras , int totalViews , int totalPoints ) {
		cameras = new Camera[totalCameras];
		views = new View[totalViews];
		points = new Point[totalPoints];

		for (int i = 0; i < cameras.length; i++) {
			cameras[i] = new Camera();
		}
		for (int i = 0; i < views.length; i++) {
			views[i] = new View();
		}
		for (int i = 0; i < points.length; i++) {
			points[i] = new Point(pointSize);
		}
	}

	/**
	 * Specifies the camera model being used.
	 * @param which Which camera is being specified
	 * @param fixed If these parameters are constant or not
	 * @param model The camera model
	 */
	public void setCamera(int which , boolean fixed , BundleAdjustmentCamera model  ) {
		cameras[which].known = fixed;
		cameras[which].model = model;
	}

	public void setCamera( int which , boolean fixed , CameraPinhole intrinsic ) {
		setCamera(which,fixed,new BundleAdjustmentPinhole(intrinsic));
	}

	public void setCamera( int which , boolean fixed , CameraPinholeRadial intrinsic ) {
		setCamera(which,fixed,new BundleAdjustmentPinholeRadial(intrinsic));
	}

	/**
	 * Specifies the spacial transform for a view.
	 * @param which Which view is being specified/
	 * @param fixed If these parameters are fixed or not
	 * @param worldToView The transform from world to view reference frames
	 */
	public void setView(int which , boolean fixed , Se3_F64 worldToView ) {
		views[which].known = fixed;
		views[which].worldToView.set(worldToView);
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
	 * Specifies that the view uses the specified camera
	 * @param viewIndex index of view
	 * @param cameraIndex index of camera
	 */
	public void connectViewToCamera( int viewIndex , int cameraIndex ) {
		if( views[viewIndex].camera != -1 )
			throw new RuntimeException("View has already been assigned a camera");
		views[viewIndex].camera = cameraIndex;
	}

	/**
	 * Specifies that the point was observed in this view.
	 * @param pointIndex index of point
	 * @param viewIndex index of view
	 */
	public void connectPointToView( int pointIndex , int viewIndex ) {
		Point p = points[pointIndex];

		for (int i = 0; i < p.views.size; i++) {
			if( p.views.data[i] == viewIndex )
				throw new IllegalArgumentException("Tried to add the same view twice");
		}
		p.views.add(viewIndex);
	}

	/**
	 * Returns the number of cameras with parameters that are not fixed
	 * @return non-fixed camera count
	 */
	public int getUnknownCameraCount() {
		int total = 0;
		for (int i = 0; i < cameras.length; i++) {
			if( !cameras[i].known) {
				total++;
			}
		}
		return total;
	}

	/**
	 * Returns the number of view with parameters that are not fixed
	 * @return non-fixed view count
	 */
	public int getUnknownViewCount() {
		int total = 0;
		for (int i = 0; i < views.length; i++) {
			if( !views[i].known) {
				total++;
			}
		}
		return total;
	}

	/**
	 * Counts the total number of unknown camera parameters that will be optimized/
	 *
	 * @return Number of parameters
	 */
	public int getUnknownCameraParameterCount() {
		int total = 0;
		for (int i = 0; i < cameras.length; i++) {
			if( !cameras[i].known) {
				total += cameras[i].model.getParameterCount();
			}
		}
		return total;
	}

	/**
	 * Returns the total number of parameters which will be optimised
	 * @return number of parameters
	 */
	public int getParameterCount() {
		return getUnknownViewCount()*6 + points.length*3 + getUnknownCameraParameterCount();
	}

	public Camera[] getCameras() {
		return cameras;
	}

	public View[] getViews() {
		return views;
	}

	public Point[] getPoints() {
		return points;
	}

	/**
	 * Removes the points specified in which from the list of points. 'which' must be ordered
	 * from lowest to highest index.
	 *
	 * @param which Ordered list of point indexes to remove
	 */
	public void removePoints( GrowQueue_I32 which ) {
		Point results[] = new Point[points.length-which.size];

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

	public static class Camera {
		/**
		 * If the parameters are assumed to be known and should not be optimised.
		 */
		public boolean known = true;
		public BundleAdjustmentCamera model;
	}

	public static class View {
		/**
		 * If the parameters are assumed to be known and should not be optimised.
		 */
		public boolean known = true;
		/**
		 * Transform from this view to the world
		 */
		public Se3_F64 worldToView = new Se3_F64();
		/**
		 * The camera associated with this view
		 */
		public int camera = -1;
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
				throw new RuntimeException("BUG");
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

		public double distance(Point3D_F64 p) {
			double dx = coordinate[0] - p.x;
			double dy = coordinate[1] - p.y;
			double dz = coordinate[2] - p.z;

			return Math.sqrt(dx*dx + dy*dy + dz*dz);
		}

		public double distance(Point p) {
			double dx = coordinate[0] - p.coordinate[0];
			double dy = coordinate[1] - p.coordinate[1];
			double dz = coordinate[2] - p.coordinate[2];

			return Math.sqrt(dx*dx + dy*dy + dz*dz);
		}
	}

}

