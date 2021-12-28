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
import boofcv.abst.geo.RefinePnP;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray_F64;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>Provides everything you need to convert a image based fiducial detector into one which can estimate
 * the fiducial's pose given control points. The camera pose is found using a solution to the Pose-N-Point (PnP)
 * problem.</p>
 *
 * <p>Stability is computed by perturbing each control point by the user provided amount of disturbance. The largest
 * delta for location and orientation is then found and saved.</p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class FiducialDetectorPnP<T extends ImageBase<T>> implements FiducialDetector<T> {

	private @Nullable LensDistortionNarrowFOV lensDistortion;

	// transform to remove lens distortion
	protected @Nullable Point2Transform2_F64 pixelToNorm;

	// 2D-3D pairs for just the detected points
	private List<Point2D3D> detected2D3D = new ArrayList<>();
	// list of the pixel observations for the most recently requested fiducial
	private List<PointIndex2D_F64> detectedPixels;

	// if a lens distortion model was set or not
	boolean hasCameraModel = false;

	// non-linear refinement of pose estimate
	private Estimate1ofPnP estimatePnP = FactoryMultiView.computePnPwithEPnP(10, 0.2);
	//	private Estimate1ofPnP estimatePnP = FactoryMultiView.pnp_1(EnumPNP.IPPE,0,0);
	private RefinePnP refinePnP = FactoryMultiView.pnpRefine(1e-8, 100);
	private WorldToCameraToPixel w2p = new WorldToCameraToPixel();

	// when computing the pose, this is the initial estimate before non-linear refinement
	private Se3_F64 initialEstimate = new Se3_F64();

	// Work space for computing stability
	private FourPointSyntheticStability stability = new FourPointSyntheticStability();
	private Se3_F64 targetToCamera = new Se3_F64();

	// workspace for pose estimation
	DogArray_F64 errors = new DogArray_F64();
	Point2D_F64 predicted = new Point2D_F64();
	List<Point2D3D> filtered = new ArrayList<>();

	/**
	 * Width of the fiducial. used to compute stability
	 *
	 * @param which specifies which fiducial
	 * @return the width
	 */
	public abstract double getSideWidth( int which );

	/**
	 * Height of the fiducial. used to compute stability
	 *
	 * @param which specifies which fiducial
	 * @return the height
	 */
	public abstract double getSideHeight( int which );

	/**
	 * Estimates the stability by perturbing each land mark by the specified number of pixels in the distorted image.
	 */
	@Override
	public boolean computeStability( int which, double disturbance, FiducialStability results ) {

		if (!getFiducialToCamera(which, targetToCamera))
			return false;

		stability.setShape(getSideWidth(which), getSideHeight(which));

		stability.computeStability(targetToCamera, disturbance, results);
		return true;
	}

	@Override
	public void setLensDistortion( @Nullable LensDistortionNarrowFOV distortion, int width, int height ) {
		if (distortion != null) {
			this.hasCameraModel = true;
			this.lensDistortion = distortion;
			this.pixelToNorm = lensDistortion.undistort_F64(true, false);
			Point2Transform2_F64 normToPixel = lensDistortion.distort_F64(false, true);
			stability.setTransforms(pixelToNorm, normToPixel);
		} else {
			this.hasCameraModel = false;
			this.lensDistortion = null;
			this.pixelToNorm = null;
		}
	}

	@Override
	public @Nullable LensDistortionNarrowFOV getLensDistortion() {
		return lensDistortion;
	}

	@Override
	public boolean getFiducialToCamera( int which, Se3_F64 fiducialToCamera ) {
		if (!hasCameraModel)
			return false;

		detectedPixels = getDetectedControl(which);
		if (detectedPixels.size() < 3)
			return false;

		// 2D-3D point associations
		createDetectedList(which, detectedPixels);

		return estimatePose(which, detected2D3D, fiducialToCamera);
	}

	/**
	 * Create the list of observed points in 2D3D
	 */
	private void createDetectedList( int which, List<PointIndex2D_F64> pixels ) {
		Objects.requireNonNull(pixelToNorm);
		detected2D3D.clear();
		List<Point2D3D> all = getControl3D(which);
		for (int i = 0; i < pixels.size(); i++) {
			Point2D_F64 a = pixels.get(i).p;
			Point2D3D b = all.get(i);

			pixelToNorm.compute(a.x, a.y, b.observation);
			detected2D3D.add(b);
		}
	}

	/**
	 * Given the mapping of 2D observations to known 3D points estimate the pose of the fiducial.
	 * This solves the P-n-P problem.
	 *
	 * Do a simple form of robust estimation. Prune points which are greater than 3 standard deviations
	 * and likely noise the recompute the pose
	 */
	protected boolean estimatePose( int which, List<Point2D3D> points, Se3_F64 fiducialToCamera ) {
		if (!estimatePnP.process(points, initialEstimate)) {
			return false;
		}
		filtered.clear();
		// Don't bother if there are hardly any points to work with
		if (points.size() > 6) {
			w2p.configure(Objects.requireNonNull(lensDistortion), initialEstimate);

			// compute the error for each point in image pixels
			errors.reset();
			for (int idx = 0; idx < detectedPixels.size(); idx++) {
				PointIndex2D_F64 dp = detectedPixels.get(idx);
				w2p.transform(points.get(idx).location, predicted);
				errors.add(predicted.distance2(dp.p));
			}

			// compute the prune threshold based on the standard deviation. well variance really
			double stdev = 0;
			for (int i = 0; i < errors.size; i++) {
				stdev += errors.get(i);
			}

			// prune points 3 standard deviations away
			// Don't prune if 3 standard deviations is less than 1.5 pixels since that's about what
			// you would expect and you might make the solution worse
			double sigma3 = Math.max(1.5, 4*stdev);

			for (int i = 0; i < points.size(); i++) {
				if (errors.get(i) < sigma3) {
					filtered.add(points.get(i));
				}
			}
			// recompute pose esitmate without the outliers
			if (filtered.size() != points.size()) {
				if (!estimatePnP.process(filtered, initialEstimate)) {
					return false;
				}
			}
		} else {
			filtered.addAll(points);
		}
		return refinePnP.fitModel(points, initialEstimate, fiducialToCamera);
	}

	/**
	 * Returns a list of detected control points in the image for the specified fiducial. Observations
	 * will be in distorted image pixels.
	 */
	public abstract List<PointIndex2D_F64> getDetectedControl( int which );

	/**
	 * 3D location of control points in the fiducial reference frame
	 *
	 * @return 3D location of control points
	 */
	protected abstract List<Point2D3D> getControl3D( int which );

	@Override
	public boolean is3D() {
		return hasCameraModel;
	}
}
