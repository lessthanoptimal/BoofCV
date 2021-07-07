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

import boofcv.abst.disparity.StereoDisparitySparse;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
// TODO Pixel to norm. norm to rect, rect to pixel
public class TestStereoSparse3D extends BoofStandardJUnit {

	StereoParameters param = new StereoParameters();

	DMatrixRMaj K1, K2;

	public TestStereoSparse3D() {

		K1 = new DMatrixRMaj(3, 3, true, 150, 0, 310, 0, 180, 245, 0, 0, 1);
		K2 = new DMatrixRMaj(3, 3, true, 120, 0, 330, 0, 160, 220, 0, 0, 1);

		param.right_to_left = new Se3_F64();
		param.right_to_left.getT().setTo(0.2, 0, 0.1);
//		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.02,0.01,-0.03,param.rightToLeft.getR());

		param.left = PerspectiveOps.matrixToPinhole(K1, 640, 480, new CameraPinholeBrown());
		param.right = PerspectiveOps.matrixToPinhole(K2, 640, 480, new CameraPinholeBrown());
		param.left.radial = new double[2];
		param.right.radial = new double[2];
	}

	/**
	 * Provide perfect image processing and validate the geometry
	 */
	@Test void checkGeometry() {
		Dummy disparity = new Dummy();
		StereoSparse3D<GrayF32> alg = new StereoSparse3D<>(disparity, GrayF32.class);

		alg.setCalibration(param);

		Point3D_F64 X = new Point3D_F64(0.2, -0.34, 3);

		// original pixel coordinates
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(), K1, X, null);
		Point2D_F64 x2 = PerspectiveOps.renderPixel(param.right_to_left.invert(null), K2, X, null);

		Point2Transform2_F64 pixelToRect1 = RectifyImageOps.transformPixelToRect(param.left, alg.rect1);
		Point2Transform2_F64 pixelToRect2 = RectifyImageOps.transformPixelToRect(param.right, alg.rect2);

		// rectified coordinates
		Point2D_F64 r1 = new Point2D_F64();
		Point2D_F64 r2 = new Point2D_F64();
		pixelToRect1.compute(x1.x, x1.y, r1);
		pixelToRect2.compute(x2.x, x2.y, r2);

		// compute the true disparity
		disparity.d = r1.x - r2.x;

		assertTrue(alg.process(x1.x, x1.y));

		double x = alg.getX()/alg.getW();
		double y = alg.getY()/alg.getW();
		double z = alg.getZ()/alg.getW();

		assertEquals(X.x, x, 1e-8);
		assertEquals(X.y, y, 1e-8);
		assertEquals(X.z, z, 1e-8);
	}

	@SuppressWarnings("rawtypes")
	private static class Dummy implements StereoDisparitySparse {
		double d;

		@Override public void setImages( ImageGray imageLeft, ImageGray imageRight ) {}

		@Override public boolean process( int x, int y ) {return true;}

		@Override public double getDisparity() { return d; }

		@Override public int getBorderX() {return 0;}

		@Override public int getBorderY() {return 0;}

		@Override public int getMinDisparity() {return 0;}

		@Override public int getMaxDisparity() {return 200;}

		@Override public Class getInputType() {return null;}
	}
}
