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

package boofcv.alg.geo.robust;

import boofcv.struct.geo.Point2D3D;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDistanceTranGivenRotSq {

	DistanceTranGivenRotSq alg;

	public TestDistanceTranGivenRotSq() {
		alg = new DistanceTranGivenRotSq();
	}

	@Test
	public void testPerfect() {
		Se3_F64 keyToCurr = new Se3_F64();
		keyToCurr.getR().set(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.05, -0.03, 0.02,null));
		keyToCurr.getT().set(0.1,-0.1,0.01);

		Point3D_F64 X = new Point3D_F64(0.1,-0.05,3);

		Point2D3D obs = new Point2D3D();
		obs.location = X.copy();

		SePointOps_F64.transform(keyToCurr, X, X);

		obs.observation.x = X.x/X.z;
		obs.observation.y = X.y/X.z;

		alg.setRotation(keyToCurr.getR());
		alg.setModel(keyToCurr.getT());
		assertEquals(0, alg.computeDistance(obs), 1e-8);
	}

	@Test
	public void testNoisy() {
		Se3_F64 keyToCurr = new Se3_F64();
		keyToCurr.getR().set(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.05, -0.03, 0.02, null));
		keyToCurr.getT().set(0.1,-0.1,0.01);

		Point3D_F64 X = new Point3D_F64(0.1,-0.05,3);

		Point2D3D obs = new Point2D3D();
		obs.location = X.copy();

		SePointOps_F64.transform(keyToCurr, X, X);

		obs.observation.x = X.x/X.z+1;
		obs.observation.y = X.y/X.z+1;

		alg.setRotation(keyToCurr.getR());
		alg.setModel(keyToCurr.getT());
		assertTrue(alg.computeDistance(obs) > 1e-8);
	}
}
