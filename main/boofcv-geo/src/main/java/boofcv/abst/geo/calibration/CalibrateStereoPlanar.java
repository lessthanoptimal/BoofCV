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

package boofcv.abst.geo.calibration;

import boofcv.abst.geo.bundle.MetricBundleAdjustmentUtils;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.bundle.BundleAdjustmentMetricResidualFunction;
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.geo.bundle.CodecSceneStructureMetric;
import boofcv.alg.geo.bundle.cameras.BundlePinholeBrown;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.fitting.se.FitSpecialEuclideanOps_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import lombok.Getter;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Given a sequence of observations from a stereo camera compute the intrinsic calibration
 * of each camera and the extrinsic calibration between the two cameras. A Planar calibration
 * grid is used, which must be completely visible in all images.
 * </p>
 *
 * <p>
 * Calibration is performed by first independently determining the intrinsic parameters of each camera as well as
 * their extrinsic parameters relative to the calibration grid. Then the extrinsic parameters between the two cameras
 * is found by creating two point clouds composed of the calibration points in each camera's view. Then the rigid
 * body motion is found which transforms one point cloud into the other.
 * </p>
 *
 * <p>
 * See comments in {@link CalibrateMonoPlanar} about when the y-axis should be inverted.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CalibrateStereoPlanar implements VerbosePrint {

	// transform from world to camera in each view
	List<Se3_F64> viewLeft = new ArrayList<>();
	List<Se3_F64> viewRight = new ArrayList<>();

	// calibrates the left and right camera image
	@Getter CalibrateMonoPlanar calibLeft;
	@Getter CalibrateMonoPlanar calibRight;

	List<Point2D_F64> layout;

	MetricBundleAdjustmentUtils bundleUtils = new MetricBundleAdjustmentUtils();

	@Nullable PrintStream verbose;

	/**
	 * Configures stereo calibration
	 *
	 * @param layout How calibration points are laid out on the target
	 */
	public CalibrateStereoPlanar( List<Point2D_F64> layout ) {
		calibLeft = new CalibrateMonoPlanar(layout);
		calibRight = new CalibrateMonoPlanar(layout);
		this.layout = layout;
	}

	/**
	 * Puts the class into its initial state.
	 */
	public void reset() {
		viewLeft.clear();
		viewRight.clear();
		calibLeft.reset();
		calibRight.reset();
	}

	/**
	 * Specify calibration assumptions.
	 *
	 * @param assumeZeroSkew If true zero skew is assumed.
	 * @param numRadialParam Number of radial parameters
	 * @param includeTangential If true it will estimate tangential distortion parameters.
	 */
	public void configure( boolean assumeZeroSkew,
						   int numRadialParam,
						   boolean includeTangential ) {
		calibLeft.configurePinhole(assumeZeroSkew, numRadialParam, includeTangential);
		calibRight.configurePinhole(assumeZeroSkew, numRadialParam, includeTangential);
	}

	/**
	 * Adds a pair of images that observed the same target.
	 *
	 * @param left Image of left target.
	 * @param right Image of right target.
	 */
	public void addPair( CalibrationObservation left, CalibrationObservation right ) {
		calibLeft.addImage(left);
		calibRight.addImage(right);
	}

	/**
	 * Compute stereo calibration parameters
	 *
	 * @return Stereo calibration parameters
	 */
	public StereoParameters process() {

		// calibrate left and right cameras
		if (verbose != null) verbose.println("Mono Left");
		CameraPinholeBrown leftParam = calibrateMono(calibLeft, viewLeft);
		if (verbose != null) verbose.println("Mono right");
		CameraPinholeBrown rightParam = calibrateMono(calibRight, viewRight);

		// fit motion from right to left
		Se3_F64 rightToLeft = computeRightToLeft();

		var results = new StereoParameters(leftParam, rightParam, rightToLeft);
		refineAll(results);
		return results;
	}

	/**
	 * Compute intrinsic calibration for one of the cameras
	 */
	private CameraPinholeBrown calibrateMono( CalibrateMonoPlanar calib, List<Se3_F64> location ) {
		calib.setVerbose(verbose, null);
		CameraPinholeBrown intrinsic = calib.process();

		SceneStructureMetric structure = calib.getStructure();

		for (int i = 0; i < structure.motions.size; i++) {
			location.add(structure.motions.data[i].motion);
		}

		return intrinsic;
	}

	/**
	 * Creates two 3D point clouds for the left and right camera using the known calibration points and camera
	 * calibration. Then find the optimal rigid body transform going from the right to left views.
	 *
	 * @return Transform from right to left view.
	 */
	private Se3_F64 computeRightToLeft() {
		// location of points in the world coordinate system
		List<Point2D_F64> points2D = layout;
		List<Point3D_F64> points3D = new ArrayList<>();

		for (Point2D_F64 p : points2D) { // lint:forbidden ignore_line
			points3D.add(new Point3D_F64(p.x, p.y, 0));
		}

		// create point cloud in each view
		List<Point3D_F64> left = new ArrayList<>();
		List<Point3D_F64> right = new ArrayList<>();

		for (int i = 0; i < viewLeft.size(); i++) {
			Se3_F64 worldToLeft = viewLeft.get(i);
			Se3_F64 worldToRight = viewRight.get(i);

			// These points can really be arbitrary and don't have to be target points
			for (Point3D_F64 p : points3D) { // lint:forbidden ignore_line
				Point3D_F64 l = SePointOps_F64.transform(worldToLeft, p, null);
				Point3D_F64 r = SePointOps_F64.transform(worldToRight, p, null);

				left.add(l);
				right.add(r);
			}
		}

		// find the transform from right to left cameras
		return FitSpecialEuclideanOps_F64.fitPoints3D(right, left);
	}

	/**
	 * Jointly refines both cameras together
	 *
	 * @param parameters (input) initial estimate and is updated if refine is successful
	 */
	private void refineAll( StereoParameters parameters ) {

		Se3_F64 left_to_right = parameters.right_to_left.invert(null);

		final SceneStructureMetric structure = bundleUtils.getStructure();
		final SceneObservations observations = bundleUtils.getObservations();

		final SceneStructureMetric structureLeft = calibLeft.getStructure();
		final SceneStructureMetric structureRight = calibRight.getStructure();

		int numViews = structureLeft.views.size;

		// left and right cameras. n views, and 1 known calibration target
		structure.initialize(2, numViews*2, numViews + 1, layout.size(), 1);
		// initialize the cameras
		structure.setCamera(0, false, structureLeft.cameras.get(0).model);
		structure.setCamera(1, false, structureRight.cameras.get(0).model);
		// configure the known calibration target
		structure.setRigid(0, true, new Se3_F64(), layout.size());
		SceneStructureMetric.Rigid rigid = structure.rigids.data[0];
		for (int i = 0; i < layout.size(); i++) {
			rigid.setPoint(i, layout.get(i).x, layout.get(i).y, 0);
		}

		// initialize the views. Right views will be relative to left and will share the same baseline
		int left_to_right_idx = structure.addMotion(false, left_to_right);
		for (int viewIndex = 0; viewIndex < numViews; viewIndex++) {
			int world_to_left_idx = structure.addMotion(false, structureLeft.motions.get(viewIndex).motion);
			structure.setView(viewIndex*2, 0, world_to_left_idx, -1);
			structure.setView(viewIndex*2 + 1, 1, left_to_right_idx, viewIndex*2);
		}

		// Add observations for left and right camera
		observations.initialize(structure.views.size, true);
		for (int viewIndex = 0; viewIndex < numViews; viewIndex++) {
			SceneObservations.View oviewLeft = observations.getViewRigid(viewIndex*2);
			CalibrationObservation left = calibLeft.observations.get(viewIndex);
			for (int j = 0; j < left.size(); j++) {
				PointIndex2D_F64 p = left.get(j);
				oviewLeft.add(p.index, (float)p.p.x, (float)p.p.y);
				structure.connectPointToView(p.index, viewIndex*2);
			}
		}
		for (int viewIndex = 0; viewIndex < numViews; viewIndex++) {
			SceneObservations.View oviewRight = observations.getViewRigid(viewIndex*2 + 1);
			CalibrationObservation right = calibRight.observations.get(viewIndex);
			for (int j = 0; j < right.size(); j++) {
				PointIndex2D_F64 p = right.get(j);
				oviewRight.add(p.index, (float)p.p.x, (float)p.p.y);
				structure.connectPointToView(p.index, viewIndex*2 + 1);
			}
		}

		if (verbose != null) verbose.println("Joint bundle adjustment");
		if (!bundleUtils.process())
			return;

		// save the output
		structure.motions.get(left_to_right_idx).motion.invert(parameters.right_to_left);
		BundleAdjustmentOps.convert(((BundlePinholeBrown)structure.cameras.get(0).model),
				parameters.left.width, parameters.left.height, parameters.left);
		BundleAdjustmentOps.convert(((BundlePinholeBrown)structure.cameras.get(1).model),
				parameters.left.width, parameters.left.height, parameters.right);
	}

	public void printStatistics() {
		List<ImageResults> errors = computeErrors();

		double totalError = 0;
		for (int i = 0; i < errors.size(); i++) {
			ImageResults r = errors.get(i);
			totalError += r.meanError;

			String side = (i%2 == 0) ? "left" : "right";
			System.out.printf("%5s %3d Euclidean ( mean = %7.1e max = %7.1e ) bias ( X = %8.1e Y %8.1e )\n",
					side, i/2, r.meanError, r.maxError, r.biasX, r.biasY);
		}
		System.out.println("Average Mean Error = " + (totalError/errors.size()));
	}

	public List<ImageResults> computeErrors() {
		final SceneStructureMetric structure = bundleUtils.getStructure();
		final SceneObservations observations = bundleUtils.getObservations();
		List<ImageResults> errors = new ArrayList<>();

		double[] parameters = new double[structure.getParameterCount()];
		double[] residuals = new double[observations.getObservationCount()*2];
		CodecSceneStructureMetric codec = new CodecSceneStructureMetric();
		codec.encode(structure, parameters);

		BundleAdjustmentMetricResidualFunction function = new BundleAdjustmentMetricResidualFunction();
		function.configure(structure, observations);
		function.process(parameters, residuals);

		int idx = 0;
		for (int i = 0; i < observations.viewsRigid.size; i++) {
			SceneObservations.View v = observations.viewsRigid.data[i];
			ImageResults r = new ImageResults(v.size());

			double sumX = 0;
			double sumY = 0;
			double meanErrorMag = 0;
			double maxError = 0;

			for (int j = 0; j < v.size(); j++) {
				double x = residuals[idx++];
				double y = residuals[idx++];
				double nerr = r.pointError[j] = Math.sqrt(x*x + y*y);

				meanErrorMag += nerr;
				maxError = Math.max(maxError, nerr);

				sumX += x;
				sumY += y;
			}

			r.biasX = sumX/v.size();
			r.biasY = sumY/v.size();
			r.meanError = meanErrorMag/v.size();
			r.maxError = maxError;

			errors.add(r);
		}

		return errors;
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = out;
	}
}
