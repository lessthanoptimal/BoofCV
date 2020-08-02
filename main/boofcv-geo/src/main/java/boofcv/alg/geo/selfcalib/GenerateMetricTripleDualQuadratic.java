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
import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.robust.ModelGeneratorViews;
import boofcv.alg.geo.structure.DecomposeAbsoluteDualQuadratic;
import boofcv.alg.geo.trifocal.TrifocalExtractGeometries;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.struct.image.ImageDimension;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.List;

/**
 * Computes a calibrating homography from a set of image triplets using the linear dual quadratic approach.
 *
 * NOTE:
 * <ol>
 *     <li>It's assumed that pixel coordinates are centered around the principle point, i.e. (cx, cy) = (0,0)</li>
 *     <li>When used to elevate a camera to metric using output from this function the sign of translation is lost</li>
 * </ol>
 *
 * @see SelfCalibrationLinearDualQuadratic
 * @see TrifocalExtractGeometries
 * @see DecomposeAbsoluteDualQuadratic
 *
 * @author Peter Abeles
 */
public class GenerateMetricTripleDualQuadratic implements
		ModelGeneratorViews<MetricCameraTriple, AssociatedTriple, ImageDimension> {

	Estimate1ofTrifocalTensor trifocal;
	SelfCalibrationLinearDualQuadratic selfCalib;

	// used to get camera matrices from the trifocal tensor
	final TrifocalExtractGeometries extractor = new TrifocalExtractGeometries();
	final DecomposeAbsoluteDualQuadratic decomposeDualQuad = new DecomposeAbsoluteDualQuadratic();

	// storage for camera matrices
	final DMatrixRMaj P1 = CommonOps_DDRM.identity(3,4);
	final DMatrixRMaj P2 = new DMatrixRMaj(3,4);
	final DMatrixRMaj P3 = new DMatrixRMaj(3,4);

	// rectifying homography
	final DMatrixRMaj H = new DMatrixRMaj(4,4);
	// intrinsic calibration matrix
	final DMatrixRMaj K = new DMatrixRMaj(3,3);

	// Storage for tensor
	final TrifocalTensor tensor = new TrifocalTensor();

	public GenerateMetricTripleDualQuadratic(Estimate1ofTrifocalTensor trifocal,
											 SelfCalibrationLinearDualQuadratic selfCalib) {
		this.trifocal = trifocal;
		this.selfCalib = selfCalib;
	}

	protected GenerateMetricTripleDualQuadratic() {}

	@Override
	public boolean generate(List<AssociatedTriple> dataSet, MetricCameraTriple result) {
		// Get trifocal tensor
		if( !trifocal.process(dataSet,tensor) )
			return false;

		// Get camera matrices from trifocal
		extractor.setTensor(tensor);
		extractor.extractCamera(P2,P3);

		// Determine metric parameters
		selfCalib.reset();
		selfCalib.addCameraMatrix(P1);
		selfCalib.addCameraMatrix(P2);
		selfCalib.addCameraMatrix(P3);

		GeometricResult results = selfCalib.solve();
		if( results != GeometricResult.SUCCESS )
			return false;

		// Convert results into results format
		if( !decomposeDualQuad.decompose(selfCalib.getQ()) )
			return false;

		if( !decomposeDualQuad.computeRectifyingHomography(H) )
			return false;

		selfCalib.getSolutions().get(0).copyTo(result.view1);
		selfCalib.getSolutions().get(1).copyTo(result.view2);
		selfCalib.getSolutions().get(2).copyTo(result.view3);

		// get the motion but ignore intrinsic parameters since those are already known
		if( !MultiViewOps.projectiveToMetric(P2,H,result.view_1_to_2,K) )
			return false;
		if( !MultiViewOps.projectiveToMetric(P3,H,result.view_1_to_3,K) )
			return false;

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return trifocal.getMinimumPoints();
	}

	@Override public void setView(int view, ImageDimension viewInfo) {}
	@Override public int getNumberOfViews() {return 3;}
}
