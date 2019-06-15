/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.calib.CameraModel;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ejml.data.DMatrixRMaj;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Full implementation of the Zhang99 camera calibration algorithm using planar calibration targets.  First
 * linear approximations of camera parameters are computed, which are then refined using non-linear estimation.
 * One difference from the original paper is that tangential distortion can be included. No linear estimate
 * if found for tangential, they are estimated by initializing the non-linear estimate with all zero.
 * </p>
 *
 * <p>
 * When processing the results be sure to take in account the coordinate system being left or right handed.  Calibration
 * works just fine with either coordinate system, but most 3D geometric algorithms assume a right handed coordinate
 * system while most images are left handed.
 * </p>
 *
 * <p>
 * A listener can be provide that will give status updates and allows requests for early termination.  If a request
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
public class CalibrationPlanarGridZhang99 {

	Zhang99Camera cameraGenerator;

	// estimation algorithms
	private Zhang99ComputeTargetHomography computeHomography;
	private Zhang99CalibrationMatrixFromHomographies computeK;
	private RadialDistortionEstimateLinear computeRadial;
	private Zhang99DecomposeHomography decomposeH = new Zhang99DecomposeHomography();

	// contains found parameters
	public SceneStructureMetric structure;
	public SceneObservations observations;

	// provides information on calibration status
	private Listener listener;

	// where calibration points are layout on the target.
	private List<Point2D_F64> layout;

	// Use a robust non-linear solver. This can run significantly slower
	private boolean robust=false;

	private PrintStream verbose = null;

	/**
	 * Configures calibration process.
	 *
	 * @param layout Layout of calibration points on the target
	 */
	public CalibrationPlanarGridZhang99(List<Point2D_F64> layout, Zhang99Camera cameraGenerator)
	{
		this.cameraGenerator = cameraGenerator;
		this.layout = layout;
		computeHomography = new Zhang99ComputeTargetHomography(layout);
		computeK = new Zhang99CalibrationMatrixFromHomographies(cameraGenerator.isZeroSkew());
		computeRadial = new RadialDistortionEstimateLinear(layout,cameraGenerator.numRadial());
	}

	/**
	 * Used to listen in on progress and request that processing be stopped
	 *
	 * @param listener The listener
	 */
	public void setListener(Listener listener) {
		this.listener = listener;
	}

	/**
	 * Processes observed calibration point coordinates and computes camera intrinsic and extrinsic
	 * parameters.
	 *
	 * @param observations Set of observed grid locations in pixel coordinates.
	 * @return true if successful and false if it failed
	 */
	public boolean process( List<CalibrationObservation> observations ) {

		// compute initial parameter estimates using linear algebra
		if( !linearEstimate(observations) )
			return false;

		status("Non-linear refinement");
		// perform non-linear optimization to improve results
		if( !performBundleAdjustment())
			return false;

		return true;
	}

	/**
	 * Find an initial estimate for calibration parameters using linear techniques.
	 */
	protected boolean linearEstimate(List<CalibrationObservation> observations  )
	{
		status("Estimating Homographies");
		List<DMatrixRMaj> homographies = new ArrayList<>();
		List<Se3_F64> motions = new ArrayList<>();

		for( CalibrationObservation obs : observations ) {
			if( !computeHomography.computeHomography(obs) )
				return false;

			DMatrixRMaj H = computeHomography.getHomography();

			homographies.add(H);
		}

		status("Estimating Calibration Matrix");
		computeK.process(homographies);

		DMatrixRMaj K = computeK.getCalibrationMatrix();

		decomposeH.setCalibrationMatrix(K);
		for( DMatrixRMaj H : homographies ) {
			motions.add(decomposeH.decompose(H));
		}

		status("Estimating Radial Distortion");
		computeRadial.process(K, homographies, observations);

		double distort[] = computeRadial.getParameters();

		convertIntoBundleStructure(motions, K,distort,observations);
		return true;
	}

	private void status( String message ) {
		if( listener != null ) {
			if( !listener.zhangUpdate(message) )
				throw new RuntimeException("User requested termination of calibration");
		}
	}

