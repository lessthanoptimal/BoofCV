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

import boofcv.BoofTesting;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.alg.geo.rectify.DisparityParameters;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.ConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.distort.PointToPixelTransform_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.dense.row.CommonOps_DDRM;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMultiViewStereoOps extends BoofStandardJUnit {
	int width = 80;
	int height = 82;
	CameraPinhole intrinsic = new CameraPinhole(40, 41, 0, 42.1, 40.6, width, height);
	CameraPinholeBrown intrinsicBrown = new CameraPinholeBrown(40, 41, 0, 42.1, 40.6, width, height).fsetRadial(0.05, -0.06);
	DisparityParameters parameters = new DisparityParameters(2, 100, 1.5, intrinsic);

	@Test void maskOutPointsInCloud() {
		GrayF32 inverseDepth = new GrayF32(width, height);
		List<Point3D_F64> cloud = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 2), -1, 1, 100, rand);

		Se3_F64 cloud_to_camera = SpecialEuclideanOps_F64.eulerXyz(-0.1, 0.05, 0.2, 0.01, 0.02, -0.03, null);
		var norm_to_pixel = new LensDistortionPinhole(intrinsic).distort_F64(false, true);
		float tolerance = 1e-3f;
		GrayU8 mask = inverseDepth.createSameShape(GrayU8.class);

		// Render the cloud onto the disparity image
		renderCloudToInverseDepth(cloud, cloud_to_camera, new LensDistortionPinhole(intrinsic), inverseDepth);

		// The cloud and inverse depth image will match up perfectly. So each of the points in the cloud will cause
		// the mask to be close to one. Some points might render on top of each other.
		MultiViewStereoOps.maskOutPointsInCloud(
				cloud, inverseDepth, cloud_to_camera, norm_to_pixel, tolerance, mask);
		assertTrue(cloud.size()*0.97 <= ImageStatistics.sum(mask));

		// Make a point barely within tolerance. The mask should not change
		int maskCountBefore = ImageStatistics.sum(mask);
		ImageMiscOps.fill(mask, 0);
		ImageMiscOps.findAndProcess(inverseDepth, ( v ) -> v > 0, ( int x, int y ) -> {
			inverseDepth.data[inverseDepth.getIndex(x, y)] += tolerance - 0.0001f;
			return false;
		});
		MultiViewStereoOps.maskOutPointsInCloud(
				cloud, inverseDepth, cloud_to_camera, norm_to_pixel, tolerance, mask);
		assertEquals(maskCountBefore, ImageStatistics.sum(mask));

		// Make that same point outside of tolerance. The pixel should not be masked
		ImageMiscOps.fill(mask, 0);
		ImageMiscOps.findAndProcess(inverseDepth, ( v ) -> v > 0, ( int x, int y ) -> {
			inverseDepth.data[inverseDepth.getIndex(x, y)] += tolerance + 0.0002f;
			return false;
		});
		MultiViewStereoOps.maskOutPointsInCloud(
				cloud, inverseDepth, cloud_to_camera, norm_to_pixel, tolerance, mask);
		assertEquals(maskCountBefore - 1, ImageStatistics.sum(mask));
	}

	/**
	 * Renders the cloud into the disparity image and removes points which land on the same pixel
	 */
	void renderCloudToInverseDepth( List<Point3D_F64> cloud, Se3_F64 cloud_to_stereo,
									LensDistortionNarrowFOV distortion,
									ImageGray<?> inverseDepth ) {
		// mark all pixels as invalid initially
		GImageMiscOps.fill(inverseDepth, -1);

		var w2p = new WorldToCameraToPixel();
		w2p.configure(distortion, cloud_to_stereo);

		Point2D_F64 pixel = new Point2D_F64();
		for (int i = cloud.size() - 1; i >= 0; i--) {
			assertTrue(w2p.transform(cloud.get(i), pixel));
			assertTrue(BoofMiscOps.isInside(inverseDepth, pixel.x, pixel.y));
			double z = w2p.getCameraPt().z;
			if (z <= 0.0)
				continue;

			double inv = 1.0/z;

			int xx = (int)(pixel.x + 0.5);
			int yy = (int)(pixel.y + 0.5);

			double value = GeneralizedImageOps.get(inverseDepth, xx, yy);

			GeneralizedImageOps.set(inverseDepth, xx, yy, inv);
		}
	}

	/**
	 * Test by comparing against a simplistic implementation and explicitly checking the masked area
	 */
	@Test void disparityToCloud_consumer_masked_F32() {
		var disparity = new GrayF32(width, height);

		// Randomly fill in the disparity image
		ImageMiscOps.fillUniform(disparity, rand, 0, parameters.disparityRange);

		// Mark half as invite
		ImageMiscOps.fillRectangle(disparity, parameters.disparityRange, 0, 0, width/2, height);

		// make a couple of pixels are invalid
		disparity.set(10, 12, parameters.disparityRange);
		disparity.set(2, 21, parameters.disparityRange);

		// Project the cloud onto the disparity image
		var distortionFactory = new LensDistortionPinhole(intrinsic);

		Point2Transform2_F64 pixel_to_norm = distortionFactory.undistort_F64(true, false);
		Point2D_F64 norm = new Point2D_F64();

		// Verify the results by computing the 3D point using a brute force method
		MultiViewStereoOps.disparityToCloud(disparity, parameters,
				(( pixX, pixY, x, y, z ) -> {
					double d = GeneralizedImageOps.get(disparity, pixX, pixY);
					double expectedZ = MultiViewOps.disparityToRange(
							d + parameters.disparityMin, intrinsic.fx, parameters.baseline);
					if (Double.isInfinite(expectedZ))
						assertTrue(Double.isInfinite(z));
					else {
						pixel_to_norm.compute(pixX, pixY, norm);
						assertEquals(expectedZ, z, 1e-4);
						assertEquals(expectedZ*norm.x, x, 1e-4);
						assertEquals(expectedZ*norm.y, y, 1e-4);
					}
					// mark the pixel so we can test to see if it was computed
					disparity.set(pixX, pixY, parameters.disparityRange);
				}));

		// make sure the only spots with non-invalid pixels are where they were masked out
		ImageMiscOps.findAndProcess(disparity, ( v ) -> v < parameters.disparityRange, ( x, y ) -> {
			assertEquals(disparity.unsafe_get(x, y), parameters.disparityRange);
			return true; // continue the search
		});
	}

	/**
	 * Test by comparing against a simplistic implementation
	 */
	@Test void disparityToCloud_consumer() {
		var disparityU8 = new GrayU8(width, height);

		disparityToCloud_consumer(disparityU8, new LensDistortionPinhole(intrinsic), null);
		disparityToCloud_consumer(disparityU8, new LensDistortionPinhole(intrinsicBrown),
				new LensDistortionPinhole(intrinsicBrown).undistort_F64(true, false));

		var disparityF32 = new GrayF32(width, height);
		disparityToCloud_consumer(disparityF32, new LensDistortionPinhole(intrinsic), null);
		disparityToCloud_consumer(disparityF32, new LensDistortionPinhole(intrinsicBrown),
				new LensDistortionPinhole(intrinsicBrown).undistort_F64(true, false));
	}

	private void disparityToCloud_consumer( ImageGray<?> disparity, LensDistortionNarrowFOV distortionFactory,
											@Nullable Point2Transform2_F64 pointToNorm ) {

		// Randomly fill in the disparity image
		GImageMiscOps.fillUniform(disparity, rand, 0, parameters.disparityRange - 1);

		// make a couple of pixels are invalid
		GeneralizedImageOps.get(disparity, 10, 12, parameters.disparityRange);
		GeneralizedImageOps.get(disparity, 2, 21, parameters.disparityRange);

		Point2Transform2_F64 pixel_to_norm = distortionFactory.undistort_F64(true, false);
		Point2D_F64 norm = new Point2D_F64();

		// Verify the results by computing the 3D point using a brute force method
		MultiViewStereoOps.disparityToCloud(disparity, parameters,
				pointToNorm == null ? null : new PointToPixelTransform_F64(pointToNorm),
				(( pixX, pixY, x, y, z ) -> {
					double d = GeneralizedImageOps.get(disparity, pixX, pixY);
					double expectedZ = MultiViewOps.disparityToRange(
							d + parameters.disparityMin, intrinsic.fx, parameters.baseline);
					if (Double.isInfinite(expectedZ))
						assertTrue(Double.isInfinite(z));
					else {
						pixel_to_norm.compute(pixX, pixY, norm);
						assertEquals(expectedZ, z, 1e-4);
						assertEquals(expectedZ*norm.x, x, 1e-4);
						assertEquals(expectedZ*norm.y, y, 1e-4);
					}
				}));
	}

	@Test void averageScore() {
		var grayU8 = new GrayU8(30, 40);
		var scores = new GrayF32(grayU8.width, grayU8.height);

		ImageMiscOps.fillUniform(grayU8, rand, 0, 200);
		ImageMiscOps.fillUniform(scores, rand, 0, 200);

		int maxRange = 100;

		// Compute expected results using a lambda
		var storage = new SumStorage();
		grayU8.forEachPixel(( x, y, v ) -> {
			if (v >= maxRange)
				return;
			storage.sum += scores.get(x, y);
			storage.count++;
		});

		// Test against two image types
		double expected = storage.sum/storage.count;
		assertEquals(expected, MultiViewStereoOps.averageScore(grayU8, maxRange, scores), 1e-4);

		var grayF32 = new GrayF32(30, 40);
		ConvertImage.convert(grayU8, grayF32);
		assertEquals(expected, MultiViewStereoOps.averageScore(grayF32, maxRange, scores), 1e-4);
	}

	@Test void invalidateUsingError() {
		var grayU8 = new GrayU8(30, 40);
		ImageMiscOps.fillUniform(grayU8, rand, 0, 200);
		GrayU8 expected = grayU8.createSameShape().setTo(grayU8);

		var grayF32 = new GrayF32(30, 40);
		ConvertImage.convert(grayU8, grayF32);

		var scores = new GrayF32(grayU8.width, grayU8.height);
		ImageMiscOps.fillUniform(scores, rand, 0, 200);

		float threshold = 50;
		int maxRange = 100;

		// Compare against a lambda implementation
		ImageMiscOps.filter(expected, ( x, y, d ) -> scores.get(x, y) >= threshold ? maxRange : d);

		MultiViewStereoOps.invalidateUsingError(grayU8, maxRange, scores, threshold);
		BoofTesting.assertEquals(expected, grayU8, 1e-4);

		MultiViewStereoOps.invalidateUsingError(grayF32, maxRange, scores, threshold);
		BoofTesting.assertEquals(expected, grayF32, 1e-4);
	}

	private static class SumStorage {
		public double sum = 0.0;
		public int count = 0;
	}

	@Test void invalidateBorder() {
		int width = 30;
		int height = 20;
		int radius = 3;
		int invalid = 100;
		var disparity = new GrayF32(width, height);
		ImageMiscOps.fill(disparity, 50);

		// shift the image
		var dist_to_undist = new PixelTransform<Point2D_F64>() {
			@Override public void compute( int x, int y, Point2D_F64 output ) {
				output.setTo(x + 10, y + 1);
			}
		};

		// don't distort the image
		var undist_to_rect = CommonOps_DDRM.identity(3, 3);

		MultiViewStereoOps.invalidateBorder(width, height,
				dist_to_undist, undist_to_rect, radius, invalid, disparity);

		// Find all the points removed
		int found = 0;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				if (disparity.get(j,i) == invalid)
					found++;
			}
		}

		// Compute the region blocked out by subtracting an outer rectangle from an inner rectangle
		int expected = (width-(10-radius))*height;
		expected -= (width-(10+radius+1))*(height-radius-2);

		assertEquals(expected, found);
	}
}
