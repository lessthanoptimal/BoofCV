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

package boofcv.alg.shapes.corner;

import boofcv.abst.distort.FDistort;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.misc.CircularIndex;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilLine2D_F64;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.transform.ConvertTransform_F64;
import georegression.transform.affine.AffinePointOps_F64;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRefineCornerLinesToImage {

	Random rand = new Random(234);

	int width = 400, height = 500;
	ImageGray work; // original image before affine has been applied
	ImageGray image; // image after affine applied

	int x0 = 200, y0 = 160;
	int x1 = 260, y1 = 400; // that's exclusive

	int white = 200;

	Class imageTypes[] = new Class[]{GrayU8.class,GrayF32.class};

	/**
	 * The shape it is fit to is a rectangle that's aligned to the image axis.  No interpolation and a very
	 * high accuracy shape should be found.
	 */
	@Test
	public void perfectRectangle() {
		for( Class imageType : imageTypes ) {
			for (int i = 0; i < 2; i++) {
				perfectRectangle(i == 0, imageType);
			}
		}
	}

	public void perfectRectangle(boolean black, Class imageType) {
		setup(null,black,imageType);

		RefineCornerLinesToImage alg = new RefineCornerLinesToImage(imageType);
		alg.setImage(image);

		optimizedCorner(alg,x0,y0,x0,y1,x1,y0);
		optimizedCorner(alg,x0,y1,x1,y1,x0,y0);
		optimizedCorner(alg,x1,y1,x1,y0,x0,y1);
		optimizedCorner(alg,x1,y0,x0,y0,x1,y1);
	}

	private void optimizedCorner( RefineCornerLinesToImage alg,
								  int x0 , int y0 , int x1 , int y1 , int x2 , int y2 )
	{
		double tol = 1e-5;
		int r = 2;

		// give it initial conditions which are offset from the truth
		for (int y = -r; y <= r; y++) {
			for (int x = -r; x <= r; x++) {
				Point2D_F64 c = new Point2D_F64(x0+x,y0+y);
				Point2D_F64 endL = new Point2D_F64(x1,y1);
				Point2D_F64 endR = new Point2D_F64(x2,y2);

				assertTrue(alg.refine(c, endL, endR));

				Point2D_F64 refined = alg.getRefinedCorner();
				assertEquals(x0,refined.x,tol);
				assertEquals(y0,refined.y,tol);
			}
		}
	}

	/**
	 * Apply different types of distortion to the rectangle and give it a noisy initial estimate
	 */
	@Test
	public void noisyDistorted() {
		// distorted and undistorted views
		Affine2D_F64 affines[] = new Affine2D_F64[3];
		affines[0] = new Affine2D_F64();
		affines[1] = new Affine2D_F64();
		affines[2] = new Affine2D_F64(1.3,0.05,-0.15,0.87,0.1,0.6);
		ConvertTransform_F64.convert(new Se2_F64(0, 0, 0.2), affines[0]);
		ConvertTransform_F64.convert(new Se2_F64(0, 0, 0.4), affines[1]);

		for (Class imageType : imageTypes) {
			for (Affine2D_F64 a : affines) {
//				System.out.println(imageType+"  "+a);
				fit_noisy_affine(true, a, imageType);
				fit_noisy_affine(false, a, imageType);
			}
		}
	}

	public void fit_noisy_affine(boolean black, Affine2D_F64 affine, Class imageType) {
		setup(affine, black, imageType);

		double tol = 0.9;

		RefineCornerLinesToImage alg = new RefineCornerLinesToImage(imageType);

		Polygon2D_F64 input = new Polygon2D_F64(4);
		AffinePointOps_F64.transform(affine,new Point2D_F64(x0,y0),input.get(0));
		AffinePointOps_F64.transform(affine,new Point2D_F64(x0,y1),input.get(1));
		AffinePointOps_F64.transform(affine,new Point2D_F64(x1,y1),input.get(2));
		AffinePointOps_F64.transform(affine,new Point2D_F64(x1,y0),input.get(3));

		Polygon2D_F64 expected = input.copy();

		for (int i = 0; i < 10; i++) {
			// add some noise
			input.set(expected);
			addNoise(input, 2);

			alg.setImage(image);

			for (int j = 0; j < 4; j++) {
				int left = (j+1)%4;
				int right = CircularIndex.minusPOffset(j, 1, 4);

				assertTrue(alg.refine(input.get(j), input.get(left), input.get(right)));

				Point2D_F64 refined = alg.getRefinedCorner();
				Point2D_F64 e = expected.get(j);
				double difference = refined.distance(e);
				assertTrue(i+" "+j+" "+difference,difference <= tol );

			}
		}
	}

	private void addNoise(Polygon2D_F64 input, double spread) {
		for (int i = 0; i < input.size(); i++) {
			Point2D_F64 v = input.get(i);
			v.x += rand.nextDouble()*spread - spread/2.0;
			v.y += rand.nextDouble()*spread - spread/2.0;
		}
	}

	/**
	 * Basic optimization test.  Give it a perfect initial guess and see generates the
	 * expected solution\
	 */
	@Test
	public void optimize() {
		for( Class imageType : imageTypes ) {
			for (int i = 0; i < 2; i++) {
				optimize(i==0,imageType);
			}
		}
	}

	public void optimize(boolean black , Class imageType) {
		setup(null, black, imageType);

		RefineCornerLinesToImage alg = new RefineCornerLinesToImage(imageType);
		alg.setImage(image);

		LineGeneral2D_F64 found = new LineGeneral2D_F64();

		LineSegment2D_F64 line0 = new LineSegment2D_F64(x0,y0,x1,y0);
		LineSegment2D_F64 line1 = new LineSegment2D_F64(x0,y0,x0,y1);

		assertTrue(alg.optimize(line0.a,line0.b, found));
		checkSolution(line0,found);

		assertTrue(alg.optimize(line1.a,line1.b, found));
		checkSolution(line1,found);

	}

	private void checkSolution( LineSegment2D_F64 expectedLS , LineGeneral2D_F64 found ) {
		LineGeneral2D_F64 expected = new LineGeneral2D_F64();
		UtilLine2D_F64.convert(expectedLS,expected);

		expected.normalize();
		found.normalize();

		if( Math.signum(expected.C) != Math.signum(found.C) ) {
			expected.A *= -1;
			expected.B *= -1;
			expected.C *= -1;
		}

		assertEquals(expected.A,found.A,1e-8);
		assertEquals(expected.B,found.B,1e-8);
		assertEquals(expected.C,found.C,1e-8);
	}

	private void setup( Affine2D_F64 affine, boolean black , Class imageType ) {
		work = GeneralizedImageOps.createSingleBand(imageType, width, height);
		image = GeneralizedImageOps.createSingleBand(imageType,width,height);

		int bg = black ? white : 0;
		int fg = black ? 0 : white;
		GImageMiscOps.fill(work, bg);
		GImageMiscOps.fillRectangle(work, fg, x0, y0, x1 - x0, y1 - y0);

		if( affine != null ) {
			new FDistort(work, image).border(bg).affine(affine).apply();
		} else {
			image.setTo(work);
		}

//		BufferedImage out = ConvertBufferedImage.convertTo(image, null, true);
//		ShowImages.showWindow(out, "Rendered");
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}
}
