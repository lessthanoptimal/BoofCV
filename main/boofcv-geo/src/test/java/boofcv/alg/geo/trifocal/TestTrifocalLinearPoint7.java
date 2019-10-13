/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.geo.NormalizationPoint2D;
import boofcv.struct.geo.AssociatedTriple;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.dense.row.SingularOps_DDRM;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestTrifocalLinearPoint7 extends CommonTrifocalChecks {

	/**
	 * Check the linear constraint matrix by seeing if the correct solution is in the null space
	 */
	@Test
	void checkLinearSystem() {
		// if planar then the nullity isn't 1. so I'm not going to do that. The real test is below
//		createSceneObservations(true);

		TrifocalLinearPoint7 alg = new TrifocalLinearPoint7();

		// construct in pixel coordinates for ease
		alg.N1 = new NormalizationPoint2D(0,1,0,1);
		alg.N2 = new NormalizationPoint2D(0,1,0,1);
		alg.N3 = new NormalizationPoint2D(0,1,0,1);

		alg.createLinearSystem(observationsNorm);

		DMatrixRMaj A = alg.A;

		// Compare against a linear matrix created with more concise code
		DMatrixRMaj expectedA = createSystem(observationsNorm);
		assertTrue(MatrixFeatures_DDRM.isIdentical(A,expectedA, UtilEjml.TEST_F64));

		// make sure there's a unique solution
		assertEquals(1,SingularOps_DDRM.nullity(A, UtilEjml.TEST_F64));
	}

	DMatrixRMaj createSystem( List<AssociatedTriple> observations ) {
		int N = observations.size();
		DMatrixRMaj A = new DMatrixRMaj(4*N,27);

		for (int i = 0; i < N; i++) {
			AssociatedTriple T = observations.get(i);
			Point2D_F64 p1 = T.p1;
			Point2D_F64 p2 = T.p2;
			Point2D_F64 p3 = T.p3;

			for (int k = 0; k < 3; k++) {
				fillLine(A,i*4  ,0,2,0,2,k,p1,p2,p3);
				fillLine(A,i*4+1,0,2,1,2,k,p1,p2,p3);
				fillLine(A,i*4+2,1,2,0,2,k,p1,p2,p3);
				fillLine(A,i*4+3,1,2,1,2,k,p1,p2,p3);

				// not sure if this is the correct 9. need to think more
//				fillLine(A,i*4  ,0,1,0,1,k,p1,p2,p3);
//				fillLine(A,i*4+1,0,2,0,1,k,p1,p2,p3);
//				fillLine(A,i*4+2,0,2,0,2,k,p1,p2,p3);
//				fillLine(A,i*4+3,1,0,1,0,k,p1,p2,p3);
//				fillLine(A,i*4+4,1,2,1,0,k,p1,p2,p3);
//				fillLine(A,i*4+5,1,0,1,2,k,p1,p2,p3);
//				fillLine(A,i*4+6,2,0,2,0,k,p1,p2,p3);
//				fillLine(A,i*4+7,2,1,2,0,k,p1,p2,p3);
//				fillLine(A,i*4+8,2,1,2,1,k,p1,p2,p3);
			}
		}
		return A;
	}

	private void fillLine( DMatrixRMaj A , int row , int i , int j , int l , int m,
						   int k ,
						   Point2D_F64 p1 , Point2D_F64 p2, Point2D_F64 p3 )
	{
		int idx0 = A.getIndex(row,9*k+j*3+l);
		int idx1 = A.getIndex(row,9*k+i*3+l);
		int idx2 = A.getIndex(row,9*k+j*3+m);
		int idx3 = A.getIndex(row,9*k+i*3+m);

		double x1k = k < 2 ? p1.getIdx(k) : 1;
		double x2i = i < 2 ? p2.getIdx(i) : 1;
		double x2j = j < 2 ? p2.getIdx(j) : 1;
		double x3l = l < 2 ? p3.getIdx(l) : 1;
		double x3m = m < 2 ? p3.getIdx(m) : 1;

		A.data[idx0] = -x1k*x2i*x3m;
		A.data[idx1] =  x1k*x2j*x3m;
		A.data[idx2] =  x1k*x2i*x3l;
		A.data[idx3] = -x1k*x2j*x3l;
	}

	@Test
	void fullTest() {
		// see if it works with planar and non-planar scenes. It's interesting that it appears to be
		// working with planar scenes even though the null space isn't unique, see test above
		fullTest(true);
		fullTest(false);
	}
	void fullTest(boolean planar ) {
		createSceneObservations(planar);
		TrifocalLinearPoint7 alg = new TrifocalLinearPoint7();

		assertTrue(alg.process(observationsPixels,found));

		// validate the solution by using a constraint
		for( AssociatedTriple a : observationsPixels ) {
			DMatrixRMaj A = MultiViewOps.constraint(found,a.p1,a.p2,a.p3,null);

			assertEquals(0,NormOps_DDRM.normF(A),1e-6);
		}

		// see if epipoles are zero
		for (int i = 0; i < 3; i++) {
			DMatrixRMaj T = found.getT(i);
			// sanity check that it just isn't zero
			assertTrue(NormOps_DDRM.normF(T) > 1e-7 );
			// rank should be 2
			assertEquals(2,SingularOps_DDRM.rank(T, UtilEjml.TEST_F64));
		}
	}
}
