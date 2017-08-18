/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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
import boofcv.testing.BoofTesting;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Distance2D_F64;
import georegression.struct.ConvertFloatType;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import georegression.transform.ConvertTransform_F64;
import org.junit.Test;

import static java.lang.Math.max;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class TestRefinePolygonToGrayLine extends CommonFitPolygonChecks {

	/**
	 * Give it a shape which is too small and see if it fails
	 */
	@Test
	public void fit_tooSmall() {
		final boolean black = true;

		Polygon2D_F64 input = new Polygon2D_F64(5,5, 5,6, 6,6, 6,5);
		rectangles.add(new Rectangle2D_I32(x0,y0,x1,y1));

		for (Class imageType : imageTypes) {
			renderDistortedRectangles(black,imageType);

			RefinePolygonToGrayLine alg = createAlg(input.size(),imageType);

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

		rectangles.add(new Rectangle2D_I32(x0,y0,x1,y1));
		Polygon2D_F64 input = new Polygon2D_F64(x0,y0 , x0,y1, x1,y1, x1,y0);

		for (Class imageType : imageTypes) {
			renderDistortedRectangles(black,imageType);

			RefinePolygonToGrayLine alg = createAlg(input.size(),imageType);

			Polygon2D_F64 output = new Polygon2D_F64(4);
			alg.setImage(image);
			assertTrue(alg.refine(input,output));

			// do it again with a sub-image
			Polygon2D_F64 output2 = new Polygon2D_F64(4);
			image = BoofTesting.createSubImageOf_S(image);
			alg.setImage(image);
			assertTrue(alg.refine(input,output2));

			assertTrue(UtilPolygons2D_F64.isIdentical(output, output2, 1e-8));
		}
	}

	/**
	 * Fit a square which is alligned to the image axis.  It should get a nearly perfect fit.
	 * Initial conditions are be a bit off.
	 */
	@Test
	public void alignedSquare() {
		rectangles.add(new Rectangle2D_I32(x0,y0,x1,y1));
		Polygon2D_F64 original = new Polygon2D_F64(x0,y0 , x0,y1, x1,y1, x1,y0);

		for (Class imageType : imageTypes) {
			for (int i = 0; i < 2; i++) {
				boolean black = i == 0;

				renderDistortedRectangles(black,imageType);

				RefinePolygonToGrayLine alg = createAlg(original.size(),imageType);

				for (int j = 0; j < 20; j++) {
					Polygon2D_F64 input = original.copy();
					addNoise(input,2);

					Polygon2D_F64 output = new Polygon2D_F64(original.size());
					alg.setImage(image);
					assertTrue(alg.refine(input,output));

					assertTrue(original.isIdentical(output, 0.01));
				}
			}
		}
	}

	/**
	 * See if it handles lines along the image border correctly
	 */
	@Test
	public void fitWithEdgeOnBorder() {
		x0 = 0; x1 = 100;
		y0 = 100; y1 = 200;
		rectangles.add(new Rectangle2D_I32(x0,y0,x1,y1));

		for (Class imageType : imageTypes) {

			renderDistortedRectangles(true,imageType);

			RefinePolygonToGrayLine alg = createAlg(4,imageType);

			Polygon2D_F64 input = createFromSquare(null);

			input.get(0).set(x0,y0+1.1);
			input.get(1).set(x0,y1-1.1);

			Polygon2D_F64 found = new Polygon2D_F64(4);

			alg.setImage(image);
			assertTrue(alg.refine(input,found));

			Polygon2D_F64 expected = createFromSquare(null);
			assertTrue(expected.isIdentical(found, 0.01));
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

		rectangles.add(new Rectangle2D_I32(x0,y0,x1,y1));

		for (Class imageType : imageTypes) {
			for (Affine2D_F64 a : affines) {
//				System.out.println(imageType+"  "+a);
				fit_perfect_affine(true, a, imageType);
				fit_perfect_affine(false, a, imageType);
			}
		}
	}

	public void fit_perfect_affine(boolean black, Affine2D_F64 affine, Class imageType) {
		this.transform.set(affine);
		renderDistortedRectangles(black, imageType);

		RefinePolygonToGrayLine alg = createAlg(4,imageType);

		Polygon2D_F64 input = createFromSquare(affine);

		Polygon2D_F64 expected = input.copy();
		Polygon2D_F64 found = new Polygon2D_F64(4);

		alg.setImage(image);
		assertTrue(alg.refine(input,found));

		// input shouldn't be modified
		assertTrue(expected.isIdentical(input,0));
		// should be close to the expected
		assertTrue(expected.isIdentical(found,0.27));

		// do it again with a sub-image to see if it handles that
		image = BoofTesting.createSubImageOf_S(image);
		alg.setImage(image);
		assertTrue(alg.refine(input,found));
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
		rectangles.add(new Rectangle2D_I32(x0,y0,x1,y1));

		for (Class imageType : imageTypes) {
			for (Affine2D_F64 a : affines) {
//				System.out.println(imageType+"  "+a);
				fit_perfect_transform(true, a, imageType);
				fit_perfect_transform(false, a, imageType);
			}
		}
	}

	public void fit_perfect_transform(boolean black, Affine2D_F64 regToDist, Class imageType) {
		this.transform.set(regToDist);
		renderDistortedRectangles(black, imageType);

		RefinePolygonToGrayLine alg = createAlg(4,imageType);

		Polygon2D_F64 input = createFromSquare(null);

		Polygon2D_F64 expected = input.copy();
		Polygon2D_F64 found = new Polygon2D_F64(4);

		alg.setImage(image);
		// fail without the transform
		assertFalse(alg.refine(input,found));

		// work when the transform is applied
		PixelTransformAffine_F32 transform = new PixelTransformAffine_F32();
		Affine2D_F32 regToDist_F32 = new Affine2D_F32();
		ConvertFloatType.convert(regToDist, regToDist_F32);
		transform.set(regToDist_F32);
		alg.setTransform(transform);
		alg.setImage(image);
		assertTrue(alg.refine(input,found));

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
		rectangles.add(new Rectangle2D_I32(x0,y0,x1,y1));

		for (Class imageType : imageTypes) {
			for (Affine2D_F64 a : affines) {
//				System.out.println(imageType+"  "+a);
				fit_noisy_affine(true, a, imageType);
				fit_noisy_affine(false, a, imageType);
			}
		}
	}

	public void fit_noisy_affine(boolean black, Affine2D_F64 affine, Class imageType) {
		this.transform.set(affine);
		renderDistortedRectangles(black, imageType);

		RefinePolygonToGrayLine alg = createAlg(4,imageType);

		Polygon2D_F64 input = createFromSquare(affine);

		Polygon2D_F64 expected = input.copy();
		Polygon2D_F64 found = new Polygon2D_F64(4);

		for (int i = 0; i < 10; i++) {
			// add some noise
			input.set(expected);
			addNoise(input, 2);

			alg.setImage(image);
			assertTrue(alg.refine(input,found));

			// should be close to the expected
			double before = computeMaxDistance(input,expected);
			double after = computeMaxDistance(found,expected);

			assertTrue(after < before);
			assertTrue(expected.isIdentical(found, 0.5));

			//----- Reverse the order and it should still work
			input.flip();
			assertTrue(alg.refine(input,found));
			found.flip();
			after = computeMaxDistance(found, expected);

			assertTrue(after<before);
			assertTrue(expected.isIdentical(found, 0.5));
		}
	}

	private RefinePolygonToGrayLine createAlg(int numSides , Class imageType) {
		return new RefinePolygonToGrayLine(numSides,imageType);
	}

	/**
	 * Optimize a line with a perfect initial guess
	 */
	@Test
	public void optimize_line_perfect() {
		rectangles.add(new Rectangle2D_I32(x0,y0,x1,y1));
		for (Class imageType : imageTypes) {
			optimize_line_perfect(true, imageType);
			optimize_line_perfect(false, imageType);
		}
	}

	public void optimize_line_perfect(boolean black, Class imageType) {
		renderDistortedRectangles(black, imageType);

		RefinePolygonToGrayLine alg = createAlg(4,imageType);

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

}
