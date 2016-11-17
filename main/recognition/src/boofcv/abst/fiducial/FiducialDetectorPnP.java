/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.geo.RefinePnP;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.ImageBase;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Provides everything you need to convert a image based fiducial detector into one which can estimate
 * the fiducial's pose given control points.  The camera pose is found using a solution to the Pose-N-Point (PnP)
 * problem.</p>
 *
 * <p>Stability is computed by perturbing each control point by the user provided amount of disturbance.  The largest
 * delta for location and orientation is then found and saved.</p>
 *
 * @author Peter Abeles
 */
public abstract class FiducialDetectorPnP<T extends ImageBase>
		implements FiducialDetector<T> {

	private LensDistortionNarrowFOV lensDistortion;

	// transform to remove lens distortion
	private Point2Transform2_F64 pixelToNorm;

	// 2D-3D pairs for just the detected points
	private List<Point2D3D> detected2D3D = new ArrayList<>();
	// list of the pixel observations for the most recently requested fiducial
	private List<PointIndex2D_F64> detectedPixels;
	private Point2D_F64 workPt = new Point2D_F64();

	// if a lens distortion model was set or not
	boolean hasCameraModel = false;

	// non-linear refinement of pose estimate
	private Estimate1ofPnP estimatePnP = FactoryMultiView.computePnPwithEPnP(10, 0.2);
	private RefinePnP refinePnP = FactoryMultiView.refinePnP(1e-8,100);

	// when computing the pose, this is the initial estimate before non-linear refinement
	private Se3_F64 initialEstimate = new Se3_F64();

	// max found location and orientation error when computing stability
	private double maxLocation;
	private double maxOrientation;

	// Work space for computing stability
	private Se3_F64 targetToCamera = new Se3_F64();
	private Se3_F64 targetToCameraSample = new Se3_F64();
	private Se3_F64 referenceCameraToTarget = new Se3_F64();
	private Se3_F64 difference = new Se3_F64();
	private Rodrigues_F64 rodrigues = new Rodrigues_F64();

	/**
	 * Estimates the stability by perturbing each land mark by the specified number of pixels in the distorted image.
	 */
	@Override
	public boolean computeStability(int which, double disturbance, FiducialStability results) {
		if( !getFiducialToCamera(which, targetToCamera))
			return false;
		targetToCamera.invert(referenceCameraToTarget);

		maxOrientation = 0;
		maxLocation = 0;
		for (int i = 0; i < detected2D3D.size(); i++) {

			estimatePose(which, detected2D3D, targetToCameraSample);
			referenceCameraToTarget.concat(targetToCameraSample, difference);
			Point2D3D p23 = detected2D3D.get(i);
			Point2D_F64 p = detectedPixels.get(i);
			workPt.set(p);

			perturb(which, disturbance,workPt,p,p23);
		}

		results.location = maxLocation;
		results.orientation = maxOrientation;

		return true;
	}

	private void perturb( int which, double disturbance ,
						  Point2D_F64 pixel , Point2D_F64 original , Point2D3D p23 ) {
		pixel.x = original.x + disturbance;
		computeDisturbance(which, pixel, p23);
		pixel.x = original.x - disturbance;
		computeDisturbance(which, pixel, p23);
		pixel.y = original.y;
		pixel.y = original.y + disturbance;
		computeDisturbance(which, pixel, p23);
		pixel.y = original.y - disturbance;
		computeDisturbance(which, pixel, p23);
	}

	private void computeDisturbance(int which, Point2D_F64 pixel, Point2D3D p23) {
		pixelToNorm.compute(pixel.x,pixel.y,p23.observation);
		if( estimatePose(which, detected2D3D, targetToCameraSample) ) {
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

	@Override
	public void setLensDistortion(LensDistortionNarrowFOV distortion) {
		if( distortion != null ) {
			this.hasCameraModel = true;
			this.lensDistortion = distortion;
			this.pixelToNorm = lensDistortion.undistort_F64(true, false);
		} else {
			this.hasCameraModel = false;
			this.lensDistortion = null;
			this.pixelToNorm = null;
		}
	}

	@Override
	public LensDistortionNarrowFOV getLensDistortion() {
		return lensDistortion;
	}

	@Override
	public boolean getFiducialToCamera(int which, Se3_F64 fiducialToCamera) {
		if( !hasCameraModel )
			return false;

		detectedPixels = getDetectedControl(which);
		if( detectedPixels.size() < 3 )
			return false;

		// 2D-3D point associations
		createDetectedList(which, detectedPixels);

		return estimatePose(which, detected2D3D, fiducialToCamera);
	}

	/**
	 * Create the list of observed points in 2D3D
	 */
	public void createDetectedList(int which, List<PointIndex2D_F64> pixels) {
		detected2D3D.clear();
		List<Point2D3D> all = getControl3D(which);
		for (int i = 0; i < pixels.size(); i++) {
			PointIndex2D_F64 a = pixels.get(i);
			Point2D3D b = all.get(i);

			pixelToNorm.compute(a.x,a.y, b.observation);
			detected2D3D.add( b );
		}
	}

	/**
	 * Given the mapping of 2D observations to known 3D points estimate the pose of the fiducial.
	 * This solves the P-n-P problem.
	 */
	protected boolean estimatePose( int which ,List<Point2D3D> points , Se3_F64 fiducialToCamera ) {
		return estimatePnP.process(points, initialEstimate) &&
				refinePnP.fitModel(points, initialEstimate, fiducialToCamera);
	}

	/**
	 * Returns a list of detected control points in the image for the specified fiducial.  Observations
	 * will be in distorted image pixels.
	 */
	protected abstract List<PointIndex2D_F64> getDetectedControl(int which );

	/**
	 * 3D location of control points in the fiducial reference frame
	 * @return 3D location of control points
	 */
	protected abstract List<Point2D3D> getControl3D( int which );

	@Override
	public boolean is3D() {
		return hasCameraModel;
	}
}
