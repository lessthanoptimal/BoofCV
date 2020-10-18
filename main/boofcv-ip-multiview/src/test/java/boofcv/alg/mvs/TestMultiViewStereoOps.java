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

import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMultiViewStereoOps extends BoofStandardJUnit {
	int width = 80;
	int height = 82;
	CameraPinhole intrinsic = new CameraPinhole(40, 41, 0, 42.1, 40.6, width, height);

	@Test void maskOutPointsInCloud() {
		List<Point3D_F64> cloud = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 2), -1, 1, 100, rand);
		var disparity = new GrayF32(width, height);
		var parameters = new DisparityParameters(2, 100, 1.5, intrinsic);
		Se3_F64 cloud_to_stereo = SpecialEuclideanOps_F64.eulerXyz(-0.1, 0.05, 0.2, 0.01, 0.02, -0.03, null);
		var norm_to_pixel = new LensDistortionPinhole(intrinsic).distort_F64(false, true);
		double tolerance = 1.0;
		GrayU8 mask = disparity.createSameShape(GrayU8.class);

		// Render the cloud onto the disparity image
		renderCloudToDisparity(cloud, cloud_to_stereo, parameters, disparity);

		// The cloud and disparity image will match up perfectly. So each of the points in the cloud will cause
		// the mask to be set to 1
		MultiViewStereoOps.maskOutPointsInCloud(
				cloud, disparity, parameters, cloud_to_stereo, norm_to_pixel, tolerance, mask);
		assertEquals(cloud.size(), ImageStatistics.sum(mask));

		// Make a disparity point barely within tolerance. The mask should not change
		ImageMiscOps.fill(mask, 0);
		ImageMiscOps.findAndProcess(disparity, ( v ) -> v < parameters.disparityRange, ( int x, int y ) -> {
			disparity.data[disparity.getIndex(x, y)] += tolerance - 0.0001;
			return false;
		});
		MultiViewStereoOps.maskOutPointsInCloud(
				cloud, disparity, parameters, cloud_to_stereo, norm_to_pixel, tolerance, mask);
		assertEquals(cloud.size(), ImageStatistics.sum(mask));

		// Make that same point outside of tolerance. The pixel should not be masked
		ImageMiscOps.fill(mask, 0);
		ImageMiscOps.findAndProcess(disparity, ( v ) -> v < parameters.disparityRange, ( int x, int y ) -> {
			disparity.data[disparity.getIndex(x, y)] += 0.0002;
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
								 DisparityParameters param, GrayF32 disparity ) {
		// mark all pixels as invalid initially
		GImageMiscOps.fill(disparity, param.disparityRange);

		var w2p = new WorldToCameraToPixel();
		w2p.configure(intrinsic, cloud_to_stereo);

		Point2D_F64 pixel = new Point2D_F64();
		for (int i = cloud.size() - 1; i >= 0; i--) {
			assertTrue(w2p.transform(cloud.get(i), pixel));
			assertTrue(BoofMiscOps.isInside(disparity, pixel.x, pixel.y));
			double d = param.baseline*param.pinhole.fx/w2p.getCameraPt().z - param.disparityMin;
			assertTrue(d >= 0.0 && d < param.disparityRange);

			int xx = (int)pixel.x;
			int yy = (int)pixel.y;

			if (disparity.get(xx, yy) != param.disparityRange) {
				cloud.remove(i);
			} else {
				disparity.set(xx, yy, (float)d);
			}
		}
	}
}
