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

package boofcv.chainrule;

import boofcv.alg.geo.bundle.cameras.BundlePinholeSnavely;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.CROperatorForward;
import org.ddogleg.chainrule.CommonCROperatorBackwardsJac;
import org.ddogleg.chainrule.CommonCROperatorBackwardsJac.JacobianCase;
import org.ddogleg.chainrule.CommonCROperatorForward;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCROperatorCameraSnavely extends BoofStandardJUnit {
	@TestFactory Stream<DynamicTest> forward() {
		return new CommonCROperatorForward(() -> List.of(new CROperatorCameraSnavely()), rand) {
			@Override public CRArray<double[]> randomInput( int which, CROperatorForward<double[]> alg ) {
				// in front of the camera, which looks down −z, and well away from the singularity at z = 0
				if (which == CROperatorCameraSnavely.IN_POINT) {
					double z = -2 - rand.nextDouble();
					return new CRArray<>(new double[]{rand.nextGaussian(), rand.nextGaussian(), z}, 0);
				}
				double f = 400 + 100*rand.nextDouble();
				return new CRArray<>(new double[]{f, 0.1*rand.nextGaussian(), 0.01*rand.nextGaussian()}, 0);
			}
		}.tests();
	}

	@TestFactory Stream<DynamicTest> jacobian() {
		return new CommonCROperatorBackwardsJac<double[]>(() -> List.of(
				new JacobianCase<double[]>(new CROperatorCameraSnavely()).in(0.1, -0.2, -1.5).in(500, 0.0, 0.0),
				new JacobianCase<double[]>(new CROperatorCameraSnavely()).in(0.3, 0.4, -2.0).in(450, -0.12, 0.03),
				new JacobianCase<double[]>(new CROperatorCameraSnavely()).in(-0.5, 0.2, -1.1).in(800, 0.2, -0.05)
		), rand).tests();
	}

	/// Same projection as BoofCV's bundle adjustment camera for this data set
	@Test void sameAsBundlePinholeSnavely() {
		var camera = new BundlePinholeSnavely();
		camera.f = 450;
		camera.k1 = -0.12;
		camera.k2 = 0.03;

		var alg = new CROperatorCameraSnavely();
		alg.setInput(CROperatorCameraSnavely.IN_POINT, new CRArray<>(new double[]{0.3, 0.4, -2.0}, 0));
		alg.setInput(CROperatorCameraSnavely.IN_CAMERA, new CRArray<>(new double[]{450, -0.12, 0.03}, 0));
		var found = new CRArray<>(new double[2], 0);
		alg.forward(found);

		var expected = new Point2D_F64();
		camera.project(0.3, 0.4, -2.0, expected);
		assertEquals(expected.x, found.d(0), 1e-12);
		assertEquals(expected.y, found.d(1), 1e-12);
	}
}
