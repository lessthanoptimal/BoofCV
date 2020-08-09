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
import boofcv.alg.geo.selfcalib.SelfCalibrationEssentialGuessAndCheck;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTuple;
import boofcv.struct.image.ImageDimension;
import lombok.Getter;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

import static boofcv.misc.BoofMiscOps.assertBoof;

/**
 * Wrapper around {@link SelfCalibrationEssentialGuessAndCheck} for {@link ProjectiveToMetricCameras}.
 *
 * @author Peter Abeles
 */
public class ProjectiveToMetricCameraEssentialGuessAndCheck implements ProjectiveToMetricCameras {

	final @Getter SelfCalibrationEssentialGuessAndCheck selfCalib;

	//--------------- Internal Work Space
	DMatrixRMaj K = new DMatrixRMaj(3,3);
	FastQueue<AssociatedPair> pairs = new FastQueue<>(AssociatedPair::new);
	DMatrixRMaj F21 = new DMatrixRMaj(3,3);

	public ProjectiveToMetricCameraEssentialGuessAndCheck(SelfCalibrationEssentialGuessAndCheck selfCalib) {
		this.selfCalib = selfCalib;
	}

	@Override
	public boolean process(List<ImageDimension> dimensions, List<DMatrixRMaj> views,
						   List<AssociatedTuple> observations, MetricCameras metricViews)
	{
		assertBoof(views.size()+1==dimensions.size());
		metricViews.reset();

		// initialize
		selfCalib.imageLengthPixels = dimensions.get(0).getMaxLength();

		// Convert the projective cameras into a fundamental matrix
		DMatrixRMaj P2 = views.get(0);
		MultiViewOps.projectiveToFundamental(P2,F21);

		// Convert observations into AssociatedPairs
		MultiViewOps.convert(observations,0,1,pairs);

		// Projective to Metric calibration
		if( !selfCalib.process(F21,P2,pairs.toList()) )
			return false;
//		if( alg.isLimit )
//			return false;
		final DMatrixRMaj H = selfCalib.rectifyingHomography;

		// it solved directoy for the focal lengths in the first two views
		metricViews.intrinsics.grow().fsetK(selfCalib.focalLengthA,selfCalib.focalLengthA,0,0,0,-1,-1);
		metricViews.intrinsics.grow().fsetK(selfCalib.focalLengthB,selfCalib.focalLengthB,0,0,0,-1,-1);
		// extract camera motion for view 2 using found
		PerspectiveOps.pinholeToMatrix(metricViews.intrinsics.get(1),K);
		if( !MultiViewOps.projectiveToMetricKnownK(P2,H,K,metricViews.motion_1_to_k.grow()) )
			return false;
		// For the remaining views use H to find motion and intrinsics
		for (int viewIdx = 1; viewIdx < views.size(); viewIdx++) {
			if( !MultiViewOps.projectiveToMetric(views.get(viewIdx),H,metricViews.motion_1_to_k.grow(),K) )
				return false;
			PerspectiveOps.matrixToPinhole(K,-1,-1,metricViews.intrinsics.grow());
		}

		return true;
	}

	@Override
	public int getMinimumViews() {
		return 2;
	}
}
