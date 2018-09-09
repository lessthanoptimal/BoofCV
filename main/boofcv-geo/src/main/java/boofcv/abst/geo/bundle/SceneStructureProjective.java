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

import org.ejml.data.DMatrixRMaj;

/**
 * Specifies a scene in an arbitrary projective geometry for Bundle Adjustment. Each view
 * is specified using a 3x4 projective camera matrix. Points for the scene can be 3D or 4D
 * homogenous coordinates.
 *
 * @author Peter Abeles
 */
public class SceneStructureProjective extends SceneStructureCommon
{
	public View[] views;

	/**
	 * Configure bundle adjustment
	 * @param homogenous if true then homogeneous coordinates are used
	 */
	public SceneStructureProjective(boolean homogenous) {
		super(homogenous);
	}

	/**
	 * Call this function first. Specifies number of each type of data which is available.
	 *
	 * @param totalViews Number of views
	 * @param totalPoints Number of points
	 */
	public void initialize( int totalViews , int totalPoints ) {
		views = new View[totalViews];
		points = new Point[totalPoints];

		for (int i = 0; i < views.length; i++) {
			views[i] = new View();
		}
		for (int i = 0; i < points.length; i++) {
			points[i] = new Point(pointSize);
		}
	}
	/**
	 * Specifies the spacial transform for a view.
	 * @param which Which view is being specified/
	 * @param fixed If these parameters are fixed or not
	 * @param worldToView The 3x4 projective camera matrix that converts a point in world to view pixels
	 */
	public void setView(int which , boolean fixed , DMatrixRMaj worldToView ) {
		views[which].known = fixed;
		views[which].worldToView.set(worldToView);
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
	 * Returns the total number of parameters which will be optimised
	 * @return number of parameters
	 */
	@Override
	public int getParameterCount() {
		return getUnknownViewCount()*12 + points.length*3;
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
		 * Projective camera matrix. x' = P*X. P is the projective matrix, x' are
		 * pixel observations, and X is the 3D feature.
		 */
		public DMatrixRMaj worldToView = new DMatrixRMaj(3,4);
	}

}

