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

package boofcv.abst.calib;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.distort.PointTransformHomography_F64;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericPlanarCalibrationDetectorChecks {

	int width = 300,height= 300;

	ImageFloat32 original = new ImageFloat32(width,height);
	ImageFloat32 distorted = new ImageFloat32(width, height);
	List<Point2D_F64> points = new ArrayList<Point2D_F64>();

	PointTransform_F32 d2o;
	PointTransform_F64 o2d;


	public abstract void renderTarget( ImageFloat32 original , List<Point2D_F64> points );

	public abstract PlanarCalibrationDetector createDetector();

	public GenericPlanarCalibrationDetectorChecks() {
		renderTarget(original,points);
	}

	/**
	 * Makes sure origin in the target's physical center.  This is done by seeing that most extreme
	 * points are all equally distant.  Can't use the mean since the target might not evenly distributed.
	 *
	 * Should this really be a requirement?  There is some mathematical justification for it and make sense
	 * when using it as a fiducial.
	 */
	@Test
	public void targetIsCentered() {
		List<Point2D_F64> layout = createDetector().getLayout();

		double minX=Double.MAX_VALUE,maxX=-Double.MAX_VALUE;
		double minY=Double.MAX_VALUE,maxY=-Double.MAX_VALUE;

		for( Point2D_F64 p : layout ) {
			if( p.x < minX )
				minX = p.x;
			if( p.x > maxX )
				maxX = p.x;
			if( p.y < minY )
				minY = p.y;
			if( p.y > maxY )
				maxY = p.y;
		}

		assertEquals(Math.abs(minX),Math.abs(maxX),1e-8);
		assertEquals(Math.abs(minY),Math.abs(maxY),1e-8);
	}

	/**
	 * Easy case with no distortion
	 */
	@Test
	public void undistorted() {
		PlanarCalibrationDetector detector = createDetector();

//		display(original);

		assertTrue(detector.process(original));

		List<Point2D_F64> found = detector.getDetectedPoints();

		checkList(found, false);
	}

	/**
	 * Pinch it a little bit like found with perspective distortion
	 */
	@Test
	public void distorted() {
		PlanarCalibrationDetector detector = createDetector();

		createTransform(width / 5, height / 5, width * 4 / 5, height / 6, width - 1, height - 1, 0, height - 1);

		PixelTransform_F32 pixelTransform = new PointToPixelTransform_F32(d2o);

		ImageMiscOps.fill(distorted, 0xff);
		DistortImageOps.distortSingle(original, distorted, pixelTransform, null, TypeInterpolate.BILINEAR);

//		display(distorted);

		assertTrue(detector.process(distorted));

		List<Point2D_F64> found = detector.getDetectedPoints();
		checkList(found, true);
	}

	private void display( ImageFloat32 image ) {
		BufferedImage visualized = ConvertBufferedImage.convertTo(image, null, true);
		ShowImages.showWindow(visualized, "Distorted");

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void checkList( List<Point2D_F64> found , boolean applyTransform ) {
		List<Point2D_F64> expected = new ArrayList<Point2D_F64>();

		if( !applyTransform ) {
			expected.addAll(this.points);
		} else {
			for (int i = 0; i < points.size(); i++) {
				Point2D_F64 p = points.get(i).copy();

				o2d.compute(p.x, p.y, p);
				expected.add(p);
			}
		}

		assertEquals(expected.size(),found.size());

		// the order is important.  check to see that they are close and in the correct order
		for (int i = 0; i < found.size(); i++) {
			Point2D_F64 f = found.get(i);
			Point2D_F64 e = expected.get(i);

			assertTrue(f.distance(e) < 3 );
		}
	}

	public void createTransform( double x0 , double y0 , double x1 , double y1 ,
								 double x2 , double y2 , double x3 , double y3 )
	{
		// Homography estimation algorithm.  Requires a minimum of 4 points
		Estimate1ofEpipolar computeHomography = FactoryMultiView.computeHomography(true);

		// Specify the pixel coordinates from destination to target
		ArrayList<AssociatedPair> associatedPairs = new ArrayList<AssociatedPair>();
		associatedPairs.add(new AssociatedPair(x0, y0, 0, 0));
		associatedPairs.add(new AssociatedPair(x1, y1, width-1, 0));
		associatedPairs.add(new AssociatedPair(x2, y2, width-1, height-1));
		associatedPairs.add(new AssociatedPair(x3, y3, 0, height - 1));

		// Compute the homography
		DenseMatrix64F H = new DenseMatrix64F(3, 3);
		computeHomography.process(associatedPairs, H);

		// Create the transform for distorting the image
		d2o = new PointTransformHomography_F32(H);
		CommonOps.invert(H);
		o2d = new PointTransformHomography_F64(H);
	}
}
