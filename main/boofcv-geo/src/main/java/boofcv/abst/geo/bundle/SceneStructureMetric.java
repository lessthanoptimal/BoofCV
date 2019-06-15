/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.bundle.cameras.BundlePinhole;
import boofcv.alg.geo.bundle.cameras.BundlePinholeBrown;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.FastQueue;

/**
 * Specifies a metric (calibrated) scene for optimizing using {@link BundleAdjustment}.
 * It specifies the relationships between cameras, views, and points. A camera projects a 3D point onto the image plane.
 * Examples of specific models include pinhole and fisheye. A view is a set of observed features from a specific
 * camera image. Views have an associated camera and specify the pose of the camera when the scene was viewed. Points
 * describe the scene's 3D structure.
 *
 * Points belonging to the general scene and rigid objects have two different sets of ID's.
 *
 * @author Peter Abeles
 */
public class SceneStructureMetric extends SceneStructureCommon {

	public FastQueue<Camera> cameras = new FastQueue<>(Camera.class,true);
	public FastQueue<View> views = new FastQueue<>(View.class,true);

	// data structures for rigid objects.
	public FastQueue<Rigid> rigids = new FastQueue<>(Rigid.class,true);
	// Lookup table from rigid point to rigid object
	public int[] lookupRigid;

	/**
	 * Configure bundle adjustment
	 * @param homogenous if true then homogeneous coordinates are used
	 */
	public SceneStructureMetric(boolean homogenous) {
		super(homogenous);
	}

	/**
	 * Call this function first. Specifies number of each type of data which is available.
	 *
	 * @param totalCameras Number of cameras
	 * @param totalViews Number of views
	 * @param totalPoints Number of points
	 */
	public void initialize( int totalCameras , int totalViews , int totalPoints ) {
		initialize(totalCameras,totalViews,totalPoints,0);
	}

	/**
	 * Call this function first. Specifies number of each type of data which is available.
	 *
	 * @param totalCameras Number of cameras
	 * @param totalViews Number of views
	 * @param totalPoints Number of points
	 * @param totalRigid Number of rigid objects
	 */
	public void initialize( int totalCameras , int totalViews , int totalPoints , int totalRigid ) {
		cameras.resize(totalCameras);
		views.resize(totalViews);
		points.resize(totalPoints);
		rigids.resize(totalRigid);

		for (int i = 0; i < points.size; i++) {
			points.data[i].reset();
		}

		for (int i = 0; i < rigids.size; i++) {
			rigids.get(i).reset();
		}
		// forget old assignments
		lookupRigid = null;
	}

	/**
	 * Assigns an ID to all rigid points. This function does not need to be called by the user as it will be called
	 * by the residual function if needed
	 */
	public void assignIDsToRigidPoints() {
		// return if it has already been assigned
		if( lookupRigid != null )
			return;
		// Assign a unique ID to each point belonging to a rigid object
		// at the same time create a look up table that allows for the object that a point belongs to be quickly found
		lookupRigid = new int[ getTotalRigidPoints() ];
		int pointID = 0;
		for (int i = 0; i < rigids.size; i++) {
			Rigid r = rigids.data[i];
			r.indexFirst = pointID;
			for (int j = 0; j < r.points.length; j++, pointID++) {
				lookupRigid[pointID] = i;
			}
		}

	}

	/**
	 * Returns true if the scene contains rigid objects
	 */
	public boolean hasRigid() {
		return rigids.size > 0;
	}

	/**
	 * Specifies the camera model being used.
	 * @param which Which camera is being specified
	 * @param fixed If these parameters are constant or not
	 * @param model The camera model
	 */
	public void setCamera(int which , boolean fixed , BundleAdjustmentCamera model  ) {
		cameras.get(which).known = fixed;
		cameras.get(which).model = model;
	}

	public void setCamera( int which , boolean fixed , CameraPinhole intrinsic ) {
		setCamera(which,fixed,new BundlePinhole(intrinsic));
	}

	public void setCamera( int which , boolean fixed , CameraPinholeBrown intrinsic ) {
		setCamera(which,fixed,new BundlePinholeBrown(intrinsic));
	}

	/**
	 * Specifies the spacial transform for a view.
	 * @param which Which view is being specified/
	 * @param fixed If these parameters are fixed or not
	 * @param worldToView The transform from world to view reference frames
	 */
	public void setView(int which , boolean fixed , Se3_F64 worldToView ) {
		views.get(which).known = fixed;
		views.get(which).worldToView.set(worldToView);
	}

