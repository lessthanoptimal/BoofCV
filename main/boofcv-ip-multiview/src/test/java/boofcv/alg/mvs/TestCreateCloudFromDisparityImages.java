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

import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.distort.PointToPixelTransform_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCreateCloudFromDisparityImages extends BoofStandardJUnit {

	int width = 80;
	int height = 50;

	Point2Transform2_F64 n_to_p;
	PixelTransform<Point2D_F64> p_to_n;
	Se3_F64 world_to_view = SpecialEuclideanOps_F64.eulerXyz(1.5, 0.05, 0.0, 0.05, 0, 0.01, null);

	public TestCreateCloudFromDisparityImages() {
		var pinhole = new CameraPinhole(50, 50, 0.0, width/2, height/2, width, height);
		n_to_p = new LensDistortionPinhole(pinhole).distort_F64(false, true);
		p_to_n = new PointToPixelTransform_F64(new LensDistortionPinhole(pinhole).
				distort_F64(true, false));
	}

	/**
	 * Adds a view and compares its cloud
	 */
	@Test void oneView() {
		var inverseDepth = new GrayF32(width, height);

		ImageMiscOps.fillUniform(inverseDepth, rand, 0, 0.5f);

		// set one pixel to be invalid as a test
		inverseDepth.set(20, 30, -1);

		var alg = new CreateCloudFromDisparityImages();
		assertEquals(0, alg.addInverseDepth(inverseDepth, world_to_view, n_to_p, p_to_n));

		// See if the invalid pixel was skipped over
		assertEquals(width*height - 1, alg.cloud.size);

		DogArray<Point3D_F64> expected = new DogArray<>(Point3D_F64::new);
		MultiViewStereoOps.inverseToCloud(inverseDepth, p_to_n,
				( pixX, pixY, x, y, z ) -> expected.grow().setTo(x, y, z));

		// While not a strict requirement, the order of the two point clouds should match because they are both
		// processed in a row-major order
		for (int i = 0; i < expected.size; i++) {
			Point3D_F64 e = expected.get(i);
			SePointOps_F64.transformReverse(world_to_view, e, e);
			assertEquals(0.0, e.distance(alg.cloud.get(i)), UtilEjml.TEST_F64);
		}
	}

	/**
	 * If the views are identical then each point should only be added once
	 */
	@Test void twoIdenticalViews() {
		var inverseDepth = new GrayF32(width, height);

		ImageMiscOps.fillUniform(inverseDepth, rand, 0, 0.5f);

		var alg = new CreateCloudFromDisparityImages();
		assertEquals(0, alg.addInverseDepth(inverseDepth, world_to_view, n_to_p, p_to_n));

		assertEquals(width*height, alg.cloud.size);
		assertEquals(1, alg.viewPointIdx.size);

		// add it again and see if no new points were added but the views increased
		assertEquals(1, alg.addInverseDepth(inverseDepth, world_to_view, n_to_p, p_to_n));

		assertEquals(width*height, alg.cloud.size);
		assertEquals(2, alg.viewPointIdx.size);
	}

	/**
	 * Checks to see it obeys the similarity tolerance
	 */
	@Test void disparitySimilarTol() {
		float tol = 0.5f;
		var inverseDepth = new GrayF32(width, height);
		GrayU8 mask = inverseDepth.createSameShape(GrayU8.class);

		ImageMiscOps.fillUniform(inverseDepth, rand, 0, 0.5f);

		var alg = new CreateCloudFromDisparityImages();
		alg.disparitySimilarTol = tol;
		assertEquals(0, alg.addInverseDepth(inverseDepth, world_to_view, n_to_p, p_to_n));
		assertEquals(width*height, alg.cloud.size);

		// Changing the disparity, but just under the tolerance. Nothing should be added
		inverseDepth.data[72] += tol - 0.001f;
		assertEquals(1, alg.addInverseDepth(inverseDepth, world_to_view, n_to_p, p_to_n));
		assertEquals(width*height, alg.cloud.size);

		// It should now be above the tolerance
		inverseDepth.data[72] += 0.002f;
		// zero the mask again so that it can add points
		ImageMiscOps.fill(mask, 0);
		assertEquals(2, alg.addInverseDepth(inverseDepth, world_to_view, n_to_p, p_to_n));
		assertEquals(width*height + 1, alg.cloud.size);
	}
}
