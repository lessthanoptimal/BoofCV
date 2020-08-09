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

package boofcv.abst.geo.selfcalib;

import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.MetricCameras;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.selfcalib.ResolveSignAmbiguityPositiveDepth;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic;
import boofcv.alg.geo.structure.DecomposeAbsoluteDualQuadratic;
import boofcv.struct.geo.AssociatedTuple;
import boofcv.struct.image.ImageDimension;
import lombok.Getter;
import org.ddogleg.struct.FastAccess;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.List;

import static boofcv.misc.BoofMiscOps.assertBoof;

/**
 * Wrapper around {@link SelfCalibrationLinearDualQuadratic} for {@link ProjectiveToMetricCameras}.
 *
 * @author Peter Abeles
 */
public class ProjectiveToMetricCameraDualQuadratic implements ProjectiveToMetricCameras {

	final @Getter SelfCalibrationLinearDualQuadratic selfCalib;

	// used to get camera matrices from the trifocal tensor
	final DecomposeAbsoluteDualQuadratic decomposeDualQuad = new DecomposeAbsoluteDualQuadratic();

	// storage for camera matrices
	final DMatrixRMaj P1 = CommonOps_DDRM.identity(3,4);

	// rectifying homography
	final DMatrixRMaj H = new DMatrixRMaj(4,4);
	// intrinsic calibration matrix
	final DMatrixRMaj K = new DMatrixRMaj(3,3);

	final ResolveSignAmbiguityPositiveDepth resolveSign = new ResolveSignAmbiguityPositiveDepth();

	public ProjectiveToMetricCameraDualQuadratic(SelfCalibrationLinearDualQuadratic selfCalib) {
		this.selfCalib = selfCalib;
	}

	@Override
	public boolean process(List<ImageDimension> dimensions, List<DMatrixRMaj> views,
						   List<AssociatedTuple> observations, MetricCameras metricViews)
	{
		assertBoof(views.size()+1==dimensions.size());
		metricViews.reset();

		// Determine metric parameters
		selfCalib.reset();
		selfCalib.addCameraMatrix(P1);
		for (int i = 0; i < views.size(); i++) {
			selfCalib.addCameraMatrix(views.get(i));
		}

		GeometricResult results = selfCalib.solve();
		if( results != GeometricResult.SUCCESS )
			return false;

		// Convert results into results format
		if( !decomposeDualQuad.decompose(selfCalib.getQ()) )
			return false;

		if( !decomposeDualQuad.computeRectifyingHomography(H) )
			return false;

		FastAccess<SelfCalibrationLinearDualQuadratic.Intrinsic> solutions = selfCalib.getSolutions();
		if( solutions.size != 3 )
			return false;

		for (int i = 0; i < solutions.size; i++) {
			solutions.get(i).copyTo(metricViews.intrinsics.grow());
		}

		// skip the first view since it's the origin and already known
		for (int i = 0; i < views.size(); i++) {
			DMatrixRMaj P = views.get(i);
			PerspectiveOps.pinholeToMatrix(metricViews.intrinsics.get(i+1),K);
			if( !MultiViewOps.projectiveToMetricKnownK(P,H,K,metricViews.motion_1_to_k.grow()) )
				return false;
		}

		resolveSign.process(observations, metricViews);

		return true;
	}

	@Override
	public int getMinimumViews() {
		return selfCalib.getMinimumProjectives();
	}
}
