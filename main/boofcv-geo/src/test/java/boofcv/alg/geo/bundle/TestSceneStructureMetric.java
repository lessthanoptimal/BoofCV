/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle;

import boofcv.abst.geo.bundle.SceneStructureMetric;
import georegression.struct.se.Se3_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSceneStructureMetric {
	@Test
	public void assignIDsToRigidPoints() {
		SceneStructureMetric scene = new SceneStructureMetric(false);

		scene.initialize(1,2,3,2);
		scene.setRigid(0,false,new Se3_F64(),2);
		scene.setRigid(1,true,new Se3_F64(),3);

		scene.assignIDsToRigidPoints();

		for (int i = 0; i < 2; i++) {
			assertEquals(0,scene.lookupRigid[i]);
		}
		for (int i = 0; i < 3; i++) {
			assertEquals(1,scene.lookupRigid[i+2]);
		}

		assertEquals(0,scene.rigids.data[0].indexFirst);
		assertEquals(2,scene.rigids.data[1].indexFirst);
	}
}