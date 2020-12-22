/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.bundle.cameras.BundleCameraProjective;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;

/**
 * Specifies a scene in an arbitrary projective geometry for Bundle Adjustment. Each view
 * is specified using a 3x4 projective camera matrix. Points for the scene can be 3D or 4D
 * homogenous coordinates.
 *
 * @author Peter Abeles
 */
public class SceneStructureProjective extends SceneStructureCommon {
	public final DogArray<View> views = new DogArray<>(View::new, View::reset);

	/**
	 * Configure bundle adjustment
	 *
	 * @param homogenous if true then homogeneous coordinates are used
	 */
	public SceneStructureProjective( boolean homogenous ) {
		super(homogenous);
	}

	/**
	 * Initialization with the assumption that a {@link boofcv.alg.geo.bundle.cameras.BundleCameraProjective}
	 * is used for all views.
	 *
	 * @param totalViews Number of views
	 * @param totalPoints Number of points
	 */
	public void initialize( int totalViews, int totalPoints ) {
		this.initialize(1, totalViews, totalPoints);
		setCamera(0, true, new BundleCameraProjective());
		for (int viewIdx = 0; viewIdx < views.size; viewIdx++) {
			connectViewToCamera(viewIdx, 0);
		}
	}

	/**
	 * Call this function first. Specifies number of each type of data which is available.
	 *
	 * @param totalCameras Number of camera models
	 * @param totalViews Number of views
	 * @param totalPoints Number of points
	 */
	public void initialize( int totalCameras, int totalViews, int totalPoints ) {
		cameras.reset();
		views.reset();
		points.reset();
		cameras.resize(totalCameras);
		views.resize(totalViews);
		points.resize(totalPoints);
	}

	/**
	 * Specifies the spacial transform for a view.
	 *
	 * @param which Which view is being specified/
	 * @param fixed If these parameters are fixed or not
	 * @param worldToView The 3x4 projective camera matrix that converts a point in world to view pixels
	 * @param width Image's width
	 * @param height Image's height
	 */
	public void setView( int which, boolean fixed, DMatrixRMaj worldToView,
						 int width, int height ) {
		views.data[which].known = fixed;
		views.data[which].worldToView.setTo(worldToView);
		views.data[which].width = width;
		views.data[which].height = height;
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
	 * Returns the number of view with parameters that are not fixed
	 *
	 * @return non-fixed view count
	 */
	public int getUnknownViewCount() {
		int total = 0;
		for (int i = 0; i < views.size; i++) {
			if (!views.data[i].known) {
				total++;
			}
		}
		return total;
	}

	/**
	 * Returns the total number of parameters which will be optimised
	 *
	 * @return number of parameters
	 */
	@Override
	public int getParameterCount() {
		return getUnknownViewCount()*12 + getUnknownCameraParameterCount() + points.size*pointSize;
	}

	public DogArray<View> getViews() {
		return views;
	}

	public static class View {
		/** If the parameters are assumed to be known and should not be optimised. */
		public boolean known = true;

		/** Images shape. Used to normalize points */
		public int width, height;

		/**
		 * Projective camera matrix. x' = P*X. P is the projective matrix, x' are
		 * pixel observations, and X is the 3D feature.
		 */
		public DMatrixRMaj worldToView = new DMatrixRMaj(3, 4);

		/** The camera associated with this view */
		public int camera = -1;

		public void reset() {
			known = false;
			width = height = 0;
			worldToView.zero();
			camera = -1;
		}
	}
}