	/**
	 * Use non-linear optimization to improve the parameter estimates
	 */
	public boolean performBundleAdjustment()
	{
		// Configure the sparse Levenberg-Marquardt solver
		ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
		configLM.hessianScaling = false;

		ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
		configSBA.configOptimizer = configLM;

		BundleAdjustment<SceneStructureMetric> bundleAdjustment;
		if( robust ) {
			configLM.mixture = 0;
			bundleAdjustment = FactoryMultiView.bundleDenseMetric(true,configSBA);
		} else {
			bundleAdjustment = FactoryMultiView.bundleSparseMetric(configSBA);
		}

		bundleAdjustment.setVerbose(verbose,0);
		// Specifies convergence criteria
		bundleAdjustment.configure(1e-20, 1e-20, 200);

		bundleAdjustment.setParameters(structure,observations);
		return bundleAdjustment.optimize(structure);
	}

	/**
	 * Convert it into a data structure understood by {@link BundleAdjustment}
	 */
	public void convertIntoBundleStructure(List<Se3_F64> motions,
										   DMatrixRMaj K,
										   double[] distort,
										   List<CalibrationObservation> obs ) {

		structure = new SceneStructureMetric(false);
		observations = new SceneObservations(motions.size(),true);

		structure.initialize(1,motions.size(),layout.size(),1);

		// A single camera is assumed, that's what is being calibrated!
		structure.setCamera(0,false,cameraGenerator.initalizeCamera(K,distort));
		// A single rigid planar target is being viewed. It is assumed to be centered at the origin
		structure.setRigid(0,true,new Se3_F64(),layout.size());
		// Where the points are on the calibration target
		SceneStructureMetric.Rigid rigid = structure.rigids.data[0];
		for (int i = 0; i < layout.size(); i++) {
			rigid.setPoint(i,layout.get(i).x,layout.get(i).y,0);
		}

		// Add the initial estimate of each view's location and the points observed
		for (int viewIdx = 0; viewIdx < motions.size(); viewIdx++) {
			structure.setView(viewIdx,false,motions.get(viewIdx));
			SceneObservations.View v = observations.getViewRigid(viewIdx);
			structure.connectViewToCamera(viewIdx,0);
			CalibrationObservation ca = obs.get(viewIdx);
			for (int j = 0; j < ca.size(); j++) {
				PointIndex2D_F64 p = ca.get(j);
				v.add(p.index, (float)p.x, (float)p.y);
				structure.connectPointToView(p.index,viewIdx);
			}
		}
	}

	public List<ImageResults> computeErrors() {
		List<ImageResults> errors = new ArrayList<>();

		double[] parameters = new double[structure.getParameterCount()];
		double[] residuals = new double[observations.getObservationCount()*2];
		CodecSceneStructureMetric codec = new CodecSceneStructureMetric();
		codec.encode(structure,parameters);

		BundleAdjustmentMetricResidualFunction function = new BundleAdjustmentMetricResidualFunction();
		function.configure(structure,observations);
		function.process(parameters,residuals);

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
				maxError = Math.max(maxError,nerr);

				sumX += x;
				sumY += y;
			}

			r.biasX = sumX / v.size();
			r.biasY = sumY / v.size();
			r.meanError = meanErrorMag / v.size();
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
	public static void applyDistortion(Point2D_F64 normPt, double[] radial, double t1 , double t2 )
	{
		final double x = normPt.x;
		final double y = normPt.y;

		double a = 0;
		double r2 = x*x + y*y;
		double r2i = r2;
		for( int i = 0; i < radial.length; i++ ) {
			a += radial[i]*r2i;
			r2i *= r2;
		}

		normPt.x = x + x*a + 2*t1*x*y + t2*(r2 + 2*x*x);
		normPt.y = y + y*a + t1*(r2 + 2*y*y) + 2*t2*x*y;
	}

	public SceneStructureMetric getStructure() {
		return structure;
	}

	public void setVerbose( PrintStream out , int level ) {
		this.verbose = out;
	}

	public void setRobust( boolean robust ) {
		this.robust = robust;
	}

	public static int totalPoints( List<CalibrationObservation> observations ) {
		int total = 0;
		for (int i = 0; i < observations.size(); i++) {
			total += observations.get(i).size();
		}
		return total;
	}

	public interface Listener
	{
		/**
		 * Updated to update the status and request that processing be stopped
		 *
		 * @param taskName Name of the task being performed
		 * @return true to continue and false to request a stop
		 */
		boolean zhangUpdate( String taskName );
	}
}
