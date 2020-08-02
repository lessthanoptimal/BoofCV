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
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.struct.image.ImageDimension;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Computes a calibrating homography from a set of image triplets by estimating the focus by scoring candidates
 * with reprojection residuals.
 *
 * NOTE:
 * <ol>
 *     <li>It's assumed that pixel coordinates are centered around the principle point, i.e. (cx, cy) = (0,0)</li>
 *     <li>Skew is assumed to be zero.</li>
 * </ol>
 *
 * @see SelfCalibrationEssentialGuessAndCheck
 *
 * @author Peter Abeles
 */
public class GenerateMetricTripleEssentialGuessAndCheck implements
		ModelGeneratorViews<MetricCameraTriple, AssociatedTriple, ImageDimension> {

	// estimates the trifocal tensor
	Estimate1ofTrifocalTensor trifocal;
	// performs projective to metric self calibration
	SelfCalibrationEssentialGuessAndCheck alg;

	// Decomposing the trifocal tensor
	public final TrifocalExtractGeometries trifocalGeo = new TrifocalExtractGeometries();

	//--------------- Internal Work Space
	TrifocalTensor tensor = new TrifocalTensor();
	DMatrixRMaj K = new DMatrixRMaj(3,3);
	FastQueue<AssociatedPair> pairs = new FastQueue<>(AssociatedPair::new);
	DMatrixRMaj F21 = new DMatrixRMaj(3,3);
	DMatrixRMaj F31 = new DMatrixRMaj(3,3);
	DMatrixRMaj P2 = new DMatrixRMaj(3,4);
	DMatrixRMaj P3 = new DMatrixRMaj(3,4);

	public GenerateMetricTripleEssentialGuessAndCheck(Estimate1ofTrifocalTensor trifocal, SelfCalibrationEssentialGuessAndCheck alg) {
		this.trifocal = trifocal;
		this.alg = alg;
	}

	protected GenerateMetricTripleEssentialGuessAndCheck() {}

	@Override
	public boolean generate(List<AssociatedTriple> dataSet, MetricCameraTriple result) {
		// Compute the trifocal tensor
		if( !trifocal.process(dataSet,tensor) )
			return false;

		trifocalGeo.setTensor(tensor);
		trifocalGeo.extractFundmental(F21,F31);
		trifocalGeo.extractCamera(P2,P3);

		pairs.resize(dataSet.size());
		for (int i = 0; i < dataSet.size(); i++) {
			AssociatedTriple t = dataSet.get(i);
			pairs.get(i).set(t.p1,t.p2);
		}

		// Projective to Metric calibration
		if( !alg.process(F21,P2,pairs.toList()) )
			return false;
//		if( alg.isLimit )
//			return false;
		DMatrixRMaj H = alg.rectifyingHomography;

		// copy and convert the results into the output format
		result.view1.fsetK(alg.focalLengthA,alg.focalLengthA,0,0,0,-1,-1);
		result.view2.fsetK(alg.focalLengthB,alg.focalLengthB,0,0,0,-1,-1);
		PerspectiveOps.pinholeToMatrix(result.view2,K);
		if( !MultiViewOps.projectiveToMetricKnownK(P2,H,K,result.view_1_to_2) )
			return false;
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
		alg.imageLengthPixels = Math.max(viewInfo.width, viewInfo.height);
	}

	@Override
	public int getNumberOfViews() {
		return 3;
	}
}
