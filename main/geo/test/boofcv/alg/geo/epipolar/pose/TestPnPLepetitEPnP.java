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

package boofcv.alg.geo.epipolar.pose;

import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.GeoTestingOps;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.RandomMatrices;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestPnPLepetitEPnP {

	Random rand = new Random(234);
	
	@Test
	public void standardTests() {
		CommonMotionNPoint test = new CommonMotionNPoint() {
			@Override
			public Se3_F64 compute(List<AssociatedPair> obs, List<Point3D_F64> locations) {

				PnPLepetitEPnP alg = new PnPLepetitEPnP();
				
				List<Point2D_F64> currObs = new ArrayList<Point2D_F64>();
				for( AssociatedPair a : obs ) {
					currObs.add(a.currLoc);
				}
				
				alg.process(locations,currObs);

				return alg.getSolutionMotion();
			}
		};

		for( int i = 10; i <= 10; i++ ) {
			test.testNoMotion(i);
			test.standardTest(i);
			test.planarTest(i);
		}
	}
	
	@Test
	public void selectWorldControlPoints() {

		List<Point3D_F64> worldPts = GeoTestingOps.randomPoints_F64(-1, 10, -5, 20, 0.1, 0.5, 30, rand);
		List<Point3D_F64> controlPts = declarePointList(4);
		
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

	@Test
	public void selectControlPoints_planar() {
		
		fail("give it planar data");
		
		fail("check the axis to see if they are correct");
	}

	@Test
	public void computeBarycentricCoordinates() {
		List<Point3D_F64> worldPoints = GeoTestingOps.randomPoints_F64(-1, 10, -5, 20, 0.1, 0.5, 30, rand);
		List<Point3D_F64> worldControlPts = declarePointList(4);
		
		PnPLepetitEPnP alg = new PnPLepetitEPnP();

		alg.selectWorldControlPoints(worldPoints, worldControlPts);

		DenseMatrix64F alpha = new DenseMatrix64F(1,1);

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

	@Test
	public void constructM() {
		List<Point2D_F64> obsPts = GeoTestingOps.randomPoints_F64(-1, 2, -5, 20, 30, rand);
		DenseMatrix64F M = RandomMatrices.createRandom(2 * obsPts.size(), 12,rand);
		DenseMatrix64F alpha = RandomMatrices.createRandom(obsPts.size(),4,rand);
		
		PnPLepetitEPnP.constructM(obsPts, alpha, M);

		// check for zeros in the expected locations
		for( int row = 0; row < obsPts.size()*2; row += 2 ) {
			for( int i = 0; i < 4; i++ ) {
				assertEquals(0,M.get(i*3+1,row),1e-8);
				assertEquals(0,M.get(i*3,row+1),1e-8);
			}
		}
	}

	@Test
	public void extractNullPoints() {
		// create a singular matrix
		SimpleMatrix M = SimpleMatrix.wrap(RandomMatrices.createSingularValues(12, 40, rand, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 0));

		PnPLepetitEPnP alg = new PnPLepetitEPnP();
		alg.extractNullPoints(M.getMatrix());

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
	@Test
	public void estimateCase1() {
		PnPLepetitEPnP alg = new PnPLepetitEPnP();

		// skip the adjust step
		alg.alphas = new DenseMatrix64F(0,0);
		alg.nullPts[0] = GeoTestingOps.randomPoints_F64(5,10,-1, 2, -5, 20, 4, rand);
		double beta = 10;
		int i = 0;
		for( Point3D_F64 p : alg.nullPts[0]) {
			Point3D_F64 c = alg.controlWorldPts.get(i++);
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

	@Test
	public void estimateCase4() {
		fail("finish");
	}

	private List<Point3D_F64> declarePointList( int N ) {
		List<Point3D_F64> ret = new ArrayList<Point3D_F64>();
		for( int i = 0; i < N; i++ ) {
			ret.add( new Point3D_F64());
		}
		return ret;
	}
}
