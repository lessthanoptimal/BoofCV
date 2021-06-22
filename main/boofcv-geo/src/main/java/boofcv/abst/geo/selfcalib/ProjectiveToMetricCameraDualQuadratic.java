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

package boofcv.abst.geo.selfcalib;

import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.MetricCameras;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.selfcalib.RefineDualQuadraticAlgebraicError;
import boofcv.alg.geo.selfcalib.RefineDualQuadraticAlgebraicError.CameraState;
import boofcv.alg.geo.selfcalib.ResolveSignAmbiguityPositiveDepth;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic;
import boofcv.alg.geo.structure.DecomposeAbsoluteDualQuadratic;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.ElevateViewInfo;
import boofcv.struct.geo.AssociatedTuple;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrix3;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static boofcv.misc.BoofMiscOps.checkEq;

/**
 * Wrapper around {@link SelfCalibrationLinearDualQuadratic} for {@link ProjectiveToMetricCameras}.
 *
 * @author Peter Abeles
 */
public class ProjectiveToMetricCameraDualQuadratic implements ProjectiveToMetricCameras, VerbosePrint {

	/** Self calibration algorithm used internally */
	final @Getter SelfCalibrationLinearDualQuadratic selfCalib;

	/** Refines intrinsic parameter estimate using algebraic error */
	final @Getter RefineDualQuadraticAlgebraicError refiner = new RefineDualQuadraticAlgebraicError();

	/** Accept a solution if the number of invalid features is less than or equal to this fraction */
	public double invalidFractionAccept = 0.15;

	// used to get camera matrices from the trifocal tensor
	final DecomposeAbsoluteDualQuadratic decomposeDualQuad = new DecomposeAbsoluteDualQuadratic();

	// storage for camera matrices
	final DMatrixRMaj P1 = CommonOps_DDRM.identity(3, 4);

	// rectifying homography
	final DMatrixRMaj H = new DMatrixRMaj(4, 4);
	// intrinsic calibration matrix
	final DMatrixRMaj K = new DMatrixRMaj(3, 3);

	final ResolveSignAmbiguityPositiveDepth resolveSign = new ResolveSignAmbiguityPositiveDepth();

	@Nullable PrintStream verbose;

	// Workspace for refining the camera estimates
	DogArray<CameraPinhole> workCameras = new DogArray<>(CameraPinhole::new, CameraPinhole::reset);
	// Number of times each camera observed an image
	DogArray_I32 cameraCounts = new DogArray_I32();

	public ProjectiveToMetricCameraDualQuadratic( SelfCalibrationLinearDualQuadratic selfCalib ) {
		this.selfCalib = selfCalib;
	}

	@Override
	public boolean process( List<ElevateViewInfo> views, List<DMatrixRMaj> cameraMatrices,
							List<AssociatedTuple> observations, MetricCameras metricViews ) {
		checkEq(cameraMatrices.size() + 1, views.size(), "View[0] is implicitly identity and not included");
		metricViews.reset();

		// Perform self calibration and estimate the metric views
		if (!solveThenDecompose(cameraMatrices))
			return false;

		FastAccess<SelfCalibrationLinearDualQuadratic.Intrinsic> solutions = selfCalib.getIntrinsics();
		if (solutions.size != views.size()) {
			if (verbose != null) verbose.println("FAILED solution.size miss match");
			return false;
		}

		// Figure out how many cameras there are
		int numCameras = 0;
		for (int i = 0; i < views.size(); i++) {
			numCameras = Math.max(numCameras, views.get(i).cameraID);
		}
		numCameras += 1;

		// Average cameras if observed across multiple views
		averageCommonCameras(views, solutions, numCameras);

		// Refine the linear estimate and apply non-linear constraints, like same camera
		refineCamerasAlgebraic(views, cameraMatrices, numCameras);

		// Copy results into output and compute extrinsics
		if (!reformatResults(views, cameraMatrices, metricViews))
			return false;

		// Need to resolve the sign ambiguity
		resolveSign.process(observations, metricViews);

		// Sanity check results by seeing if too many points are behind the camera, which is physically impossible
		if (checkBehindCamera(cameraMatrices.size(), observations.size()))
			return true;

		// Failed, but print what it found to help debug
		if (verbose != null) printFoundMetric(views, metricViews);

		return false;
	}

