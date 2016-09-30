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

package boofcv.alg.geo.pose;

import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.sfm.StereoPose;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPnPStereoResidualReprojection extends CommonStereoMotionNPoint {

	@Test
	public void basicTest() {
		Se3_F64 worldToLeft = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.1, 1, -0.2, worldToLeft.getR());
		worldToLeft.getT().set(-0.3, 0.4, 1);

		generateScene(10,worldToLeft,false);

		PnPStereoResidualReprojection alg = new PnPStereoResidualReprojection();
		alg.setModel(new StereoPose(worldToLeft,leftToRight));

		// compute errors with perfect model
		double error[] = new double[ alg.getN() ];
		int index = alg.computeResiduals(pointPose.get(0),error,0);
		assertEquals(alg.getN(), index);

		assertEquals(0,error[0],1e-8);
		assertEquals(0,error[1],1e-8);

		// compute errors with an incorrect model
		worldToLeft.getR().set(2,1,2);
		alg.setModel(new StereoPose(worldToLeft,leftToRight));
		index = alg.computeResiduals(pointPose.get(0),error,0);
		assertEquals(alg.getN(), index);

		assertTrue(Math.abs(error[0]) > 1e-8);
		assertTrue(Math.abs(error[1]) > 1e-8);
	}

	@Test
	public void compareToReprojection() {
		Se3_F64 worldToLeft = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.1, 1, -0.2, worldToLeft.getR());
		worldToLeft.getT().set(-0.3, 0.4, 1);

		generateScene(10,worldToLeft,false);

		// make the input model incorrect
		worldToLeft.getR().set(2,1,2);

		// compute the error in normalized image coordinates per element
		PnPStereoResidualReprojection alg = new PnPStereoResidualReprojection();
		alg.setModel(new StereoPose(worldToLeft,leftToRight));

		// compute errors with perfect model
		double error[] = new double[ alg.getN() ];
		alg.computeResiduals(pointPose.get(0),error,0);

		double found = 0;
		for( double e : error ) {
			found += e*e;
		}

		PnPStereoDistanceReprojectionSq validation = new PnPStereoDistanceReprojectionSq();
		StereoParameters param = new StereoParameters();
		param.rightToLeft = this.param.rightToLeft;
		// intrinsic parameters are configured to be identical to normalized image coordinates
		param.left = new CameraPinholeRadial(1,1,0,0,0,0,0).fsetRadial(0,0);
		param.right = new CameraPinholeRadial(1,1,0,0,0,0,0).fsetRadial(0,0);
		validation.setStereoParameters(param);
		validation.setModel(worldToLeft);
		double expected = validation.computeDistance(pointPose.get(0));

		assertEquals(expected,found,1e-8);
	}
}
