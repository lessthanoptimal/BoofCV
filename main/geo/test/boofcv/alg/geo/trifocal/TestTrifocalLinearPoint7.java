/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTrifocalLinearPoint7 {

	Random rand = new Random(234);

	// camera calibration matrix
	DenseMatrix64F K = new DenseMatrix64F(3,3,true,60,0.01,200,0,80,150,0,0,1);
//	DenseMatrix64F K = CommonOps.identity(3);

	List<Point3D_F64> worldPts = new ArrayList<Point3D_F64>();
	List<AssociatedTriple> observations = new ArrayList<AssociatedTriple>();

	Se3_F64 se2,se3;
	TrifocalTensor tensor;

	public TestTrifocalLinearPoint7() {
		se2 = new Se3_F64();
		se3 = new Se3_F64();

		RotationMatrixGenerator.eulerXYZ(0.2, 0.001, -0.02, se2.R);
		se2.getT().set(0.3,0,0.05);

		RotationMatrixGenerator.eulerXYZ(0.8,-0.02,0.003,se3.R);
		se3.getT().set(0.6, 0.2, -0.02);

		DenseMatrix64F P2 = PerspectiveOps.createCameraMatrix(se2.R, se2.T, K, null);
		DenseMatrix64F P3 = PerspectiveOps.createCameraMatrix(se3.R, se3.T, K, null);
		tensor = MultiViewOps.createTrifocal(P2, P3, null);

		for( int i = 0; i < 20; i++ ) {
			Point3D_F64 p = new Point3D_F64();
			p.x = rand.nextGaussian()*0.5;
			p.y = rand.nextGaussian()*0.5;
			p.z = rand.nextGaussian()*0.5+2.5;

			worldPts.add(p);

			AssociatedTriple o = new AssociatedTriple();
			o.p1 = PerspectiveOps.renderPixel(new Se3_F64(), null, p);
			o.p2 = PerspectiveOps.renderPixel(se2,K,p);
			o.p3 = PerspectiveOps.renderPixel(se3,K,p);

			observations.add(o);
		}
	}

	/**
	 * Check the linear constraint matrix by seeing if the correct solution is in the null space
	 */
	@Test
	public void checkLinearSystem() {

		TrifocalLinearPoint7 alg = new TrifocalLinearPoint7();

		// construct in pixel coordinates for ease
		alg.N1 = CommonOps.identity(3);
		alg.N2 = CommonOps.identity(3);
		alg.N3 = CommonOps.identity(3);

		alg.createLinearSystem(observations);

		DenseMatrix64F A = alg.A;

		DenseMatrix64F X = new DenseMatrix64F(27,1);
		for( int i = 0; i < 9; i++ ) {
			X.data[i] = tensor.T1.get(i);
			X.data[i+9] = tensor.T2.get(i);
			X.data[i+18] = tensor.T3.get(i);
		}

		DenseMatrix64F Y = new DenseMatrix64F(A.numRows,1);

		CommonOps.mult(A,X,Y);

		for( int i = 0; i < Y.numRows; i++ ) {
			assertEquals(0,Y.get(i),1e-8);
		}
	}

	@Test
	public void fullTest() {
		TrifocalLinearPoint7 alg = new TrifocalLinearPoint7();

		assertTrue(alg.process(observations));

		TrifocalTensor found = alg.getSolution();

		// validate the solution by using a constraint
		for( AssociatedTriple a : observations ) {
			DenseMatrix64F A = MultiViewOps.constraint(found,a.p1,a.p2,a.p3,null);

			assertEquals(0,NormOps.normF(A),1e-7);
		}
	}
}
