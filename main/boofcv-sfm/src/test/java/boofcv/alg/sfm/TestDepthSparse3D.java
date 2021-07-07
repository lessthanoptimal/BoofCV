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

package boofcv.alg.sfm;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayU16;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDepthSparse3D extends BoofStandardJUnit {

	int w = 10;
	int h = 20;

	@Test void basicTest() {
		GrayU16 depth = new GrayU16(w, h);
		depth.set(5, 6, 1000);

		CameraPinholeBrown param = new CameraPinholeBrown(1, 1, 0, 5, 10, w, h).fsetRadial(0, 0);

		PixelTransform<Point2D_F32> v2d = new PixelTransform<Point2D_F32>() {

			@Override
			public void compute( int x, int y, Point2D_F32 output ) {
				output.x = x + 1;
				output.y = y + 2;
			}

			@Override
			public PixelTransform<Point2D_F32> copyConcurrent() {
				return null;
			}
		};

		DepthSparse3D<GrayU16> alg = new DepthSparse3D.I<>(2.1);
		alg.configure(LensDistortionFactory.narrow(param), v2d);

		alg.setDepthImage(depth);

		assertTrue(alg.process(4, 4));

		Point3D_F64 found = alg.getWorldPt();

		Point2D_F64 norm = new Point2D_F64();
		PerspectiveOps.convertPixelToNorm(param, new Point2D_F64(4, 4), norm);
		double z = 1000*2.1;

		assertEquals(z, found.z, 1e-8);
		assertEquals(norm.x*z, found.x, 1e-8);
		assertEquals(norm.y*z, found.y, 1e-8);
	}
}
