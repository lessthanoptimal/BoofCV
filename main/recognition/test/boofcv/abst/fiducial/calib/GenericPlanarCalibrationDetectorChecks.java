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

package boofcv.abst.fiducial.calib;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.distort.PointTransformHomography_F64;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public abstract class GenericPlanarCalibrationDetectorChecks {

	int width = 300,height= 300;

	GrayF32 original;
	GrayF32 distorted;
	List<CalibrationObservation> solutions = new ArrayList<>();

	Point2Transform2_F32 d2o;
	Point2Transform2_F64 o2d;

	public abstract void renderTarget(GrayF32 original , List<CalibrationObservation> solutions );

	public abstract DetectorFiducialCalibration createDetector();

	@Before
	public void setup() {
		original = new GrayF32(width,height);
		distorted = new GrayF32(width, height);
		renderTarget(original, solutions);
	}


	/**
	 * Nothing was detected.  make sure it doesn't return null.
	 */
	@Test
	public void checkDetectionsNotNull() {
		DetectorFiducialCalibration detector = createDetector();

		detector.process(original.createSameShape());

		assertTrue( detector.getDetectedPoints() != null );
		assertTrue( detector.getDetectedPoints().size() == 0 );
	}

	/**
	 * First call something was detected, second call nothing was detected.  it should return an empty list
	 */
	@Test
	public void checkDetectionsResetOnFailure() {
		DetectorFiducialCalibration detector = createDetector();

		detector.process(original);
		assertTrue( detector.getDetectedPoints().size() > 0 );

		detector.process(original.createSameShape());

		assertTrue( detector.getDetectedPoints() != null );
		assertTrue( detector.getDetectedPoints().size() == 0 );
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

		assertEquals(Math.abs(minX), Math.abs(maxX), 1e-8);
		assertEquals(Math.abs(minY), Math.abs(maxY), 1e-8);
	}

	/**
	 * Make sure new instances of calibration points are returned each time
	 */
	@Test
	public void dataNotRecycled() {
		DetectorFiducialCalibration detector = createDetector();

		assertTrue(detector.process(original));
		CalibrationObservation found0 = detector.getDetectedPoints();

		assertTrue(detector.process(original));
		CalibrationObservation found1 = detector.getDetectedPoints();

		assertEquals(found0.size(),found1.size());
		assertTrue(found0 != found1);
		for (int i = 0; i < found0.size(); i++) {
			assertFalse(found1.points.contains(found0.points.get(0)));
		}
	}

	/**
	 * Easy case with no distortion
	 */
	@Test
	public void undistorted() {
		DetectorFiducialCalibration detector = createDetector();

//		display(original);

		assertTrue(detector.process(original));

		CalibrationObservation found = detector.getDetectedPoints();

		checkList(found, false);
	}

	/**
	 * Pinch it a little bit like what is found with perspective distortion
	 */
	@Test
	public void distorted() {
		DetectorFiducialCalibration detector = createDetector();

		createTransform(width / 5, height / 5, width * 4 / 5, height / 6, width - 1, height - 1, 0, height - 1);

		PixelTransform2_F32 pixelTransform = new PointToPixelTransform_F32(d2o);

		ImageMiscOps.fill(distorted, 0xff);
		DistortImageOps.distortSingle(original, distorted, pixelTransform,
				InterpolationType.BILINEAR, BorderType.EXTENDED);

//		display(distorted);

		assertTrue(detector.process(distorted));

		CalibrationObservation found = detector.getDetectedPoints();
		checkList(found, true);
	}

	private void display( GrayF32 image ) {
		BufferedImage visualized = ConvertBufferedImage.convertTo(image, null, true);
		ShowImages.showWindow(visualized, "Input");

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void checkList(CalibrationObservation found , boolean applyTransform ) {
		List<CalibrationObservation> expectedList = new ArrayList<>();

		if( !applyTransform ) {
			expectedList.addAll(this.solutions);
		} else {
			for (int i = 0; i < solutions.size(); i++) {
				CalibrationObservation orig = solutions.get(i);
				CalibrationObservation mod = new CalibrationObservation();
				for (int j = 0; j < orig.size(); j++) {
					Point2D_F64 p = orig.points.get(i).copy();

					o2d.compute(p.x, p.y, p);
					mod.add(p, orig.get(j).index );
				}
				expectedList.add(mod);
			}
		}

		assertEquals(expectedList.get(0).size(),found.size());

		// the order is important.  check to see that they are close and in the correct order
		boolean anyMatched = false;
		for (int i = 0; i < expectedList.size(); i++) {
			CalibrationObservation expected = expectedList.get(i);
			boolean matched = true;

			for (int j = 0; j < expected.size(); j++) {
				if( found.get(j).index != expected.get(j).index ) {
					matched = false;
					break;
				}

				Point2D_F64 f = found.get(i);
				Point2D_F64 e = expected.get(i);
				if( f.distance(e) >= 3 ) {
					matched = false;
					break;
				}
			}
			if( matched ) {
				anyMatched = true;
				break;
			}
		}
		assertTrue(anyMatched);
	}

	public void createTransform( double x0 , double y0 , double x1 , double y1 ,
								 double x2 , double y2 , double x3 , double y3 )
	{
		// Homography estimation algorithm.  Requires a minimum of 4 points
		Estimate1ofEpipolar computeHomography = FactoryMultiView.computeHomography(true);

		// Specify the pixel coordinates from destination to target
		ArrayList<AssociatedPair> associatedPairs = new ArrayList<>();
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

	/**
	 * Observations points should always be in increasing order
	 */
	@Test
	public void checkPointIndexIncreasingOrder() {
		DetectorFiducialCalibration detector = createDetector();

		assertTrue(detector.process(original));
		CalibrationObservation found = detector.getDetectedPoints();

		assertEquals(detector.getLayout().size(),found.size());

		for (int i = 0; i < found.size(); i++) {
			assertEquals(i, found.get(i).index);
		}
	}
}
