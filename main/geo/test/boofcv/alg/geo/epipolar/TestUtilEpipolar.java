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

package boofcv.alg.geo.epipolar;

import boofcv.alg.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
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
public class TestUtilEpipolar {

	Random rand = new Random(234234);

	@Test
	public void computeNormalization() {

		List<AssociatedPair> list = new ArrayList<AssociatedPair>();
		for( int i = 0; i < 12; i++ ) {
			AssociatedPair p = new AssociatedPair();

			p.currLoc.set(rand.nextDouble()*5,rand.nextDouble()*5);
			p.keyLoc.set(rand.nextDouble() * 5, rand.nextDouble() * 5);

			list.add(p);
		}

		// compute statistics
		double meanX0 = 0;
		double meanY0 = 0;
		double meanX1 = 0;
		double meanY1 = 0;

		for( AssociatedPair p : list ) {
			meanX0 += p.keyLoc.x;
			meanY0 += p.keyLoc.y;
			meanX1 += p.currLoc.x;
			meanY1 += p.currLoc.y;
		}

		meanX0 /= list.size();
		meanY0 /= list.size();
		meanX1 /= list.size();
		meanY1 /= list.size();

		double sigmaX0 = 0;
		double sigmaY0 = 0;
		double sigmaX1 = 0;
		double sigmaY1 = 0;

		for( AssociatedPair p : list ) {
			sigmaX0 += Math.pow(p.keyLoc.x-meanX0,2);
			sigmaY0 += Math.pow(p.keyLoc.y-meanY0,2);
			sigmaX1 += Math.pow(p.currLoc.x-meanX1,2);
			sigmaY1 += Math.pow(p.currLoc.y-meanY1,2);
		}

		sigmaX0 = Math.sqrt(sigmaX0/list.size());
		sigmaY0 = Math.sqrt(sigmaY0/list.size());
		sigmaX1 = Math.sqrt(sigmaX1/list.size());
		sigmaY1 = Math.sqrt(sigmaY1/list.size());

		// test the output
		DenseMatrix64F N1 = new DenseMatrix64F(3,3);
		DenseMatrix64F N2 = new DenseMatrix64F(3,3);

		UtilEpipolar.computeNormalization(N1,N2,list);

		assertEquals(1/sigmaX0, N1.get(0,0),1e-8);
		assertEquals(1/sigmaY0, N1.get(1,1),1e-8);
		assertEquals(-meanX0/sigmaX0, N1.get(0,2),1e-8);
		assertEquals(-meanY0/sigmaY0, N1.get(1,2),1e-8);
		assertEquals(1, N1.get(2,2),1e-8);

		assertEquals(1/sigmaX1, N2.get(0,0),1e-8);
		assertEquals(1/sigmaY1, N2.get(1,1),1e-8);
		assertEquals(-meanX1/sigmaX1, N2.get(0,2),1e-8);
		assertEquals(-meanY1/sigmaY1, N2.get(1,2),1e-8);
		assertEquals(1, N2.get(2,2),1e-8);
	}

	/**
	 * Test it against a simple test case
	 */
	@Test
	public void normalize() {
		DenseMatrix64F N = new DenseMatrix64F(3,3,true,1,2,3,4,5,6,7,8,9);

		Point2D_F64 a = new Point2D_F64(3,4);
		Point2D_F64 found = new Point2D_F64(3,4);
		Point2D_F64 expected = new Point2D_F64(3,4);

		expected.x = a.x * N.get(0,0) + N.get(0,2);
		expected.y = a.y * N.get(1,1) + N.get(1,2);


		UtilEpipolar.pixelToNormalized(a, found, N);

		assertEquals(found.x,expected.x,1e-8);
		assertEquals(found.y,expected.y,1e-8);
	}

	@Test
	public void computeEssential() {
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(0.05, -0.04, 0.1, null);
		Vector3D_F64 T = new Vector3D_F64(2,1,-3);
		T.normalize();

		DenseMatrix64F E = UtilEpipolar.computeEssential(R,T);

		// Test using the following theorem:  x2^T*E*x1 = 0
		Point3D_F64 P = new Point3D_F64(0.1,0.1,2);

		Point2D_F64 x0 = new Point2D_F64(P.x/P.z,P.y/P.z);
		SePointOps_F64.transform(new Se3_F64(R,T),P,P);
		Point2D_F64 x1 = new Point2D_F64(P.x/P.z,P.y/P.z);

		double val = GeometryMath_F64.innerProd(x1,E,x0);
		assertEquals(0,val,1e-8);
	}

	@Test
	public void computeHomography_calibrated() {
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(0.1,-0.01,0.2, null);
		Vector3D_F64 T = new Vector3D_F64(1,1,0.1);
		T.normalize();
		double d = 2;
		Vector3D_F64 N = new Vector3D_F64(0,0,1);

		DenseMatrix64F H = UtilEpipolar.computeHomography(R, T, d, N);

		// Test using the following theorem:  x2 = H*x1
		Point3D_F64 P = new Point3D_F64(0.1,0.1,d); // a point on the plane

		Point2D_F64 x0 = new Point2D_F64(P.x/P.z,P.y/P.z);
		SePointOps_F64.transform(new Se3_F64(R,T),P,P);
		Point2D_F64 x1 = new Point2D_F64(P.x/P.z,P.y/P.z);
		Point2D_F64 found = new Point2D_F64();

		GeometryMath_F64.mult(H, x0, found);
		assertEquals(x1.x,found.x,1e-8);
		assertEquals(x1.y,found.y,1e-8);
	}

	@Test
	public void computeHomography_uncalibrated() {
		DenseMatrix64F K = new DenseMatrix64F(3,3,true,0.1,0.001,200,0,0.2,250,0,0,1);
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(0.1,-0.01,0.2, null);
		Vector3D_F64 T = new Vector3D_F64(1,1,0.1);
		T.normalize();
		double d = 2;
		Vector3D_F64 N = new Vector3D_F64(0,0,1);

		DenseMatrix64F H = UtilEpipolar.computeHomography(R, T, d, N, K);

		// Test using the following theorem:  x2 = H*x1
		Point3D_F64 P = new Point3D_F64(0.1,0.1,d); // a point on the plane

		Point2D_F64 x0 = new Point2D_F64(P.x/P.z,P.y/P.z);
		GeometryMath_F64.mult(K,x0,x0);
		SePointOps_F64.transform(new Se3_F64(R,T),P,P);
		Point2D_F64 x1 = new Point2D_F64(P.x/P.z,P.y/P.z);
		GeometryMath_F64.mult(K,x1,x1);
		Point2D_F64 found = new Point2D_F64();

		GeometryMath_F64.mult(H, x0, found);
		assertEquals(x1.x,found.x,1e-8);
		assertEquals(x1.y,found.y,1e-8);
	}
	
	@Test
	public void extractEpipoles() {
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(1,2,-0.5,null);
		Vector3D_F64 T = new Vector3D_F64(0.5,0.7,-0.3);
		
		DenseMatrix64F E = UtilEpipolar.computeEssential(R,T);
		
		assertTrue(NormOps.normF(E)!=0);
		
		Point3D_F64 e1 = new Point3D_F64();
		Point3D_F64 e2 = new Point3D_F64();
		
		UtilEpipolar.extractEpipoles(E,e1,e2);

		Point3D_F64 temp = new Point3D_F64();
		
		GeometryMath_F64.mult(E,e1,temp);
		assertEquals(0,temp.norm(),1e-8);

		GeometryMath_F64.multTran(E,e2,temp);
		assertEquals(0,temp.norm(),1e-8);
	}
}
