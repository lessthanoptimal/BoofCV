/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrix2x2;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.equation.Equation;
import org.ejml.ops.ConvertDMatrixStruct;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPnPInfinitesimalPlanePoseEstimation {
	Random rand = new Random(234);

	@Test
	public void perfectObservations() {
		Se3_F64 actual = new Se3_F64();

		for (int i = 0; i < 100; i++) {
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,
					0.2+rand.nextGaussian()*0.05,
					0.1+rand.nextGaussian()*0.05,
					0.05+rand.nextGaussian()*0.001,actual.R);
			actual.T.set(rand.nextGaussian(),1+rand.nextGaussian(),20+rand.nextGaussian()*2);

			List<AssociatedPair> observation = createRandomInputs(20,actual);

			PnPInfinitesimalPlanePoseEstimation alg = new PnPInfinitesimalPlanePoseEstimation();

			assertTrue(alg.process(observation));

			assertTrue(alg.getError0()<alg.getError1());

			double error0 = computeModelError(alg.getWorldToCamera0(),actual);
			double error1 = computeModelError(alg.getWorldToCamera1(),actual);

			assertTrue(error0<error1);
			assertTrue(error0 < 0.006);
		}

	}

	public double computeModelError( Se3_F64 found , Se3_F64 expected ) {
		Se3_F64 d = found.concat(expected.invert(null),null);

		// it should be identity
		d.R.data[0] -= 1;
		d.R.data[4] -= 1;
		d.R.data[8] -= 1;

		return NormOps_DDRM.normF(d.R)/9 + d.T.norm()/3;
	}

	@Test
	public void estimateTranslation() {
		Se3_F64 actual = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.1,0.05,0,actual.R);
		actual.T.set(0.1,0,3);

		List<AssociatedPair> observation = createRandomInputs(20,actual);

		PnPInfinitesimalPlanePoseEstimation alg = new PnPInfinitesimalPlanePoseEstimation();

		Vector3D_F64 found = new Vector3D_F64();
		alg.estimateTranslation(actual.R,observation,found);

		assertTrue(actual.T.isIdentical(found,UtilEjml.TEST_F64));
	}

	private List<AssociatedPair> createRandomInputs( int N , Se3_F64 worldToCamera ) {
		List<AssociatedPair> list = new ArrayList<>();

		for (int i = 0; i < N; i++) {
			Point3D_F64 world = new Point3D_F64();
			world.x = rand.nextGaussian();
			world.y = rand.nextGaussian();
			world.z = 0;

			Point3D_F64 camera = new Point3D_F64();
			SePointOps_F64.transform(worldToCamera,world,camera);

			AssociatedPair pair = new AssociatedPair();
			pair.p1.set(world.x,world.y);
			pair.p2.set(camera.x/camera.z,camera.y/camera.z);

			list.add( pair );
		}
		return list;
	}

	@Test
	public void constructR() {
		Equation eq = new Equation();

		eq.process("R_v=randn(3,3)");
		eq.process("R22=[1 2;3 4]");
		eq.process("c=[1.1;2.1]");
		eq.process("a=3.1");
		eq.process("b=[-0.4;0.2]");
		eq.process("R1=R_v*[R22 c;b' a]");
		eq.process("R2=R_v*[R22, -c;-b', a]");

		DMatrixRMaj found = new DMatrixRMaj(3,3);
		DMatrixRMaj R_v = eq.lookupDDRM("R_v");
		DMatrix2x2 R22 = new DMatrix2x2();
		ConvertDMatrixStruct.convert(eq.lookupDDRM("R22"),R22);
		Vector3D_F64 ca = new Vector3D_F64(1.1,2.1,3.1);

		PnPInfinitesimalPlanePoseEstimation.constructR(found,R_v,R22,-0.4,0.2,ca,1,new DMatrixRMaj(3,3));
		assertTrue(MatrixFeatures_DDRM.isIdentical(eq.lookupDDRM("R1"),found,UtilEjml.TEST_F64));

		PnPInfinitesimalPlanePoseEstimation.constructR(found,R_v,R22,-0.4,0.2,ca,-1,new DMatrixRMaj(3,3));
		assertTrue(MatrixFeatures_DDRM.isIdentical(eq.lookupDDRM("R2"),found,UtilEjml.TEST_F64));
	}

	@Test
	public void compute_B() {
		Equation eq = new Equation();
		eq.process("v=[1.1,0.5]'");
		eq.process("R_v=[1,2,3;4,5,6;7,8,9]'");
		eq.process("B=[eye(2),-v]*R_v");

		DMatrixRMaj v = eq.lookupDDRM("v");
		DMatrixRMaj R_v = eq.lookupDDRM("R_v");
		DMatrixRMaj expected = eq.lookupDDRM("B");

		double v1 = v.get(0);
		double v2 = v.get(1);
		DMatrix2x2 B = new DMatrix2x2();

		PnPInfinitesimalPlanePoseEstimation.compute_B(B,R_v,v1,v2);
		assertEquals(expected.get(0,0),B.a11, UtilEjml.TEST_F64);
		assertEquals(expected.get(0,1),B.a12, UtilEjml.TEST_F64);
		assertEquals(expected.get(1,0),B.a21, UtilEjml.TEST_F64);
		assertEquals(expected.get(1,1),B.a22, UtilEjml.TEST_F64);
	}

	@Test
	public void largestSingularValue2x2() {
		DMatrix2x2 M = new DMatrix2x2(1,-1.5,0.5,1.8);

		SimpleMatrix A = new SimpleMatrix(new double[][]{{M.a11,M.a12},{M.a21,M.a22}});

		double[] s = A.svd().getSingularValues();

		PnPInfinitesimalPlanePoseEstimation alg = new PnPInfinitesimalPlanePoseEstimation();

		double found = alg.largestSingularValue2x2(M);
		assertEquals(s[0],found,UtilEjml.TEST_F64);
	}

	@Test
	public void compute_Rv() {
		Equation eq = new Equation();
		eq.process("v=[1.1,0.5]'");
		eq.process("t=normF(v)");
		eq.process("s=normF([v',1]')");
		eq.process("cosT=1.0/s");
		eq.process("sinT=sqrt(1-1.0/s^2)");
		eq.process("Kx=(1.0/t)*[[0 0;0 0] v;-v' 0]");
		eq.process("R_v = eye(3) + sinT*Kx + (1.0-cosT)*Kx*Kx");
		PnPInfinitesimalPlanePoseEstimation alg = new PnPInfinitesimalPlanePoseEstimation();

		DMatrixRMaj V = eq.lookupDDRM("v");
		alg.v1 = V.get(0);
		alg.v2 = V.get(1);
		alg.compute_Rv();

		DMatrixRMaj expected_R_v = eq.lookupDDRM("R_v");

		assertTrue(MatrixFeatures_DDRM.isEquals(expected_R_v,alg.R_v, UtilEjml.TEST_F64));
	}
}