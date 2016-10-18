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

import boofcv.abst.geo.optimization.ResidualsCodecToMatrix;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.geo.Point2D3D;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import org.ddogleg.optimization.DerivativeChecker;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPnPJacobianRodrigues {

	Random rand = new Random(48854);
	int numPoints = 3;

	PnPRodriguesCodec codec = new PnPRodriguesCodec();
	ResidualsCodecToMatrix<Se3_F64,Point2D3D> func =
			new ResidualsCodecToMatrix<>(codec, new PnPResidualReprojection(), new Se3_F64());

	/**
	 * Compare to numerical differentiation
	 */
	@Test
	public void compareToNumerical() {
		compareToNumerical(0);
		compareToNumerical(0.1);
	}

	private void compareToNumerical(double noise) {

		Se3_F64 worldToCamera = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.1, 1, -0.2, worldToCamera.getR());
		worldToCamera.getT().set(-0.3,0.4,1);

		List<Point2D3D> observations = new ArrayList<>();

		for( int i = 0; i < numPoints; i++ ) {
			Point2D3D p = new Point2D3D();

			p.location.set( rand.nextGaussian()*0.1,
					rand.nextGaussian()*0.2 , 3 + rand.nextGaussian() );

			p.observation = PerspectiveOps.renderPixel(worldToCamera, null, p.location);

			p.observation.x += rand.nextGaussian()*noise;
			p.observation.y += rand.nextGaussian()*noise;

			observations.add(p);
		}

		PnPJacobianRodrigues alg = new PnPJacobianRodrigues();
		alg.setObservations(observations);
		func.setObservations(observations);

		double []param = new double[ codec.getParamLength() ];

		codec.encode(worldToCamera,param);

//		DerivativeChecker.jacobianPrint(func, alg, param, 1e-6);
		assertTrue(DerivativeChecker.jacobian(func, alg, param, 1e-6));
	}
}
