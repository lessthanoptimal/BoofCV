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
import boofcv.struct.distort.Point2Transform3_F32;
import georegression.geometry.ConvertRotation3D_F32;
import georegression.geometry.UtilVector3D_F32;
import georegression.misc.GrlConstants;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Vector3D_F32;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestNarrowToWidePtoP_F32 {
	/**
	 * With no translation request a point in the center.  Should appear to be in the center in both views.
	 */
	@Test
	public void centerIsCenter() {
		NarrowToWidePtoP_F32 alg = createAlg();

		Point2D_F32 found = new Point2D_F32();
		alg.compute(250,250,found);

		assertEquals(480,found.x, GrlConstants.FLOAT_TEST_TOL_SQRT);
		assertEquals(480,found.y, GrlConstants.FLOAT_TEST_TOL_SQRT);
	}

	/**
	 * Rotate the camera and see if the point moves in the expected way
	 */
	@Test
	public void rotateCamera() {
		NarrowToWidePtoP_F32 alg = createAlg();
		Point2D_F32 found = new Point2D_F32();

		DenseMatrix64F R = ConvertRotation3D_F32.eulerToMatrix(EulerType.YXZ,0.1f,0,0,null);
		alg.setRotationWideToNarrow(R);
		alg.compute(250,250,found);
		assertTrue(480<found.x - 5);

		R = ConvertRotation3D_F32.eulerToMatrix(EulerType.YXZ,-0.1f,0,0,null);
		alg.setRotationWideToNarrow(R);
		alg.compute(250,250,found);
		assertTrue(480>found.x + 5);

		R = ConvertRotation3D_F32.eulerToMatrix(EulerType.YXZ,0,-0.1f,0,null);
		alg.setRotationWideToNarrow(R);
		alg.compute(250,250,found);
		assertTrue(480<found.y - 5);

		R = ConvertRotation3D_F32.eulerToMatrix(EulerType.YXZ,0,0.1f,0,null);
		alg.setRotationWideToNarrow(R);
		alg.compute(250,250,found);
		assertTrue(480>found.y + 5);
	}

	/**
	 * Request points at the border and see if it has the expected vertical and horizontal FOV
	 */
	@Test
	public void checkFOVBounds() {
		NarrowToWidePtoP_F32 alg = createAlg();

		Point2D_F32 foundA = new Point2D_F32();
		Point2D_F32 foundB = new Point2D_F32();

		Point3D_F32 vA = new Point3D_F32();
		Point3D_F32 vB = new Point3D_F32();

		// Compute the horizontal FOV
		alg.compute(0,250,foundA);
		alg.compute(500,250,foundB);

		Point2Transform3_F32 wideToSphere = createModelWide().undistortPtoS_F32();
		wideToSphere.compute(foundA.x,foundA.y,vA);
		wideToSphere.compute(foundB.x,foundB.y,vB);

		float found = UtilVector3D_F32.acute(new Vector3D_F32(vA),new Vector3D_F32(vB));
		float expected = 2.0f * (float)Math.atan(250.0f/400.0f);

		assertEquals(expected,found,0.01f);

		// Compute the vertical FOV
		alg.compute(250,0,foundA);
		alg.compute(250,500,foundB);

		wideToSphere.compute(foundA.x,foundA.y,vA);
		wideToSphere.compute(foundB.x,foundB.y,vB);

		found = UtilVector3D_F32.acute(new Vector3D_F32(vA),new Vector3D_F32(vB));
		expected = 2.0f * (float)Math.atan(250.0f/400.0f);

		assertEquals(expected,found,0.001f);
	}

	public static NarrowToWidePtoP_F32 createAlg() {
		return new NarrowToWidePtoP_F32(createModelNarrow(), createModelWide());
	}

	public static LensDistortionWideFOV createModelWide() {
		CameraUniversalOmni model = new CameraUniversalOmni(2);

		model.fsetK(1.349e3f,1.343e3f,0,480,480,960,1080);
		model.fsetMirror(3.61f);
		model.fsetRadial(7.308e-1f,1.855e1f);
		model.fsetTangental(-1.288e-2f,-1.1342e-2f);
		return new LensDistortionUniversalOmni(model);
	}

	public static LensDistortionNarrowFOV createModelNarrow() {
		CameraPinhole model = new CameraPinhole();
		model.fsetK(400,400,0,250,250,500,500);
		return new LensDistortionPinhole(model);
	}
}