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

package boofcv.alg.shapes.corner;

import boofcv.abst.distort.FDistort;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.PointGradient_F64;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSubpixelSparseCornerFit {

	Random rand = new Random(345);

	Class imageTypes[] = new Class[]{ImageUInt8.class};

	double tol;

	/**
	 * Creates a square and tests subpixel on each corner
	 */
	@Test
	public void easyCase_square() {
		tol = 0.01;
		for( Class imageType : imageTypes ) {
			ImageSingleBand image = GeneralizedImageOps.createSingleBand(imageType,400,500);

			GImageMiscOps.fill(image,200);
			GImageMiscOps.fillRectangle(image,0,50,52,20,30);

			SubpixelSparseCornerFit alg = new SubpixelSparseCornerFit(imageType);
			alg.setWeightToggle(-1);
			alg.setIgnoreRadius(1);
			alg.setLocalRadius(4);
			alg.setMaxOptimizeSteps(100);
			alg.setImage(image);

			checkConverge(alg,50,52,2);
			checkConverge(alg,50,81,2);
			checkConverge(alg,69,81,2);
			checkConverge(alg,69,52,2);
		}
	}

	/**
	 * Test to see if it handles sub-images correctly
	 */
	@Test
	public void easyCase_square_subimage() {
		tol = 0.01;
		for( Class imageType : imageTypes ) {
			ImageSingleBand image = GeneralizedImageOps.createSingleBand(imageType,400,500);

			GImageMiscOps.fill(image,200);
			GImageMiscOps.fillRectangle(image,0,50,52,20,30);

			SubpixelSparseCornerFit alg = new SubpixelSparseCornerFit(imageType);
			alg.setWeightToggle(-1);
			alg.setIgnoreRadius(1);
			alg.setLocalRadius(4);
			alg.setMaxOptimizeSteps(100);
			alg.setImage(BoofTesting.createSubImageOf_S(image));

			checkConverge(alg,50,52,2);
			checkConverge(alg,50,81,2);
			checkConverge(alg,69,81,2);
			checkConverge(alg,69,52,2);
		}
	}

	/**
	 * Very small chessboard and checks the inner corners
	 */
	@Test
	public void easyCase_chess() {
		tol = 0.05;
		for (Class imageType : imageTypes) {
			ImageSingleBand image = GeneralizedImageOps.createSingleBand(imageType, 400, 500);

			GImageMiscOps.fill(image, 200);
			GImageMiscOps.fillRectangle(image, 0, 50, 52, 20, 20);
			GImageMiscOps.fillRectangle(image, 0, 50, 92, 20, 20);
			GImageMiscOps.fillRectangle(image, 0, 90, 52, 20, 20);
			GImageMiscOps.fillRectangle(image, 0, 90, 92, 20, 20);

			GImageMiscOps.fillRectangle(image, 0, 70, 72, 20, 20);

			SubpixelSparseCornerFit alg = new SubpixelSparseCornerFit(imageType);
			alg.setWeightToggle(0);
			alg.setIgnoreRadius(2);
			alg.setLocalRadius(7);
			alg.setMaxOptimizeSteps(100);
			alg.setImage(image);

			// the corner is going to get "sucked" towards the other adjacent square
			checkConverge(alg, 69.5, 71.5, 2);
			checkConverge(alg, 69.5, 91.5, 2);
			checkConverge(alg, 89.5, 91.5, 2);
			checkConverge(alg, 89.5, 71.5, 2);
		}
	}

	/**
	 * Rotate image about its center and see if it can still localize the corners.  The rotation
	 * will cause the lines to become blurry.
	 */
	@Test
	public void rotation() {
		tol = 0.3; // interpolation will reduce the accuracy
		for( Class imageType : imageTypes ) {
			ImageSingleBand image = GeneralizedImageOps.createSingleBand(imageType,400,500);
			ImageSingleBand rotated = GeneralizedImageOps.createSingleBand(imageType,400,500);

			SubpixelSparseCornerFit alg = new SubpixelSparseCornerFit(imageType);
			alg.setWeightToggle(-1);
			alg.setIgnoreRadius(2);
			alg.setLocalRadius(6);
			alg.setMaxOptimizeSteps(100);
			alg.setImage(rotated);

			for (int i = 1; i < 5; i++) {
				float angle = 0.05f + i*0.15f;
				System.out.println("Angle = "+angle);
				GImageMiscOps.fill(image, 200);
				GImageMiscOps.fillRectangle(image, 0, 100, 102, 50, 55);

				new FDistort(image, rotated).border(200).rotate(angle).apply();
				int halfW = image.width / 2, halfH = image.height / 2;
				PixelTransform_F32 inputToOutput = DistortSupport.transformRotate(
						halfW, halfH, halfW, halfH, -angle);

				inputToOutput.compute(100, 102);
				checkConverge(alg, inputToOutput.distX, inputToOutput.distY, 3);
				inputToOutput.compute(100, 156);
				checkConverge(alg, inputToOutput.distX, inputToOutput.distY, 3);
				inputToOutput.compute(149, 156);
				checkConverge(alg, inputToOutput.distX, inputToOutput.distY, 3);
				inputToOutput.compute(149, 102);
				checkConverge(alg, inputToOutput.distX, inputToOutput.distY, 3);
			}
		}
	}


	/**
	 * Assumes gradient operator does the correct thing, checks to see the number of points and
	 * if they have been massaged
	 */
	@Test
	public void computeLocalGradient() {
		SubpixelSparseCornerFit<ImageUInt8> alg = new SubpixelSparseCornerFit<ImageUInt8>(ImageUInt8.class);

		ImageUInt8 input = new ImageUInt8(20,25);
		ImageMiscOps.fillUniform(input,rand,0,200);

		alg.setIgnoreRadius(1);
		alg.setLocalRadius(4);
		alg.setImage(input);

		alg.computeLocalGradient(10,11);
		assertEquals((9*9-3*3),alg.points.size());

		// all coordinates should range from -1 to 1, after normalization
		for (int i = 0; i < alg.points.size; i++) {
			PointGradient_F64 p = alg.points.get(i);

			assertTrue(-1<=p.x && 1>=p.x);
			assertTrue(-1<=p.y && 1>=p.y);
		}

		// test border handling
		alg.points.reset();
		alg.computeLocalGradient(0,0);
		assertEquals((5*5-2*2),alg.points.size());
	}

	@Test
	public void massageGradient() {
		SubpixelSparseCornerFit<ImageUInt8> alg = new SubpixelSparseCornerFit<ImageUInt8>(ImageUInt8.class);

		alg.points.grow().set(1,2,100,150);
		alg.points.grow().set(2,3,120,200);
		alg.points.grow().set(3,2,1,2);

		List<PointGradient_F64> significant = new ArrayList<PointGradient_F64>();

		alg.massageGradient(significant);

		assertEquals(2,significant.size());

		PointGradient_F64 a = significant.get(0);
		PointGradient_F64 b = significant.get(1);

		double max = Math.sqrt(120 * 120 + 200 * 200);

		assertEquals(100/max,a.dx,1e-8);
		assertEquals(150/max,a.dy,1e-8);

		assertEquals(120/max,b.dx,1e-8);
		assertEquals(200/max,b.dy,1e-8);
	}

	private void checkConverge( SubpixelSparseCornerFit alg , double cx , double cy , int r ) {
//		System.out.println("============ cx "+cx+"  cy "+cy);
		for (int y = -r; y <= r ; y++) {
			for (int x = -r; x <= r; x++) {
				assertTrue(alg.refine((int)(cx + x+0.5), (int)(cy + y+0.5)));

				double foundX = alg.getRefinedX();
				double foundY = alg.getRefinedY();

				assertEquals(x+" "+y,cx,foundX,tol);
				assertEquals(x+" "+y,cy,foundY,tol);
			}
		}
	}

}
