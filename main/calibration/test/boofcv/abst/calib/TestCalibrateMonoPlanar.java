/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.calib;

import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestCalibrateMonoPlanar {

	List<Se3_F64> pose = new ArrayList<Se3_F64>();
	PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridChess(3, 4, 30);
	DenseMatrix64F K = new DenseMatrix64F(3,3,true,300,0,250,0,350,400,0,0,1);
	int width = 500;
	int height = 600;

	public TestCalibrateMonoPlanar() {
		double z = -250;
		double w = 40;

		pose.add( new Se3_F64(RotationMatrixGenerator.eulerXYZ(0,0,0,null),new Vector3D_F64(0,0,z)));
		pose.add( new Se3_F64(RotationMatrixGenerator.eulerXYZ(0.1,0,0,null),new Vector3D_F64(w,0,z)));
		pose.add( new Se3_F64(RotationMatrixGenerator.eulerXYZ(0,0.1,0,null),new Vector3D_F64(w,w,z)));
		pose.add( new Se3_F64(RotationMatrixGenerator.eulerXYZ(0,0,0.1,null),new Vector3D_F64(0,-w,z)));
		pose.add( new Se3_F64(RotationMatrixGenerator.eulerXYZ(0.05,0,0.1,null),new Vector3D_F64(0,-w,z)));

	}

	/**
	 * Calibrates the images once by manually flipping the images and another time by setting flipY
	 * to true.  Results should be identical
	 */
	@Test
	public void testFlipY() {
		IntrinsicParameters foundManual = computeIntrinsic(true);
		IntrinsicParameters foundOther = computeIntrinsic(false);

		assertEquals(foundManual.cx,foundOther.cx,1e-8);
		assertEquals(foundManual.cy,foundOther.cy,1e-8);
		assertEquals(foundManual.fx,foundOther.fx,1e-8);
		assertEquals(foundManual.fy,foundOther.fy,1e-8);
	}

	private IntrinsicParameters computeIntrinsic(boolean manualFlip) {
		PlanarCalibrationDetector detector = new FakeDetector(manualFlip);

		CalibrateMonoPlanar alg = new CalibrateMonoPlanar(detector,!manualFlip);

		alg.configure(target,false,2);

		for( int i = 0; i < pose.size(); i++ )
			alg.addImage(new ImageFloat32(width,height));

		return alg.process();
	}

	private class FakeDetector implements PlanarCalibrationDetector {

		boolean manualFlip;
		int count = 0;

		List<Point2D_F64> obs;

		private FakeDetector(boolean manualFlip) {
			this.manualFlip = manualFlip;
		}

		@Override
		public boolean process(ImageFloat32 input) {

			Se3_F64 se = pose.get(count++);

			obs = new ArrayList<Point2D_F64>();

			for( Point2D_F64 p2 : target.points ) {
				Point3D_F64 p3 = new Point3D_F64(p2.x,p2.y,0);

				Point3D_F64 a = SePointOps_F64.transform(se,p3,null);

				Point2D_F64 pixel = new Point2D_F64();

				// to normalized image coordinates
				pixel.x = a.x/a.z;
				pixel.y = a.y/a.z;

				// to pixels
				GeometryMath_F64.mult(K,pixel,pixel);

				if( pixel.x < 0 || pixel.x >= width || pixel.y < 0 || pixel.y >= height )
					throw new RuntimeException("Adjust test setup, bad observation");

				if( manualFlip ) {
					pixel.y = height - pixel.y - 1;
				}

				obs.add(pixel);
			}


			return true;
		}

		@Override
		public List<Point2D_F64> getPoints() {
			return obs;
		}
	}
}
