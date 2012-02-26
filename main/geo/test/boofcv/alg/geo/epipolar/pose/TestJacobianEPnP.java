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

package boofcv.alg.geo.epipolar.pose;

import boofcv.numerics.optimization.JacobianChecker;
import georegression.struct.point.Point3D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestJacobianEPnP {
	
	Random rand = new Random(234);

	@Test
	public void numericalCheck() {

		List<Point3D_F64> worldPts = new ArrayList<Point3D_F64>();
		List<Point3D_F64 > nullPts[] = new ArrayList[4];
		
		worldPts.add( new Point3D_F64(0,0,0));
		worldPts.add( new Point3D_F64(1,0,0));
		worldPts.add( new Point3D_F64(0,2,0));
		worldPts.add( new Point3D_F64(0,0,3));
		
		for( int i = 0; i < 4; i++ ) {
			nullPts[i] = new ArrayList<Point3D_F64>();
			for( int j = 0; j < 4; j++ ) {
				nullPts[i].add( new Point3D_F64(rand.nextDouble(),rand.nextDouble(),rand.nextGaussian()));
			}
		}


		JacobianEPnP jacobian = new JacobianEPnP();
		ResidualsEPnP residuals = new ResidualsEPnP();

		residuals.setParameters(worldPts, nullPts);
		jacobian.setUp(residuals);

		boolean worked = JacobianChecker.jacobian(residuals,jacobian,new double[]{1,2,3,4},1e-6);
		assertTrue(worked);
	}
}
