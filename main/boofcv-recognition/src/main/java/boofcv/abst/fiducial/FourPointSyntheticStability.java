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

package boofcv.abst.fiducial;

import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the stability for a fiducial using 4 synthetic corners that are position based on the fiducial's
 * width and height given the current estimated marker to camera transform. The 4 corners are placed symmetrically
 * around the marker's origin at (-w/2,-h/2) (-w/2,h/2) (w/2,h/2) (w/2,-h/2). Stability is computed by varying
 * the projected corners in pixel coordinates then recomputing the camera to fiducial pose and seeing how much
 * it has changed.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class FourPointSyntheticStability {

	// location of point in marker reference frame and 2D normalized image coordinate observation
	List<Point2D3D> points2D3D = new ArrayList<>();
	// reference pixel observations. not actual observations
	List<Point2D_F64> refPixels = new ArrayList<>();
	List<Point2D_F64> refNorm = new ArrayList<>();

	// transform from pixel to normalized image coordinates and the reverse
	protected Point2Transform2_F64 pixelToNorm;
	protected Point2Transform2_F64 normToPixel;

	// Used to estimate target to camera pose
	private Estimate1ofPnP estimatePnP = FactoryMultiView.pnp_1(EnumPNP.IPPE, -1, -1);

	Se3_F64 referenceCameraToTarget = new Se3_F64();
	Se3_F64 targetToCameraSample = new Se3_F64();
	Se3_F64 difference = new Se3_F64();

	Rodrigues_F64 rodrigues = new Rodrigues_F64();

	// maximum difference in orientation found in radians
	double maxOrientation = 0;
	// maxium change in location that was found
	double maxLocation = 0;

	public FourPointSyntheticStability() {
		for (int i = 0; i < 4; i++) {
			points2D3D.add(new Point2D3D());
			refPixels.add(new Point2D_F64());
			refNorm.add(new Point2D_F64());
		}
	}

	/**
	 * Specifies how to convert to and from pixels
	 */
	public void setTransforms( Point2Transform2_F64 pixelToNorm,
							   Point2Transform2_F64 normToPixel ) {
		this.pixelToNorm = pixelToNorm;
		this.normToPixel = normToPixel;
	}

	/**
	 * Specifes how big the fiducial is along two axises
	 *
	 * @param width Length along x-axis
	 * @param height Length along y-axis
	 */
	public void setShape( double width, double height ) {
		points2D3D.get(0).location.setTo(-width/2, -height/2, 0);
		points2D3D.get(1).location.setTo(-width/2, height/2, 0);
		points2D3D.get(2).location.setTo(width/2, height/2, 0);
		points2D3D.get(3).location.setTo(width/2, -height/2, 0);
	}

	/**
	 * Estimate how sensitive this observation is to pixel noise
	 *
	 * @param targetToCamera Observed target to camera pose estimate
	 * @param disturbance How much the observation should be noised up, in pixels
	 * @param results description how how sensitive the stability estimate is
	 */
	public void computeStability( Se3_F64 targetToCamera,
								  double disturbance,
								  FiducialStability results ) {

		targetToCamera.invert(referenceCameraToTarget);

		maxOrientation = 0;
		maxLocation = 0;

		Point3D_F64 cameraPt = new Point3D_F64();
		for (int i = 0; i < points2D3D.size(); i++) {
			Point2D3D p23 = points2D3D.get(i);

			targetToCamera.transform(p23.location, cameraPt);
			p23.observation.x = cameraPt.x/cameraPt.z;
			p23.observation.y = cameraPt.y/cameraPt.z;

			refNorm.get(i).setTo(p23.observation);

			normToPixel.compute(p23.observation.x, p23.observation.y, refPixels.get(i));
		}

		for (int i = 0; i < points2D3D.size(); i++) {
			// see what happens if you tweak this observation a little bit
			perturb(disturbance, refPixels.get(i), points2D3D.get(i));
			// set it back to the nominal value
			points2D3D.get(i).observation.setTo(refNorm.get(i));
		}

		results.location = maxLocation;
		results.orientation = maxOrientation;
	}

	/**
	 * Perturb the observation in 4 different ways
	 *
	 * @param disturbance distance of pixel the observed point will be offset by
	 * @param pixel observed pixel
	 * @param p23 observation plugged into PnP
	 */
	private void perturb( double disturbance, Point2D_F64 pixel, Point2D3D p23 ) {
		double x;
		double y = pixel.y;

		x = pixel.x + disturbance;
		computeDisturbance(x, y, p23);
		x = pixel.x - disturbance;
		computeDisturbance(x, y, p23);
		x = pixel.x;
		y = pixel.y + disturbance;
		computeDisturbance(x, y, p23);
		y = pixel.y - disturbance;
		computeDisturbance(x, y, p23);
	}

	private void computeDisturbance( double x, double y, Point2D3D p23 ) {
		pixelToNorm.compute(x, y, p23.observation);
		if (estimatePnP.process(points2D3D, targetToCameraSample)) {
			referenceCameraToTarget.concat(targetToCameraSample, difference);

			double d = difference.getT().norm();
			ConvertRotation3D_F64.matrixToRodrigues(difference.getR(), rodrigues);
			double theta = Math.abs(rodrigues.theta);
			if (theta > maxOrientation) {
				maxOrientation = theta;
			}
			if (d > maxLocation) {
				maxLocation = d;
			}
		}
	}
}