	/**
	 * Performs self calibration and then finds rectifying homography
	 *
	 * @return true if successful
	 */
	boolean solveThenDecompose( List<DMatrixRMaj> views ) {
		// Determine metric parameters
		selfCalib.reset();
		selfCalib.addCameraMatrix(P1);
		for (int i = 0; i < views.size(); i++) {
			selfCalib.addCameraMatrix(views.get(i));
		}

		GeometricResult results = selfCalib.solve();
		if (results != GeometricResult.SUCCESS) {
			if (verbose != null) verbose.println("FAILED geometric");
			return false;
		}

		// Convert results into results format
		if (!decomposeDualQuad.decompose(selfCalib.getQ())) {
			if (verbose != null) verbose.println("FAILED decompose Dual Quad");
			return false;
		}

		if (!decomposeDualQuad.computeRectifyingHomography(H)) {
			if (verbose != null) verbose.println("FAILED rectify homography");
			return false;
		}

		return true;
	}

	/**
	 * This refines intrinsic parameters by minimizing algebraic error. If anything goes wrong it doesn't update
	 * the intrinsics. Also updates the rectifying homography.
	 */
	void refineCamerasAlgebraic( List<ElevateViewInfo> views, List<DMatrixRMaj> cameraMatrices, int numCameras ) {
		// Refiner can't handle non-zero skew yet
		if (!selfCalib.zeroSkew) {
			if (verbose != null) verbose.println("Skipping refine since skew is not zero");
			return;
		}

		// Just skip everything if it has been turned off
		if (refiner.converge.maxIterations <= 0)
			return;

		// Sanity check the P0 is implicit
		BoofMiscOps.checkEq(views.size(), cameraMatrices.size() + 1);

		// Make sure refiner applies the same constraints that the linear estimator applies
		refiner.knownAspect = selfCalib.isKnownAspect();
		refiner.knownPrinciplePoint = true;

		// Configure the refiner. If multiple views use the same camera this constraint is applied
		refiner.initialize(numCameras, views.size());
		DMatrix3 planeAtInfinity = selfCalib.getPlaneAtInfinity();
		refiner.setPlaneAtInfinity(planeAtInfinity.a1, planeAtInfinity.a2, planeAtInfinity.a3);

		for (int cameraIdx = 0; cameraIdx < numCameras; cameraIdx++) {
			CameraPinhole merged = workCameras.get(cameraIdx);
			refiner.setCamera(cameraIdx, merged.fx, merged.cx, merged.cy, merged.fy/merged.fx);
		}

		for (int viewIdx = 0; viewIdx < views.size(); viewIdx++) {
			if (viewIdx == 0)
				refiner.setProjective(0, P1);
			else
				refiner.setProjective(viewIdx, cameraMatrices.get(viewIdx - 1));
			refiner.setViewToCamera(viewIdx, views.get(viewIdx).cameraID);
		}

		// Refine and change nothing if it fails
		if (!refiner.refine()) {
			if (verbose != null) verbose.println("Refine failed! Ignoring results");
			return;
		}

		if (verbose != null) {
			for (int i = 0; i < numCameras; i++) {
				CameraState refined = refiner.getCameras().get(i);
				verbose.printf("refined[%d] fx=%.1f fy=%.1f\n", i, refined.fx, refined.fx*refined.aspectRatio);
			}
		}

		// Save the refined intrinsic parameters
		for (int i = 0; i < views.size(); i++) {
			ElevateViewInfo info = views.get(i);
			CameraState refined = refiner.getCameras().get(info.cameraID);
			CameraPinhole estimated = workCameras.get(info.cameraID);

			estimated.fx = refined.fx;
			estimated.fy = refined.aspectRatio*refined.fx;
			// refiner doesn't support non-zero skew yet
		}

		// Update rectifying homography using the new parameters
		// NOTE: This formulation of H requires P1=[I|0] which is true in this case
		PerspectiveOps.pinholeToMatrix(workCameras.get(views.get(0).cameraID), K);
		MultiViewOps.canonicalRectifyingHomographyFromKPinf(K, refiner.planeAtInfinity, H);
	}

