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

package boofcv.alg.geo.pose;

import boofcv.abst.geo.optimization.ResidualsCodecToMatrix;
import boofcv.struct.sfm.Stereo2D3D;
import boofcv.struct.sfm.StereoPose;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.se.Se3_F64;
import org.ddogleg.optimization.JacobianChecker;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPnPStereoJacobianRodrigues extends CommonStereoMotionNPoint {

	int numPoints = 3;

	Se3ToStereoPoseCodec codec = new Se3ToStereoPoseCodec(new PnPRodriguesCodec());

	/**
	 * Compare to numerical differentiation
	 */
	@Test
	public void compareToNumerical() {
		compareToNumerical(0);
		compareToNumerical(0.1);
	}

	private void compareToNumerical(double noise) {

		Se3_F64 worldToLeft = new Se3_F64();
		RotationMatrixGenerator.eulerXYZ(0.1, 1, -0.2, worldToLeft.getR());
		worldToLeft.getT().set(-0.3,0.4,1);

		generateScene(numPoints,worldToLeft,false);
		addNoise(noise);


		PnPStereoJacobianRodrigues alg = new PnPStereoJacobianRodrigues();
		alg.setLeftToRight(leftToRight);
		alg.setObservations(pointPose);

		StereoPose storage = new StereoPose(new Se3_F64(),leftToRight);
		ResidualsCodecToMatrix<StereoPose,Stereo2D3D> func =
				new ResidualsCodecToMatrix<StereoPose,Stereo2D3D>
						(codec,new PnPStereoResidualReprojection(),storage);

		func.setObservations(pointPose);

		StereoPose pose = new StereoPose(worldToLeft,leftToRight);
		double []param = new double[ codec.getParamLength() ];

		codec.encode(pose,param);

//		JacobianChecker.jacobianPrint(func,alg,param,1e-6);
		assertTrue(JacobianChecker.jacobian(func, alg, param, 1e-6));
	}
}
