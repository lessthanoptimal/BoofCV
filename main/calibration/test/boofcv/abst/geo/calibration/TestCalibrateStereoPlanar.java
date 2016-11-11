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

package boofcv.abst.geo.calibration;

import boofcv.abst.fiducial.calib.CalibrationDetectorSquareGrid;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayF32;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCalibrateStereoPlanar {
	CameraPinholeRadial intrinsic = new CameraPinholeRadial(200,210,0,320,240,640,480).
			fsetRadial(0.01, -0.02).fsetTangental(0.03,0.03);
	Point2Transform2_F64 normToPixel = LensDistortionOps.transformPoint(intrinsic).distort_F64(false, true);

	GrayF32 blank = new GrayF32(intrinsic.width,intrinsic.height);

	List<Se3_F64> targetToLeft = new ArrayList<>();

	Se3_F64 leftToRight = new Se3_F64();

	public TestCalibrateStereoPlanar() {
		double z = 250;
		double w = 40;

		targetToLeft.add(new Se3_F64(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0, 0, 0, null), new Vector3D_F64(0, 0, z)));
		targetToLeft.add(new Se3_F64(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.1, 0, 0, null), new Vector3D_F64(w, 0, z)));
		targetToLeft.add(new Se3_F64(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0, 0.1, 0, null), new Vector3D_F64(w, w, z)));
		targetToLeft.add(new Se3_F64(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0, 0, 0.1, null), new Vector3D_F64(0, -w, z)));
		targetToLeft.add(new Se3_F64(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.05, 0, 0.1, null), new Vector3D_F64(0, -w, z)));

		leftToRight.getT().set(100,0,0);
	}

	/**
	 * Give it a fake feature detector and a fairly benign scenario and see if it can correctly
	 * estimate the camera parameters.
	 */
	@Test
	public void fullBasic() {

		FakeDetector detector = new FakeDetector();
		CalibrateStereoPlanar alg = new CalibrateStereoPlanar(detector);
		alg.configure(true, 2, true);

		for (int i = 0; i < targetToLeft.size(); i++) {
			alg.addPair(blank,blank);
		}

		StereoParameters found = alg.process();

		checkIntrinsic(found.left);
		checkIntrinsic(found.right);
		Se3_F64 rightToLeft = found.getRightToLeft();
		Se3_F64 expected = leftToRight.invert(null);

		assertEquals(0,expected.getT().distance(rightToLeft.T),1.01e-3);
		assertTrue(MatrixFeatures.isIdentity(rightToLeft.getR(), 1e-3));
	}

	private void checkIntrinsic(CameraPinholeRadial found) {
		assertEquals(intrinsic.fx,found.fx,1e-3);
		assertEquals(intrinsic.fy,found.fy,1e-3);
		assertEquals(intrinsic.cx,found.cx,1e-3);
		assertEquals(intrinsic.cy,found.cy,1e-3);
		assertEquals(intrinsic.skew,found.skew,1e-3);

		assertEquals(intrinsic.radial[0],found.radial[0],1e-5);
		assertEquals(intrinsic.radial[1],found.radial[1],1e-5);

		assertEquals(intrinsic.t1,found.t1,1e-5);
		assertEquals(intrinsic.t2,found.t2,1e-5);
	}

	private class FakeDetector implements DetectorFiducialCalibration {

		int count = 0;

		CalibrationObservation obs;

		List<Point2D_F64> layout = CalibrationDetectorSquareGrid.createLayout(4, 3, 30, 30);

		@Override
		public boolean process(GrayF32 input) {

			int location = count/2;
			boolean left = count%2 == 0;
			count += 1;

			Se3_F64 t2l = targetToLeft.get(location);
			Se3_F64 t2c = new Se3_F64();
			if( left ) {
				t2c.set(t2l);
			} else {
				t2l.concat(leftToRight,t2c);
			}

			obs = new CalibrationObservation();

			for( int i = 0; i < layout.size(); i++ ) {
				Point2D_F64 p2 = layout.get(i);

				// location of calibration point on the target
				Point3D_F64 p3 = new Point3D_F64(p2.x,p2.y,0);

				Point3D_F64 a = SePointOps_F64.transform(t2c, p3, null);

				Point2D_F64 pixel = new Point2D_F64();
				normToPixel.compute(a.x / a.z, a.y / a.z, pixel);

				if( pixel.x < 0 || pixel.x >= intrinsic.width-1 || pixel.y < 0 || pixel.y >= input.height-1 )
					throw new RuntimeException("Adjust test setup, bad observation");

				obs.add(pixel, i);
			}

			return true;
		}

		@Override
		public CalibrationObservation getDetectedPoints() {
			return obs;
		}

		@Override
		public List<Point2D_F64> getLayout() {
			return layout;
		}
	}
}