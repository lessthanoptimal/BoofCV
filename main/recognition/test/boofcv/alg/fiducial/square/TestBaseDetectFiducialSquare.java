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

package boofcv.alg.fiducial.square;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.distort.*;
import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestBaseDetectFiducialSquare {

	List<Point3D_F64> worldPts = new ArrayList<>();
	int width = 640;
	int height = 480;

	public TestBaseDetectFiducialSquare() {
		// corners of the fiducial in world coordinates
		double r = 2;
		worldPts.add(new Point3D_F64(-0.5*r, -0.5*r, 0));
		worldPts.add(new Point3D_F64(-0.5*r,  0.5*r, 0));
		worldPts.add(new Point3D_F64( 0.5*r,  0.5*r, 0));
		worldPts.add(new Point3D_F64( 0.5*r, -0.5*r, 0));
	}

	/**
	 * Basic test where a distorted pattern is places in the image and the found coordinates
	 * are compared against ground truth
	 */
	@Test
	public void findPatternEasy() {
		checkFindKnown(new CameraPinholeRadial(500,500,0,320,240,width,height),1.1);
	}

	private void checkFindKnown(CameraPinholeRadial intrinsic, double tol ) {
		GrayU8 pattern = createPattern(6*20, false);
		Quadrilateral_F64 where = new Quadrilateral_F64(50,50,  130,60,  140,150,  40,140);
//		Quadrilateral_F64 where = new Quadrilateral_F64(50,50,  100,50,  100,150,  50,150);

		GrayU8 image = new GrayU8(width,height);
		ImageMiscOps.fill(image, 255);
		render(pattern, where, image);

		Dummy dummy = new Dummy();
		dummy.configure(new LensDistortionRadialTangential(intrinsic),width,height,false);
		dummy.process(image);

		assertEquals(1,dummy.detected.size());

		Quadrilateral_F64 found = dummy.getFound().get(0).distortedPixels;
//		System.out.println("found "+found);
//		System.out.println("where "+where);
		checkMatch(where, found.a, tol);
		checkMatch(where, found.b, tol);
		checkMatch(where, found.c, tol);
		checkMatch(where, found.d, tol);

		// see if the undistorted image is as expected
		checkPattern( dummy.detected.get(0) );
	}

	private void checkMatch( Quadrilateral_F64 q , Point2D_F64 p , double tol ) {

		if( q.a.distance(p) <= tol)
			return;
		if( q.b.distance(p) <= tol)
			return;
		if( q.c.distance(p) <= tol)
			return;
		if( q.d.distance(p) <= tol)
			return;
		fail("no match "+p+"    "+q);
	}

	/**
	 * Ensure that lens distortion is being removed from the fiducial square.  All check to make sure the class
	 * updates everything when init is called again with a different model.
	 */
	@Test
	public void lensRemoval() {
		List<Point2D_F64> expected = new ArrayList<>();
		expected.add( new Point2D_F64(60,300+120));
		expected.add( new Point2D_F64(60,300));
		expected.add( new Point2D_F64(60+120,300));
		expected.add( new Point2D_F64(60+120,300+120));

		DetectCorner detector = new DetectCorner();

		CameraPinholeRadial intrinsic = new CameraPinholeRadial(500,500,0,320,240,width,height).fsetRadial(-0.1,-0.05);
		detectWithLensDistortion(expected, detector, intrinsic);

		intrinsic = new CameraPinholeRadial(500,500,0,320,240,width,height).fsetRadial(0.1,0.05);
		detectWithLensDistortion(expected, detector, intrinsic);
	}

	private void detectWithLensDistortion(List<Point2D_F64> expected, DetectCorner detector, CameraPinholeRadial intrinsic) {
		// create a pattern with a corner for orientation and put it into the image
		GrayU8 pattern = createPattern(6*20, true);
		GrayU8 image = new GrayU8(width,height);
		ImageMiscOps.fill(image, 255);
		image.subimage(60, 300, 60 + pattern.width, 300 + pattern.height, null).setTo(pattern);
		// place the pattern right next to one of the corners to maximize distortion

		// add lens distortion
		Point2Transform2_F32 distToUndistort = LensDistortionOps.transformPoint(intrinsic).undistort_F32(true, true);
		Point2Transform2_F64 undistTodist = LensDistortionOps.transformPoint(intrinsic).distort_F64(true, true);
		InterpolatePixelS interp = FactoryInterpolation.createPixelS(0, 255,
				InterpolationType.BILINEAR, BorderType.ZERO, GrayU8.class);
		ImageDistort<GrayU8,GrayU8> distorter = FactoryDistort.distortSB(false, interp, GrayU8.class);
		distorter.setModel(new PointToPixelTransform_F32(distToUndistort));
		GrayU8 distorted = new GrayU8(width,height);
		distorter.apply(image, distorted);

		detector.configure(new LensDistortionRadialTangential(intrinsic),width,height, false);
		detector.process(distorted);

		assertEquals(1, detector.getFound().size());
		FoundFiducial ff = detector.getFound().get(0);

		// see if the returned corners
		Point2D_F64 expectedDist = new Point2D_F64();
		for (int j = 0; j < 4; j++) {
			Point2D_F64 f = ff.distortedPixels.get(j);
			Point2D_F64 e = expected.get((j + 1) % 4);
			undistTodist.compute(e.x, e.y, expectedDist);
			assertTrue(f.distance(expectedDist) <= 0.4 );
		}

		// The check to see if square is correctly undistorted is inside the processing function itself
	}

	@Test
	public void computeFractionBoundary() {
		Dummy alg = new Dummy();

		alg.borderWidthFraction = 0.25;
		alg.square.reshape(100, 100);
		GImageMiscOps.fillRectangle(alg.square,200,25,25,50,50);
		double found = alg.computeFractionBoundary(100);
		assertEquals(1.0, found, 1e-8);

		GImageMiscOps.fillRectangle(alg.square,200,0,0,100,50);
		found = alg.computeFractionBoundary(100);
		assertEquals(0.5, found, 1e-8);
	}

	/**
	 * See if it can handle the situations where intrinsic camera parameters was not set.  Should just
	 * detect its location and
	 */
	@Test
	public void intrinsicNotSet() {
		List<Point2D_F64> expected = new ArrayList<>();
		expected.add( new Point2D_F64(200,300+120));
		expected.add( new Point2D_F64(200,300));
		expected.add( new Point2D_F64(200+120,300));
		expected.add( new Point2D_F64(200+120,300+120));

		// create a pattern with a corner for orientation and put it into the image
		GrayU8 pattern = createPattern(6*20, true);

		GrayU8 image = new GrayU8(width,height);

		for (int i = 0; i < 4; i++) {

			ImageMiscOps.fill(image, 255);

			image.subimage(200, 300, 200 + pattern.width, 300 + pattern.height, null).setTo(pattern);

			DetectCorner detector = new DetectCorner();

			detector.process(image);

			assertEquals(1, detector.getFound().size());
			FoundFiducial ff = detector.getFound().get(0);

			// make sure the returned quadrilateral makes sense
			for (int j = 0; j < 4; j++) {
				int index = j - i;
				if (index < 0) index = 4 + index;
				Point2D_F64 f = ff.distortedPixels.get(index);
				Point2D_F64 e = expected.get((j + 1) % 4);
				assertTrue(f.distance(e) <= 1e-8);
			}

			ImageMiscOps.rotateCW(pattern);
		}
	}

	/**
	 * Creates a square pattern image of the specified size
	 *
	 * @param squareLowLeft If true a square will be added to the image lower left
	 */
	public static GrayU8 createPattern(int length, boolean squareLowLeft) {
		GrayU8 pattern = new GrayU8( length , length );

		if( length%6 != 0 )
			throw new RuntimeException("Must be divisible by 6!");

		int b = length/6;

		for (int y = 0; y < length; y++) {
			for (int x = 0; x < length; x++) {
				int color = (x < b || y < b || x >= length-b || y >= length-b ) ? 0 : 255;
				pattern.set(x,y,color);
			}
		}

		if( squareLowLeft ) {
			for (int y = 0; y < 2*b; y++) {
				for (int x = 0; x < 2*b; x++) {
					pattern.set(x + b, y + 3*b, 0);
				}
			}
		}

		return pattern;
	}

	public static void checkPattern( GrayF32 image ) {

		int x0 = image.width/6;
		int y0 = image.height/6;
		int x1 = image.width-x0;
		int y1 = image.height-y0;

		double totalBorder = 0;
		int countBorder = 0;
		double totalInner = 0;
		int countInner = 0;

		// the border regions can be ambiguous so sum up around them
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				if( x < (x0-1) || x >= (x1+1) || y < (y0-1) || y >= (y1+1) ) {
					totalBorder += image.get(x,y);
					countBorder++;
				} else if( x >= (x0+1) && x < (x1-1) && y >= (y0+1) && y < (y1-1) ) {
					totalInner += image.get(x,y);
					countInner++;
				}
			}
		}

		totalBorder /= countBorder;
		totalInner /= countInner;

		assertTrue( totalBorder < 15 );
		assertTrue( totalInner > 245 );
	}

	/**
	 * Draws a distorted pattern onto the output
	 */
	public static void render(GrayU8 pattern , Quadrilateral_F64 where , GrayU8 output ) {

		int w = pattern.width;
		int h = pattern.height;

		ArrayList<AssociatedPair> associatedPairs = new ArrayList<>();
		associatedPairs.add(new AssociatedPair(where.a,new Point2D_F64(0,0)));
		associatedPairs.add(new AssociatedPair(where.b,new Point2D_F64(w,0)));
		associatedPairs.add(new AssociatedPair(where.c,new Point2D_F64(w,h)));
		associatedPairs.add(new AssociatedPair(where.d,new Point2D_F64(0,h)));

		Estimate1ofEpipolar computeHomography = FactoryMultiView.computeHomography(true);

		DenseMatrix64F H = new DenseMatrix64F(3,3);
		computeHomography.process(associatedPairs, H);

		// Create the transform for distorting the image
		PointTransformHomography_F32 homography = new PointTransformHomography_F32(H);
		PixelTransform2_F32 pixelTransform = new PointToPixelTransform_F32(homography);

		// Apply distortion and show the results
		DistortImageOps.distortSingle(pattern, output, pixelTransform, InterpolationType.BILINEAR, BorderType.SKIP);

//		ShowImages.showWindow(output, "Rendered");
//		try {Thread.sleep(10000);} catch (InterruptedException e) {}
	}

	public static class Dummy extends BaseDetectFiducialSquare<GrayU8> {

		public List<GrayF32> detected = new ArrayList<>();

		protected Dummy() {
			super(FactoryThresholdBinary.globalFixed(50,true,GrayU8.class),
					FactoryShapeDetector.polygon(new ConfigPolygonDetector(false, 4,4),GrayU8.class),0.25,0.65,100, GrayU8.class);
		}

		@Override
		public boolean processSquare(GrayF32 square, Result result, double a , double b) {
			detected.add(square.clone());
			return true;
		}
	}

	/**
	 * Accepts the pattern when it's in the lower left corner
	 */
	public static class DetectCorner extends BaseDetectFiducialSquare<GrayU8> {
		protected DetectCorner() {
			super(FactoryThresholdBinary.globalFixed(50, true, GrayU8.class),
					FactoryShapeDetector.polygon(new ConfigPolygonDetector(false, 4,4),GrayU8.class),0.25,0.65,100, GrayU8.class);
		}

		@Override
		public boolean processSquare(GrayF32 square, Result result, double a , double b) {

//			square.printInt();

			int w2 = square.width/2;
			int h2 = square.height/2;
			int w4 = square.width/4;
			int h4 = square.height/4;
			int w = square.width;
			int h = square.height;

			float sum[] = new float[4];
			sum[0] = ImageStatistics.sum(square.subimage(w4    ,h4    ,w2    ,h2   ,null));
			sum[1] = ImageStatistics.sum(square.subimage(w2    ,h4    ,w2+w4 ,h2   ,null));
			sum[2] = ImageStatistics.sum(square.subimage(w2    ,h2    ,w2+w4 ,h2+h4,null));
			sum[3] = ImageStatistics.sum(square.subimage(w4    ,h2    ,w2    ,h2+h4,null));

			int indexMin = -1;
			float min = Float.MAX_VALUE;
			for (int i = 0; i < 4; i++) {
				if( sum[i] < min ) {
					min = sum[i];
					indexMin = i;
				}
			}

			double minFrc = sum[indexMin] / sum[(indexMin+1)%4];

			// if lens distortion is not corrected for its about 0.05 and 0.077 for the two different distortions
			assertTrue(minFrc<0.035);

			result.lengthSide = 2.0;
			// number of image clockwise rotations to put min in lower-left corner
			result.rotation = 3-indexMin;
			return true;
		}
	}
}