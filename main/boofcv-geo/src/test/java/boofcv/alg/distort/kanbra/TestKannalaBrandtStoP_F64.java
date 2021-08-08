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

package boofcv.alg.distort.kanbra;

import boofcv.struct.calib.CameraKannalaBrandt;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestKannalaBrandtStoP_F64 extends BoofStandardJUnit {
	/**
	 * pass in variable parameter lengths and see if bad stuff happens
	 */
	@Test void doesItBlowUp() {
		List<CameraKannalaBrandt> models = new ArrayList<>();

		models.add(new CameraKannalaBrandt().fsetK(500, 550, 0.0, 600, 650));
		models.add(new CameraKannalaBrandt().fsetK(500, 550, 0.1, 600, 650));
		models.add(new CameraKannalaBrandt().fsetK(500, 550, 0.0, 600, 650).fsetSymmetric(1.0,0.1));
		models.add(new CameraKannalaBrandt().fsetK(500, 550, 0.0, 600, 650).fsetRadial(1.0,0.1));
		models.add(new CameraKannalaBrandt().fsetK(500, 550, 0.0, 600, 650).fsetTangent(1.0,0.1));
		models.add(new CameraKannalaBrandt().fsetK(500, 550, 0.0, 600, 650).
				fsetSymmetric(1.0,0.1).fsetRadial(1.0,0.1).fsetTangent(1.0,0.1));


		Point3D_F64 P3 = new Point3D_F64(0.1, -0.05, 0.8);
		Point2D_F64 pixel = new Point2D_F64();

		for (CameraKannalaBrandt camera : models) {
			new KannalaBrandtStoP_F64(camera).compute(P3.x, P3.y, P3.z, pixel);
			assertFalse(UtilEjml.isUncountable(pixel.normSq()));
		}
	}

	/**
	 * Qualitative checks for when there's only symmetric distortion
	 */
	@Test void onlySymmetric() {
		// different symmetric coefficient that has a known behavior to the distortion
		CameraKannalaBrandt fish1 = new CameraKannalaBrandt().fsetK(500, 550, 0.0, 600, 650).fsetSymmetric(1.0,0.1);
		CameraKannalaBrandt fish2 = new CameraKannalaBrandt().fsetK(500, 550, 0.0, 600, 650).fsetSymmetric(1.0,0.4);

		Point2D_F64 pixel1 = new Point2D_F64();
		Point2D_F64 pixel2 = new Point2D_F64();

		// rotate around the circle. This should work in every direction
		for (int i = 0; i < 20; i++ ) {
			double theta = Math.PI*2.0*i/20.0;
			double r = 0.5;
			double x = r * Math.cos(theta);
			double y = r * Math.sin(theta);

			Point3D_F64 P3 = new Point3D_F64(x, y, 0.8);

			new KannalaBrandtStoP_F64(fish1).compute(P3.x, P3.y, P3.z, pixel1);
			new KannalaBrandtStoP_F64(fish2).compute(P3.x, P3.y, P3.z, pixel2);

			pixel1.x -= (double) fish1.cx;
			pixel1.y -= (double) fish1.cy;

			pixel2.x -= (double) fish2.cx;
			pixel2.y -= (double) fish2.cy;

			// NOTE: The norm changes because fx != fy
//			System.out.printf("angle=%.3f (%.2f %.2f) n=%.2f %.2f\n", theta, x,y,pixel1.norm(), pixel2.norm());

			// larger positive coefficients should push points out farther in the radial direction
			assertTrue(pixel1.norm() < pixel2.norm());
		}
	}
}
