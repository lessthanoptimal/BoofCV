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
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.VerbosePrint;
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

		FastAccess<SelfCalibrationLinearDualQuadratic.Intrinsic> solutions = selfCalib.getSolutions();
		if (solutions.size != views.size()) {
			if (verbose != null) verbose.println("FAILED solution.size miss match");
			return false;
		}

		if (!reformatResults(cameraMatrices, solutions, metricViews))
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
	 * Given the results from self calibration, reformat them so fit the expected format specified by the interface
	 *
	 * @param cameraMatrices (Input)
	 * @param solutions (Input)
	 * @param metricViews (Output)
	 * @return true if successful
	 */
	boolean reformatResults( List<DMatrixRMaj> cameraMatrices,
							 FastAccess<SelfCalibrationLinearDualQuadratic.Intrinsic> solutions,
							 MetricCameras metricViews ) {
		for (int i = 0; i < solutions.size; i++) {
			solutions.get(i).copyTo(metricViews.intrinsics.grow());
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
	boolean checkBehindCamera(  int numViews, int numObservations ) {
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

	@Override public int getMinimumViews() { return selfCalib.getMinimumProjectives(); }

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(verbose, configuration, resolveSign);
	}
}
