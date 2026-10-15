/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.mesh;

import boofcv.struct.mesh.VertexMesh;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestFirstPersonCameraControl {
	/// Enough vertexes that the index used to sample them overflows an int
	@Test void selectInitialParameters_largeMesh() {
		var mesh = new VertexMesh();
		for (int i = 0; i < 2_200_000; i++) {
			mesh.vertexes.append(i, 0, 0);
		}

		var alg = new FirstPersonCameraControl();
		alg.selectInitialParameters(mesh);

		// 1000 points are sampled, one every 2200 vertexes, and the median is sample 499
		assertEquals(499*2200/150.0, alg.getFps().motionUnit, 1e-8);
	}
}
