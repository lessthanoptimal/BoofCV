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

package boofcv.abst.geo.calibration;

import boofcv.abst.fiducial.calib.CalibrationDetectorSquareGrid;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCalibrateStereoPlanar extends BoofStandardJUnit {
	CameraPinholeBrown intrinsic = new CameraPinholeBrown(250,260,0,320,240,640,480).
			fsetRadial(0.01, -0.02).fsetTangental(0.03,0.03);
	Point2Transform2_F64 normToPixel = LensDistortionFactory.narrow(intrinsic).distort_F64(false, true);

	List<Se3_F64> targetToLeft = new ArrayList<>();

	Se3_F64 leftToRight = new Se3_F64();

	List<Point2D_F64> layout = CalibrationDetectorSquareGrid.createLayout(4, 3, 30, 30);

	public TestCalibrateStereoPlanar() {
		double z = 250;
		double w = 40;

		targetToLeft.add(SpecialEuclideanOps_F64.eulerXyz(0,0,z,0,0,0,null));
		targetToLeft.add(SpecialEuclideanOps_F64.eulerXyz(0,0,z*0.7,0.01,-0.05,0,null));
		targetToLeft.add(SpecialEuclideanOps_F64.eulerXyz(w,0,z,0.15,0,0,null));
		targetToLeft.add(SpecialEuclideanOps_F64.eulerXyz(w,w,z,0,0.1,0,null));
		targetToLeft.add(SpecialEuclideanOps_F64.eulerXyz(w,w,z*0.8,-0.1,0,0.05,null));
		targetToLeft.add(SpecialEuclideanOps_F64.eulerXyz(0,-w,z,0,0,0.15,null));
		targetToLeft.add(SpecialEuclideanOps_F64.eulerXyz(0,-w,z,0,-0.1,0.1,null));

		leftToRight.getT().setTo(100,0,0);
	}

	/**
	 * Give it a fake feature detector and a fairly benign scenario and see if it can correctly
	 * estimate the camera parameters.
	 */
	@Test void fullBasic() {

		CalibrateStereoPlanar alg = new CalibrateStereoPlanar(layout);
		alg.configure(true, 2, true);

		for (int i = 0; i < targetToLeft.size(); i++) {
			alg.addPair(createFakeObservations(i,true),createFakeObservations(i,false));
		}

		StereoParameters found = alg.process();

		checkIntrinsic(found.left);
		checkIntrinsic(found.right);
		Se3_F64 rightToLeft = found.getRightToLeft();
		Se3_F64 expected = leftToRight.invert(null);

		assertEquals(0,expected.getT().distance(rightToLeft.T),Math.abs(rightToLeft.T.x)*0.01);
		assertTrue(MatrixFeatures_DDRM.isIdentity(rightToLeft.getR(), 2e-3));
	}

	private void checkIntrinsic(CameraPinholeBrown found) {
		assertEquals(intrinsic.fx,found.fx,intrinsic.width*1e-3);
		assertEquals(intrinsic.fy,found.fy,intrinsic.width*1e-3);
		assertEquals(intrinsic.cx,found.cx,intrinsic.width*1e-3);
		assertEquals(intrinsic.cy,found.cy,intrinsic.width*1e-3);
		assertEquals(intrinsic.skew,found.skew,intrinsic.width*1e-3);

		assertEquals(intrinsic.radial[0],found.radial[0],1e-3);
		assertEquals(intrinsic.radial[1],found.radial[1],1e-3);

		assertEquals(intrinsic.t1,found.t1,1e-4);
		assertEquals(intrinsic.t2,found.t2,1e-4);

		assertEquals(intrinsic.width,found.width,1e-3);
		assertEquals(intrinsic.height,found.height,1e-3);
	}

	private CalibrationObservation createFakeObservations( int which , boolean left ) {
		Se3_F64 t2l = targetToLeft.get(which);
		Se3_F64 t2c;

		if( left ) {
			t2c = t2l;
		} else {
			t2c = new Se3_F64();
			t2l.concat(leftToRight,t2c);
		}

		CalibrationObservation set = new CalibrationObservation(intrinsic.width,intrinsic.height);

		for( int i = 0; i < layout.size(); i++ ) {
			Point2D_F64 p2 = layout.get(i);
			// location of calibration point on the target
			Point3D_F64 p3 = new Point3D_F64(p2.x,p2.y,0);

			Point3D_F64 a = SePointOps_F64.transform(t2c,p3,null);

			Point2D_F64 pixel = new Point2D_F64();
			normToPixel.compute(a.x / a.z, a.y / a.z, pixel);

			if( pixel.x < 0 || pixel.x >= intrinsic.width-1 || pixel.y < 0 || pixel.y >= intrinsic.height-1 )
				throw new RuntimeException("Adjust test setup, bad observation");

			set.add(pixel,i);
		}

		return set;
	}
}
