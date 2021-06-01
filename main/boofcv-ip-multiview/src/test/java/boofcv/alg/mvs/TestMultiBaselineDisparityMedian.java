/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_F32;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMultiBaselineDisparityMedian extends BoofStandardJUnit {

	// Standard disparity parameters. Most test use the same for convenience
	CameraPinhole intrinsic = new CameraPinhole(40, 41, 0, 42.1, 40.6, 80, 82);
	DisparityParameters parameters = new DisparityParameters(5, 100, 500, intrinsic);

	/** Test everything all together with a simple problem */
	@Test void simpleAll() {
		var alg = new MultiBaselineDisparityMedian();
		var distort = new PixelTransformAffine_F64();
		distort.getModel().setTo(2, 0, 0, 2, 0, 0);
		intrinsic.width = 100;
		intrinsic.height = 80;
		alg.initialize(intrinsic, distort);

		for (int i = 0; i < 3; i++) {
			// to make it easy every image is the same size as fused
			var disparity = new GrayF32(100, 80);
			var mask = new GrayU8(100, 80);
			var rect = new DMatrixRMaj(3, 3);

			// see next test below for an explanation of this logic
			ImageMiscOps.fill(disparity, 5 + i);
			ImageMiscOps.fillRectangle(mask, 1, 1, 1, 79, 59);
			CommonOps_DDRM.diag(rect, 3, 0.5, 0.5, 1); // undoes distortion model to keep pixels the same

			alg.addDisparity(disparity, mask, parameters, rect);
		}

		// wrong size to make sure it's resized
		var found = new GrayF32(1, 1);
		alg.process(found);
		assertEquals(intrinsic.width, found.width);
		assertEquals(intrinsic.height, found.height);

		for (int y = 0; y < intrinsic.height; y++) {
			for (int x = 0; x < intrinsic.width; x++) {
				if (y == 0 || x == 0) {
					assertEquals(alg.fusedDisparityRange, found.get(x, y));
				} else if (y < 60 && x < 80) {
					assertEquals(alg.fusedDisparityRange - 1, found.get(x, y));
				} else {
					assertEquals(alg.fusedDisparityRange, found.get(x, y));
				}
			}
		}
	}

	/**
	 * Adds a information from a new disparity image to the fused image. Checks to see if masks and parts of the
	 * warping between the two images are handled correctly.
	 */
	@Test void addToFusedImage() {
		var alg = new MultiBaselineDisparityMedian();
		var distort = new PixelTransformAffine_F64();
		distort.getModel().setTo(2, 0, 0, 2, 0, 0);
		alg.initialize(intrinsic, distort);
		alg.fusedBaseline = parameters.baseline;

		var image = new MultiBaselineDisparityMedian.DisparityImage();
		image.disparity.reshape(intrinsic.width, intrinsic.height - 12);
		ImageMiscOps.fill(image.disparity, 5);
		image.mask.reshape(image.disparity);
		image.parameters.setTo(parameters);
		// only part of the mask has valid values
		ImageMiscOps.fillRectangle(image.mask, 1, 1, 1, 79, 59);
		// when inverted this will counter act the distort above
		CommonOps_DDRM.diag(image.undist_to_rect_px, 3, 0.5, 0.5, 1);

		assertTrue(alg.addToFusedImage(image));
		for (int y = 0; y < intrinsic.height; y++) {
			for (int x = 0; x < intrinsic.width; x++) {
				if (y == 0 || x == 0 || y >= image.disparity.height) {
					assertEquals(0, alg.fused.get(x, y).size, x + " " + y);
				} else if (y < 60 && x < 80) {
					assertEquals(1, alg.fused.get(x, y).size);
					assertEquals(10, alg.fused.get(x, y).get(0));
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

		var alg = new MultiBaselineDisparityMedian();
		var distort = new PixelTransformAffine_F64();
		distort.getModel().setTo(1, 0, 0, 1, 0, 0);
		alg.initialize(intrinsic, distort);
		alg.fusedBaseline = parameters.baseline;

		var image = new MultiBaselineDisparityMedian.DisparityImage();
		image.disparity.reshape(intrinsic.width, intrinsic.height);
		image.mask.reshape(image.disparity);
		image.parameters.setTo(parameters);
		ImageMiscOps.fill(image.mask, 1);
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
		var alg = new MultiBaselineDisparityMedian();
		intrinsic.width = 10;
		intrinsic.height = 8;
		alg.initialize(intrinsic, new DoNothingPixelTransform_F64());

		// add elements to each fused pixel that will be easy to compute the solution for
		int counter = 0;
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 10; x++, counter++) {
				for (int i = 0; i < counter; i++) {
					alg.fused.get(x, y).add(i + 0.5f);
				}
			}
		}

		GrayF32 found = new GrayF32(10, 8);
		assertTrue(alg.computeFused(found));

		// manually compute the solution. Use a brute force approach for median value since it requires no thinking
		DogArray_F32 expected = new DogArray_F32();
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 10; x++) {
				if (expected.size == 0)
					assertEquals(Float.MAX_VALUE, found.get(x, y), UtilEjml.TEST_F32);
				else if (expected.size == 1)
					assertEquals(0.5f, found.get(x, y), UtilEjml.TEST_F32);
				else if (expected.size == 2)
					assertEquals(1.0f, found.get(x, y), UtilEjml.TEST_F32);
				else {
					assertEquals(expected.data[expected.size/2], found.get(x, y), UtilEjml.TEST_F32);
				}
				expected.add(expected.size + 0.5f);
			}
		}
	}

	/**
	 * Should fail if it can't find a valid pixel
	 */
	@Test void computeFused_AllInvalid() {
		var alg = new MultiBaselineDisparityMedian();
		intrinsic.width = 10;
		intrinsic.height = 8;
		alg.initialize(intrinsic, new DoNothingPixelTransform_F64());

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

		var alg = new MultiBaselineDisparityMedian();
		alg.initialize(intrinsic, new DoNothingPixelTransform_F64());
		alg.fusedBaseline = 500; // keep the baseline the same, yes this should be checked too but isn't here

		var image = new MultiBaselineDisparityMedian.DisparityImage();
		image.disparity.reshape(width, height);
		ImageMiscOps.fill(image.disparity, imageValue);
		image.mask.reshape(image.disparity);
		ImageMiscOps.fill(image.mask, 1);
		image.parameters.setTo(param2);
		CommonOps_DDRM.setIdentity(image.undist_to_rect_px);

		assertTrue(alg.addToFusedImage(image));
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (expected > 0) {
					assertEquals(1, alg.fused.get(x, y).size);
					float found = alg.fused.get(x, y).get(0);
					assertEquals(expected, found, UtilEjml.TEST_F32);
				} else {
					assertEquals(0, alg.fused.get(x, y).size);
				}
			}
		}
	}

	/**
	 * Ensures the adjusted disparity image produces the same point cloud and has an expected range
	 */
	@Test void computeDynamicParameters() {
		float fx = 800.0f; // an arbitrary focal length
		var alg = new MultiBaselineDisparityMedian();
		var disparity = new GrayF32(40, 30);

		//  Give it a max value more than the fixed range of 100
		for (int i = 0; i < 30; i++) {
			disparity.set(i, 5, i + 80);
		}

		// The max is too high and it should to scale it down
		alg.fusedBaseline = 1.5;
		alg.computeDynamicParameters(disparity);
		assertEquals(alg.fusedDisparityRange - 1, ImageStatistics.max(disparity), UtilEjml.TEST_F32);
		assertTrue(alg.fusedBaseline < 1.5);
		// Compute the Z distance of a point with original parameters now try it again with the new parameters
		assertEquals(1.5*fx/109.0, alg.fusedBaseline*fx/99.0, UtilEjml.TEST_F32);

		// Max value is too small and range should be increased
		for (int i = 0; i < 30; i++) {
			disparity.set(i, 5, i + 5);
		}
		alg.fusedBaseline = 1.5;
		alg.computeDynamicParameters(disparity);
		assertEquals(alg.fusedDisparityRange - 1, ImageStatistics.max(disparity), UtilEjml.TEST_F32);
		assertTrue(alg.fusedBaseline > 1.5);
		assertEquals(1.5*fx/34, alg.fusedBaseline*fx/99.0, UtilEjml.TEST_F32);
	}
}
