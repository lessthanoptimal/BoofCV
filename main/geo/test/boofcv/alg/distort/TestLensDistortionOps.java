/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F32;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.shapes.RectangleLength2D_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestLensDistortionOps {

	Point2D_F32 p = new Point2D_F32();
	int width = 300;
	int height = 350;

	/**
	 * Checks the border of the returned transform.  Makes sure that the entire original image is visible.
	 * Also makes sure that the requested inverse transform is actually the inverse.
	 */
	@Test
	public void fullView_Transform() {
		IntrinsicParameters param = new IntrinsicParameters().
				fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(0.1, 0.05);

		PointTransform_F32 adjToDist = LensDistortionOps.fullView(param, null, true);
		PointTransform_F32 distToAdj = LensDistortionOps.fullView(param, null, false);

		checkBorderOutside(adjToDist,distToAdj);

		param = new IntrinsicParameters().
				fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(-0.1,-0.05);
		adjToDist = LensDistortionOps.fullView(param, null, true);
		distToAdj = LensDistortionOps.fullView(param, null, false);
		checkBorderOutside(adjToDist,distToAdj);
	}

	private void checkBorderOutside(PointTransform_F32 tran, PointTransform_F32 tranInv) {
		for( int y = 0; y < height; y++ ) {
			checkBorderOutside(0, y, tran, tranInv);
			checkBorderOutside(width - 1, y, tran, tranInv);
		}

		for( int x = 0; x < width; x++ ) {
			checkBorderOutside(x, 0, tran, tranInv);
			checkBorderOutside(x, height - 1, tran, tranInv);
		}
	}

	private void checkBorderOutside(int x, int y, PointTransform_F32 tran, PointTransform_F32 tranInv) {
		tran.compute(x, y, p);

		float tol = 0.1f;

		String s = x+" "+y+" -> "+p.x+" "+p.y;
		assertTrue(s,
				p.x <= 1 + tol || p.x >= width - 1 - tol ||
						p.y <= 1 + tol || p.y >= height - 1 - tol);

		// check the inverse
		tranInv.compute(p.x,p.y,p);

		assertEquals(p.x,x, 0.01f);
		assertEquals(p.x,x, 0.01f);
	}

	/**
	 * Sees if the adjusted intrinsic parameters is correct
	 */
	@Test
	public void fullView_intrinsic() {

		// distorted pixel in original image
		float pixelX = 12.5f,pixelY = height-3;

		IntrinsicParameters orig = new IntrinsicParameters().
				fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(0.1, 0.05);

		PointTransform_F32 distToNorm = LensDistortionOps.distortTransform(orig).undistort_F32(true, false);

		Point2D_F32 norm = new Point2D_F32();
		distToNorm.compute(pixelX, pixelY, norm);

		IntrinsicParameters adjusted = new IntrinsicParameters();
		PointTransform_F32 distToAdj = LensDistortionOps.fullView(orig, adjusted, false);

		Point2D_F32 adjPixel = new Point2D_F32();
		Point2D_F32 normFound = new Point2D_F32();
		distToAdj.compute(pixelX,pixelY,adjPixel);

		PerspectiveOps.convertPixelToNorm(adjusted, adjPixel, normFound);

		// see if the normalized image coordinates are the same
		assertEquals(norm.x, normFound.x,1e-3);
		assertEquals(norm.y, normFound.y, 1e-3);
	}

	/**
	 * Checks the border of the returned transform.  Makes sure that no none-visible portion is visible.
	 * Also makes sure that the requested inverse transform is actually the inverse.
	 */
	@Test
	public void allInside_Transform() {
		IntrinsicParameters param =
				new IntrinsicParameters().fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(0.1, 1e-4);

		PointTransform_F32 adjToDist = LensDistortionOps.allInside(param, null, true);
		PointTransform_F32 distToAdj = LensDistortionOps.allInside(param, null, false);
		checkInside(adjToDist, distToAdj);

		// distort it in the other direction
		param = new IntrinsicParameters().fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(-0.1,-1e-4);

		adjToDist = LensDistortionOps.allInside(param, null, true);
		distToAdj = LensDistortionOps.allInside(param, null, false);

		checkInside(adjToDist, distToAdj);
	}

	private void checkInside(PointTransform_F32 tran, PointTransform_F32 tranInv ) {
		double closestT = Double.MAX_VALUE;
		double closestB = Double.MAX_VALUE;

		for( int y = 0; y < height; y++ ) {
			checkInside(0,y,tran,tranInv);
			checkInside(width-1,y,tran,tranInv);

			closestT = Math.min(closestT,distanceEdge(0,y,tran));
			closestB = Math.min(closestB,distanceEdge(width-1,y,tran));
		}

		// should be close to the edge at some point
		assertTrue( closestT < 1 );
		assertTrue( closestB < 1 );

		closestT = closestB = Double.MAX_VALUE;
		for( int x = 0; x < width; x++ ) {
			checkInside(x,0,tran,tranInv);
			checkInside(x,height-1,tran,tranInv);

			closestT = Math.min(closestT,distanceEdge(x,0,tran));
			closestB = Math.min(closestB,distanceEdge(x,height-1,tran));
		}

		// should be close to the edge at some point
		assertTrue(closestT < 1);
		assertTrue(closestB < 1);
	}

	private void checkInside( int x , int y , PointTransform_F32 tran , PointTransform_F32 tranInv ) {
		tran.compute(x, y, p);

		float tol = 0.1f;

		String s = x+" "+y+" -> "+p.x+" "+p.y;
		assertTrue(s, p.x >= -tol && p.x < width + tol);
		assertTrue(s, p.y >= -tol && p.y < height + tol);

		// check the inverse
		tranInv.compute(p.x,p.y,p);

		assertEquals(p.x, x, 0.01f);
		assertEquals(p.x, x, 0.01f);
	}

	private double distanceEdge( int x , int y ,  PointTransform_F32 tran ) {
		tran.compute(x, y, p);

		double min = Double.MAX_VALUE;

		if( x < min ) min = x;
		if( y < min ) min = y;
		if( width-x-1 < min ) min = width-x-1;
		if( height-y-1 < min ) min = height-y-1;

		return min;
	}

	/**
	 * Sees if the adjusted intrinsic parameters is correct but computing normalized image coordinates first
	 * with the original distorted image and then with the adjusted undistorted image.
	 */
	@Test
	public void allInside_intrinsic() {

		// distorted pixel in original image
		float pixelX = 12.5f,pixelY = height-3;

		IntrinsicParameters orig = new IntrinsicParameters().
				fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(0.1, 0.05);

		PointTransform_F32 distToNorm = LensDistortionOps.distortTransform(orig).undistort_F32(true, false);

		Point2D_F32 norm = new Point2D_F32();
		distToNorm.compute(pixelX, pixelY, norm);

		IntrinsicParameters adjusted = new IntrinsicParameters();
		PointTransform_F32 distToAdj = LensDistortionOps.allInside(orig,adjusted,false);

		Point2D_F32 adjPixel = new Point2D_F32();
		Point2D_F32 normFound = new Point2D_F32();
		distToAdj.compute(pixelX,pixelY,adjPixel);

		PerspectiveOps.convertPixelToNorm(adjusted, adjPixel, normFound);

		// see if the normalized image coordinates are the same
		assertEquals(norm.x, normFound.x, 1e-3);
		assertEquals(norm.y, normFound.y, 1e-3);
	}


	@Test
	public void boundBoxInside() {
		// basic sanity check
		Affine2D_F32 affine = new Affine2D_F32(1,1,0,1,1,2);
		PixelTransformAffine_F32 transform = new PixelTransformAffine_F32(affine);
		RectangleLength2D_F32 found = LensDistortionOps.boundBoxInside(20, 10, transform);

		assertEquals(10,found.x0,1e-4);
		assertEquals(2 ,found.y0,1e-4);
		assertEquals(20-9,found.width,1e-4);
		assertEquals(10, found.height,1e-4);
	}
}
