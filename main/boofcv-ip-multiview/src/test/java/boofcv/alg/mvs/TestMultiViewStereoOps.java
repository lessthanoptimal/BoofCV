/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
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
		GrayF32 disparity = new GrayF32(width, height);
		List<Point3D_F64> cloud = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 2), -1, 1, 100, rand);

		Se3_F64 cloud_to_stereo = SpecialEuclideanOps_F64.eulerXyz(-0.1, 0.05, 0.2, 0.01, 0.02, -0.03, null);
		var norm_to_pixel = new LensDistortionPinhole(intrinsic).distort_F64(false, true);
		float tolerance = 1.0f;
		GrayU8 mask = disparity.createSameShape(GrayU8.class);

		// Render the cloud onto the disparity image
		renderCloudToDisparity(cloud, cloud_to_stereo, new LensDistortionPinhole(intrinsic), parameters, disparity);

		// The cloud and disparity image will match up perfectly. So each of the points in the cloud will cause
		// the mask to be set to 1
		MultiViewStereoOps.maskOutPointsInCloud(
				cloud, disparity, parameters, cloud_to_stereo, norm_to_pixel, tolerance, mask);
		assertEquals(cloud.size(), ImageStatistics.sum(mask));

		// Make a disparity point barely within tolerance. The mask should not change
		ImageMiscOps.fill(mask, 0);
		ImageMiscOps.findAndProcess(disparity, ( v ) -> v < parameters.disparityRange, ( int x, int y ) -> {
			disparity.data[disparity.getIndex(x, y)] += tolerance - 0.0001f;
			return false;
		});
		MultiViewStereoOps.maskOutPointsInCloud(
				cloud, disparity, parameters, cloud_to_stereo, norm_to_pixel, tolerance, mask);
		assertEquals(cloud.size(), ImageStatistics.sum(mask));

		// Make that same point outside of tolerance. The pixel should not be masked
		ImageMiscOps.fill(mask, 0);
		ImageMiscOps.findAndProcess(disparity, ( v ) -> v < parameters.disparityRange, ( int x, int y ) -> {
			disparity.data[disparity.getIndex(x, y)] += 0.0002f;
			return false;
		});
		MultiViewStereoOps.maskOutPointsInCloud(
				cloud, disparity, parameters, cloud_to_stereo, norm_to_pixel, tolerance, mask);
		assertEquals(cloud.size() - 1, ImageStatistics.sum(mask));
	}

	/**
	 * Renders the cloud into the disparity image and removes points which land on the same pixel
	 */
	void renderCloudToDisparity( List<Point3D_F64> cloud, Se3_F64 cloud_to_stereo,
								 LensDistortionNarrowFOV distortion,
								 DisparityParameters param, ImageGray<?> disparity ) {
		// mark all pixels as invalid initially
		GImageMiscOps.fill(disparity, param.disparityRange);

		var w2p = new WorldToCameraToPixel();
		w2p.configure(distortion, cloud_to_stereo);

		Point2D_F64 pixel = new Point2D_F64();
		for (int i = cloud.size() - 1; i >= 0; i--) {
			assertTrue(w2p.transform(cloud.get(i), pixel));
			assertTrue(BoofMiscOps.isInside(disparity, pixel.x, pixel.y));
			double d = param.baseline*param.pinhole.fx/w2p.getCameraPt().z - param.disparityMin;
			assertTrue(d >= 0.0 && d < param.disparityRange);

			int xx = (int)(pixel.x + 0.5);
			int yy = (int)(pixel.y + 0.5);

			double value = GeneralizedImageOps.get(disparity, xx, yy);

			if (Math.abs(value - param.disparityRange) > 5e-4) {
				cloud.remove(i);
			} else {
				GeneralizedImageOps.set(disparity, xx, yy, d);
			}
		}
	}

	/**
	 * Test by comparing against a simplistic implementation and explicitly checking the masked area
	 */
	@Test void disparityToCloud_consumer_masked_F32() {
		var disparity = new GrayF32(width, height);
		var mask = new GrayU8(width, height);

		// mask out 1/2 the image
		ImageMiscOps.fillRectangle(mask, 1, 0, 0, width/2, height);

		// Randomly fill in the disparity image
		ImageMiscOps.fillUniform(disparity, rand, 0, parameters.disparityRange);

		// make a couple of pixels are invalid
		disparity.set(10, 12, parameters.disparityRange);
		disparity.set(2, 21, parameters.disparityRange);

		// Project the cloud onto the disparity image
		var distortionFactory = new LensDistortionPinhole(intrinsic);

		Point2Transform2_F64 pixel_to_norm = distortionFactory.undistort_F64(true, false);
		Point2D_F64 norm = new Point2D_F64();

		// Verify the results by computing the 3D point using a brute force method
		MultiViewStereoOps.disparityToCloud(disparity, mask, parameters,
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
					// make sure the mask was obeys
					assertEquals(0, mask.get(pixX, pixY));
					// mark the pixel so we can test to see if it was computed
					disparity.set(pixX, pixY, parameters.disparityRange);
				}));

		// make sure the only spots with non-invalid pixels are where they were msaked out
		ImageMiscOps.findAndProcess(disparity, ( v ) -> v < parameters.disparityRange, ( x, y ) -> {
			assertEquals(mask.unsafe_get(x, y), 1);
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
}
