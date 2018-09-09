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

import boofcv.abst.geo.bundle.SceneStructureCommon.Point;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.sorting.QuickSelect;
import org.ddogleg.struct.GrowQueue_F64;

/**
 * Normalizes variables in the scene to improve optimization performance.
 *
 * TODO describe scaling of scene points
 * TODO describe scaling of cameras
 *
 * @author Peter Abeles
 */
public class ScaleMetricScene {

	/**
	 * This sets the order of magnitude for point coordinates
	 */
	double desiredDistancePoint = 100;

	/**
	 * Median of all the points. They are offset by this amount so that they are zero mean
	 */
	Point3D_F64 medianPoint = new Point3D_F64();
	/**
	 * The median distance from median point
	 */
	double medianDistancePoint;

	GrowQueue_F64 scaleCamera = new GrowQueue_F64();

	/**
	 * Configures how scaling is applied
	 * @param desiredDistancePoint desired scale for points to have
	 */
	public ScaleMetricScene(double desiredDistancePoint) {
		this.desiredDistancePoint = desiredDistancePoint;
	}

	public ScaleMetricScene() {
	}

	public void computeScale(SceneStructureMetric structure ) {
		computePointStatistics(structure.points);
	}

	void computePointStatistics(Point[] points ) {
		final int length = points.length;
		double v[] = new double[length];

		for (int axis = 0; axis < 3; axis++) {
			double maxAbs = 0;
			for (int i = 0; i < length; i++) {
				v[i] = points[i].coordinate[axis];
				maxAbs = Math.max( maxAbs , Math.abs(v[i]));
			}

			double median = QuickSelect.select(v,length/2,length);
			switch( axis ) {
				case 0: medianPoint.x = median; break;
				case 1: medianPoint.y = median; break;
				case 2: medianPoint.z = median; break;
			}
		}

		for (int i = 0; i < length; i++) {
			v[i] = points[i].distanceSq(medianPoint);
		}
		medianDistancePoint = Math.sqrt(QuickSelect.select(v,length/2,length));

//		System.out.println("Median P ="+ medianPoint);
//		System.out.println("Median R ="+ medianDistancePoint);
//		System.out.println("Scale    ="+ (desiredDistancePoint / medianDistancePoint));
	}

	public void applyScale( SceneStructureMetric structure ,
							BundleAdjustmentObservations observations ) {

		double scale = desiredDistancePoint / medianDistancePoint;

		for (int i = 0; i < structure.points.length; i++) {
			Point p = structure.points[i];
			p.coordinate[0] = scale*(p.coordinate[0] - medianPoint.x);
			p.coordinate[1] = scale*(p.coordinate[1] - medianPoint.y);
			p.coordinate[2] = scale*(p.coordinate[2] - medianPoint.z);
		}

		Point3D_F64 c = new Point3D_F64();
		for (int i = 0; i < structure.views.length; i++) {
			SceneStructureMetric.View view = structure.views[i];

			// X_w = R'*(X_c - T) let X_c = 0 then X_w = -R'*T is center of camera in world
			GeometryMath_F64.multTran(view.worldToView.R,view.worldToView.T,c);

			// Apply transform
			c.x = -scale*(c.x + medianPoint.x);
			c.y = -scale*(c.y + medianPoint.y);
			c.z = -scale*(c.z + medianPoint.z);

			// -R*T
			GeometryMath_F64.mult(view.worldToView.R,c,view.worldToView.T);
			view.worldToView.T.scale(-1);
		}

		// NOTE: No need to adjust observations since the scaling will be undone because it's a homogeneous coordinate
	}

	public void undoScale( SceneStructureMetric structure ,
						   BundleAdjustmentObservations observations ) {

		double scale = desiredDistancePoint / medianDistancePoint;

		for (int i = 0; i < structure.points.length; i++) {
			Point p = structure.points[i];
			p.coordinate[0] = p.coordinate[0]/scale + medianPoint.x;
			p.coordinate[1] = p.coordinate[1]/scale + medianPoint.y;
			p.coordinate[2] = p.coordinate[2]/scale + medianPoint.z;
		}

		Point3D_F64 c = new Point3D_F64();
		for (int i = 0; i < structure.views.length; i++) {
			SceneStructureMetric.View view = structure.views[i];

			// X_w = R'*(X_c - T) let X_c = 0 then X_w = -R'*T is center of camera in world
			GeometryMath_F64.multTran(view.worldToView.R,view.worldToView.T,c);

			// Apply transform
			c.x = (-c.x/scale + medianPoint.x);
			c.y = (-c.y/scale + medianPoint.y);
			c.z = (-c.z/scale + medianPoint.z);

			// -R*T
			GeometryMath_F64.mult(view.worldToView.R,c,view.worldToView.T);
			view.worldToView.T.scale(-1);
		}
	}
}
