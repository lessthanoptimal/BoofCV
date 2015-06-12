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

package boofcv.alg.shapes.polygon;

import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Distance2D_F64;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.transform.ConvertTransform_F64;
import georegression.transform.affine.AffinePointOps_F64;
import org.junit.Test;

import static java.lang.Math.max;
import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class TestRefinePolygonLineToImage extends BaseFitPolygon{

	/**
	 * Give it a shape which is too small and see if it fails
	 */
	@Test
	public void fit_tooSmall() {
		final boolean black = true;

		Polygon2D_F64 input = new Polygon2D_F64(5,5, 5,6, 6,6, 6,5);

		for (Class imageType : imageTypes) {
			setup(new Affine2D_F64(), black, imageType);

			RefinePolygonLineToImage alg = createAlg(input.size(),black, imageType);

			Polygon2D_F64 output = new Polygon2D_F64(input.size());
			alg.setImage(image);
			assertFalse(alg.refine(input, output));
		}
	}

	/**
	 * Makes sure it can handle sub-images
	 */
	@Test
	public void fit_subimage() {
		final boolean black = true;

		Polygon2D_F64 input = new Polygon2D_F64(x0,y0 , x0,y1, x1,y1, x1,y0);

		for (Class imageType : imageTypes) {
			setup(new Affine2D_F64(), black, imageType);

			RefinePolygonLineToImage alg = createAlg(input.size(),black, imageType);

			Polygon2D_F64 output = new Polygon2D_F64(4);
			alg.setImage(image);
			assertTrue(alg.refine(input, output));

			// do it again with a sub-image
			Polygon2D_F64 output2 = new Polygon2D_F64(4);
			image = BoofTesting.createSubImageOf_S(image);
			alg.setImage(image);
			assertTrue(alg.refine(input, output2));

			assertTrue(UtilPolygons2D_F64.isIdentical(output, output2, 1e-8));
		}
	}

	/**
	 * Fit a square which is alligned to the image axis.  It should get a nearly perfect fit.
	 * Initial conditions are be a bit off.
	 */
	@Test
	public void alignedSquare() {
		Polygon2D_F64 original = new Polygon2D_F64(x0,y0 , x0,y1, x1,y1, x1,y0);

		for (Class imageType : imageTypes) {
			for (int i = 0; i < 2; i++) {
				boolean black = i == 0;

				setup(new Affine2D_F64(), black, imageType);

				RefinePolygonLineToImage alg = createAlg(original.size(),black, imageType);

				for (int j = 0; j < 20; j++) {
					Polygon2D_F64 input = original.copy();
					addNoise(input,2);

					Polygon2D_F64 output = new Polygon2D_F64(original.size());
					alg.setImage(image);
					assertTrue(alg.refine(input, output));

					assertTrue(original.isIdentical(output, 0.01));
				}
			}
		}
	}

	/**
	 * Perfect initial guess.
	 */
	@Test
	public void fit_perfect_affine() {
		// distorted and undistorted views
		Affine2D_F64 affines[] = new Affine2D_F64[2];
		affines[0] = new Affine2D_F64();
		affines[1] = new Affine2D_F64(1.3,0.05,-0.15,0.87,0.1,0.6);
		ConvertTransform_F64.convert(new Se2_F64(0,0,0.2),affines[0]);

		for (Class imageType : imageTypes) {
			for (Affine2D_F64 a : affines) {
//				System.out.println(imageType+"  "+a);
				fit_perfect_affine(true, a, imageType);
				fit_perfect_affine(false, a, imageType);
			}
		}
	}

	public void fit_perfect_affine(boolean black, Affine2D_F64 affine, Class imageType) {
		setup(affine, black, imageType);

		RefinePolygonLineToImage alg = createAlg(4,black, imageType);

		Polygon2D_F64 input = new Polygon2D_F64(4);
		AffinePointOps_F64.transform(affine,new Point2D_F64(x0,y0),input.get(0));
		AffinePointOps_F64.transform(affine,new Point2D_F64(x0,y1),input.get(1));
		AffinePointOps_F64.transform(affine,new Point2D_F64(x1,y1),input.get(2));
		AffinePointOps_F64.transform(affine,new Point2D_F64(x1,y0),input.get(3));

		Polygon2D_F64 expected = input.copy();
		Polygon2D_F64 found = new Polygon2D_F64(4);

		alg.setImage(image);
		assertTrue(alg.refine(input, found));

		// input shouldn't be modified
		assertTrue(expected.isIdentical(input,0));
		// should be close to the expected
		assertTrue(expected.isIdentical(found,0.27));

		// do it again with a sub-image to see if it handles that
		image = BoofTesting.createSubImageOf_S(image);
		alg.setImage(image);
		assertTrue(alg.refine(input, found));
		assertTrue(expected.isIdentical(input,0));
		assertTrue(expected.isIdentical(found,0.27));
	}

	/**
	 * Perfect initial guess. But provide a transform which will undo the affine transform
	 */
	@Test
	public void fit_perfect_transform() {
		// distorted and undistorted views
		Affine2D_F64 affines[] = new Affine2D_F64[1];
		affines[0] = new Affine2D_F64(0.8,0,0,0.8,0,0);

		for (Class imageType : imageTypes) {
			for (Affine2D_F64 a : affines) {
//				System.out.println(imageType+"  "+a);
				fit_perfect_transform(true, a, imageType);
				fit_perfect_transform(false, a, imageType);
			}
		}
	}

	public void fit_perfect_transform(boolean black, Affine2D_F64 regToDist, Class imageType) {
		setup(regToDist, black, imageType);

		RefinePolygonLineToImage alg = createAlg(4,black, imageType);

		Polygon2D_F64 input = new Polygon2D_F64(4);
		input.get(0).set(x0,y0);
		input.get(1).set(x0,y1);
		input.get(2).set(x1,y1);
		input.get(3).set(x1,y0);

		Polygon2D_F64 expected = input.copy();
		Polygon2D_F64 found = new Polygon2D_F64(4);

		alg.setImage(image);
		// fail without the transform
		assertFalse(alg.refine(input, found));

		// work when the transform is applied
		PixelTransformAffine_F32 transform = new PixelTransformAffine_F32();
		transform.set(regToDist);
		alg.getSnapToEdge().setTransform(image.width, image.height, transform);
		alg.setImage(image);
		assertTrue(alg.refine(input, found));

		// should be close to the expected
		assertTrue(expected.isIdentical(found,0.3));
	}

	/**
	 * Fit the quad with a noisy initial guess
	 */
	@Test
	public void fit_noisy_affine() {
		// distorted and undistorted views
		Affine2D_F64 affines[] = new Affine2D_F64[2];
		affines[0] = new Affine2D_F64();
		affines[1] = new Affine2D_F64(1.3,0.05,-0.15,0.87,0.1,0.6);
		ConvertTransform_F64.convert(new Se2_F64(0,0,0.2),affines[0]);

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

		RefinePolygonLineToImage alg = createAlg(4,black, imageType);

		Polygon2D_F64 input = new Polygon2D_F64(4);
		AffinePointOps_F64.transform(affine,new Point2D_F64(x0,y0),input.get(0));
		AffinePointOps_F64.transform(affine,new Point2D_F64(x0,y1),input.get(1));
		AffinePointOps_F64.transform(affine,new Point2D_F64(x1,y1),input.get(2));
		AffinePointOps_F64.transform(affine,new Point2D_F64(x1,y0),input.get(3));

		Polygon2D_F64 expected = input.copy();
		Polygon2D_F64 found = new Polygon2D_F64(4);

		for (int i = 0; i < 10; i++) {
			// add some noise
			input.set(expected);
			addNoise(input, 2);

			alg.setImage(image);
			assertTrue(alg.refine(input, found));

			// should be close to the expected
			double before = computeMaxDistance(input,expected);
			double after = computeMaxDistance(found,expected);

			assertTrue(after<before);
			assertTrue(expected.isIdentical(found,0.5));
		}
	}

	private void addNoise(Polygon2D_F64 input, double spread) {
		for (int i = 0; i < input.size(); i++) {
			Point2D_F64 v = input.get(i);
			v.x += rand.nextDouble()*spread - spread/2.0;
			v.y += rand.nextDouble()*spread - spread/2.0;
		}
	}

	private RefinePolygonLineToImage createAlg( int numSides , boolean black, Class imageType) {
		return new RefinePolygonLineToImage(numSides,black,imageType);
	}

	/**
	 * Optimize a line with a perfect initial guess
	 */
	@Test
	public void optimize_line_perfect() {
		for (Class imageType : imageTypes) {
			optimize_line_perfect(true, imageType);
			optimize_line_perfect(false, imageType);
		}
	}

	public void optimize_line_perfect(boolean black, Class imageType) {
		setup(null, black, imageType);

		RefinePolygonLineToImage alg = createAlg(4,black, imageType);

		Quadrilateral_F64 input = new Quadrilateral_F64(x0,y0,x0,y1,x1,y1,x1,y0);
		LineGeneral2D_F64 found = new LineGeneral2D_F64();

		alg.setImage(image);
		assertTrue(alg.optimize(input.a, input.b, found));

		assertTrue(Distance2D_F64.distance(found, input.a) <= 1e-4);
		assertTrue(Distance2D_F64.distance(found, input.b) <= 1e-4);
	}

	private double computeMaxDistance(Polygon2D_F64 expected, Polygon2D_F64 found ) {
		double a = 0;
		for (int i = 0; i < expected.size(); i++) {
			a = max(a,expected.get(i).distance(found.get(i)));
		}

		return a;
	}

	/**
	 * Try optimizing an edge which goes in the wrong direction.  should fail
	 */
	@Test
	public void optimize_line_wrongEdge() {
		final boolean black = true;

		for (Class imageType : imageTypes) {
			setup(null, black, imageType);

			RefinePolygonLineToImage alg = createAlg(4,black, imageType);

			Quadrilateral_F64 input = new Quadrilateral_F64(x0,y0,x0,y1,x1,y1,x1,y0);
			LineGeneral2D_F64 found = new LineGeneral2D_F64();

			alg.setImage(image);
			assertFalse(alg.optimize(input.b, input.a, found));
			// should be a to b
		}
	}


	/**
	 * Makes sure the transform doesn't extend points outside the original image
	 */
	@Test
	public void setTransform_input() {
		PointTransformHomography_F32 H = new PointTransformHomography_F32();
		PixelTransform_F32 transform = new PointToPixelTransform_F32(H);

		RefinePolygonLineToImage alg = createAlg(4,true, ImageUInt8.class);

		// correct example
		alg.getSnapToEdge().setTransform(20,30,transform);

		// bad example
		try {
			H.getModel().a11 = 2;
			alg.getSnapToEdge().setTransform(20, 30, transform);
			fail("Should have thrown exception");
		} catch( RuntimeException ignore ) {}
	}


}
