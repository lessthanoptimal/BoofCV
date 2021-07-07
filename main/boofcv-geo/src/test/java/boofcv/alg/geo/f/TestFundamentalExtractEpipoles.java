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

package boofcv.alg.geo.f;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.NormOps_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestFundamentalExtractEpipoles extends BoofStandardJUnit {
	@Test void process() {
		FundamentalExtractEpipoles alg = new FundamentalExtractEpipoles();

		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(400,400,0.1,410,399);
		for (int i = 0; i < 100; i++) {
			double rotX = rand.nextGaussian();
			double rotY = rand.nextGaussian();
			double rotZ = rand.nextGaussian();

			DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,rotX,rotY,rotZ,null);
			Vector3D_F64 T = new Vector3D_F64(rand.nextGaussian(),rand.nextGaussian(),rand.nextGaussian());

			DMatrixRMaj E = MultiViewOps.createEssential(R, T, null);

			assertTrue(NormOps_DDRM.normF(E)!=0);

			Point3D_F64 e1 = new Point3D_F64();
			Point3D_F64 e2 = new Point3D_F64();


			alg.process(E, e1, e2);

			Point3D_F64 temp = new Point3D_F64();

			GeometryMath_F64.mult(E,e1,temp);
			assertEquals(0,temp.norm(),1e-8);

			GeometryMath_F64.multTran(E,e2,temp);
			assertEquals(0,temp.norm(),1e-8);

			DMatrixRMaj F = MultiViewOps.createFundamental(E,K);
			alg.process(F, e1, e2);
			GeometryMath_F64.mult(F,e1,temp);
			assertEquals(0,temp.norm(),1e-8);
			GeometryMath_F64.multTran(F,e2,temp);
			assertEquals(0,temp.norm(),1e-8);
		}
	}
}
