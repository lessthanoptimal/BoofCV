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

package boofcv.alg.shapes.ellipse;

import boofcv.alg.shapes.TestShapeFittingOps;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestBinaryEllipseDetectorPixel {

	/**
	 * Test the whole pipeline with a rendered image
	 */
	@Test
	public void all() {
		fail("implement");
	}

	/**
	 * Undistort the image when no distoriton is provided
	 */
	@Test
	public void undistortContour() {
		fail("implement");
	}

	/**
	 * Undistort the image when distortion model is provided
	 */
	@Test
	public void undistortContour_WithDistortion() {
		fail("implement");
	}

	/**
	 * Test to see if it is approximately elliptical when the number of pixels is smaller
	 * than the threshold
	 */
	@Test
	public void isApproximatelyElliptical_small() {
		EllipseRotated_F64 ellipse = new EllipseRotated_F64(5,3,10,6,0);

		List<Point2D_F64> negative = TestShapeFittingOps.createRectangle_F64(20,10,60-4);
		List<Point2D_F64> positive = TestShapeFittingOps.createEllipse_F64(ellipse,60-4);

		BinaryEllipseDetectorPixel alg = new BinaryEllipseDetectorPixel();
		alg.setMaxDistanceFromEllipse(1.5);

		assertFalse(alg.isApproximatelyElliptical(ellipse,negative,100));
		assertTrue(alg.isApproximatelyElliptical(ellipse,positive,100));
	}

	/**
	 * Test to see if it is approximately elliptical when the number of pixels is larger
	 * than the threshold
	 */
	@Test
	public void isApproximatelyElliptical_large() {
		EllipseRotated_F64 ellipse = new EllipseRotated_F64(5,3,10,6,0);

		List<Point2D_F64> negative = TestShapeFittingOps.createRectangle_F64(20,10,60-4);
		List<Point2D_F64> positive = TestShapeFittingOps.createEllipse_F64(ellipse,60-4);

		BinaryEllipseDetectorPixel alg = new BinaryEllipseDetectorPixel();
		alg.setMaxDistanceFromEllipse(1.5);

		assertFalse(alg.isApproximatelyElliptical(ellipse,negative,20));
		assertTrue(alg.isApproximatelyElliptical(ellipse,positive,20));
	}


}