	/**
	 * Declares the data structure for a rigid object. Location of points are set by accessing the object directly.
	 * Rigid objects are useful in known scenes with calibration targets.
	 *
	 * @param which Index of rigid object
	 * @param fixed If the pose is known or not
	 * @param worldToObject Initial estimated location of rigid object
	 * @param totalPoints Total number of points attached to this rigid object
	 */
	public void setRigid( int which , boolean fixed , Se3_F64 worldToObject , int totalPoints ) {
		Rigid r = rigids.data[which] = new Rigid();
		r.known = fixed;
		r.objectToWorld.set(worldToObject);
		r.points = new Point[totalPoints];
		for (int i = 0; i < totalPoints; i++) {
			r.points[i] = new Point(pointSize);
		}
	}

	/**
	 * Specifies that the view uses the specified camera
	 * @param viewIndex index of view
	 * @param cameraIndex index of camera
	 */
	public void connectViewToCamera( int viewIndex , int cameraIndex ) {
		if( views.get(viewIndex).camera != -1 )
			throw new RuntimeException("View has already been assigned a camera");
		views.get(viewIndex).camera = cameraIndex;
	}

	/**
	 * Returns the number of cameras with parameters that are not fixed
	 * @return non-fixed camera count
	 */
	public int getUnknownCameraCount() {
		int total = 0;
		for (int i = 0; i < cameras.size; i++) {
			if( !cameras.data[i].known) {
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
		for (int i = 0; i < views.size; i++) {
			if( !views.get(i).known) {
				total++;
			}
		}
		return total;
	}

	/**
	 * Returns the number of view with parameters that are not fixed
	 * @return non-fixed view count
	 */
	public int getUnknownRigidCount() {
		int total = 0;
		for (int i = 0; i < rigids.size; i++) {
			if( !rigids.data[i].known) {
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
		for (int i = 0; i < cameras.size; i++) {
			if( !cameras.data[i].known) {
				total += cameras.data[i].model.getIntrinsicCount();
			}
		}
		return total;
	}

	/**
	 * Returns total number of points associated with rigid objects.
	 */
	public int getTotalRigidPoints() {
		if( rigids == null )
			return 0;

		int total = 0;
		for (int i = 0; i < rigids.size; i++) {
			total += rigids.data[i].points.length;
		}
		return total;
	}

	/**
	 * Returns the total number of parameters which will be optimised
	 * @return number of parameters
	 */
	@Override
	public int getParameterCount() {
		return getUnknownViewCount()*6 + getUnknownRigidCount()*6 + points.size*pointSize + getUnknownCameraParameterCount();
	}

	public FastQueue<Camera> getCameras() {
		return cameras;
	}

	public FastQueue<View> getViews() {
		return views;
	}

	public FastQueue<Rigid> getRigids() { return rigids; }

	/**
	 * Camera which is viewing the scene. Contains intrinsic parameters.
	 */
	public static class Camera {
		/**
		 * If the parameters are assumed to be known and should not be optimised.
		 */
		public boolean known = true;
		public BundleAdjustmentCamera model;

		public <T extends BundleAdjustmentCamera>T getModel() {
			return (T)model;
		}
	}

	/**
	 * Observations from a camera of points.
	 */
	public static class View {
		/**
		 * If the parameters are assumed to be known and should not be optimised.
		 */
		public boolean known = true;
		/**
		 * Transform from world into this view
		 */
		public Se3_F64 worldToView = new Se3_F64();
		/**
		 * The camera associated with this view
		 */
		public int camera = -1;
	}

	/**
	 * A set of connected points that form a rigid structure. The 3D location of points
	 * in the rigid body's reference frame is constant.
	 */
	public static class Rigid {
		public boolean known = false;

		/**
		 * Transform from world into the rigid object's frame
		 */
		public Se3_F64 objectToWorld = new Se3_F64();

		/**
		 * Location of points in object's reference frame. The coordinate is always fixed.
		 */
		public Point[] points;

		/**
		 * Index of the first point in the list
		 */
		public int indexFirst;

		public void reset() {
			objectToWorld.reset();
			points = null;
			indexFirst = -1;
		}

		public void setPoint( int which , double x , double y , double z ) {
			points[which].set(x,y,z);
		}

		public void setPoint( int which , double x , double y , double z , double w ) {
			points[which].set(x,y,z,w);
		}

		public void getPoint(int which , Point3D_F64 p ) {
			points[which].get(p);
		}

		public void getPoint(int which , Point4D_F64 p ) {
			points[which].get(p);
		}

		public int getTotalPoints() {
			return points.length;
		}
	}
}

