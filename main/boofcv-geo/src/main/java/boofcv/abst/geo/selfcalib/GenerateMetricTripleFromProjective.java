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

import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.alg.geo.MetricCameras;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.robust.ModelGeneratorViews;
import boofcv.alg.geo.selfcalib.MetricCameraTriple;
import boofcv.alg.geo.trifocal.TrifocalExtractGeometries;
import boofcv.struct.calib.ElevateViewInfo;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.AssociatedTuple;
import boofcv.struct.geo.AssociatedTupleN;
import boofcv.struct.geo.TrifocalTensor;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Wrapper around {@link ProjectiveToMetricCameras} and {@link Estimate1ofTrifocalTensor} for use in robust model
 * fitting.
 *
 * @author Peter Abeles
 */
public class GenerateMetricTripleFromProjective implements
		ModelGeneratorViews<MetricCameraTriple, AssociatedTriple, ElevateViewInfo> {
	// Computes a trifocal tensor from input observations from which projective cameras are extracted
	public Estimate1ofTrifocalTensor trifocal;
	// from projective cameras computes metric cameras
	public ProjectiveToMetricCameras projectiveToMetric;
	// used to get camera matrices from the trifocal tensor
	@Getter final TrifocalExtractGeometries extractor = new TrifocalExtractGeometries();
	@Getter final TrifocalTensor tensor = new TrifocalTensor();

	// storage for camera matrices
	@Getter final DMatrixRMaj P2;
	@Getter final DMatrixRMaj P3;

	// Data structures which have been converted
	@Getter final DogArray<AssociatedTuple> observationsN = new DogArray<>(() -> new AssociatedTupleN(3));
	@Getter final DogArray<DMatrixRMaj> projective = new DogArray<>(() -> new DMatrixRMaj(3, 4));
	@Getter final DogArray<ElevateViewInfo> views = new DogArray<>(ElevateViewInfo::new);
	@Getter final MetricCameras metricN = new MetricCameras();

	public GenerateMetricTripleFromProjective( Estimate1ofTrifocalTensor trifocal,
											   ProjectiveToMetricCameras projectiveToMetric ) {
		this.trifocal = trifocal;
		this.projectiveToMetric = projectiveToMetric;

		views.resize(3);
		projective.resize(2);
		P2 = projective.get(0);
		P3 = projective.get(1);
	}

	@Override
	public void setView( int view, ElevateViewInfo viewInfo ) {
		views.get(view).setTo(viewInfo);
	}

	@Override
	public int getNumberOfViews() {
		return 3;
	}

	@Override
	public boolean generate( List<AssociatedTriple> observationTriple, MetricCameraTriple output ) {
		// Get trifocal tensor
		if (!trifocal.process(observationTriple, tensor))
			return false;

		// Get camera matrices from trifocal
		extractor.setTensor(tensor);
		extractor.extractCamera(P2, P3);

		MultiViewOps.convertTr(observationTriple, observationsN);

		if (!projectiveToMetric.process(views.toList(), projective.toList(), observationsN.toList(), metricN))
			return false;

		// Converts the output
		output.view_1_to_2.setTo(metricN.motion_1_to_k.get(0));
		output.view_1_to_3.setTo(metricN.motion_1_to_k.get(1));

		output.view1.setTo(metricN.intrinsics.get(0));
		output.view2.setTo(metricN.intrinsics.get(1));
		output.view3.setTo(metricN.intrinsics.get(2));

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return trifocal.getMinimumPoints();
	}
}
