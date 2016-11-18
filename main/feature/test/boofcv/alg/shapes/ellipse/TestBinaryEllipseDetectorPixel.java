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

import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.shapes.TestShapeFittingOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import georegression.metric.UtilAngle;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.EllipseRotated_F64;
import org.ddogleg.struct.FastQueue;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
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
	public void basicOnImage() {

		List<EllipseRotated_F64> expected = new ArrayList<>();
		expected.add( new EllipseRotated_F64(30,38,10,8,0));
		expected.add( new EllipseRotated_F64(115,80,20,15,Math.PI/2.0));

		GrayU8 input = renderEllipses(200,300,expected, 0);
		GrayU8 binary = input.createSameShape();
		ThresholdImageOps.threshold(input,binary,100,true);

		// detect ovals in binary image
		BinaryEllipseDetectorPixel alg = new BinaryEllipseDetectorPixel();

		alg.process(binary);

		// compare against expected results
		List<BinaryEllipseDetectorPixel.Found> found = alg.getFound();

		List<EllipseRotated_F64> foundEllipses = new ArrayList<>();

		for( BinaryEllipseDetectorPixel.Found f : found ) {
			assertTrue( f.contour.size() > 10);
			foundEllipses.add(f.ellipse);
		}

		checkEquals(expected,foundEllipses,1.0,0.1);
	}

	/**
	 * Undistort the image when no distoriton is provided
	 */
	@Test
	public void undistortContour() {
		List<Point2D_I32> input = new ArrayList<>();
		FastQueue<Point2D_F64> output = new FastQueue<>(Point2D_F64.class, true);

		for (int i = 0; i < 10; i++) {
			input.add( new Point2D_I32(i,i));
		}

		BinaryEllipseDetectorPixel alg = new BinaryEllipseDetectorPixel();

		alg.undistortContour(input,output);

		assertEquals(input.size(),output.size);
		for (int i = 0; i < input.size(); i++) {
			Point2D_I32 p = input.get(i);
			assertEquals(p.x,output.get(i).x,1e-8);
			assertEquals(p.y,output.get(i).y,1e-8);
		}
	}

	/**
	 * Undistort the image when distortion model is provided
	 */
	@Test
	public void undistortContour_WithDistortion() {


		List<Point2D_I32> input = new ArrayList<>();
		FastQueue<Point2D_F64> output = new FastQueue<>(Point2D_F64.class, true);

		for (int i = 0; i < 10; i++) {
			input.add( new Point2D_I32(i,i));
		}

		BinaryEllipseDetectorPixel alg = new BinaryEllipseDetectorPixel();
		alg.setLensDistortion(new PixelTransformAffine_F32(new Affine2D_F32(1,0,0,1,10.0f,0)));

		alg.undistortContour(input,output);

		assertEquals(input.size(),output.size);
		for (int i = 0; i < input.size(); i++) {
			Point2D_I32 p = input.get(i);
			assertEquals(p.x+10,output.get(i).x,1e-8);
			assertEquals(p.y,output.get(i).y,1e-8);
		}
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

	public static void checkEquals( List<EllipseRotated_F64> expected ,
									List<EllipseRotated_F64> found , double tol , double tolPhi )
	{
		assertEquals(expected.size(),found.size());

		boolean matched[] = new boolean[expected.size()];

		for( EllipseRotated_F64 f : found ) {
			boolean foundMatch = false;
			for (int i = 0; i < expected.size(); i++) {
				EllipseRotated_F64 e = expected.get(i);

				if( Math.abs(f.a-e.a) <= tol && Math.abs(f.b-e.b) <= tol) {
					boolean angleMatch = true;
					// if it's a circle ignore the angle
					if( Math.abs(e.a - e.b)/Math.max(e.a,e.b) > 0.01 ) {
						angleMatch = UtilAngle.distHalf(f.phi,e.phi) <= tolPhi;
					}
					if( angleMatch && e.center.distance(f.center) <= tol ) {
						foundMatch = matched[i] = true;
					}
				}

			}
			if( !foundMatch ) {
				System.out.println("Found");
				System.out.println(f);
				System.out.println("\nExpected");
				for (int i = 0; i < expected.size(); i++) {
					System.out.println(expected.get(i));
				}
			}
			assertTrue(foundMatch);
		}

		for (int i = 0; i < matched.length; i++) {
			assertTrue(matched[i]);
		}
	}

	public static void checkEquals( EllipseRotated_F64 expected ,
									EllipseRotated_F64 found , double tol , double tolPhi ) {

		assertEquals(expected.a , found.a, tol);
		assertEquals(expected.b , found.b, tol);
		assertEquals(expected.center.x , found.center.x, tol);
		assertEquals(expected.center.y , found.center.y, tol);
		assertTrue(UtilAngle.dist(found.phi, expected.phi) <= tolPhi);
	}

	public static GrayU8 renderEllipses(int width, int height, List<EllipseRotated_F64> ellipses, int color)
	{
		// render a binary image with two ovals
		BufferedImage buffered = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = buffered.createGraphics();

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(Color.WHITE);
		g2.fillRect(0,0,width,height);

		g2.setColor(new Color(color,color,color));
		for( EllipseRotated_F64 ellipse : ellipses ) {
			AffineTransform tx = new AffineTransform();
			tx.concatenate( AffineTransform.getTranslateInstance(ellipse.center.x,ellipse.center.y));
			tx.concatenate( AffineTransform.getRotateInstance(ellipse.phi));

			int a = (int)Math.round(ellipse.a);
			int b = (int)Math.round(ellipse.b);

			g2.setTransform(tx);
			g2.fillOval(-a,-b,a*2,b*2);
		}

//		ShowImages.showDialog(buffered);

		GrayU8 input = new GrayU8(width,height);
		ConvertBufferedImage.convertFrom(buffered,input);

		return input;
	}
}
