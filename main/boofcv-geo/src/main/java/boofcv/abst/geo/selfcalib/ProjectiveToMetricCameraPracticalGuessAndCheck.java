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

import boofcv.alg.geo.MetricCameras;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.selfcalib.ResolveSignAmbiguityPositiveDepth;
import boofcv.alg.geo.selfcalib.SelfCalibrationPraticalGuessAndCheckFocus;
import boofcv.struct.geo.AssociatedTuple;
import boofcv.struct.image.ImageDimension;
import lombok.Getter;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.List;

import static boofcv.misc.BoofMiscOps.assertBoof;

/**
 * Wrapper around {@link SelfCalibrationPraticalGuessAndCheckFocus} for {@link ProjectiveToMetricCameras}.
 *
 * @author Peter Abeles
 */
public class ProjectiveToMetricCameraPracticalGuessAndCheck implements ProjectiveToMetricCameras {

	final @Getter SelfCalibrationPraticalGuessAndCheckFocus selfCalib;

	// intrinsic calibration matrix
	final DMatrixRMaj K = new DMatrixRMaj(3,3);

	final ResolveSignAmbiguityPositiveDepth resolveSign = new ResolveSignAmbiguityPositiveDepth();

	public ProjectiveToMetricCameraPracticalGuessAndCheck(SelfCalibrationPraticalGuessAndCheckFocus selfCalib) {
		this.selfCalib = selfCalib;
	}

	@Override
	public boolean process(List<ImageDimension> dimensions, List<DMatrixRMaj> views,
						   List<AssociatedTuple> observations, MetricCameras metricViews)
	{
		assertBoof(views.size()+1==dimensions.size());
		metricViews.reset();

		// tell it the image size
		ImageDimension dimension = dimensions.get(0);
		selfCalib.setCamera(0.0,0.0,0.0,dimension.width, dimension.height);

		// Perform self calibration
		if( !selfCalib.process(views) )
			return false;

		DMatrixRMaj H = selfCalib.getRectifyingHomography();

		// the top left 3x3 matrix is K in view 1
		CommonOps_DDRM.extract(H,0,0,K);
		PerspectiveOps.matrixToPinhole(K,-1,-1,metricViews.intrinsics.grow());

		// Get the solution for the remaining cameras / views
		for (int viewIdx = 0; viewIdx < views.size(); viewIdx++) {
			DMatrixRMaj P = views.get(viewIdx);
			if( !MultiViewOps.projectiveToMetric(P,H,metricViews.motion_1_to_k.grow(),K) )
				return false;
			PerspectiveOps.matrixToPinhole(K,-1,-1,metricViews.intrinsics.grow());
		}

		resolveSign.process(observations, metricViews);

		return true;
	}

	@Override
	public int getMinimumViews() {
		return 2;
	}
}
