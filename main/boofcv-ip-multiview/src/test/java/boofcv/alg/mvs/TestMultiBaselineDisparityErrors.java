/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.mvs;

import boofcv.alg.distort.DoNothingPixelTransform_F64;
import boofcv.alg.distort.PixelTransformAffine_F64;
import boofcv.alg.geo.rectify.DisparityParameters;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMultiBaselineDisparityErrors extends BoofStandardJUnit {

	// Standard disparity parameters. Most test use the same for convenience
	CameraPinhole intrinsic = new CameraPinhole(40, 41, 0, 42.1, 40.6, 80, 82);
	DisparityParameters parameters = new DisparityParameters(5, 100, 500, intrinsic);

	/** Test everything all together with a simple problem */
	@Test void simpleAll() {
		var alg = new MultiBaselineDisparityErrors();
		var distort = new PixelTransformAffine_F64();
		distort.getModel().setTo(2, 0, 0, 2, 0, 0);
		intrinsic.width = 100;
		intrinsic.height = 80;
		alg.initialize(intrinsic.width, intrinsic.height, distort);

		for (int i = 0; i < 3; i++) {
			// to make it easy every image is the same size as fused
			var disparity = new GrayF32(100, 80);
			var scores = new GrayF32(100, 80);
			var rect = new DMatrixRMaj(3, 3);

			// This will set only valid disparity values inside the image
			ImageMiscOps.fill(disparity, parameters.disparityRange);
			ImageMiscOps.fillRectangle(disparity, 5 + i, 1, 1, 79, 59);

			CommonOps_DDRM.diag(rect, 3, 0.5, 0.5, 1); // undoes distortion model to keep pixels the same

			alg.addDisparity(disparity, scores, parameters, rect);
		}

		// wrong size to make sure it's resized
		var found = new GrayF32(1, 1);
		alg.process(found);
		assertEquals(intrinsic.width, found.width);
		assertEquals(intrinsic.height, found.height);

		for (int y = 0; y < intrinsic.height; y++) {
			for (int x = 0; x < intrinsic.width; x++) {
				if (y == 0 || x == 0) {
					assertEquals(-1f, found.get(x, y));
				} else if (y < 60 && x < 80) {
					assertTrue(found.get(x, y) > 0);
				} else {
					assertEquals(-1f, found.get(x, y));
				}
			}
		}
	}

	/**
	 * Adds a information from a new disparity image to the fused image. Checks to see if masks and parts of the
	 * warping between the two images are handled correctly.
	 */
	@Test void addToFusedImage() {
		var alg = new MultiBaselineDisparityErrors();
		var distort = new PixelTransformAffine_F64();
		distort.getModel().setTo(2, 0, 0, 2, 0, 0);
		alg.initialize(intrinsic.width, intrinsic.height, distort);

		var image = new MultiBaselineDisparityErrors.DisparityImage();
		image.disparity.reshape(intrinsic.width, intrinsic.height - 12);
		image.score.reshape(image.disparity);
		ImageMiscOps.fill(image.disparity, 5);
		image.parameters.setTo(parameters);

		// This will set only valid disparity values inside the image
		ImageMiscOps.fill(image.disparity, parameters.disparityRange);
		ImageMiscOps.fillRectangle(image.disparity, 5, 1, 1, 79, 59);

		// when inverted this will counteract the distortion above
		CommonOps_DDRM.diag(image.undist_to_rect_px, 3, 0.5, 0.5, 1);

		assertTrue(alg.addToFusedImage(image));
		for (int y = 0; y < intrinsic.height; y++) {
			for (int x = 0; x < intrinsic.width; x++) {
				if (y == 0 || x == 0 || y >= image.disparity.height) {
					assertEquals(0, alg.fused.get(x, y).size, x + " " + y);
				} else if (y < 60 && x < 80) {
					assertEquals(1, alg.fused.get(x, y).size);
					assertEquals(1, alg.fused.getScore(x, y).size);
					assertTrue(alg.fused.get(x, y).get(0) > 0.0f);
				} else {
					assertEquals(0, alg.fused.get(x, y).size);
				}
			}
		}
	}

	/** Checks to see if it handles points with zero disparity at infinity correctly */
	@Test void addToFusedImage_infinity() {
		// make the images smaller for slightly faster processing since size doesn't matter
		intrinsic.fsetShape(10, 12);

		var alg = new MultiBaselineDisparityErrors();
		var distort = new PixelTransformAffine_F64();
		distort.getModel().setTo(1, 0, 0, 1, 0, 0);
		alg.initialize(intrinsic.width, intrinsic.height, distort);

		var image = new MultiBaselineDisparityErrors.DisparityImage();
		image.disparity.reshape(intrinsic.width, intrinsic.height);
		image.score.reshape(image.disparity);
		image.parameters.setTo(parameters);
		CommonOps_DDRM.diag(image.undist_to_rect_px, 3, 1, 1, 1);
		// every point will be at infinity
		ImageMiscOps.fill(image.disparity, 0);
		image.parameters.disparityMin = 0;

		// Every point should be filled in with zero disparity
		assertTrue(alg.addToFusedImage(image));
		for (int y = 0; y < intrinsic.height; y++) {
			for (int x = 0; x < intrinsic.width; x++) {
				assertEquals(1, alg.fused.get(x, y).size);
				assertEquals(0, alg.fused.get(x, y).get(0));
			}
		}
	}

	/**
	 * Given an already constructed fused image, compute the disparity image output.
	 */
	@Test void computeFused() {
		var alg = new MultiBaselineDisparityErrors();
		intrinsic.width = 10;
		intrinsic.height = 8;
		alg.initialize(intrinsic.width, intrinsic.height, new DoNothingPixelTransform_F64());

		// add elements to each fused pixel that will be easy to compute the solution for
		int counter = 0;
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 10; x++, counter++) {
				for (int i = 0; i < counter; i++) {
					alg.fused.get(x, y).add(i + 0.5f);
					alg.fused.getScore(x, y).add(1.0f);
				}
			}
		}

		var found = new GrayF32(10, 8);
		assertTrue(alg.computeFused(found));

		// manually compute the solution. Use a brute force approach for median value since it requires no thinking
		float sum = 0.0f;
		float total = 0;
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 10; x++) {
				if (total == 0)
					assertEquals(-1, found.get(x, y), UtilEjml.TEST_F32);
				else {
					assertEquals(sum/total, found.get(x, y), UtilEjml.TEST_F32);
				}
				sum += total + 0.5f;
				total++;
			}
		}
	}

	/**
	 * Should fail if it can't find a valid pixel
	 */
	@Test void computeFused_AllInvalid() {
		var alg = new MultiBaselineDisparityErrors();
		intrinsic.width = 10;
		intrinsic.height = 8;
		alg.initialize(intrinsic.width, intrinsic.height, new DoNothingPixelTransform_F64());

		// This will fail because every pixel is empty
		assertFalse(alg.computeFused(new GrayF32(10, 8)));
	}

	/** See if it handles the change in disparity parameters between images correctly */
	@Test void differentDisparityParameters() {
		checkDisparityConversion(5, 0.5, 1.0, (5 + 5)*2.0);
		checkDisparityConversion(80, 0.5, 1.0, (80 + 5)*2.0);
		checkDisparityConversion(80, 2.0, 0.5, (80 + 5));
	}

	/**
	 * @param imageValue pixel value in disparity image
	 * @param focalScale How much the fused focal length is scaled by
	 * @param baselineScale How much the fused baseline is scaled by
	 * @param expected Expected value of fused disparity
	 */
	void checkDisparityConversion( float imageValue, double focalScale, double baselineScale, double expected ) {
		int width = 3;
		int height = 2;

		var intrinsic2 = new CameraPinhole(intrinsic);
		intrinsic2.fx *= focalScale;
		intrinsic2.fy *= focalScale;

		var param2 = new DisparityParameters(5, 100, 500*baselineScale, intrinsic2);

		var alg = new MultiBaselineDisparityErrors();
		alg.initialize(intrinsic.width, intrinsic.height, new DoNothingPixelTransform_F64());

		var image = new MultiBaselineDisparityErrors.DisparityImage();
		image.disparity.reshape(width, height);
		image.score.reshape(image.disparity);
		ImageMiscOps.fill(image.disparity, imageValue);
		image.parameters.setTo(param2);
		CommonOps_DDRM.setIdentity(image.undist_to_rect_px);

		assertTrue(alg.addToFusedImage(image));
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (expected > 0) {
					assertEquals(1, alg.fused.get(x, y).size);
					float found = alg.fused.get(x, y).get(0);
					assertTrue(found > 0);
				} else {
					assertEquals(0, alg.fused.get(x, y).size);
				}
			}
		}
	}
}
