/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestImageDeformPointMLS_F32 {

	int width = 45;
	int height = 60;
	int cols = 10;
	int rows = 12;

	/**
	 * Makes sure the output is approximately independent of the number of rows/columns in interpolation grid.
	 * There was a problem where different scales were provided to each axis messing up similarity and rigid
	 */
	@Test
	void test_shape_independent() {
		for( TypeDeformMLS type : TypeDeformMLS.values() ) {
			var alg = new ImageDeformPointMLS_F32(type);

			// try different shapes of image and grid and see if anything breaks
			check_shape_independent(60, 60, 30, 30, alg );
			check_shape_independent(60, 80, 30, 30, alg );
			check_shape_independent(80, 60, 30, 30, alg );
			check_shape_independent(60, 60, 30, 35, alg );
			check_shape_independent(60, 60, 35, 30, alg );
		}
	}

	private void check_shape_independent( int width , int height , int rows , int cols , ImageDeformPointMLS_F32 alg ) {
		alg.configure(width, height, rows, cols);
		alg.addControl(5, 5);
		alg.addControl(10, 20);
		alg.addControl(30, 50);
		alg.addControl(16, 0);
		checkNoTransform(alg);
	}

	private void checkNoTransform(ImageDeformPointMLS_F32 alg) {
		alg.fixate();

		// should be no change now
		Point2D_F32 found = new Point2D_F32();
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				alg.compute(j,i, found);
				assertEquals(j, found.x, GrlConstants.TEST_F32);
				assertEquals(i, found.y, GrlConstants.TEST_F32);
			}
		}
	}

	/**
	 * When sampled exactly on a control point the distortion should be the distortion for that point
	 */
	@Test
	void testAllAtOnce_OnControlPoints() {
		for( TypeDeformMLS type : TypeDeformMLS.values() ) {
//			System.out.println("type "+type);
			var alg = new ImageDeformPointMLS_F32(type);
			alg.configure(100, 100, 11, 11);

			// carefully place control points on grid points to minimze the affect of the bilinear interpolation step
			alg.addControl(10, 0);
			alg.addControl(10, 20);
			alg.addControl(30, 40);
			alg.addControl(80, 30);

			alg.setDistorted(0, 10, 5);
			alg.setDistorted(1, 14, 30);
			alg.setDistorted(2, 25, 45);
			alg.setDistorted(3, 20, 8);

			alg.fixate();

			checkCompute(10, 0, 10, 5, alg);
			checkCompute(10, 20, 14, 30, alg);
			checkCompute(30, 40, 25, 45, alg);
			checkCompute(80, 30, 20, 8, alg);
		}
	}

	private void checkCompute( float x , float y , float expectedX , float expectedY , ImageDeformPointMLS_F32 alg ) {
		Point2D_F32 found = new Point2D_F32();
		alg.compute(x,y, found);

		assertEquals(expectedX, found.x, GrlConstants.TEST_F32);
		assertEquals(expectedY, found.y, GrlConstants.TEST_F32);
	}

	/**
	 * See if the distorted point is closer to the closest control point
	 */
	@Test
	void testAllAtOnce_CloserToCloser() {
		for( TypeDeformMLS type : TypeDeformMLS.values() ) {
			var alg = new ImageDeformPointMLS_F32(type);
			alg.configure(width, height, rows, cols);

			alg.addControl(5, 5);
			alg.addControl(10, 20);
			alg.addControl(30, 50);
			alg.addControl(16, 0);

			alg.setDistorted(0, 10, 12);
			alg.setDistorted(1, 14, 30);
			alg.setDistorted(2, 25, 45);
			alg.setDistorted(3, 20, 8);

			alg.fixate();

			Point2D_F32 a = new Point2D_F32();
			Point2D_F32 b = new Point2D_F32();

			alg.compute(4, 4, a);
			alg.compute(1, 4, b);

			float distA = a.distance(10, 12);
			float distB = b.distance(10, 12);

			assertTrue(distA < distB);
		}
	}

	/**
	 * Should produce identical results when fixate is called multiple times
	 */
	@Test
	void multipleCallsToFixate() {
		for( TypeDeformMLS type : TypeDeformMLS.values() ) {
			var alg = new ImageDeformPointMLS_F32(type);
			alg.configure(width, height, rows, cols);

			alg.addControl(5, 5);
			alg.addControl(10, 20);
			alg.addControl(30, 50);
			alg.addControl(16, 0);

			alg.setDistorted(0, 10, 12);
			alg.setDistorted(1, 14, 30);
			alg.setDistorted(2, 25, 45);
			alg.setDistorted(3, 20, 8);

			alg.fixate();

			Point2D_F32 expected = new Point2D_F32();
			alg.compute(4, 4, expected);

			Point2D_F32 found = new Point2D_F32();
			alg.fixate();
			alg.compute(4, 4, found);
			assertTrue(found.distance(expected) <= GrlConstants.TEST_F32);

			alg.fixate();
			alg.compute(4, 4, found);
			assertTrue(found.distance(expected) <= GrlConstants.TEST_F32);
		}
	}

	@Test
	void computeAverageP() {
		var alg = new ImageDeformPointMLS_F32(TypeDeformMLS.AFFINE);
		alg.configure(width,height, rows, cols);

		alg.addControl(10,15);
		alg.addControl(5,4);
		alg.addControl(20,24);

		// set p locations
		alg.controls.get(0).p.set(12,17);
		alg.controls.get(1).p.set(2,7);
		alg.controls.get(2).p.set(18,30);


		var weights = new float[]{0.1f,0.6f,0.3f};
		alg.totalWeight = 1;
		alg.aveP.set(1,2);

		alg.computeAverageP(weights);

		float expectedX = 12*0.1f + 2*0.6f + 18*0.3f;
		float expectedY = 17*0.1f + 7*0.6f + 30*0.3f;

		assertEquals(expectedX, alg.aveP.x, GrlConstants.TEST_F32);
		assertEquals(expectedY, alg.aveP.y, GrlConstants.TEST_F32);
	}

	@Test
	void computeAverageQ() {
		var alg = new ImageDeformPointMLS_F32(TypeDeformMLS.AFFINE);
		alg.configure(width,height, rows, cols);

		alg.addControl(10,15);
		alg.addControl(5,4);
		alg.addControl(20,24);

		var weights = new float[]{0.1f,0.6f,0.3f};
		alg.totalWeight = 1;
		alg.aveQ.set(1,2);

		alg.computeAverageQ(weights);

		float expectedX = 10*0.1f + 5*0.6f + 20*0.3f;
		float expectedY = 15*0.1f + 4*0.6f + 24*0.3f;

		assertEquals(expectedX, alg.aveQ.x, GrlConstants.TEST_F32);
		assertEquals(expectedY, alg.aveQ.y, GrlConstants.TEST_F32);
	}

	@Test
	void computeWeights() {
		ImageDeformPointMLS_F32 alg = new ImageDeformPointMLS_F32(TypeDeformMLS.AFFINE);
		alg.configure(width,height, rows, cols);

		alg.addControl(10,15);
		alg.addControl(5,4);
		alg.addControl(20,24);

		alg.weights.resize(alg.controls.size);

		// test an edge case
		alg.computeWeights(5/alg.scaleX, 4/alg.scaleY, alg.weights.data);
		checkWeights(alg,0,1,0);
		alg.computeWeights(20/alg.scaleX, 24/alg.scaleY, alg.weights.data);
		checkWeights(alg,0,0,1);

		// this should be a bit fuzzier
		alg.computeWeights(14/alg.scaleX, 17/alg.scaleY, alg.weights.data);
		assertTrue( alg.weights.data[0] > alg.weights.data[1]);
		assertTrue( alg.weights.data[0] > alg.weights.data[2]);
		assertTrue( alg.weights.data[2] > alg.weights.data[1]);

		// do a manual computation of the weight
		float alpha = 2.1f;
		double x2 = alg.scaleX*alg.scaleX;
		double y2 = alg.scaleY*alg.scaleY;

		double expected0 = Math.pow(2*2/x2 + 1/y2,-alpha);

		alg.setAlpha(alpha);
		alg.computeWeights(12/alg.scaleX,16/alg.scaleY, alg.weights.data);
		assertEquals(expected0, alg.weights.data[0], GrlConstants.TEST_F32);

	}

	private void checkWeights(ImageDeformPointMLS_F32 alg, float... expected) {
		for (int i = 0; i < alg.weights.size(); i++) {
			float expectedW = expected[i] * alg.totalWeight;
			assertEquals(alg.weights.data[i], expectedW, GrlConstants.TEST_F32);
		}
	}

	@Test
	void interpolateDeformedPoint() {
		ImageDeformPointMLS_F32 alg = new ImageDeformPointMLS_F32(TypeDeformMLS.AFFINE);
		alg.configure(width,height, rows, cols);

		float x0 = 4, x1 = 6;
		float y0 = 5, y1 = 10;

		alg.getGrid(2,3).set(x0,y0);
		alg.getGrid(3,3).set(x0,y1);
		alg.getGrid(3,4).set(x1,y1);
		alg.getGrid(2,4).set(x1,y0);

		// when sampled exactly on the coordinate it should be the distored value at that coordinate
		CheckInterpolated( 3,2,x0,y0, alg );
		CheckInterpolated( 3,3,x0,y1, alg );
		CheckInterpolated( 4,3,x1,y1, alg );
		CheckInterpolated( 4,2,x1,y0, alg );

		// try values exactly between
		CheckInterpolated( 3.5f,2,0.5f*x0+0.5f*x1,y0, alg );
		CheckInterpolated( 3.7f,2,0.3f*x0+0.7f*x1,y0, alg );
		CheckInterpolated( 3f,2.5f,x0,0.5f*y0+0.5f*y1, alg );
		CheckInterpolated( 3f,2.7f,x0,0.3f*y0+0.7f*y1, alg );

	}

	private void CheckInterpolated(float x , float y , float expectedX , float expectedY , ImageDeformPointMLS_F32 alg ) {

		Point2D_F32 p = new Point2D_F32();
		alg.interpolateDeformedPoint(x,y, p);

		assertEquals(expectedX, p.x, GrlConstants.TEST_F32);
		assertEquals(expectedY, p.y, GrlConstants.TEST_F32);
	}

	@Test
	void copyConcurrent() {
		for( TypeDeformMLS type : TypeDeformMLS.values() ) {
//			System.out.println("type "+type);
			ImageDeformPointMLS_F32 orig = new ImageDeformPointMLS_F32(type);
			orig.configure(100, 100, 11, 11);

			// carefully place control points on grid points to minimze the affect of the bilinear interpolation step
			orig.addControl(10, 0);
			orig.addControl(10, 20);
			orig.addControl(30, 40);
			orig.addControl(80, 30);

			orig.setDistorted(0, 10, 5);
			orig.setDistorted(1, 14, 30);
			orig.setDistorted(2, 25, 45);
			orig.setDistorted(3, 20, 8);

			orig.fixate();

			ImageDeformPointMLS_F32 copy = orig.copyConcurrent();

			checkCompute(10, 0 , orig, copy);
			checkCompute(10, 20, orig, copy);
			checkCompute(30, 40, orig, copy);
			checkCompute(80, 30, orig, copy);
		}
	}

	private void checkCompute( float x , float y ,
							   ImageDeformPointMLS_F32 orig, ImageDeformPointMLS_F32 copy) {
		Point2D_F32 expected = new Point2D_F32();
		Point2D_F32 found = new Point2D_F32();
		orig.compute(x,y, expected);
		copy.compute(x,y, found);

		assertEquals(expected.x, found.x, GrlConstants.TEST_F32);
		assertEquals(expected.y, found.y, GrlConstants.TEST_F32);
	}
}