	/**
	 * If multiple views use the same camera the found intrinsics will be averaged across those views.
	 */
	void averageCommonCameras( List<ElevateViewInfo> views,
							   FastAccess<SelfCalibrationLinearDualQuadratic.Intrinsic> solutions,
							   int numCameras ) {
		cameraCounts.resetResize(numCameras, 0);
		workCameras.resetResize(numCameras);
		for (int i = 0; i < views.size(); i++) {
			ElevateViewInfo info = views.get(i);
			CameraPinhole merged = workCameras.get(info.cameraID);
			SelfCalibrationLinearDualQuadratic.Intrinsic estimated = solutions.get(i);

			merged.fx += estimated.fx;
			merged.fy += estimated.fy;
			merged.skew += estimated.skew;

			cameraCounts.data[info.cameraID]++;

			if (verbose != null) {
				verbose.printf("view[%d] fx=%.1f fy=%.1f skew=%.2f\n", i, estimated.fx, estimated.fy, estimated.skew);
			}
		}

		for (int i = 0; i < numCameras; i++) {
			CameraPinhole merged = workCameras.get(i);
			int divisor = cameraCounts.get(i);
			if (divisor == 1)
				continue;

			merged.fx /= divisor;
			merged.fy /= divisor;
			merged.skew /= divisor;

			// Principle point must be zero. This is here to emphasize that
			merged.cx = 0.0;
			merged.cy = 0.0;
		}

		if (verbose != null) {
			for (int i = 0; i < numCameras; i++) {
				CameraPinhole cam = workCameras.get(i);
				verbose.printf("camera[%d] fx=%.1f fy=%.1f skew=%.2f, count=%d\n",
						i, cam.fx, cam.fy, cam.skew, cameraCounts.get(i));
			}
		}
	}

	/**
	 * Given the results from self calibration, reformat them so fit the expected format specified by the interface
	 *
	 * @param cameraMatrices (Input)
	 * @param metricViews (Output)
	 * @return true if successful
	 */
	boolean reformatResults( List<ElevateViewInfo> views,
							 List<DMatrixRMaj> cameraMatrices,
							 MetricCameras metricViews ) {

		// Copy found intrinsics over
		for (int i = 0; i < views.size(); i++) {
			metricViews.intrinsics.grow().setTo(workCameras.get(views.get(i).cameraID));
		}

		// skip the first view since it's the origin and already known
		double largestT = 0.0;
		for (int i = 0; i < cameraMatrices.size(); i++) {
			DMatrixRMaj P = cameraMatrices.get(i);
			PerspectiveOps.pinholeToMatrix(metricViews.intrinsics.get(i + 1), K);
			if (!MultiViewOps.projectiveToMetricKnownK(P, H, K, metricViews.motion_1_to_k.grow())) {
				if (verbose != null) verbose.println("FAILED projectiveToMetricKnownK");
				return false;
			}
			largestT = Math.max(largestT, metricViews.motion_1_to_k.getTail().T.norm());
		}

		// Ensure the found motion has a scale around 1.0
		for (int i = 0; i < metricViews.motion_1_to_k.size; i++) {
			metricViews.motion_1_to_k.get(i).T.divide(largestT);
		}
		return true;
	}

	/**
	 * When resolving for the sign ambiguity it has to check to see if triangulated points are behind the camera.
	 * This looks at the number of points behind the camera and decides if it can trust this solution or not
	 *
	 * @return true If it passes the test
	 */
	boolean checkBehindCamera( int numViews, int numObservations ) {
		// bestInvalid is the sum across all views. Use the average fraction across all views as the test
		double fractionInvalid = (resolveSign.bestInvalid/(double)numViews)/numObservations;
		if (fractionInvalid <= invalidFractionAccept) {
			return true;
		}

		if (verbose != null) {
			verbose.printf("FAILED: Features behind camera. fraction=%.3f threshold=%.3f\n",
					fractionInvalid, invalidFractionAccept);
		}
		return false;
	}

	private void printFoundMetric( List<ElevateViewInfo> views, MetricCameras metricViews ) {
		PrintStream verbose = Objects.requireNonNull(this.verbose);

		for (int i = 0; i < views.size(); i++) {
			CameraPinhole cam = metricViews.intrinsics.get(i);
			verbose.printf("metric[%d] fx=%.1f fy=%.1f", i, cam.fx, cam.fy);
			if (i == 0) {
				verbose.println();
				continue;
			}
			Se3_F64 m = metricViews.motion_1_to_k.get(i - 1);
			double theta = ConvertRotation3D_F64.matrixToRodrigues(m.R, null).theta;
			verbose.printf(" T=(%.2f %.2f %.2f) R=%.4f\n", m.T.x, m.T.y, m.T.z, theta);
		}
	}

	@Override public int getMinimumViews() {return selfCalib.getMinimumProjectives();}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(verbose, configuration, resolveSign, refiner);
	}
}
