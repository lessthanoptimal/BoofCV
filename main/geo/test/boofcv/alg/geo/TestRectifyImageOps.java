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

package boofcv.alg.geo;

import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.distort.PointTransform_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRectifyImageOps {

	Point2D_F32 p = new Point2D_F32();
	int width = 300;
	int height = 350;

	Random rand = new Random(12);

	/**
	 * After the camera matrix has been adjusted and a forward rectification transform has been applied
	 * the output image will be shrink and contained inside the output image.
	 */
	@Test
	public void fullViewLeft_calibrated() {

		fullViewLeft_calibrated(false);
		fullViewLeft_calibrated(true);
	}

	public void fullViewLeft_calibrated( boolean leftHanded ) {

		IntrinsicParameters param =
				new IntrinsicParameters(300,320,0,150,130,width,height, leftHanded, new double[]{0.1,1e-4});

		// do nothing rectification
		DenseMatrix64F rect1 = CommonOps.identity(3);
		DenseMatrix64F rect2 = CommonOps.identity(3);
		DenseMatrix64F rectK = UtilIntrinsic.calibrationMatrix(param,null);

		RectifyImageOps.fullViewLeft(param,rect1,rect2,rectK);

		// check left image
		PointTransform_F32 tran = RectifyImageOps.rectifyTransform(param, rect1);
		checkInside(tran);
		// the right view is not checked since it is not part of the contract
	}

	private void checkInside(PointTransform_F32 tran) {
		for( int y = 0; y < height; y++ ) {
			checkInside(0,y,tran);
			checkInside(width-1,y,tran);
		}

		for( int x = 0; x < width; x++ ) {
			checkInside(x,0,tran);
			checkInside(x,height-1,tran);
		}
	}

	private void checkInside( int x , int y , PointTransform_F32 tran ) {
		tran.compute(x, y, p);

		float tol = 0.1f;

		String s = x+" "+y+" -> "+p.x+" "+p.y;
		assertTrue(s,p.x >= -tol && p.x < width+tol );
		assertTrue(s,p.y >= -tol && p.y < height+tol );
	}

	@Test
	public void allInsideLeft_calibrated() {
		allInsideLeft_calibrated(false);
		allInsideLeft_calibrated(true);
	}

	public void allInsideLeft_calibrated( boolean leftHanded ) {
		IntrinsicParameters param =
				new IntrinsicParameters(300,320,0,150,130,width,height, leftHanded, new double[]{0.1,1e-4});

		// do nothing rectification
		DenseMatrix64F rect1 = CommonOps.identity(3);
		DenseMatrix64F rect2 = CommonOps.identity(3);
		DenseMatrix64F rectK = UtilIntrinsic.calibrationMatrix(param,null);

		RectifyImageOps.allInsideLeft(param, rect1, rect2, rectK);

		// check left image
		PointTransform_F32 tran = RectifyImageOps.rectifyTransformInv(param, rect1);
		checkInside(tran);
		// the right view is not checked since it is not part of the contract
	}

	@Test
	public void fullViewLeft_uncalibrated() {
		// do nothing rectification
		DenseMatrix64F rect1 = CommonOps.diag(2, 3, 1);
		DenseMatrix64F rect2 = CommonOps.diag(0.5, 2, 1);

		RectifyImageOps.fullViewLeft(300,250,rect1,rect2);

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
		CommonOps.invert(rect1,inv);
		PointTransformHomography_F32 tran = new PointTransformHomography_F32(inv);
		checkInside(tran);
		// the right view is not checked since it is not part of the contract
	}

	/**
	 * Transforms and then performs the inverse transform to distorted rectified pixel
	 */
	@Test
	public void rectifyTransform_and_rectifyTransformInv() {
		rectifyTransform_and_rectifyTransformInv(true);
		rectifyTransform_and_rectifyTransformInv(false);
	}

	public void rectifyTransform_and_rectifyTransformInv( boolean flipY ) {
		IntrinsicParameters param =
				new IntrinsicParameters(300,320,0,150,130,width,height, flipY, new double[]{0.1,1e-4});

		DenseMatrix64F rect = new DenseMatrix64F(3,3,true,1.1,0,0,0,2,0,0.1,0,3);

		PointTransform_F32 forward = RectifyImageOps.rectifyTransform(param,rect);
		PointTransform_F32 inverse = RectifyImageOps.rectifyTransformInv(param, rect);

		float x = 20,y=30;
		Point2D_F32 out = new Point2D_F32();

		forward.compute(x,y,out);

		// sanity check
		assertTrue(Math.abs(x - out.x) > 1e-4);
		assertTrue( Math.abs(y-out.y) > 1e-4);

		inverse.compute(out.x,out.y,out);

		assertEquals(x, out.x, 1e-4);
		assertEquals(y, out.y, 1e-4);
	}

	/**
	 * Test by using other tested functions, then manually applying the last step
	 */
	@Test
	public void rectifyNormalized_F64() {
		rectifyNormalized_F64(true);
		rectifyNormalized_F64(false);
	}

	public void rectifyNormalized_F64( boolean flipY ) {
		IntrinsicParameters param =
				new IntrinsicParameters(300,320,0,150,130,width,height, flipY, new double[]{0.1,1e-4});

		DenseMatrix64F rect = new DenseMatrix64F(3,3,true,1.1,0,0,0,2,0,0.1,0,3);
		DenseMatrix64F rectK = UtilIntrinsic.calibrationMatrix(param,null);

		DenseMatrix64F rectK_inv = new DenseMatrix64F(3,3);
		CommonOps.invert(rectK,rectK_inv);

		PointTransform_F32 tranRect = RectifyImageOps.rectifyTransform(param,rect);
		PointTransform_F64 alg = RectifyImageOps.rectifyNormalized_F64(param,rect,rectK);

		double x=10,y=20;

		// compute expected results
		Point2D_F32 rectified = new Point2D_F32();
		tranRect.compute((float)x,(float)y,rectified);
		Point2D_F64 expected = new Point2D_F64();
		// the transform converts it back into the flipped input coordinates, we don't want that
		double yFlip = flipY ? param.height-rectified.y-1 : rectified.y;
		GeometryMath_F64.mult(rectK_inv,new Point2D_F64(rectified.x,yFlip),expected);

		// compute the 'found' results
		Point2D_F64 found = new Point2D_F64();
		alg.compute(x,y,found);

		assertEquals(expected.x, found.x, 1e-4);
		assertEquals(expected.y, found.y, 1e-4);
	}
}
