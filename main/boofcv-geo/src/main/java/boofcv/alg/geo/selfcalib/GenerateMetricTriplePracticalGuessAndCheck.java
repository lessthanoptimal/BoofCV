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

package boofcv.alg.geo.selfcalib;

import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.robust.ModelGeneratorViews;
import boofcv.alg.geo.trifocal.TrifocalExtractGeometries;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.struct.image.ImageDimension;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes a calibrating homography from a set of image triplets.
 *
 * @see SelfCalibrationGuessAndCheckFocus
 *
 * @author Peter Abeles
 */
public class GenerateMetricTriplePracticalGuessAndCheck implements
		ModelGeneratorViews<MetricCameraTriple, AssociatedTriple, ImageDimension> {

	Estimate1ofTrifocalTensor trifocal;
	SelfCalibrationGuessAndCheckFocus alg;

	// used to get camera matrices from the trifocal tensor
	final TrifocalExtractGeometries extractor = new TrifocalExtractGeometries();

	// Storage for the list of found camera matrices
	final List<DMatrixRMaj> cameraMatrices = new ArrayList<>();

	// storage for camera matrices
	final DMatrixRMaj P2 = new DMatrixRMaj(3,4);
	final DMatrixRMaj P3 = new DMatrixRMaj(3,4);

	//--------------- Internal Work Space
	final TrifocalTensor tensor = new TrifocalTensor();
	// intrinsic calibration matrix
	final DMatrixRMaj K = new DMatrixRMaj(3,3);

	public GenerateMetricTriplePracticalGuessAndCheck(Estimate1ofTrifocalTensor trifocal, SelfCalibrationGuessAndCheckFocus alg) {
		this.trifocal = trifocal;
		this.alg = alg;
	}

	protected GenerateMetricTriplePracticalGuessAndCheck() {}

	@Override
	public boolean generate(List<AssociatedTriple> dataSet, MetricCameraTriple result) {
		// Get trifocal tensor
		if( !trifocal.process(dataSet,tensor) )
			return false;

		// Get camera matrices from trifocal
		extractor.setTensor(tensor);
		extractor.extractCamera(P2,P3);

		cameraMatrices.clear();
		cameraMatrices.add(P2);
		cameraMatrices.add(P3);

		if( !alg.process(cameraMatrices) )
			return false;

		DMatrixRMaj H = alg.getRectifyingHomography();

		// the top left 3x3 matrix is K in view 1
		CommonOps_DDRM.extract(H,0,0,K);
		PerspectiveOps.matrixToPinhole(K,-1,-1,result.view1);
		if( !MultiViewOps.projectiveToMetric(P2,H,result.view_1_to_2,K) )
			return false;
		PerspectiveOps.matrixToPinhole(K,-1,-1,result.view2);
		if( !MultiViewOps.projectiveToMetric(P3,H,result.view_1_to_3,K) )
			return false;
		PerspectiveOps.matrixToPinhole(K,-1,-1,result.view3);

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return trifocal.getMinimumPoints();
	}

	@Override
	public void setView(int view, ImageDimension viewInfo) {
		if( view != 0 )
			return;
		alg.setCamera(0.0,0.0,0.0,viewInfo.width, viewInfo.height);
	}

	@Override
	public int getNumberOfViews() {
		return 3;
	}
}
