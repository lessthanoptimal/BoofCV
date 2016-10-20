/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort;

import boofcv.struct.distort.Point2Transform3_F32;
import boofcv.struct.distort.Point2Transform3_F64;
import boofcv.struct.distort.Point3Transform2_F32;
import boofcv.struct.distort.Point3Transform2_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import org.ejml.UtilEjml;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GeneralLensDistortionWideFOVChecks {
	protected float pixel_tol_F32 = 1e-1f;
	protected double pixel_tol_F64 = 1e-2;

	public abstract LensDistortionWideFOV create();

	@Test
	public void pixel_unit_pixel_F32() {
		LensDistortionWideFOV alg = create();

		Point2Transform3_F32 undistort = alg.undistortPtoS_F32();
		Point3Transform2_F32 distort = alg.distortStoP_F32();

		Point3D_F32 middle = new Point3D_F32();
		Point2D_F32 found = new Point2D_F32();

		undistort.compute(240,260,middle);
		distort.compute(middle.x,middle.y,middle.z,found);

		assertEquals(240, found.x, pixel_tol_F32);
		assertEquals(260, found.y, pixel_tol_F32);
	}

	@Test
	public void pixel_unit_pixel_F64() {
		LensDistortionWideFOV alg = create();

		Point2Transform3_F64 undistort = alg.undistortPtoS_F64();
		Point3Transform2_F64 distort = alg.distortStoP_F64();

		Point3D_F64 middle = new Point3D_F64();
		Point2D_F64 found = new Point2D_F64();

		undistort.compute(240,260,middle);
		distort.compute(middle.x,middle.y,middle.z,found);

		assertEquals(240, found.x, pixel_tol_F64);
		assertEquals(260, found.y, pixel_tol_F64);
	}

	/**
	 * Give it spherical coordinate pointing slightly behind.  See if it blows up when converting into pixels
	 */
	@Test
	public void blowup_extreme_angle_F32() {
		LensDistortionWideFOV alg = create();
		Point3Transform2_F32 distort = alg.distortStoP_F32();

		Point2D_F32 found = new Point2D_F32();

		float x = 1.0f;
		float z = -0.001f;
		float r = (float)Math.sqrt(x*x + z*z);

		distort.compute(x/r,0,z/x,found);

		assertTrue( !UtilEjml.isUncountable(found.x) );
		assertTrue( !UtilEjml.isUncountable(found.y) );
	}

	/**
	 * Give it spherical coordinate pointing slightly behind.  See if it blows up when converting into pixels
	 */
	@Test
	public void blowup_extreme_angle_F64() {
		LensDistortionWideFOV alg = create();
		Point3Transform2_F64 distort = alg.distortStoP_F64();

		Point2D_F64 found = new Point2D_F64();

		double x = 1.0;
		double z = -0.001;
		double r = Math.sqrt(x*x + z*z);

		distort.compute(x/r,0,z/x,found);

		assertTrue( !UtilEjml.isUncountable(found.x) );
		assertTrue( !UtilEjml.isUncountable(found.y) );
	}
}
