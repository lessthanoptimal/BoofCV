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

import boofcv.alg.geo.bundle.cameras.BundlePinhole;
import boofcv.alg.geo.bundle.cameras.BundlePinholeRadial;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
import georegression.struct.se.Se3_F64;

/**
 * Specifies a metric (calibrated) scene for optimizing using {@link BundleAdjustment}.
 * It specifies the relationships between cameras, views, and points. A camera projects a 3D point onto the image plane.
 * Examples of specific models include pinhole and fisheye. A view is a set of observed features from a specific
 * camera image. Views have an associated camera and specify the pose of the camera when the scene was viewed. Points
 * describe the scene's 3D structure.
 *
 * @author Peter Abeles
 */
public class SceneStructureMetric extends SceneStructureCommon {

	public Camera[] cameras;
	public View[] views;

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
		setCamera(which,fixed,new BundlePinhole(intrinsic));
	}

	public void setCamera( int which , boolean fixed , CameraPinholeRadial intrinsic ) {
		setCamera(which,fixed,new BundlePinholeRadial(intrinsic));
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
				total += cameras[i].model.getIntrinsicCount();
			}
		}
		return total;
	}

	/**
	 * Returns the total number of parameters which will be optimised
	 * @return number of parameters
	 */
	@Override
	public int getParameterCount() {
		return getUnknownViewCount()*6 + points.length*3 + getUnknownCameraParameterCount();
	}

	public Camera[] getCameras() {
		return cameras;
	}

	public View[] getViews() {
		return views;
	}


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
}

