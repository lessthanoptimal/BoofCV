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

package boofcv.alg.feature.associate;

import boofcv.alg.sfm.SfmTestHelper;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.calib.StereoParameters;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestStereoConsistencyCheck {

	@Test
	public void checkRectification() {
		Se3_F64 leftToRight = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.01, -0.001, 0.005, leftToRight.getR());
		leftToRight.getT().set(-0.1,0,0);

		StereoParameters param = new StereoParameters();
		param.rightToLeft = leftToRight.invert(null);

		param.left = new CameraPinholeRadial(400,500,0.1,160,120,320,240).fsetRadial(0,0);
		param.right = new CameraPinholeRadial(380,505,0.05,165,115,320,240).fsetRadial(0,0);

		Point3D_F64 X = new Point3D_F64(0.02,-0.5,3);

		Point2D_F64 leftP = new Point2D_F64();
		Point2D_F64 rightP = new Point2D_F64();
		SfmTestHelper.renderPointPixel(param, X, leftP, rightP);

		StereoConsistencyCheck alg = new StereoConsistencyCheck(1,2);
		alg.setCalibration(param);
		alg.checkPixel(leftP,rightP);

		assertEquals(alg.rectLeft.y,alg.rectRight.y,1e-5);
		assertTrue(alg.rectLeft.x > alg.rectRight.x);
	}

	@Test
	public void checkRectified() {

		StereoConsistencyCheck alg = new StereoConsistencyCheck(1,2);

		assertTrue(alg.checkRectified(new Point2D_F64(10,2),new Point2D_F64(5,2)));
		assertTrue(alg.checkRectified(new Point2D_F64(4,4),new Point2D_F64(5,2)));
		assertTrue(alg.checkRectified(new Point2D_F64(10, 0), new Point2D_F64(5, 2)));
		assertFalse(alg.checkRectified(new Point2D_F64(10, 4.0001), new Point2D_F64(5, 2)));
		assertFalse(alg.checkRectified(new Point2D_F64(10, -0.0001), new Point2D_F64(5, 2)));
		assertFalse(alg.checkRectified(new Point2D_F64(3.9999, 2), new Point2D_F64(5, 2)));
	}
}
