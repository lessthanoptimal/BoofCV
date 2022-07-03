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

package boofcv.alg.geo.rectify;

import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDisparityParameters extends BoofStandardJUnit {
	@Test void setTo() {
		var src = new DisparityParameters();
		src.pinhole.fsetK(1,2,3,4,5,6,9);
		src.rotateToRectified.set(0,0,2);
		src.baseline = 10;
		src.disparityRange = 102;
		src.disparityMin = 8;

		var dst = new DisparityParameters();
		dst.setTo(src);

		assertTrue(src.pinhole.isEquals(dst.pinhole, 1e-8));
		assertTrue(MatrixFeatures_DDRM.isEquals(src.rotateToRectified, dst.rotateToRectified));
		assertEquals(src.baseline, dst.baseline);
		assertEquals(src.disparityRange, dst.disparityRange);
		assertEquals(src.disparityMin, dst.disparityMin);
	}

	@Test void pixelTo3D() {
		var param = new DisparityParameters();
		param.pinhole.fsetK(200,205,0.05,150,160,304,400);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.1, 0, 0, param.rotateToRectified);
		param.baseline = 0.5;
		param.disparityRange = 102;
		param.disparityMin = 8;

		var expected = new Point3D_F64(0.5, -0.24, 1.5);
		var work = new Point3D_F64();
		GeometryMath_F64.mult(param.rotateToRectified, expected, work);

		// Compute the disparity
		double d = param.baseline*param.pinhole.fx/work.z - param.disparityMin;

		// Hand compute the pixels
		double pixelX = work.x*param.pinhole.fx/work.z + param.pinhole.cx;
		double pixelY = work.y*param.pinhole.fy/work.z + param.pinhole.cy;

		// See if we get the expectred results
		var found = new Point3D_F64();
		assertTrue(param.pixelTo3D(pixelX, pixelY, d, found));

		assertEquals(0.0, found.distance(expected), 1e-6);
	}
}