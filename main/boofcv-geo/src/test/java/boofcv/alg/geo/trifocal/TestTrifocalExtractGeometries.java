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

package boofcv.alg.geo.trifocal;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.geo.TrifocalTensor;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTrifocalExtractGeometries extends CommonTrifocalChecks {

	/**
	 * Randomly general several scenarios and see if it produces the correct solution
	 */
	@Test void extractEpipoles() {
		TrifocalExtractGeometries alg = new TrifocalExtractGeometries();

		for( int i = 0; i < 5; i++ ) {
			createRandomScenario(false);

			Point3D_F64 found2 = new Point3D_F64();
			Point3D_F64 found3 = new Point3D_F64();

			TrifocalTensor input = tensor.copy();

			alg.setTensor(input);
			alg.extractEpipoles(found2, found3);

			// make sure the input was not modified
			for( int j = 0; j < 3; j++ )
				assertTrue(MatrixFeatures_DDRM.isIdentical(tensor.getT(j), input.getT(j), 1e-8));

			Point3D_F64 space = new Point3D_F64();

			// check to see if it is the left-null space of their respective Fundamental matrices
			GeometryMath_F64.multTran(F2, found2, space);
			assertEquals(0,space.norm(),1e-8);

			GeometryMath_F64.multTran(F3, found3, space);
			assertEquals(0,space.norm(),1e-8);
		}
	}

	@Test void extractCamera() {
		TrifocalLinearPoint7 linear = new TrifocalLinearPoint7();

		TrifocalExtractGeometries alg = new TrifocalExtractGeometries();
		DMatrixRMaj P2 = new DMatrixRMaj(3,4);
		DMatrixRMaj P3 = new DMatrixRMaj(3,4);

		for( int trial = 0; trial < 5; trial++ ) {
			createRandomScenario(false);

			// solve for the tensor to make this more realistic
			linear.process(observationsPixels,found);

			alg.setTensor(found);
			alg.extractCamera(P2, P3);

			// Using found camera matrices render the point's location
			Point3D_F64 X = new Point3D_F64(0.1,0.05,2);

			Point2D_F64 x1 = new Point2D_F64(X.x/X.z,X.y/X.z);
			Point2D_F64 x2 = PerspectiveOps.renderPixel(P2, X);
			Point2D_F64 x3 = PerspectiveOps.renderPixel(P3, X);

			// validate correctness by testing a constraint on the points
			DMatrixRMaj A = new DMatrixRMaj(3,3);
			MultiViewOps.constraint(found, x1, x2, x3, A);

			for( int i = 0; i < 3; i++ ) {
				for( int j = 0; j < 3; j++ ) {
					assertEquals(0,A.get(i,j),1e-7);
				}
			}
		}
	}

	@Test void extractFundmental() {
		TrifocalExtractGeometries alg = new TrifocalExtractGeometries();

		DMatrixRMaj found2 = new DMatrixRMaj(3,3);
		DMatrixRMaj found3 = new DMatrixRMaj(3,3);
		for( int trial = 0; trial < 5; trial++ ) {
			createRandomScenario(false);

			TrifocalTensor input = tensor.copy();
			alg.setTensor(input);
			alg.extractFundmental(found2, found3);

			// make sure the input was not modified
			for( int i = 0; i < 3; i++ )
				assertTrue(MatrixFeatures_DDRM.isIdentical(tensor.getT(i),input.getT(i),1e-8));

			CommonOps_DDRM.scale(1.0/CommonOps_DDRM.elementMaxAbs(found2),found2);
			CommonOps_DDRM.scale(1.0/CommonOps_DDRM.elementMaxAbs(found3),found3);

			Point3D_F64 X = new Point3D_F64(0.1,0.05,2);

			// remember the first view is assumed to have a projection matrix of [I|0]
			Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(), X, null);
			Point2D_F64 x2 = PerspectiveOps.renderPixel(worldToCam2, K, X, null);
			Point2D_F64 x3 = PerspectiveOps.renderPixel(worldToCam3, K, X, null);

			assertEquals(0, MultiViewOps.constraint(found2, x1, x2), 1e-8);
			assertEquals(0, MultiViewOps.constraint(found3, x1, x3), 1e-8);
		}
	}
}
