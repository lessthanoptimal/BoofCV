/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static boofcv.misc.BoofMiscOps.checkTrue;

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

	/** List of views. A view is composed of a camera model and it's pose. */
	public final DogArray<View> views = new DogArray<>(View::new, View::reset);

	/** List of motions for the views that specifies their spatial relationship */
	public final DogArray<Motion> motions = new DogArray<>(Motion::new, Motion::reset);

	/** List of rigid objects. A rigid object is a group of 3D points that have a known relationship with each other. */
	public final DogArray<Rigid> rigids = new DogArray<>(Rigid::new);
	// Lookup table from rigid point to rigid object
	public int[] lookupRigid = new int[0];

	/**
	 * Configure bundle adjustment
	 *
	 * @param homogenous if true then homogeneous coordinates are used
	 */
	public SceneStructureMetric( boolean homogenous ) {
		super(homogenous);
	}

	/**
	 * Call this function first. Specifies number of each type of data which is available.
	 *
	 * @param totalCameras Number of cameras
	 * @param totalViews Number of views
	 * @param totalPoints Number of points
	 */
	public void initialize( int totalCameras, int totalViews, int totalPoints ) {
		initialize(totalCameras, totalViews, totalViews, totalPoints, 0);
	}

	/**
	 * Call this function first. Specifies number of each type of data which is available.
	 *
	 * @param totalCameras Number of cameras
	 * @param totalViews Number of views
	 * @param totalMotions Number of motions
	 * @param totalPoints Number of points
	 * @param totalRigid Number of rigid objects
	 */
	public void initialize( int totalCameras, int totalViews, int totalMotions, int totalPoints, int totalRigid ) {
		// Declare enough memory to store all the motions, but the way motions are constructed they are grown
		motions.resize(totalMotions);

		// Reset first so that when it resizes it will call reset() on each object
		cameras.reset();
		views.reset();
		motions.reset();
		points.reset();
		rigids.reset();

		cameras.resize(totalCameras);
		views.resize(totalViews);
		points.resize(totalPoints);
		rigids.resize(totalRigid);

		// forget old assignments
		lookupRigid = new int[0];
	}

	/**
	 * Assigns an ID to all rigid points. This function does not need to be called by the user as it will be called
	 * by the residual function if needed
	 */
	public void assignIDsToRigidPoints() {
		// return if it has already been assigned
		if (lookupRigid.length != 0)
			return;
		// Assign a unique ID to each point belonging to a rigid object
		// at the same time create a look up table that allows for the object that a point belongs to be quickly found
		lookupRigid = new int[getTotalRigidPoints()];
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
	 * Returns SE3 relationship to the specified view from it's parent.
	 *
	 * @param view The target view
	 * @return SE3 transform from parent to view.
	 */
	public Se3_F64 getParentToView( View view ) {
		return motions.get(view.parent_to_view).motion;
	}

	public Se3_F64 getParentToView( int viewIdx ) {
		View v = views.get(viewIdx);
		return motions.get(v.parent_to_view).motion;
	}

	/**
	 * Computes and returns the transform from world to view. If the view is relative then the chain
	 * is followed
	 *
	 * @param view The target view
	 * @param world_to_view Storage for the found transform. Can be null.
	 * @param tmp Optional workspace to avoid creating new memory. Can be null;
	 * @return SE3 transform from parent to view.
	 */
	public Se3_F64 getWorldToView( View view,
								   @Nullable Se3_F64 world_to_view,
								   @Nullable Se3_F64 tmp ) {
		if (world_to_view == null)
			world_to_view = new Se3_F64();

		if (tmp == null)
			tmp = new Se3_F64();

		world_to_view.setTo(getParentToView(view));

		while (view.parent != null) {
			view = view.parent;
			Se3_F64 parent_to_view = getParentToView(view);

			// world_to_view is really view_to_child
			parent_to_view.concat(world_to_view, tmp);
			world_to_view.setTo(tmp);
		}

		return world_to_view;
	}

	/**
	 * Returns true if the scene contains rigid objects
	 */
	public boolean hasRigid() {
		return rigids.size > 0;
	}

	/**
	 * Specifies the spacial transform for a view and assumes the parent is the world frame.
	 *
	 * @param viewIndex Which view is being specified.
	 * @param cameraIndex Index of camera model
	 * @param known If these parameters are fixed or not
	 * @param world_to_view The transform from world to view reference frames. Internal copy is saved.
	 */
	public void setView( int viewIndex, int cameraIndex, boolean known, Se3_F64 world_to_view ) {
		views.get(viewIndex).camera = cameraIndex;
		views.get(viewIndex).parent_to_view = addMotion(known, world_to_view);
		views.get(viewIndex).parent = null;
	}

	/**
	 * Specifies the spacial transform for a view and assumes the parent is the world frame.
	 *
	 * @param viewIndex Which view is being specified.
	 * @param cameraIndex Index of camera model
	 * @param known If the parameters are known and not optimized or unknown and optimized
	 * @param parent_to_view The transform from parent to view reference frames. Internal copy is saved.
	 * @param parent ID / index of the parent this this view is relative to
	 */
	public void setView( int viewIndex, int cameraIndex, boolean known, Se3_F64 parent_to_view, int parent ) {
		checkTrue(parent < viewIndex, "Parent must be less than viewIndex");
		views.get(viewIndex).camera = cameraIndex;
		views.get(viewIndex).parent_to_view = addMotion(known, parent_to_view);
		views.get(viewIndex).parent = parent >= 0 ? views.get(parent) : null;
	}

	/**
	 * Specifies which motion is attached to a view
	 *
	 * @param viewIndex Which view is being specified.
	 * @param cameraIndex Index of camera model
	 * @param motionIndex The motion that describes the parent_to_view relationship
	 * @param parent ID / index of the parent this this view is relative to
	 */
	public void setView( int viewIndex, int cameraIndex, int motionIndex, int parent ) {
		checkTrue(parent < viewIndex, "Parent must be less than viewIndex");
		views.get(viewIndex).camera = cameraIndex;
		views.get(viewIndex).parent_to_view = motionIndex;
		views.get(viewIndex).parent = parent >= 0 ? views.get(parent) : null;
	}

	/**
	 * Specifies a new motion.
	 *
	 * @param known If the parameters are known and not optimized or unknown and optimized
	 * @param motion The value of the motion
	 * @return Index or ID for the created motion
	 */
	public int addMotion( boolean known, Se3_F64 motion ) {
		int index = motions.size;
		Motion m = motions.grow();
		m.known = known;
		m.motion.setTo(motion);
		return index;
	}

	/**
	 * Declares the data structure for a rigid object. Location of points are set by accessing the object directly.
	 * Rigid objects are useful in known scenes with calibration targets.
	 *
	 * @param which Index of rigid object
	 * @param known If the parameters are known and not optimized or unknown and optimized
	 * @param worldToObject Initial estimated location of rigid object
	 * @param totalPoints Total number of points attached to this rigid object
	 */
	public void setRigid( int which, boolean known, Se3_F64 worldToObject, int totalPoints ) {
		Rigid r = rigids.data[which];
		r.init(totalPoints, pointSize);
		r.known = known;
		r.object_to_world.setTo(worldToObject);
	}

	/**
	 * Specifies that the view uses the specified camera
	 *
	 * @param viewIndex index of view
	 * @param cameraIndex index of camera
	 */
	public void connectViewToCamera( int viewIndex, int cameraIndex ) {
		if (views.get(viewIndex).camera != -1)
			throw new RuntimeException("View has already been assigned a camera");
		views.get(viewIndex).camera = cameraIndex;
	}

	/**
	 * Returns the number of cameras with parameters that are not fixed
	 *
	 * @return non-fixed camera count
	 */
	public int getUnknownCameraCount() {
		int total = 0;
		for (int i = 0; i < cameras.size; i++) {
			if (!cameras.data[i].known) {
				total++;
			}
		}
		return total;
	}

	/**
	 * Returns the number of motions with parameters that are not fixed
	 *
	 * @return non-fixed view count
	 */
	public int getUnknownMotionCount() {
		int total = 0;
		for (int i = 0; i < motions.size; i++) {
			if (!motions.get(i).known) {
				total++;
			}
		}
		return total;
	}

	/**
	 * Returns the number of view with parameters that are not fixed
	 *
	 * @return non-fixed view count
	 */
	public int getUnknownRigidCount() {
		int total = 0;
		for (int i = 0; i < rigids.size; i++) {
			if (!rigids.data[i].known) {
				total++;
			}
		}
		return total;
	}

	/**
	 * Returns total number of points associated with rigid objects.
	 */
	public int getTotalRigidPoints() {
		int total = 0;
		for (int i = 0; i < rigids.size; i++) {
			total += rigids.data[i].points.length;
		}
		return total;
	}

	/**
	 * Projects the requested point onto the requested view and computes its predicted pixel coordinates.
	 *
	 * @param pointIdx (Input) index of point
	 * @param viewIdx (Input) index of view
	 * @param world_to_view (Output) Internal work space
	 * @param tmpSE (Output) Internal work space
	 * @param tmpX (Output) Internal work space
	 * @param pixel (Output) The projected pixel
	 * @return true if it's in front of the camera or false if it's behind
	 */
	public boolean projectToPixel( int pointIdx, int viewIdx,
								   Se3_F64 world_to_view, Se3_F64 tmpSE,
								   Point3D_F64 tmpX, Point2D_F64 pixel ) {

		// Get the coordinate transform
		View view = views.get(viewIdx);
		getWorldToView(view, world_to_view, tmpSE);

		// extract the coordinate for 3D and homogenous case
		Point p = points.get(pointIdx);
		double x, y, z, w;
		x = p.coordinate[0];
		y = p.coordinate[1];
		z = p.coordinate[2];
		w = homogenous ? p.coordinate[3] : 1.0;

		// Project the pixel while being careful for points at infinity
		BundleAdjustmentCamera camera = Objects.requireNonNull(getViewCamera(view).model);
		SePointOps_F64.transformV(world_to_view, x, y, z, w, tmpX);
		camera.project(tmpX.x, tmpX.y, tmpX.z, pixel);

		// if the sign is positive then it's in front
		return z*w > 0.0;
	}

	/**
	 * Returns the camera for this particular view
	 */
	public Camera getViewCamera( View v ) {
		return cameras.get(v.camera);
	}

	/**
	 * Returns the total number of parameters which will be optimised
	 *
	 * @return number of parameters
	 */
	@Override
	public int getParameterCount() {
		return getUnknownMotionCount()*6 + getUnknownRigidCount()*6 + points.size*pointSize + getUnknownCameraParameterCount();
	}

	public DogArray<View> getViews() {
		return views;
	}

	public DogArray<Rigid> getRigids() {return rigids;}

	/**
	 * Checks to see if the passed in scene is identical to "this" scene. Floating point values are checked to within
	 * tolerance
	 *
	 * @return true if identical to within specified float tolerance and false if not
	 */
	public boolean isIdentical( SceneStructureMetric m, double tol ) {
		if (isHomogenous() != m.isHomogenous())
			return false;
		if (views.size != m.views.size)
			return false;
		if (motions.size != m.motions.size)
			return false;
		if (rigids.size != m.rigids.size)
			return false;
		if (cameras.size != m.cameras.size)
			return false;
		if (points.size != m.points.size)
			return false;

		for (int i = 0; i < views.size; i++) {
			if (!views.get(i).isIdentical(m.views.get(i)))
				return false;
		}

		for (int i = 0; i < motions.size; i++) {
			if (!motions.get(i).isIdentical(m.motions.get(i), tol))
				return false;
		}

		for (int i = 0; i < rigids.size; i++) {
			if (!rigids.get(i).isIdentical(m.rigids.get(i), tol))
				return false;
		}

		for (int i = 0; i < cameras.size; i++) {
			if (!cameras.get(i).isIdentical(m.cameras.get(i), tol))
				return false;
		}

		for (int i = 0; i < points.size; i++) {
			if (points.get(i).distance(m.points.get(i)) > tol)
				return false;
		}

		return true;
	}

	/**
	 * Describes a view from a camera at a particular point. References are provided to the data structures which
	 * provide the intrinsic and extrinsic parameters. A view contains no parameters that are optimized directly
	 * but instead describes the graph's structure.
	 */
	public static class View {
		/** Which motion specifies the transform from parent to this view's reference frame */
		public int parent_to_view = -1;
		/** The camera associated with this view */
		public int camera = -1;
		/** Points to the parent view or null if world is parent. Parent must be less than this view's ID. */
		public @Nullable View parent;

		public void reset() {
			parent_to_view = -1;
			camera = -1;
			parent = null;
		}

		public boolean isIdentical( View m ) {
			if (parent_to_view != m.parent_to_view)
				return false;
			if (camera != m.camera)
				return false;
			if (parent == null) {
				return m.parent == null;
			} else if (m.parent == null)
				return false;

			// NOTE: Can't check to see if the parent is the same because that requires knowing the view index
			return true;
		}
	}

	/**
	 * Describes the relationship between two views. Multiple pairs of views can reference the same motion. This
	 * is useful if a set of cameras are rigidly mounted, e.g. stereo cameras.
	 */
	public static class Motion {
		/** If the parameters are assumed to be known and should not be optimised. */
		public boolean known;
		/** Transform from parent view into this view */
		public final Se3_F64 motion = new Se3_F64();

		public void reset() {
			known = true;
			motion.reset();
		}

		public boolean isIdentical( Motion m, double tol ) {
			if (known != m.known)
				return false;
			if (motion.T.distance(m.motion.T) > tol)
				return false;
			for (int i = 0; i < 9; i++) {
				if (Math.abs(motion.R.data[i] - m.motion.R.data[i]) > tol)
					return false;
			}
			return true;
		}
	}

	/**
	 * A set of connected points that form a rigid structure. The 3D location of points
	 * in the rigid body's reference frame is constant.
	 */
	@SuppressWarnings({"NullAway.Init"})
	public static class Rigid {
		/** If the parameters are assumed to be known and should not be optimised. */
		public boolean known;

		/** Transform from world into the rigid object's frame */
		public final Se3_F64 object_to_world = new Se3_F64();

		/** Location of points in object's reference frame. The coordinate is always fixed. */
		public Point[] points = new Point[0];

		/** Index of the first point in the list */
		public int indexFirst;

		/**
		 * Resets and initialzies internal data structures. If possible previously declared data is reused
		 *
		 * @param numPoints Number of points in the rigid body
		 * @param dof DOF in each point
		 */
		public void init( int numPoints, int dof ) {
			known = false;
			object_to_world.reset();
			indexFirst = -1;

			if (points.length != numPoints)
				points = new SceneStructureCommon.Point[numPoints];

			if (points.length > 0 && (points[0] == null || points[0].coordinate.length != numPoints)) {
				for (int i = 0; i < numPoints; i++) {
					points[i] = new Point(dof);
				}
			}
		}

		public void setPoint( int which, double x, double y, double z ) {
			points[which].set(x, y, z);
		}

		public void setPoint( int which, double x, double y, double z, double w ) {
			points[which].set(x, y, z, w);
		}

		public void getPoint( int which, Point3D_F64 p ) {
			points[which].get(p);
		}

		public void getPoint( int which, Point4D_F64 p ) {
			points[which].get(p);
		}

		public int getTotalPoints() {
			return points.length;
		}

		public boolean isIdentical( Rigid m, double tol ) {
			if (known != m.known)
				return false;
			if (indexFirst != m.indexFirst)
				return false;
			if (points != null) {
				if (m.points == null)
					return false;
				if (points.length != m.points.length)
					return false;
				for (int i = 0; i < points.length; i++) {
					if (points[i].distance(m.points[i]) > tol)
						return false;
				}
			} else return m.points == null;
			return true;
		}
	}
}
