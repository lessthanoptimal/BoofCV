/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.geo.AssociatedPair3D;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSelectBestStereoTransformHPointing extends BoofStandardJUnit {
	@Test void scenario1() {
		List<Se3_F64> candidatesAtoB = new ArrayList<>();

		for (int i = 0; i < 3; i++) {
			candidatesAtoB.add(new Se3_F64());
		}

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, Math.PI/2, 0, 0, candidatesAtoB.get(0).R);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI, 0, candidatesAtoB.get(1).R);
		candidatesAtoB.get(2).T.x = 1;

		checkScenario(candidatesAtoB, 2);
	}

	@Test void scenario2() {
		List<Se3_F64> candidatesAtoB = new ArrayList<>();

		for (int i = 0; i < 3; i++) {
			candidatesAtoB.add(new Se3_F64());
		}
		candidatesAtoB.get(0).T.y = 1e-4;
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, Math.PI/2, 0, 0, candidatesAtoB.get(0).R);
		candidatesAtoB.get(1).T.x = -1;
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI, 0, candidatesAtoB.get(1).R);
		candidatesAtoB.get(2).T.x = 0.5;

		checkScenario(candidatesAtoB, 2);
	}

	void checkScenario(List<Se3_F64> candidatesAtoB, int trueBest) {
		List<AssociatedPair3D> observations = new ArrayList<>();
		Se3_F64 a_to_b = candidatesAtoB.get(trueBest);
		for (int i = 0; i < 8; i++) {
			var p = new Point3D_F64(rand.nextGaussian()*0.1, rand.nextGaussian()*0.1, 3 + rand.nextGaussian()*0.1);

			var o = new AssociatedPair3D();
			o.p1.setTo(p);
			SePointOps_F64.transform(a_to_b, p, o.p2);

			observations.add(o);
		}

		var alg = new SelectBestStereoTransformHPointing();

		var found = new Se3_F64();
		alg.select(candidatesAtoB, observations, found);

		assertTrue(MatrixFeatures_DDRM.isIdentical(a_to_b.getR(), found.getR(), 1e-6));

		double scale = found.getT().norm()/a_to_b.getT().norm();
		assertEquals(a_to_b.getT().x*scale, found.getT().x, 1e-8);
		assertEquals(a_to_b.getT().y*scale, found.getT().y, 1e-8);
		assertEquals(a_to_b.getT().z*scale, found.getT().z, 1e-8);
	}
}
