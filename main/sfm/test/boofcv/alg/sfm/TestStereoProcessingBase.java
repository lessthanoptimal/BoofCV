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

package boofcv.alg.sfm;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayU8;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestStereoProcessingBase {

	int width = 320;
	int height = 240;

	/**
	 * Center a point in the left and right images.  Search for the point and see if after rectification
	 * the point can be found on the same row in both images.
	 */
	@Test
	public void checkRectification() {
		// point being viewed
		Point3D_F64 X = new Point3D_F64(-0.01,0.1,3);

		StereoParameters param = createStereoParam(width,height);

		// create input images by rendering the point in both
		GrayU8 left = new GrayU8(width,height);
		GrayU8 right = new GrayU8(width,height);

		// compute the view in pixels of the point in the left and right cameras
		Point2D_F64 lensLeft = new Point2D_F64();
		Point2D_F64 lensRight = new Point2D_F64();
		SfmTestHelper.renderPointPixel(param,X,lensLeft,lensRight);

		// render the pixel in the image
		left.set((int)lensLeft.x,(int)lensLeft.y,200);
		right.set((int)lensRight.x,(int)lensRight.y,200);

		// test the algorithm
		StereoProcessingBase<GrayU8> alg = new StereoProcessingBase<>(GrayU8.class);
		alg.setCalibration(param);

		alg.setImages(left,right);
		alg.initialize();

		// Test tolerances are set to one pixel due to discretization errors in the image
		// sanity check test
		assertFalse(Math.abs(lensLeft.y - lensRight.y) <= 1);

		// check properties of a rectified image and stereo pairs
		Point2D_F64 foundLeft = centroid(alg.getImageLeftRect());
		Point2D_F64 foundRight = centroid(alg.getImageRightRect());

		assertTrue(Math.abs(foundLeft.y - foundRight.y) <= 1);
		assertTrue(foundRight.x < foundLeft.x);
	}

	@Test
	public void compute3D() {
		// point being viewed
		Point3D_F64 X = new Point3D_F64(-0.01,0.1,3);

		StereoParameters param = createStereoParam(width,height);

		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(param.left,null);

		// compute the view in pixels of the point in the left and right cameras
		Point2D_F64 lensLeft = new Point2D_F64();
		Point2D_F64 lensRight = new Point2D_F64();
		SfmTestHelper.renderPointPixel(param,X,lensLeft,lensRight);

		StereoProcessingBase<GrayU8> alg = new StereoProcessingBase<>(GrayU8.class);
		alg.setCalibration(param);

		// Rectify the points
		Point2Transform2_F64 rectLeft = RectifyImageOps.transformPixelToRect_F64(param.left,alg.getRect1());
		Point2Transform2_F64 rectRight = RectifyImageOps.transformPixelToRect_F64(param.right,alg.getRect2());

		Point2D_F64 l = new Point2D_F64();
		Point2D_F64 r = new Point2D_F64();

		rectLeft.compute(lensLeft.x,lensLeft.y,l);
		rectRight.compute(lensRight.x,lensRight.y,r);

		// make sure I rectified it correctly
		assertEquals(l.y,r.y,1);

		// find point in homogeneous coordinates
		Point3D_F64 found = new Point3D_F64();
		alg.computeHomo3D(l.x, l.y, found);

		// disparity between the two images
		double disparity = l.x - r.x;

		found.x /= disparity;
		found.y /= disparity;
		found.z /= disparity;

		assertTrue(found.isIdentical(X,0.01));
	}

	/**
	 * Finds the mean point in the image weighted by pixel intensity
	 */
	public static Point2D_F64 centroid( GrayU8 image ) {
		double meanX = 0;
		double meanY = 0;
		double totalPixel = ImageStatistics.sum(image);

		for( int i = 0; i < image.height; i++ ) {
			for( int j = 0; j < image.width; j++ ) {
				meanX += image.get(j,i)*j;
				meanY += image.get(j,i)*i;
			}
		}

		meanX /= totalPixel;
		meanY /= totalPixel;

		return new Point2D_F64(meanX,meanY);
	}

	public static StereoParameters createStereoParam( int width , int height ) {
		StereoParameters ret = new StereoParameters();

		ret.setRightToLeft(new Se3_F64());
		ret.getRightToLeft().getT().set(-0.2, 0.001, -0.012);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.001, -0.01, 0.0023, ret.getRightToLeft().getR());

		ret.left = new CameraPinholeRadial().fsetK(300, 320, 0, width / 2, height / 2, width, height).fsetRadial(0.1,1e-4);
		ret.right = new CameraPinholeRadial().fsetK(290, 310, 0, width / 2 + 2, height / 2 - 6, width, height).fsetRadial(0.05, -2e-4);

		return ret;
	}

}
