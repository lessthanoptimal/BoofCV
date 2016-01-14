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

import boofcv.struct.geo.Point2D3D;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPnPResidualReprojection {
	
	@Test
	public void basicTest() {
		Se3_F64 motion = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.1,1,-0.2,motion.getR());
		motion.getT().set(-0.3,0.4,1);

		Point3D_F64 world = new Point3D_F64(0.5,-0.5,3);
		Point2D_F64 obs = new Point2D_F64();
		
		Point3D_F64 temp = SePointOps_F64.transform(motion,world,null);
		obs.x = temp.x/temp.z;
		obs.y = temp.y/temp.z;

		PnPResidualReprojection alg = new PnPResidualReprojection();
		
		// compute errors with perfect model
		double error[] = new double[ alg.getN() ];
		alg.setModel(motion);
		int index = alg.computeResiduals(new Point2D3D(obs,world),error,0);
		assertEquals(alg.getN(), index);
		
		assertEquals(0,error[0],1e-8);
		assertEquals(0,error[1],1e-8);
		
		// compute errors with an incorrect model
		motion.getR().set(2,1,2);
		alg.setModel(motion);
		index = alg.computeResiduals(new Point2D3D(obs,world),error,0);
		assertEquals(alg.getN(), index);

		assertTrue(Math.abs(error[0]) > 1e-8);
		assertTrue(Math.abs(error[1]) > 1e-8);
	}
}
