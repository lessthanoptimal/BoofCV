/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.optimization.ResidualsPoseMatrix;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.numerics.optimization.JacobianChecker;
import boofcv.struct.geo.PointPosePair;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.se.Se3_F64;
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
	ResidualsPoseMatrix func = new ResidualsPoseMatrix(codec,new PnPResidualReprojection());

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
		RotationMatrixGenerator.eulerXYZ(0.1, 1, -0.2, worldToCamera.getR());
		worldToCamera.getT().set(-0.3,0.4,1);

		List<PointPosePair> observations = new ArrayList<PointPosePair>();

		for( int i = 0; i < numPoints; i++ ) {
			PointPosePair p = new PointPosePair();

			p.location.set( rand.nextGaussian()*0.1,
					rand.nextGaussian()*0.2 , 3 + rand.nextGaussian() );

			p.observed = PerspectiveOps.renderPixel(worldToCamera, null, p.location);

			p.observed.x += rand.nextGaussian()*noise;
			p.observed.y += rand.nextGaussian()*noise;

			observations.add(p);
		}

		PnPJacobianRodrigues alg = new PnPJacobianRodrigues();
		alg.setObservations(observations);
		func.setObservations(observations);

		double []param = new double[ codec.getParamLength() ];

		codec.encode(worldToCamera,param);

		assertTrue(JacobianChecker.jacobian(func, alg, param, 1e-6));
	}
}
