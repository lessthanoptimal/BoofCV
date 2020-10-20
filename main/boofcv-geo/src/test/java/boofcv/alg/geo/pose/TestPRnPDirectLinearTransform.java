/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.pose;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestPRnPDirectLinearTransform extends CommonMotionNPointHomogenous {
	/**
	 * No noise minimal case. General structure
	 */
	@Test
	void minimal_perfect() {
		Se3_F64 m = SpecialEuclideanOps_F64.eulerXyz(0.01,0.1,1,0.01,-0.02,0.015,null);
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(
				new CameraPinhole(500,500,0,250,250,1000,1000),(DMatrixRMaj)null);

		PRnPDirectLinearTransform alg = new PRnPDirectLinearTransform();

		DMatrixRMaj P = PerspectiveOps.createCameraMatrix(m.R,m.T,K,null);

		generateScene(alg.getMinimumPoints(),P,false);

		DMatrixRMaj found = new DMatrixRMaj(3,4);
		assertTrue(alg.process(worldPts,pixelsView2,found));

		CommonOps_DDRM.divide(P, NormOps_DDRM.normF(P));
		CommonOps_DDRM.divide(found, NormOps_DDRM.normF(found));
		if( Math.signum(P.get(0,0)) != Math.signum(found.get(0,0))) {
			CommonOps_DDRM.scale(-1,found);
		}

		assertTrue(MatrixFeatures_DDRM.isIdentical(P,found, UtilEjml.TEST_F64));
	}

	/**
	 * No noise. Redundant data
	 */
	@Test
	void many_perfect() {
		Se3_F64 m = SpecialEuclideanOps_F64.eulerXyz(0.01,0.1,1,0.01,-0.02,0.015,null);
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(
				new CameraPinhole(500,500,0,250,250,1000,1000),(DMatrixRMaj)null);

		PRnPDirectLinearTransform alg = new PRnPDirectLinearTransform();

		DMatrixRMaj P = PerspectiveOps.createCameraMatrix(m.R,m.T,K,null);

		for (int trial = 0; trial < 5; trial++) {
			generateScene(40-trial,P,false);

			DMatrixRMaj found = new DMatrixRMaj(3,4);
			assertTrue(alg.process(worldPts,pixelsView2,found));

			CommonOps_DDRM.divide(P, NormOps_DDRM.normF(P));
			CommonOps_DDRM.divide(found, NormOps_DDRM.normF(found));
			if( Math.signum(P.get(0,0)) != Math.signum(found.get(0,0))) {
				CommonOps_DDRM.scale(-1,found);
			}

			assertTrue(MatrixFeatures_DDRM.isIdentical(P,found, UtilEjml.TEST_F64));
		}
	}
}
