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

package boofcv.alg.geo;

import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRectifyImageOps {

	Point2D_F32 p = new Point2D_F32();
	int width = 300;
	int height = 350;

	/**
	 * After the camera matrix has been adjusted and a forward rectification transform has been applied
	 * the output image will be shrink and contained inside the output image.
	 */
	@Test
	public void fullViewLeft_calibrated() {

		CameraPinholeRadial param =
				new CameraPinholeRadial().fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(0.1,1e-4);

		// do nothing rectification
		DenseMatrix64F rect1 = CommonOps.identity(3);
		DenseMatrix64F rect2 = CommonOps.identity(3);
		DenseMatrix64F rectK = PerspectiveOps.calibrationMatrix(param, null);

		RectifyImageOps.fullViewLeft(param,rect1,rect2,rectK);

		// check left image
		Point2Transform2_F32 tran = RectifyImageOps.transformPixelToRect_F32(param, rect1);
		checkInside(tran);
		// the right view is not checked since it is not part of the contract
	}

	private void checkInside(Point2Transform2_F32 tran) {
		for( int y = 0; y < height; y++ ) {
			checkInside(0,y,tran);
			checkInside(width-1,y,tran);
		}

		for( int x = 0; x < width; x++ ) {
			checkInside(x,0,tran);
			checkInside(x,height-1,tran);
		}
	}

	private void checkInside( int x , int y , Point2Transform2_F32 tran ) {
		tran.compute(x, y, p);

		float tol = 0.1f;

		String s = x+" "+y+" -> "+p.x+" "+p.y;
		assertTrue(s,p.x >= -tol && p.x < width+tol );
		assertTrue(s, p.y >= -tol && p.y < height + tol);
	}

	@Test
	public void allInsideLeft_calibrated() {

		CameraPinholeRadial param = new CameraPinholeRadial().
				fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(0.1,1e-4);

		// do nothing rectification
		DenseMatrix64F rect1 = CommonOps.identity(3);
		DenseMatrix64F rect2 = CommonOps.identity(3);
		DenseMatrix64F rectK = PerspectiveOps.calibrationMatrix(param, null);

		RectifyImageOps.allInsideLeft(param, rect1, rect2, rectK);

		// check left image
		Point2Transform2_F32 tran = RectifyImageOps.transformRectToPixel_F32(param, rect1);
		checkInside(tran);
		// the right view is not checked since it is not part of the contract
	}

	@Test
	public void fullViewLeft_uncalibrated() {
		// do nothing rectification
		DenseMatrix64F rect1 = CommonOps.diag(2, 3, 1);
		DenseMatrix64F rect2 = CommonOps.diag(0.5, 2, 1);

		RectifyImageOps.fullViewLeft(300, 250, rect1, rect2);

		// check left image
		PointTransformHomography_F32 tran = new PointTransformHomography_F32(rect1);
		checkInside(tran);
		// the right view is not checked since it is not part of the contract
	}

	@Test
	public void allInsideLeft_uncalibrated() {
		// do nothing rectification
		DenseMatrix64F rect1 = CommonOps.diag(2, 3, 1);
		DenseMatrix64F rect2 = CommonOps.diag(0.5, 2, 1);

		RectifyImageOps.allInsideLeft(300, 250, rect1, rect2);

		// check left image
		DenseMatrix64F inv = new DenseMatrix64F(3,3);
		CommonOps.invert(rect1, inv);
		PointTransformHomography_F32 tran = new PointTransformHomography_F32(inv);
		checkInside(tran);
		// the right view is not checked since it is not part of the contract
	}

	/**
	 * Transforms and then performs the inverse transform to distorted rectified pixel
	 */
	@Test
	public void transform_PixelToRect_and_RectToPixel_F32() {

		CameraPinholeRadial param = new CameraPinholeRadial().
				fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(0.1,1e-4);

		DenseMatrix64F rect = new DenseMatrix64F(3,3,true,1.1,0,0,0,2,0,0.1,0,3);

		Point2Transform2_F32 forward = RectifyImageOps.transformPixelToRect_F32(param, rect);
		Point2Transform2_F32 inverse = RectifyImageOps.transformRectToPixel_F32(param, rect);

		float x = 20,y=30;
		Point2D_F32 out = new Point2D_F32();

		forward.compute(x,y,out);

		// sanity check
		assertTrue( Math.abs(x - out.x) > 1e-4);
		assertTrue( Math.abs(y - out.y) > 1e-4);

		inverse.compute(out.x,out.y,out);

		assertEquals(x, out.x, 1e-4);
		assertEquals(y, out.y, 1e-4);
	}

	@Test
	public void transform_PixelToRect_and_RectToPixel_F64() {

		CameraPinholeRadial param = new CameraPinholeRadial().
				fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(0.1,1e-4);

		DenseMatrix64F rect = new DenseMatrix64F(3,3,true,1.1,0,0,0,2,0,0.1,0,3);

		Point2Transform2_F64 forward = RectifyImageOps.transformPixelToRect_F64(param, rect);
		Point2Transform2_F64 inverse = RectifyImageOps.transformRectToPixel_F64(param, rect);

		double x = 20,y=30;
		Point2D_F64 out = new Point2D_F64();

		forward.compute(x,y,out);

		// sanity check
		assertTrue( Math.abs(x - out.x) > 1e-8);
		assertTrue( Math.abs(y - out.y) > 1e-8);

		inverse.compute(out.x,out.y,out);

		assertEquals(x, out.x, 1e-5);
		assertEquals(y, out.y, 1e-5);
	}

	/**
	 * Test by using other tested functions, then manually applying the last step
	 */
	@Test
	public void transformPixelToRectNorm_F64() {
		CameraPinholeRadial param = new CameraPinholeRadial().
						fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(0.1,1e-4);

		DenseMatrix64F rect = new DenseMatrix64F(3,3,true,1.1,0,0,0,2,0,0.1,0,3);
		DenseMatrix64F rectK = PerspectiveOps.calibrationMatrix(param, null);

		DenseMatrix64F rectK_inv = new DenseMatrix64F(3,3);
		CommonOps.invert(rectK,rectK_inv);

		Point2Transform2_F32 tranRect = RectifyImageOps.transformPixelToRect_F32(param, rect);
		Point2Transform2_F64 alg = RectifyImageOps.transformPixelToRectNorm_F64(param, rect, rectK);

		double x=10,y=20;

		// compute expected results
		Point2D_F32 rectified = new Point2D_F32();
		tranRect.compute((float)x,(float)y,rectified);
		Point2D_F64 expected = new Point2D_F64();
		GeometryMath_F64.mult(rectK_inv,new Point2D_F64(rectified.x,rectified.y),expected);

		// compute the 'found' results
		Point2D_F64 found = new Point2D_F64();
		alg.compute(x,y,found);

		assertEquals(expected.x, found.x, 1e-4);
		assertEquals(expected.y, found.y, 1e-4);
	}
}
