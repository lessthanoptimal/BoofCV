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

import boofcv.alg.geo.bundle.BundleAdjustmentPinhole;
import boofcv.alg.geo.bundle.BundleAdjustmentPinholeRadial;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Specifies the scene which will be optimized using Bundle Adjustment. This includes the relationship between
 * cameras, images, and points. Such as, which image is using which camera model, and which point appears in which
 * image. It's also possible to specify if a parameter is fixed or not. If fixed it will not be modified and
 * is assumed to be known a priori.
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
			points[i] = new Point();
		}

	}

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

	public void setView(int which , boolean fixed , Se3_F64 worldToView ) {
		views[which].known = fixed;
		views[which].worldToView.set(worldToView);
	}

	public void setPoint( int which , double x , double y , double z ) {
		points[which].set(x,y,z);
	}

	public void connectViewToCamera( int viewIndex , int cameraIndex ) {
		if( views[viewIndex].camera != -1 )
			throw new RuntimeException("View has already been assigned a camera");
		views[viewIndex].camera = cameraIndex;
	}

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
	 *
	 * @return
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

	public static class Point extends Point3D_F64 {
		/**
		 * Indexes of the views that this point appears in
		 */
		public GrowQueue_I32 views = new GrowQueue_I32();
	}

}

