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

package boofcv.alg.geo.calibration;

import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.geo.calibration.ImageResults;
import boofcv.alg.geo.bundle.BundleAdjustmentMetricResidualFunction;
import boofcv.alg.geo.bundle.CodecSceneStructureMetric;
import boofcv.alg.geo.calibration.cameras.Zhang99Camera;
import boofcv.factory.geo.ConfigBundleAdjustment;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraModel;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Full implementation of the Zhang99 camera calibration algorithm using planar calibration targets. The original
 * algorithm has been extended to support multiple camera models. The general process is described below:
 * </p>
 * <ol>
 *     <li>Linear estimate of pinhole camera parameters</li>
 *     <li>Estimate of camera pose</li>
 *     <li>{@link Zhang99Camera Camera model} specific initialization given the pinhole estimate</li>
 *     <li>Non-linear refinement of intrinsic and extrinsic parameters</li>
 * </ol>
 *
 * <p>
 * The algorithm has been extended to multiple camera models by providing each camera model an initial estimate
 * of the pinhole camera parameters with camera pose. If the camera model has radial distortion, as modeled by
 * {@link CameraPinholeBrown}, then an initial estimate of radial distortion is
 * {@link RadialDistortionEstimateLinear estimated} inside the camera model specific code. See specific cameras
 * for how they are all initialized.
 * </p>
 *
 * <p>
 * When processing the results be sure to take in account the coordinate system being left or right handed. Calibration
 * works just fine with either coordinate system, but most 3D geometric algorithms assume a right handed coordinate
 * system while most images are left handed.
 * </p>
 *
 * <p>
 * A listener can be provide that will give status updates and allows requests for early termination. If a request
 * for early termination is made then a RuntimeException will be thrown.
 * </p>
 *
 * <p>
 * [1] Zhengyou Zhang, "Flexible Camera Calibration By Viewing a Plane From Unknown Orientations,",
 * International Conference on Computer Vision (ICCV'99), Corfu, Greece, pages 666-673, September 1999.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CalibrationPlanarGridZhang99 implements VerbosePrint {

	Zhang99Camera cameraGenerator;

	/** Should it assume zero skew when estimating a pinhole camera? */
	@Getter @Setter public boolean zeroSkew = true;

	// estimation algorithms
	private final Zhang99ComputeTargetHomography computeHomography;
	private final Zhang99CalibrationMatrixFromHomographies computeK;
	private final Zhang99DecomposeHomography decomposeH = new Zhang99DecomposeHomography();

	/** contains found parameters */
	@Getter public SceneStructureMetric structure;
	public SceneObservations observations;

	/** provides information on calibration status as it's being computed */
	@Getter @Setter private Listener listener;

	// where calibration points are layout on the target.
	private final List<Point2D_F64> layout;

	/** Use a robust non-linear solver. This can run significantly slower */
	@Getter @Setter private boolean robust = false;

	private @Nullable PrintStream verbose = null;

	/**
	 * Configures calibration process.
	 *
	 * @param layout Layout of calibration points on the target
	 */
	public CalibrationPlanarGridZhang99( List<Point2D_F64> layout, Zhang99Camera cameraGenerator ) {
		this.cameraGenerator = cameraGenerator;
		this.layout = layout;
		computeHomography = new Zhang99ComputeTargetHomography(layout);
		computeK = new Zhang99CalibrationMatrixFromHomographies();
	}

	/**
	 * Processes observed calibration point coordinates and computes camera intrinsic and extrinsic
	 * parameters.
	 *
	 * @param observations Set of observed grid locations in pixel coordinates.
	 * @return true if successful and false if it failed
	 */
	public boolean process( List<CalibrationObservation> observations ) {
		computeK.setAssumeZeroSkew(zeroSkew);

		// compute initial parameter estimates using linear algebra
		if (!linearEstimate(observations))
			return false;

		status("Non-linear refinement");
		// perform non-linear optimization to improve results
		if (!performBundleAdjustment())
			return false;

		return true;
	}

	/**
	 * Find an initial estimate for calibration parameters using linear techniques.
	 */
	protected boolean linearEstimate( List<CalibrationObservation> observations ) {
		status("Estimating Homographies");
		List<DMatrixRMaj> homographies = new ArrayList<>();
		List<Se3_F64> motions = new ArrayList<>();

		for (int i = 0; i < observations.size(); i++) {
			CalibrationObservation obs = observations.get(i);

			if (!computeHomography.computeHomography(obs))
				return false;

			DMatrixRMaj H = computeHomography.getHomography();

			homographies.add(H);
		}

		status("Estimating Calibration Matrix");
		computeK.process(homographies);

		DMatrixRMaj K = computeK.getCalibrationMatrix();

		decomposeH.setCalibrationMatrix(K);
		for (int i = 0; i < homographies.size(); i++) {
			DMatrixRMaj H = homographies.get(i);
			motions.add(decomposeH.decompose(H));
		}

		status("Initial Model Parameters");

		convertIntoBundleStructure(motions, K, homographies, observations);
		return true;
	}

	private void status( String message ) {
		if (listener != null) {
			if (!listener.zhangUpdate(message))
				throw new RuntimeException("User requested termination of calibration");
		}
	}

	/**
	 * Use non-linear optimization to improve the parameter estimates
	 */
	public boolean performBundleAdjustment() {
		// Configure the sparse Levenberg-Marquardt solver
		ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
		configLM.hessianScaling = false;

		ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
		configSBA.configOptimizer = configLM;

		BundleAdjustment<SceneStructureMetric> bundleAdjustment;
		if (robust) {
			configLM.mixture = 0;
			bundleAdjustment = FactoryMultiView.bundleDenseMetric(true, configSBA);
		} else {
			bundleAdjustment = FactoryMultiView.bundleSparseMetric(configSBA);
		}

		bundleAdjustment.setVerbose(verbose, null);
		// Specifies convergence criteria
		bundleAdjustment.configure(1e-20, 1e-20, 200);

		bundleAdjustment.setParameters(structure, observations);
		return bundleAdjustment.optimize(structure);
	}

	/**
	 * Convert it into a data structure understood by {@link BundleAdjustment}
	 */
	public void convertIntoBundleStructure( List<Se3_F64> motions,
											DMatrixRMaj K,
											List<DMatrixRMaj> homographies,
											List<CalibrationObservation> calibrationObservations ) {

		structure = new SceneStructureMetric(false);
		structure.initialize(1, motions.size(), -1, layout.size(), 1);

		observations = new SceneObservations();
		observations.initialize(motions.size(), true);

		// A single camera is assumed, that's what is being calibrated!
		structure.setCamera(0, false, cameraGenerator.initializeCamera(K, homographies, calibrationObservations));
		// A single rigid planar target is being viewed. It is assumed to be centered at the origin
		structure.setRigid(0, true, new Se3_F64(), layout.size());
		// Where the points are on the calibration target
		SceneStructureMetric.Rigid rigid = structure.rigids.data[0];
		for (int i = 0; i < layout.size(); i++) {
			rigid.setPoint(i, layout.get(i).x, layout.get(i).y, 0);
		}

		// Add the initial estimate of each view's location and the points observed
		for (int viewIdx = 0; viewIdx < motions.size(); viewIdx++) {
			structure.setView(viewIdx, 0, false, motions.get(viewIdx));
			SceneObservations.View v = observations.getViewRigid(viewIdx);
			CalibrationObservation ca = calibrationObservations.get(viewIdx);
			for (int j = 0; j < ca.size(); j++) {
				PointIndex2D_F64 p = ca.get(j);
				v.add(p.index, (float)p.p.x, (float)p.p.y);
				structure.connectPointToView(p.index, viewIdx);
			}
		}
	}

	public List<ImageResults> computeErrors() {
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
				double x = r.residuals[j*2] = residuals[idx++];
				double y = r.residuals[j*2 + 1] = residuals[idx++];
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

	public CameraModel getCameraModel() {
		return cameraGenerator.getCameraModel(structure.cameras.get(0).model);
	}

	/**
	 * Applies radial and tangential distortion to the normalized image coordinate.
	 *
	 * @param normPt point in normalized image coordinates
	 * @param radial radial distortion parameters
	 * @param t1 tangential parameter
	 * @param t2 tangential parameter
	 */
	public static void applyDistortion( Point2D_F64 normPt, double[] radial, double t1, double t2 ) {
		final double x = normPt.x;
		final double y = normPt.y;

		double a = 0;
		double r2 = x*x + y*y;
		double r2i = r2;
		for (int i = 0; i < radial.length; i++) {
			a += radial[i]*r2i;
			r2i *= r2;
		}

		normPt.x = x + x*a + 2*t1*x*y + t2*(r2 + 2*x*x);
		normPt.y = y + y*a + t1*(r2 + 2*y*y) + 2*t2*x*y;
	}

	public static int totalPoints( List<CalibrationObservation> observations ) {
		int total = 0;
		for (int i = 0; i < observations.size(); i++) {
			total += observations.get(i).size();
		}
		return total;
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}

	public interface Listener {
		/**
		 * Updated to update the status and request that processing be stopped
		 *
		 * @param taskName Name of the task being performed
		 * @return true to continue and false to request a stop
		 */
		boolean zhangUpdate( String taskName );
	}
}
