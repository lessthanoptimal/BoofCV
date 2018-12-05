/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F32;
import georegression.metric.Intersection2D_F32;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.shapes.RectangleLength2D_F32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestLensDistortionOps_F32 {

	Point2D_F32 pf = new Point2D_F32();
	Point2D_F32 pd = new Point2D_F32();
	int width = 300;
	int height = 350;

	@Test
	public void transformChangeModel_NONE_modified() {
		// distorted pixel in original image
		float pixelX = 12.5f,pixelY = height-3;

		CameraPinholeRadial orig = new CameraPinholeRadial().
				fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(0.1f, 0.05f);
		CameraPinhole desired = new CameraPinhole(orig);

		Point2Transform2_F32 distToNorm = LensDistortionFactory.narrow(orig).undistort_F32(true, false);

		Point2D_F32 norm = new Point2D_F32();
		distToNorm.compute(pixelX, pixelY, norm);

		CameraPinhole adjusted = new CameraPinhole();
		Point2Transform2_F32 distToAdj = LensDistortionOps_F32.
				transformChangeModel(AdjustmentType.NONE, orig, desired, false, adjusted);

		Point2D_F32 adjPixel = new Point2D_F32();
		Point2D_F32 normFound = new Point2D_F32();
		distToAdj.compute(pixelX,pixelY,adjPixel);

		PerspectiveOps.convertPixelToNorm(adjusted, adjPixel, normFound);

		// see if the normalized image coordinates are the same
		assertEquals(norm.x, normFound.x,1e-3);
		assertEquals(norm.y, normFound.y, 1e-3);
	}

	/**
	 * Checks the border of the returned transform.  Makes sure that the entire original image is visible.
	 * Also makes sure that the requested inverse transform is actually the inverse.
	 */
	@Test
	public void transformChangeModel_FULLVIEW() {
		CameraPinholeRadial param = new CameraPinholeRadial().
				fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(0.1f, 0.05f);
		CameraPinhole desired = new CameraPinhole(param);

		Point2Transform2_F32 adjToDist = LensDistortionOps_F32.transformChangeModel(
				AdjustmentType.FULL_VIEW, param, desired, true, null);
		Point2Transform2_F32 distToAdj = LensDistortionOps_F32.transformChangeModel(
				AdjustmentType.FULL_VIEW, param, desired, false, null);

		checkBorderOutside(adjToDist,distToAdj);

		param = new CameraPinholeRadial().
				fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(-0.1f,-0.05f);
		desired = new CameraPinhole(param);
		adjToDist = LensDistortionOps_F32.transformChangeModel(AdjustmentType.FULL_VIEW, param,desired, true, null);
		distToAdj = LensDistortionOps_F32.transformChangeModel(AdjustmentType.FULL_VIEW, param,desired, false, null);
		checkBorderOutside(adjToDist,distToAdj);
	}

	/**
	 * Checks to see if the returned modified model is correct
	 */
	@Test
	public void transformChangeModel_FULLVIEW_modified() {
		// distorted pixel in original image
		float pixelX = 12.5f ,pixelY = height-3;

		CameraPinholeRadial orig = new CameraPinholeRadial().
				fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(0.1f, 0.05f);
		CameraPinhole desired = new CameraPinhole(orig);

		Point2Transform2_F32 distToNorm = LensDistortionFactory.narrow(orig).undistort_F32(true, false);

		Point2D_F32 norm = new Point2D_F32();
		distToNorm.compute(pixelX, pixelY, norm);

		CameraPinholeRadial adjusted = new CameraPinholeRadial();
		Point2Transform2_F32 distToAdj = LensDistortionOps_F32.
				transformChangeModel(AdjustmentType.FULL_VIEW, orig, desired, false, adjusted);

		Point2D_F32 adjPixel  = new Point2D_F32();
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
	public void transformChangeModel_EXPAND() {
		CameraPinholeRadial param;
		param = new CameraPinholeRadial().fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(0.1f, 1e-4);
		CameraPinhole desired = new CameraPinhole(param);

		Point2Transform2_F32 adjToDist = LensDistortionOps_F32.transformChangeModel(AdjustmentType.EXPAND, param,desired, true, null);
		Point2Transform2_F32 distToAdj = LensDistortionOps_F32.transformChangeModel(AdjustmentType.EXPAND, param,desired, false, null);
		checkInside(adjToDist, distToAdj);

		// distort it in the other direction
		param = new CameraPinholeRadial().fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(-0.1f,-1e-4);

		adjToDist = LensDistortionOps_F32.transformChangeModel(AdjustmentType.EXPAND, param, desired, true, null);
		distToAdj = LensDistortionOps_F32.transformChangeModel(AdjustmentType.EXPAND, param, desired, false, null);

		checkInside(adjToDist, distToAdj);
	}

	/**
	 * Sees if the adjusted intrinsic parameters is correct but computing normalized image coordinates first
	 * with the original distorted image and then with the adjusted undistorted image.
	 */
	@Test
	public void transformChangeModel_EXPAND_modified() {

		// distorted pixel in original image
		float pixelX = 12.5f,pixelY = height-3;

		CameraPinholeRadial orig = new CameraPinholeRadial().
				fsetK(300, 320, 0, 150, 130, width, height).fsetRadial(0.1f, 0.05f);
		CameraPinhole desired = new CameraPinhole(orig);

		Point2Transform2_F32 distToNorm = LensDistortionFactory.narrow(orig).undistort_F32(true, false);

		Point2D_F32 norm = new Point2D_F32();
		distToNorm.compute(pixelX, pixelY, norm);

		CameraPinholeRadial adjusted = new CameraPinholeRadial();
		Point2Transform2_F32 distToAdj = LensDistortionOps_F32.transformChangeModel(AdjustmentType.EXPAND, orig, desired, false, adjusted);

		Point2D_F32 adjPixel = new Point2D_F32();
		Point2D_F32 normFound = new Point2D_F32();
		distToAdj.compute(pixelX,pixelY,adjPixel);

		PerspectiveOps.convertPixelToNorm(adjusted, adjPixel, normFound);

		// see if the normalized image coordinates are the same
		assertEquals(norm.x, normFound.x, 1e-3);
		assertEquals(norm.y, normFound.y, 1e-3);
	}

	private void checkBorderOutside(Point2Transform2_F32 tran, Point2Transform2_F32 tranInv) {
		for (int y = 0; y < height; y++) {
			checkBorderOutside(0, y, tran, tranInv);
			checkBorderOutside(width - 1, y, tran, tranInv);
		}

		for (int x = 0; x < width; x++) {
			checkBorderOutside(x, 0, tran, tranInv);
			checkBorderOutside(x, height - 1, tran, tranInv);
		}
	}

	private void checkBorderOutside(int x, int y, Point2Transform2_F32 tran, Point2Transform2_F32 tranInv) {
		tran.compute(x, y, pf);

		float tol = 0.1f;

		String s = x+" "+y+" -> "+ pf.x+" "+ pf.y;
		assertTrue(pf.x <= 1 + tol || pf.x >= width - 1 - tol ||
						pf.y <= 1 + tol || pf.y >= height - 1 - tol,s);

		// check the inverse
		tranInv.compute(pf.x, pf.y, pf);

		assertEquals(pf.x,x, 0.01f );
		assertEquals(pf.y,y, 0.01f );
	}

	private void checkInside(Point2Transform2_F32 tran, Point2Transform2_F32 tranInv ) {
		float closestT = Float.MAX_VALUE;
		float closestB = Float.MAX_VALUE;

		for( int y = 0; y < height; y++ ) {
			checkInside(0,y,tran,tranInv);
			checkInside(width-1,y,tran,tranInv);

			closestT = (float)Math.min(closestT,distanceEdge(0,y,tran));
			closestB = (float)Math.min(closestB,distanceEdge(width-1,y,tran));
		}

		// should be close to the edge at some point
		assertTrue( closestT < 1 );
		assertTrue( closestB < 1 );

		closestT = closestB = Float.MAX_VALUE;
		for( int x = 0; x < width; x++ ) {
			checkInside(x,0,tran,tranInv);
			checkInside(x,height-1,tran,tranInv);

			closestT = (float)Math.min(closestT,distanceEdge(x,0,tran));
			closestB = (float)Math.min(closestB,distanceEdge(x,height-1,tran));
		}

		// should be close to the edge at some point
		assertTrue(closestT < 1);
		assertTrue(closestB < 1);
	}

	private void checkInside(int x , int y , Point2Transform2_F32 tran , Point2Transform2_F32 tranInv ) {
		tran.compute(x, y, pd);

		float tol = 0.1f;

		String s = x+" "+y+" -> "+ pd.x+" "+ pd.y;
		assertTrue(pd.x >= -tol && pd.x < width + tol,s);
		assertTrue( pd.y >= -tol && pd.y < height + tol,s);

		// check the inverse
		tranInv.compute(pd.x, pd.y, pd);

		assertEquals(pd.x, x, 0.01f);
		assertEquals(pd.x, x, 0.01f);
	}


	private float distanceEdge( int x , int y ,  Point2Transform2_F32 tran ) {
		tran.compute(x, y, pd);

		float min = Float.MAX_VALUE;

		if( x < min ) min = x;
		if( y < min ) min = y;
		if( width-x-1 < min ) min = width-x-1;
		if( height-y-1 < min ) min = height-y-1;

		return min;
	}

	@Test
	public void boundBoxInside() {

		// easy cases
		checkInsideBorder(new Affine2D_F32(1,0,0,1,1,2),true);
		checkInsideBorder(new Affine2D_F32(1,0,0,2,1,2),true);

		// rotated by 90 degrees, still easy
		checkInsideBorder(new Affine2D_F32(0,-1,1,0,1,2),true);
		checkInsideBorder(new Affine2D_F32(-1,0,0,-1,1,2),true);

		// when not square the hard rules above doesn't work any more because by design some outside is included
		// inside as a compromise
	}

	@Test
	public void centerBoxInside() {

		// easy cases
		checkInsideBorder(new Affine2D_F32(1,0,0,1,1,2),true);
		checkInsideBorder(new Affine2D_F32(1,0,0,2,1,2),true);

		// rotated by 90 degrees, still easy
		checkInsideBorder(new Affine2D_F32(0,-1,1,0,1,2),true);
		checkInsideBorder(new Affine2D_F32(-1,0,0,-1,1,2),true);

		// when not square the hard rules above doesn't work any more because by design some outside is included
		// inside as a compromise
	}

	private void checkInsideBorder(Affine2D_F32 affine , boolean bound ) {
		int width = 20;
		int height = 10;
		PixelTransformAffine_F32 transform = new PixelTransformAffine_F32(affine);
		RectangleLength2D_F32 found;

		if( bound ) {
			found = LensDistortionOps_F32.boundBoxInside(width, height, transform);
		} else {
			found = LensDistortionOps_F32.centerBoxInside(width, height, transform);
		}

		// fudge factor
		found.x0 -= 0.1f;
		found.y0 -= 0.1f;
		found.width += 0.2f;
		found.height += 0.2f;

		// see if it contains the boundary points in the source image
		for (int i = 0; i < width; i++) {
			transform.compute(i,0);
			assertTrue(Intersection2D_F32.contains(found,transform.distX,transform.distY));
			transform.compute(i,height-1);
			assertTrue(Intersection2D_F32.contains(found,transform.distX,transform.distY));
		}
		for (int i = 0; i < height; i++) {
			transform.compute(0,i);
			assertTrue(Intersection2D_F32.contains(found,transform.distX,transform.distY));
			transform.compute(width-1,i);
			assertTrue(Intersection2D_F32.contains(found,transform.distX,transform.distY));
		}

		// see if points outside are outside
		for (int i = 0; i < width; i++) {
			transform.compute(i,-1);
			assertFalse(Intersection2D_F32.contains(found,transform.distX,transform.distY));
			transform.compute(i,height+1);
			assertFalse(Intersection2D_F32.contains(found,transform.distX,transform.distY));
		}
		for (int i = 1; i < height; i++) {
			transform.compute(-1,i);
			assertFalse(Intersection2D_F32.contains(found,transform.distX,transform.distY));
			transform.compute(width+1,i);
			assertFalse(Intersection2D_F32.contains(found,transform.distX,transform.distY));
		}
	}
}
