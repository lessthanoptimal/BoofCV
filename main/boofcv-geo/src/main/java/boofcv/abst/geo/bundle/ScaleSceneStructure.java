/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.geo.NormalizationPoint2D;
import boofcv.alg.geo.PerspectiveOps;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.sorting.QuickSelect;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

/**
 * Normalizes variables in the scene to improve optimization performance. Different normalization is applied
 * depending on the points being homogenous or not, metric or projective.
 *
 * <p>
 *     Homogenous:<br>
 *     Each point is normalized such that the F-norm is equal to 1. Same goes for translation if metric.
 * </p>
 * <p>
 *     Regular:<br>
 *     If points are 3D then their mean and standard deviation are computed. The points are transformed such that
 *     the set will have a mean of zero and a standard deviation of 1.
 * </p>
 *
 * How to use this class.
 * <ol>
 *     <li>Call applyScale() to compute and then apply the transform</li>
 *     <li>Call undoScale() to revert back to the original coordinate system</li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class ScaleSceneStructure {

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

	/**
	 * If true pixels will be scaled using mean and standard deviation. otherwise the known width/height
	 * are used with the image center
	 */
	boolean scalePixelsUsingStats=true;

	/**
	 * Pixel scaling for each view
	 */
	public FastQueue<NormalizationPoint2D> pixelScaling = new FastQueue<>(NormalizationPoint2D::new);

	/**
	 * Configures how scaling is applied
	 * @param desiredDistancePoint desired scale for points to have
	 */
	public ScaleSceneStructure(double desiredDistancePoint) {
		this.desiredDistancePoint = desiredDistancePoint;
	}

	public ScaleSceneStructure() {
	}

	/**
	 * Applies the scale transform to the input scene structure. Metric.
	 * @param structure 3D scene
	 * @param observations Observations of the scene
	 */
	public void applyScale( SceneStructureMetric structure ,
							SceneObservations observations ) {

		if( structure.homogenous ) {
			applyScaleToPointsHomogenous(structure);
		} else {
			computePointStatistics(structure.points);
			applyScaleToPoints3D(structure);
			applyScaleTranslation3D(structure);
		}
		// NOTE: Observations can't be centered/scaled here because that changes the camera model.
		//       That requires knowledge this class can't have access to and must be done externally
	}
	/**
	 * Applies the scale transform to the input scene structure. Metric.
	 * @param structure 3D scene
	 * @param observations Observations of the scene
	 */
	public void applyScale( SceneStructureProjective structure ,
							SceneObservations observations ) {
		if( structure.homogenous ) {
			applyScaleToPointsHomogenous(structure);
		} else {
			computePointStatistics(structure.points);
			applyScaleToPoints3D(structure);
			applyScaleTranslation3D(structure);
		}

		// Compute pixel scaling to normalize the coordinates
		computePixelScaling(structure, observations);

		// scale and translate observations, which changes camera matrix
		applyScaleToPixelsAndCameraMatrix(structure, observations);
	}

	void computePixelScaling(SceneStructureProjective structure, SceneObservations observations) {
		pixelScaling.reset();
		if( scalePixelsUsingStats ) {
			for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
				SceneObservations.View so = observations.views.get(viewIdx);
				int N = so.size();
				double meanX=0,meanY=0;
				for (int i = 0,idx=0; i < N; i++) {
					meanX += so.observations.data[idx++];
					meanY += so.observations.data[idx++];
				}
				meanX /= N;meanY /= N;
				double stdX=0,stdY=0;
				for (int i = 0,idx=0; i < N; i++) {
					double dx = meanX - so.observations.data[idx++];
					double dy = meanY - so.observations.data[idx++];
					stdX += dx*dx;
					stdY += dy*dy;
				}
				stdX = Math.sqrt(stdX/N);stdY = Math.sqrt(stdY/N);
				pixelScaling.grow().set(meanX,meanY,stdX,stdY);
			}
		} else {
			for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
				SceneStructureProjective.View sv = structure.views.data[viewIdx];
				if( sv.width <= 0 || sv.height <= 0 ) {
					throw new IllegalArgumentException("View width and height is unknown. Scale with statistics instead");
				}
				pixelScaling.grow().set(sv.width/2,sv.height/2,sv.width/2,sv.height/2);
			}
		}
	}

	public void applyScaleToPixelsAndCameraMatrix(SceneStructureProjective structure ,
												  SceneObservations observations )
	{
		for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
			NormalizationPoint2D n = pixelScaling.get(viewIdx);

			float cx = (float)n.meanX;
			float cy = (float)n.meanY;
			float stdX = (float)n.stdX;
			float stdY = (float)n.stdY;

			SceneStructureProjective.View v = structure.views.data[viewIdx];
			SceneObservations.View ov = observations.views.get(viewIdx);
			for (int pixelIdx = 0; pixelIdx < ov.size(); pixelIdx++) {
				int i = pixelIdx*2;
				float x = ov.observations.data[i];
				float y = ov.observations.data[i+1];
				ov.observations.data[i  ] = (x - cx)/ stdX;
				ov.observations.data[i+1] = (y - cy)/ stdY;
			}
			n.apply(v.worldToView,v.worldToView);
		}
	}

	public void undoScaleToPixelsAndCameraMatrix(SceneStructureProjective structure ,
												 SceneObservations observations )
	{
		for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
			NormalizationPoint2D n = pixelScaling.get(viewIdx);

			float cx = (float)n.meanX;
			float cy = (float)n.meanY;
			float stdX = (float)n.stdX;
			float stdY = (float)n.stdY;

			SceneStructureProjective.View v = structure.views.data[viewIdx];
			SceneObservations.View ov = observations.views.get(viewIdx);
			for (int pixelIdx = 0; pixelIdx < ov.size(); pixelIdx++) {
				int i = pixelIdx*2;
				float x = ov.observations.data[i];
				float y = ov.observations.data[i+1];
				ov.observations.data[i  ] = x*stdX + cx;
				ov.observations.data[i+1] = y*stdY + cy;
			}

			n.remove(v.worldToView,v.worldToView);
		}
	}

	/**
	 * For 3D points, computes the median value and variance along each dimension.
	 */
	void computePointStatistics(FastQueue<Point> points ) {
		final int length = points.size;
		double v[] = new double[length];

		for (int axis = 0; axis < 3; axis++) {
			double maxAbs = 0;
			for (int i = 0; i < length; i++) {
				v[i] = points.get(i).coordinate[axis];
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
			v[i] = points.get(i).distanceSq(medianPoint);
		}
		medianDistancePoint = Math.sqrt(QuickSelect.select(v,length/2,length));

//		System.out.println("Median P ="+ medianPoint);
//		System.out.println("Median R ="+ medianDistancePoint);
//		System.out.println("Scale    ="+ (desiredDistancePoint / medianDistancePoint));
	}


	private void applyScaleTranslation3D(SceneStructureProjective structure) {
		double scale = desiredDistancePoint / medianDistancePoint;

		DMatrixRMaj A = new DMatrixRMaj(3,3);
		DMatrixRMaj A_inv = new DMatrixRMaj(3,3);
		Point3D_F64 a = new Point3D_F64();
		Point3D_F64 c = new Point3D_F64();

		for (int i = 0; i < structure.views.size; i++) {
			SceneStructureProjective.View view = structure.views.data[i];

			// X_w = inv(A)*(X_c - T) let X_c = 0 then X_w = -inv(A)*T is center of camera in world
			CommonOps_DDRM.extract(view.worldToView,0,0,A);
			PerspectiveOps.extractColumn(view.worldToView,3,a);
			CommonOps_DDRM.invert(A,A_inv);
			GeometryMath_F64.mult(A_inv,a,c);

			// Apply transform
			c.x = -scale*(c.x + medianPoint.x);
			c.y = -scale*(c.y + medianPoint.y);
			c.z = -scale*(c.z + medianPoint.z);

			// -A*T
			GeometryMath_F64.mult(A,c,a);
			a.scale(-1);
			PerspectiveOps.insertColumn(view.worldToView,3,a);
		}
	}

	/**
	 * Undoes scale transform for metric.
	 *
	 * @param structure scene's structure
	 * @param observations observations of the scene
	 */
	public void undoScale( SceneStructureMetric structure ,
						   SceneObservations observations ) {

		if( structure.homogenous )
			return;

		double scale = desiredDistancePoint / medianDistancePoint;

		undoNormPoints3D(structure, scale);

		Point3D_F64 c = new Point3D_F64();
		for (int i = 0; i < structure.views.size; i++) {
			SceneStructureMetric.View view = structure.views.data[i];

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

	/**
	 * Undoes scale transform for projective scenes
	 *
	 * @param structure scene's structure
	 * @param observations observations of the scene
	 */
	public void undoScale( SceneStructureProjective structure ,
						   SceneObservations observations ) {

		if( !structure.homogenous ) {

			double scale = desiredDistancePoint / medianDistancePoint;

			undoNormPoints3D(structure, scale);

			DMatrixRMaj A = new DMatrixRMaj(3, 3);
			DMatrixRMaj A_inv = new DMatrixRMaj(3, 3);
			Point3D_F64 a = new Point3D_F64();
			Point3D_F64 c = new Point3D_F64();

			for (int i = 0; i < structure.views.size; i++) {
				SceneStructureProjective.View view = structure.views.data[i];

				// X_w = inv(A)*(X_c - T) let X_c = 0 then X_w = -inv(A)*T is center of camera in world
				CommonOps_DDRM.extract(view.worldToView, 0, 0, A);
				PerspectiveOps.extractColumn(view.worldToView, 3, a);
				CommonOps_DDRM.invert(A, A_inv);
				GeometryMath_F64.mult(A_inv, a, c);


				// Apply transform
				c.x = (-c.x / scale + medianPoint.x);
				c.y = (-c.y / scale + medianPoint.y);
				c.z = (-c.z / scale + medianPoint.z);

				// -A*T
				GeometryMath_F64.mult(A, c, a);
				a.scale(-1);
				PerspectiveOps.insertColumn(view.worldToView, 3, a);
			}
		}
		undoScaleToPixelsAndCameraMatrix(structure, observations);
	}

	private void undoNormPoints3D(SceneStructureCommon structure, double scale) {
		for (int i = 0; i < structure.points.size; i++) {
			Point p = structure.points.data[i];
			p.coordinate[0] = p.coordinate[0]/scale + medianPoint.x;
			p.coordinate[1] = p.coordinate[1]/scale + medianPoint.y;
			p.coordinate[2] = p.coordinate[2]/scale + medianPoint.z;
		}
	}

	private void applyScaleTranslation3D(SceneStructureMetric structure) {
		double scale = desiredDistancePoint / medianDistancePoint;

		Point3D_F64 c = new Point3D_F64();
		for (int i = 0; i < structure.views.size; i++) {
			SceneStructureMetric.View view = structure.views.data[i];

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
	}

	void applyScaleToPoints3D(SceneStructureCommon structure) {
		double scale = desiredDistancePoint / medianDistancePoint;

		for (int i = 0; i < structure.points.size; i++) {
			Point p = structure.points.data[i];
			p.coordinate[0] = scale*(p.coordinate[0] - medianPoint.x);
			p.coordinate[1] = scale*(p.coordinate[1] - medianPoint.y);
			p.coordinate[2] = scale*(p.coordinate[2] - medianPoint.z);
		}
	}

	void applyScaleToPointsHomogenous(SceneStructureCommon structure) {

		Point4D_F64 p = new Point4D_F64();
		for (int i = 0; i < structure.points.size; i++) {
			structure.points.data[i].get(p);
			p.normalize();
			structure.points.data[i].set(p.x,p.y,p.z,p.w);
		}
	}

	public boolean isScalePixelsUsingStats() {
		return scalePixelsUsingStats;
	}

	public void setScalePixelsUsingStats(boolean scalePixelsUsingStats) {
		this.scalePixelsUsingStats = scalePixelsUsingStats;
	}
}
