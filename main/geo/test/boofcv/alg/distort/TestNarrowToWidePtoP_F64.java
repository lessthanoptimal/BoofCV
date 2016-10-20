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

import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.distort.universal.LensDistortionUniversalOmni;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.distort.Point2Transform3_F64;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.UtilVector3D_F64;
import georegression.misc.GrlConstants;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestNarrowToWidePtoP_F64 {
	/**
	 * With no translation request a point in the center.  Should appear to be in the center in both views.
	 */
	@Test
	public void centerIsCenter() {
		NarrowToWidePtoP_F64 alg = createAlg();

		Point2D_F64 found = new Point2D_F64();
		alg.compute(250,250,found);

		assertEquals(480,found.x, GrlConstants.DOUBLE_TEST_TOL_SQRT);
		assertEquals(480,found.y, GrlConstants.DOUBLE_TEST_TOL_SQRT);
	}

	/**
	 * Rotate the camera and see if the point moves in the expected way
	 */
	@Test
	public void rotateCamera() {
		NarrowToWidePtoP_F64 alg = createAlg();
		Point2D_F64 found = new Point2D_F64();

		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.YXZ,0.1,0,0,null);
		alg.setRotationWideToNarrow(R);
		alg.compute(250,250,found);
		assertTrue(480<found.x - 5);

		R = ConvertRotation3D_F64.eulerToMatrix(EulerType.YXZ,-0.1,0,0,null);
		alg.setRotationWideToNarrow(R);
		alg.compute(250,250,found);
		assertTrue(480>found.x + 5);

		R = ConvertRotation3D_F64.eulerToMatrix(EulerType.YXZ,0,-0.1,0,null);
		alg.setRotationWideToNarrow(R);
		alg.compute(250,250,found);
		assertTrue(480<found.y - 5);

		R = ConvertRotation3D_F64.eulerToMatrix(EulerType.YXZ,0,0.1,0,null);
		alg.setRotationWideToNarrow(R);
		alg.compute(250,250,found);
		assertTrue(480>found.y + 5);
	}

	/**
	 * Request points at the border and see if it has the expected vertical and horizontal FOV
	 */
	@Test
	public void checkFOVBounds() {
		NarrowToWidePtoP_F64 alg = createAlg();

		Point2D_F64 foundA = new Point2D_F64();
		Point2D_F64 foundB = new Point2D_F64();

		Point3D_F64 vA = new Point3D_F64();
		Point3D_F64 vB = new Point3D_F64();

		// Compute the horizontal FOV
		alg.compute(0,250,foundA);
		alg.compute(500,250,foundB);

		Point2Transform3_F64 wideToSphere = createModelWide().undistortPtoS_F64();
		wideToSphere.compute(foundA.x,foundA.y,vA);
		wideToSphere.compute(foundB.x,foundB.y,vB);

		double found = UtilVector3D_F64.acute(new Vector3D_F64(vA),new Vector3D_F64(vB));
		double expected = 2.0 * Math.atan(250.0/400.0);

		assertEquals(expected,found,0.01);

		// Compute the vertical FOV
		alg.compute(250,0,foundA);
		alg.compute(250,500,foundB);

		wideToSphere.compute(foundA.x,foundA.y,vA);
		wideToSphere.compute(foundB.x,foundB.y,vB);

		found = UtilVector3D_F64.acute(new Vector3D_F64(vA),new Vector3D_F64(vB));
		expected = 2.0 * Math.atan(250.0/400.0);

		assertEquals(expected,found,0.001);
	}

	public static NarrowToWidePtoP_F64 createAlg() {
		return new NarrowToWidePtoP_F64(createModelNarrow(), createModelWide());
	}

	public static LensDistortionWideFOV createModelWide() {
		CameraUniversalOmni model = new CameraUniversalOmni(2);

		model.fsetK(1.349e3,1.343e3,0,480,480,960,1080);
		model.fsetMirror(3.61);
		model.fsetRadial(7.308e-1,1.855e1);
		model.fsetTangental(-1.288e-2,-1.1342e-2);
		return new LensDistortionUniversalOmni(model);
	}

	public static LensDistortionNarrowFOV createModelNarrow() {
		CameraPinhole model = new CameraPinhole();
		model.fsetK(400,400,0,250,250,500,500);
		return new LensDistortionPinhole(model);
	}
}