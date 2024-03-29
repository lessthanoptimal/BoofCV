/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.GeoTestingOps;
import boofcv.alg.geo.h.CommonHomographyChecks;
import boofcv.struct.geo.AssociatedPair;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestPnPLepetitEPnP extends BoofStandardJUnit {

//	@Test
//	public void seeCommentAboutSolutions() {
//		fail("consider");
//	}

	@Test void standardTests() {
		standardTest(0);
	}

	@Test void standardTests_iteration() {
		standardTest(10);
	}

	private void standardTest( final int numIterations ) {
		ChecksMotionNPoint test = new ChecksMotionNPoint() {
			@Override
			public Se3_F64 compute(List<AssociatedPair> obs, List<Point3D_F64> locations) {

				PnPLepetitEPnP alg = new PnPLepetitEPnP();

				List<Point2D_F64> currObs = new ArrayList<>();
				for( AssociatedPair a : obs ) {
					currObs.add(a.p2);
				}

				Se3_F64 solution = new Se3_F64();

				alg.setNumIterations(numIterations);
				alg.process(locations,currObs,solution);

				return solution;
			}
		};

		for( int i = 0; i < 20; i++ ) {
			// the minimal case is not tested here since its too unstable
			for( int N = 5; N < 10; N++ ) {
				test.testNoMotion(N);
				test.standardTest(N);
				test.planarTest(N-1);
			}
//			System.out.println();
		}
	}
	
	@Test void selectWorldControlPoints_planar() {

		List<Point3D_F64> worldPts = CommonHomographyChecks.createRandomPlane(rand, 3, 30);
		DogArray<Point3D_F64> controlPts = new DogArray<>(4, Point3D_F64::new);

		PnPLepetitEPnP alg = new PnPLepetitEPnP();

		alg.selectWorldControlPoints(worldPts, controlPts);

		assertEquals(3,alg.numControl);

		// check that each row is unique
		for( int i = 0; i < 3; i++ ) {
			Point3D_F64 ci = controlPts.get(i);

			for( int j = i+1; j < 3; j++ ) {
				Point3D_F64 cj = controlPts.get(j);

				double dx = ci.x - cj.x;
				double dy = ci.y - cj.y;
				double dz = ci.z - cj.z;

				double sum = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);

				assertTrue(sum > 0.00001 );
			}
		}
	}

	@Test void selectControlPoints() {

		List<Point3D_F64> worldPts = GeoTestingOps.randomPoints_F64(-1, 10, -5, 20, 0.1, 0.5, 30, rand);
		DogArray<Point3D_F64> controlPts = new DogArray<>(4, Point3D_F64::new);

		PnPLepetitEPnP alg = new PnPLepetitEPnP();

		alg.selectWorldControlPoints(worldPts, controlPts);

		// check that each row is unique
		for( int i = 0; i < 4; i++ ) {
			Point3D_F64 ci = controlPts.get(i);

			for( int j = i+1; j < 4; j++ ) {
				Point3D_F64 cj = controlPts.get(j);

				double dx = ci.x - cj.x;
				double dy = ci.y - cj.y;
				double dz = ci.z - cj.z;

				double sum = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);

				assertTrue(sum > 0.00001 );
			}
		}
	}

	@Test void computeBarycentricCoordinates() {
		List<Point3D_F64> worldPoints = GeoTestingOps.randomPoints_F64(-1, 10, -5, 20, 0.1, 0.5, 30, rand);
		DogArray<Point3D_F64> worldControlPts = new DogArray<>(4, Point3D_F64::new);

		PnPLepetitEPnP alg = new PnPLepetitEPnP();

		alg.selectWorldControlPoints(worldPoints, worldControlPts);

		DMatrixRMaj alpha = new DMatrixRMaj(1,1);

		alg.computeBarycentricCoordinates(worldControlPts, alpha, worldPoints);

		// make sure it sums up to one and it should add up to the original point
		for( int i = 0; i < worldPoints.size(); i++ ) {
			double x=0,y=0,z=0;

			double sum = 0;
			for( int j = 0; j < 4; j++ ) {
				Point3D_F64 cj = worldControlPts.get(j);

				double a = alpha.get(i,j);
				sum += a;
				x += a*cj.x;
				y += a*cj.y;
				z += a*cj.z;
			}
			Point3D_F64 p = worldPoints.get(i);

			assertEquals(1,sum,1e-8);
			assertEquals(p.x,x,1e-8);
			assertEquals(p.y,y,1e-8);
			assertEquals(p.z,z,1e-8);
		}
	}

	@Test void constructM() {
		List<Point2D_F64> obsPts = GeoTestingOps.randomPoints_F64(-1, 2, -5, 20, 30, rand);
		DMatrixRMaj M = RandomMatrices_DDRM.rectangle(2 * obsPts.size(), 12,rand);
		DMatrixRMaj alpha = RandomMatrices_DDRM.rectangle(obsPts.size(),4,rand);

		PnPLepetitEPnP.constructM(obsPts, alpha, M);

		// check for zeros in the expected locations
		for( int row = 0; row < obsPts.size()*2; row += 2 ) {
			for( int i = 0; i < 4; i++ ) {
				assertEquals(0,M.get(i*3+1,row),1e-8);
				assertEquals(0,M.get(i*3,row+1),1e-8);
			}
		}
	}

	@Test void extractNullPoints() {
		// create a singular matrix
		SimpleMatrix M = SimpleMatrix.wrap(RandomMatrices_DDRM.singular(12, 40, rand, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 0));

		PnPLepetitEPnP alg = new PnPLepetitEPnP();
		alg.numControl = 4;
		alg.extractNullPoints(M.getDDRM());

		// see if the first set of null points is the null space pf M*M
		List<Point3D_F64> l = alg.nullPts[0];

		SimpleMatrix v = new SimpleMatrix(12,1);
		for( int i = 0; i < 4; i++ ) {
			Point3D_F64 p = l.get(i);
			v.set(3*i+0,p.x);
			v.set(3*i+1,p.y);
			v.set(3*i+2,p.z);
		}

		SimpleMatrix MM = M.mult(M.transpose());

		SimpleMatrix x = MM.mult(v);

		double mag = x.normF();

		assertEquals(0,mag,1e-8);
	}

	/**
	 * Create two sets of points with a known scale difference.
	 */
	@Test void estimateCase1() {
		PnPLepetitEPnP alg = new PnPLepetitEPnP();

		// skip the adjust step
		alg.numControl = 4;
		alg.alphas = new DMatrixRMaj(0,0);
		alg.nullPts[0] = GeoTestingOps.randomPoints_F64(5,10,-1, 2, -5, 20, 4, rand);
		double beta = 10;
		for( int i = 0; i < alg.numControl; i++ ) {
			Point3D_F64 p = alg.nullPts[0].get(i);
			Point3D_F64 c = alg.controlWorldPts.grow();
			c.x = p.x*beta;
			c.y = p.y*beta;
			c.z = p.z*beta;
		}

		double betas[] = new double[]{1,2,3,4};
		alg.estimateCase1(betas);
		assertEquals(beta,betas[0],1e-8);
		assertEquals(0,betas[1],1e-8);
		assertEquals(0,betas[2],1e-8);
		assertEquals(0,betas[3],1e-8);
	}
}
