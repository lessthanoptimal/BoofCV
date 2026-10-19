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

import boofcv.alg.geo.bundle.cameras.BundlePinhole;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.CROperatorForward;
import org.ddogleg.chainrule.CommonCROperatorBackwardsGrad.GradientCase;
import org.ddogleg.chainrule.CommonCROperatorBackwardsGrad;
import org.ddogleg.chainrule.CommonCROperatorBackwardsJac.JacobianCase;
import org.ddogleg.chainrule.CommonCROperatorBackwardsJac;
import org.ddogleg.chainrule.CommonCROperatorForward;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TestCROperatorCameraPinhole extends BoofStandardJUnit {
	@TestFactory Stream<DynamicTest> forward() {
		return new CommonCROperatorForward(this::createVariants, rand).tests();
	}

	List<CROperatorForward<double[]>> createVariants() {
		return List.of(new CROperatorCameraPinhole(false, true),
				new CROperatorCameraPinhole(true, false),
				new CROperatorCameraPinhole(false, true, 1.5));
	}

	@TestFactory Stream<DynamicTest> gradient() {
		return new CommonCROperatorBackwardsGrad<double[]>(this::gradientVariants, rand).tests();
	}

	List<GradientCase<double[]>> gradientVariants() {
		var variants = new ArrayList<GradientCase<double[]>>();

		variants.add(new GradientCase<>(new CROperatorCameraPinhole(false, true)).
				in(1, -2, 3).in(500, 200, 400, 205));
		variants.add(new GradientCase<>(new CROperatorCameraPinhole(false, false)).
				in(1, -2, 3).in(500, 200, 400, 205, 1.1));
		variants.add(new GradientCase<>(new CROperatorCameraPinhole(true, false)).
				in(1, -2, 3, 0.2).in(500, 200, 400, 205, 1.1));

		// fixed aspect ratio: fy is not a parameter
		variants.add(new GradientCase<>(new CROperatorCameraPinhole(false, true, 1.5)).
				in(1, -2, 3).in(500, 400, 205));
		variants.add(new GradientCase<>(new CROperatorCameraPinhole(false, false, 0.9)).
				in(1, -2, 3).in(500, 400, 205, 1.1));

		return variants;
	}

	@TestFactory Stream<DynamicTest> jacobian() {
		return new CommonCROperatorBackwardsJac<double[]>(this::jacobianVariants, rand).tests();
	}

	List<JacobianCase<double[]>> jacobianVariants() {
		var variants = new ArrayList<JacobianCase<double[]>>();

		variants.add(new JacobianCase<>(new CROperatorCameraPinhole(false, true)).
				in(1, -2, 3).in(500, 200, 400, 205));
		variants.add(new JacobianCase<>(new CROperatorCameraPinhole(false, false)).
				in(1, -2, 3).in(500, 200, 400, 205, 1.1));
		variants.add(new JacobianCase<>(new CROperatorCameraPinhole(true, false)).
				in(1, -2, 3, 0.2).in(500, 200, 400, 205, 1.1));

		// fixed aspect ratio: fy is not a parameter
		variants.add(new JacobianCase<>(new CROperatorCameraPinhole(false, true, 1.5)).
				in(1, -2, 3).in(500, 400, 205));
		variants.add(new JacobianCase<>(new CROperatorCameraPinhole(false, false, 0.9)).
				in(1, -2, 3).in(500, 400, 205, 1.1));

		return variants;
	}

	public static CRArray<double[]> arrayD( double... values ) {
		return new CRArray<>(values, 0);
	}

	@Test void encode_decode() {
		for (boolean zeroSkew : new boolean[]{false, true}) {
			for (double aspect : new double[]{-1, 1.2}) {
				// no tangential, zero skew, single radial term
				var src = new CameraPinhole().fsetK(1.0, 2.0, zeroSkew ? 0.0 : 0.1, 4.0, 5.0, -1, -1);
				if (aspect > 0) {
					src.fy = src.fx/aspect;
				}

				int dof = 5;
				if (zeroSkew)
					dof -= 1;
				if (aspect > 0)
					dof -= 1;

				var params = new double[dof];
				int end = CROperatorCameraPinhole.encode(src, zeroSkew, aspect, params, 0);
				assertEquals(dof, end);

				var found = new CameraPinhole().fsetShape(-1, -1);
				CROperatorCameraPinhole.decode(params, 0, found, zeroSkew, aspect);

				assertTrue(found.isEquals(src, 1e-5));
			}
		}
	}

	@Test void encodeCamera_setParameters_aspect() {
		// fixed aspect ratio removes fy from the vector and decode reconstructs fy = fx/ratio
		double ratio = 1.5;
		var alg = new CROperatorCameraPinhole(false, true, ratio);
		alg.camera.fsetK(300, 200, 0, 400, 205, -1, -2);
		assertEquals(3, alg.getInputSize(1));

		var params = arrayD(0, 0, 0);
		alg.encodeCamera(params);
		assertArrayEquals(new double[]{300, 400, 205}, params.a);

		var found = new CROperatorCameraPinhole(false, true, ratio);
		found.setParameters(params);
		assertEquals(300, found.camera.fx);
		assertEquals(300/ratio, found.camera.fy);
		assertEquals(400, found.camera.cx);
		assertEquals(205, found.camera.cy);
		assertEquals(0.0, found.camera.skew);
	}

	@Test void setTo_CameraPinhole() {
		var pinhole = new CameraPinhole(1, 2, 3, 4, 5, -1, -2);

		var alg = new CROperatorCameraPinhole(true, false);
		alg.setTo(pinhole);
		assertEquals(alg.camera.fx, pinhole.fx);
		assertEquals(alg.camera.fy, pinhole.fy);
		assertEquals(alg.camera.cx, pinhole.cx);
		assertEquals(alg.camera.cy, pinhole.cy);
		assertEquals(alg.camera.skew, alg.camera.skew);

		alg.setZeroSkew(true);
		alg.setTo(pinhole);
		assertEquals(alg.camera.fx, pinhole.fx);
		assertEquals(alg.camera.fy, pinhole.fy);
		assertEquals(alg.camera.cx, pinhole.cx);
		assertEquals(alg.camera.cy, pinhole.cy);
		assertEquals(0.0, alg.camera.skew);
	}

	@Test void setTo_BundlePinhole() {
		var pinhole = new CameraPinhole(1, 2, 0, 4, 5, -1, -2);
		var bundle = new BundlePinhole(true);
		bundle.setTo(pinhole);

		var alg = new CROperatorCameraPinhole(true, false);
		alg.setTo(bundle);

		assertTrue(alg.zeroSkew);
		assertEquals(bundle.fx, alg.camera.fx);
		assertEquals(bundle.fy, alg.camera.fy);
		assertEquals(bundle.cx, alg.camera.cx);
		assertEquals(bundle.cy, alg.camera.cy);
		assertEquals(0.0, alg.camera.skew);
	}

	@Test void toBoof() {
		var alg = new CROperatorCameraPinhole(true, false);
		alg.camera.fsetK(1, 2, 3, 4, 5, -1, -2);

		BundlePinhole found = alg.toBoof();
		assertFalse(alg.zeroSkew);
		assertEquals(alg.camera.fx, found.fx);
		assertEquals(alg.camera.fy, found.fy);
		assertEquals(alg.camera.cx, found.cx);
		assertEquals(alg.camera.cy, found.cy);
		assertEquals(alg.camera.skew, found.skew);

		alg.setZeroSkew(true);
		found = alg.toBoof();
		assertTrue(alg.zeroSkew);
		assertEquals(alg.camera.fx, found.fx);
		assertEquals(alg.camera.fy, found.fy);
		assertEquals(alg.camera.cx, found.cx);
		assertEquals(alg.camera.cy, found.cy);
		assertEquals(0.0, found.skew);
	}
}
