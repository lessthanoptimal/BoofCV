/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort.mls;

import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F32;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestImageDeformPointMLS_F32 {

	int width = 45;
	int height = 60;
	int cols = 10;
	int rows = 12;

	/**
	 * The distorted control points are at the same location
	 */
	@Test
	public void testAllAtOnce_noChange() {
		ImageDeformPointMLS_F32 alg = new ImageDeformPointMLS_F32();
		alg.configure(width,height, rows, cols);

		alg.addControl(5,5);
		alg.addControl(10,20);
		alg.addControl(30,50);

		alg.fixateUndistorted();
		alg.fixateDistorted();

		// should be no change now
		Point2D_F32 found = new Point2D_F32();
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				alg.compute(j,i, found);
				assertEquals(j, found.x, GrlConstants.FLOAT_TEST_TOL);
				assertEquals(i, found.y, GrlConstants.FLOAT_TEST_TOL);
			}
		}
	}

	/**
	 * See how it handles distorted points
	 */
	@Test
	public void testAllAtOnce() {
		ImageDeformPointMLS_F32 alg = new ImageDeformPointMLS_F32();
		alg.configure(width,height, rows, cols);

		alg.addControl(5,5);
		alg.addControl(10,20);
		alg.addControl(30,50);

		alg.fixateUndistorted();
		alg.fixateDistorted();

	}


	@Test
	public void computeAverageP() {
		ImageDeformPointMLS_F32 alg = new ImageDeformPointMLS_F32();
		alg.configure(width,height, rows, cols);

		alg.addControl(10,15);
		alg.addControl(5,4);
		alg.addControl(20,24);

		// set p locations
		alg.controls.get(0).p.set(12,17);
		alg.controls.get(1).p.set(2,7);
		alg.controls.get(2).p.set(18,30);


		ImageDeformPointMLS_F32.AffineCache c = new ImageDeformPointMLS_F32.AffineCache();
		c.weights.data = new float[]{0.1f,0.6f,0.3f};
		c.weights.size = 3;
		c.aveP.set(1,2);

		alg.computeAverageP(c);

		float expectedX = 12*0.1f + 2*0.6f + 18*0.3f;
		float expectedY = 17*0.1f + 7*0.6f + 30*0.3f;

		assertEquals(expectedX, c.aveP.x, GrlConstants.FLOAT_TEST_TOL);
		assertEquals(expectedY, c.aveP.y, GrlConstants.FLOAT_TEST_TOL);
	}

	@Test
	public void computeAverageQ() {
		ImageDeformPointMLS_F32 alg = new ImageDeformPointMLS_F32();
		alg.configure(width,height, rows, cols);

		alg.addControl(10,15);
		alg.addControl(5,4);
		alg.addControl(20,24);

		ImageDeformPointMLS_F32.AffineCache c = new ImageDeformPointMLS_F32.AffineCache();
		c.weights.data = new float[]{0.1f,0.6f,0.3f};
		c.weights.size = 3;
		c.aveQ.set(1,2);

		alg.computeAverageQ(c);

		float expectedX = 10*0.1f + 5*0.6f + 20*0.3f;
		float expectedY = 15*0.1f + 4*0.6f + 24*0.3f;

		assertEquals(expectedX, c.aveQ.x, GrlConstants.FLOAT_TEST_TOL);
		assertEquals(expectedY, c.aveQ.y, GrlConstants.FLOAT_TEST_TOL);
	}

	@Test
	public void computeWeights() {
		ImageDeformPointMLS_F32 alg = new ImageDeformPointMLS_F32();
		alg.configure(width,height, rows, cols);

		alg.addControl(10,15);
		alg.addControl(5,4);
		alg.addControl(20,24);

		float weights[] = new float[3];

		// test an edge case
		alg.computeWeights(weights, 5/alg.scaleX, 4/alg.scaleY);
		checkWeights(weights,0,1,0);
		alg.computeWeights(weights, 20/alg.scaleX, 24/alg.scaleY);
		checkWeights(weights,0,0,1);

		// this should be a bit fuzzier
		alg.computeWeights(weights, 14/alg.scaleX, 17/alg.scaleY);
		assertTrue( weights[0] > weights[1]);
		assertTrue( weights[0] > weights[2]);
		assertTrue( weights[2] > weights[1]);

		// do a manual computation of the weight
		float alpha = 2.1f;
		double x2 = alg.scaleX*alg.scaleX;
		double y2 = alg.scaleY*alg.scaleY;

		double expected0 = Math.pow(2*2/x2 + 1/y2,-alpha);
		double expected1 = Math.pow(7*7/x2 + 12*12/y2,-alpha);
		double expected2 = Math.pow(8*8/x2 + 8*8/y2,-alpha);

		alg.setAlpha(alpha);
		alg.computeWeights(weights,12/alg.scaleX,16/alg.scaleY);
		assertEquals(expected0/((expected0+expected1+expected2)), weights[0], GrlConstants.FLOAT_TEST_TOL);

	}

	private void checkWeights(float weights[], float... expected) {
		assertArrayEquals(weights, expected, GrlConstants.FLOAT_TEST_TOL);
	}

	@Test
	public void computeGridCoordinate() {
		ImageDeformPointMLS_F32 alg = new ImageDeformPointMLS_F32();
		alg.configure(width,height, rows, cols);

		float x0 = 4, x1 = 6;
		float y0 = 5, y1 = 10;

		alg.getGrid(2,3).deformed.set(x0,y0);
		alg.getGrid(3,3).deformed.set(x0,y1);
		alg.getGrid(3,4).deformed.set(x1,y1);
		alg.getGrid(2,4).deformed.set(x1,y0);

		// when sampled exactly on the coordinate it should be the distored value at that coordinate
		checkGridCoordinate( 3,2,x0,y0, alg );
		checkGridCoordinate( 3,3,x0,y1, alg );
		checkGridCoordinate( 4,3,x1,y1, alg );
		checkGridCoordinate( 4,2,x1,y0, alg );

		// try values exactly between
		checkGridCoordinate( 3.5f,2,0.5f*x0+0.5f*x1,y0, alg );
		checkGridCoordinate( 3.7f,2,0.3f*x0+0.7f*x1,y0, alg );
		checkGridCoordinate( 3f,2.5f,x0,0.5f*y0+0.5f*y1, alg );
		checkGridCoordinate( 3f,2.7f,x0,0.3f*y0+0.7f*y1, alg );

	}

	private void checkGridCoordinate( float x , float y , float expectedX , float expectedY , ImageDeformPointMLS_F32 alg ) {

		Point2D_F32 p = new Point2D_F32();
		alg.computeGridCoordinate(x,y, p);

		assertEquals(expectedX, p.x, GrlConstants.FLOAT_TEST_TOL);
		assertEquals(expectedY, p.y, GrlConstants.FLOAT_TEST_TOL);
	}
}
