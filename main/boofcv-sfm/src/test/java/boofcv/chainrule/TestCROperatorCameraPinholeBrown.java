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

import boofcv.abst.geo.calibration.ConfigCalibrateBrown;
import boofcv.alg.geo.bundle.cameras.BundlePinholeBrown;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.CROperatorForward;
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

public class TestCROperatorCameraPinholeBrown extends BoofStandardJUnit {
	@TestFactory Stream<DynamicTest> forward() {
		return new CommonCROperatorForward(this::createVariants, rand).tests();
	}

	List<CROperatorForward<double[]>> createVariants() {
		return List.of(new CROperatorCameraPinholeBrown(false, true, 2, true),
				new CROperatorCameraPinholeBrown(true, true, 2, true),
				new CROperatorCameraPinholeBrown(false, false, 0, false),
				new CROperatorCameraPinholeBrown(false, false, 1, false),
				new CROperatorCameraPinholeBrown(false, true, 2, true, 1.5),
				new CROperatorCameraPinholeBrown(false, false, 1, false, 0.9));
	}

	@TestFactory Stream<DynamicTest> jacobian() {
		return new CommonCROperatorBackwardsJac<double[]>(this::jacobianVariants, rand).tests();
	}

	List<JacobianCase<double[]>> jacobianVariants() {
		var variants = new ArrayList<JacobianCase<double[]>>();

		variants.add(new JacobianCase<>(new CROperatorCameraPinholeBrown(false, true, 2, true)).
				in(1, -2, 3).in(5.0, 4.7, 4.0, 2.05, 0.01, -0.02, 0.02, -0.05));
		variants.add(new JacobianCase<>(new CROperatorCameraPinholeBrown(true, true, 2, true)).
				in(1, -2, 3, 0.5).in(5.0, 4.7, 4.0, 2.05, 0.01, -0.02, 0.02, -0.05));
		variants.add(new JacobianCase<>(new CROperatorCameraPinholeBrown(false, false, 0, false)).
				in(1, -2, 3).in(5.0, 4.7, 4.0, 2.05, 1.1));
		variants.add(new JacobianCase<>(new CROperatorCameraPinholeBrown(false, false, 1, false)).
				in(1, -2, 3).in(5.0, 4.7, 4.0, 2.05, 0.12, 1.1));

		// fixed aspect ratio: fy is not a parameter
		variants.add(new JacobianCase<>(new CROperatorCameraPinholeBrown(false, true, 2, true, 1.5)).
				in(1, -2, 3).in(5.0, 4.0, 2.05, 0.01, -0.02, 0.02, -0.05));
		variants.add(new JacobianCase<>(new CROperatorCameraPinholeBrown(false, false, 1, false, 0.9)).
				in(1, -2, 3).in(5.0, 4.0, 2.05, 0.12, 1.1));

		return variants;
	}

	/// CameraPinholeBrown carries values only, so which parameters are being estimated has to survive
	/// being assigned from one. Values of zero are still estimated parameters
	@Test void setTo_CameraPinholeBrown() {
		var alg = new CROperatorCameraPinholeBrown();
		assertFalse(alg.homogeneous);
		alg.zeroSkew = false;
		alg.tangential = true;

		// check nominal case with values for all parameters
		alg.setTo(new CameraPinholeBrown().fsetK(1, 2, 3, 4, 5, -1, -1).fsetRadial(0.1).fsetTangential(1.1, 1.2));

		checkK(1, 2, 3, 4, 5, alg);
		assertArrayEquals(new double[]{0.1}, alg.model.radial);
		checkTan(1.1, 1.2, alg);

		assertFalse(alg.zeroSkew);
		assertTrue(alg.tangential);

		// Zeros must not be mistaken for parameters that are switched off
		alg.setTo(new CameraPinholeBrown().fsetK(1, 2, 0, 4, 5, -1, -1));

		checkK(1, 2, 0, 4, 5, alg);
		assertArrayEquals(new double[]{}, alg.model.radial);
		checkTan(0.0, 0.0, alg);

		assertFalse(alg.zeroSkew);
		assertTrue(alg.tangential);
	}

	@Test void setTo_BundlePinholeBrown() {
		var bundle = new BundlePinholeBrown().setK(1, 2, 3, 4, 5).setRadial(0.1).setTangential(1.1, 1.2);
		bundle.aspectRatio = 1.5;
		var alg = new CROperatorCameraPinholeBrown();

		checkEquality(alg.setTo(bundle), bundle);
	}

	@Test void toBoof() {
		var alg = new CROperatorCameraPinholeBrown();
		alg.model.fsetRadial(0.1).fsetTangential(1.1, 1.2).fsetK(1, 2, 3, 4, 5);
		alg.zeroSkew = true;
		alg.tangential = true;

		checkEquality(alg, alg.toBoof());

		alg.zeroSkew = false;
		alg.tangential = false;
		alg.aspectRatio = 1.3;

		checkEquality(alg, alg.toBoof());
	}

	private static void checkEquality( CROperatorCameraPinholeBrown block, BundlePinholeBrown bundle ) {
		assertEquals(block.model.fx, bundle.fx);
		assertEquals(block.model.fy, bundle.fy);
		assertEquals(block.model.cx, bundle.cx);
		assertEquals(block.model.cy, bundle.cy);

		assertArrayEquals(block.model.radial, bundle.radial);
		if (block.tangential) {
			assertEquals(block.model.t1, bundle.t1);
			assertEquals(block.model.t2, bundle.t2);
		}
		if (!block.zeroSkew) {
			assertEquals(block.model.skew, bundle.skew);
		}
		assertEquals(block.zeroSkew, bundle.zeroSkew);
		assertEquals(block.tangential, bundle.tangential);
		assertEquals(block.aspectRatio, bundle.aspectRatio);
	}

	@Test void setK() {
		var alg = new CROperatorCameraPinholeBrown();
		alg.zeroSkew = false;

		// check nominal case with values for all parameters
		alg.setK(1, 2, 3, 4, 5);
		checkK(1, 2, 3, 4, 5, alg);
		assertFalse(alg.zeroSkew);

		// a skew of zero is still a skew being estimated
		alg.setK(1, 2, 0, 4, 5);
		checkK(1, 2, 0, 4, 5, alg);
		assertFalse(alg.zeroSkew);
	}

	@Test void setRadial() {
		var alg = new CROperatorCameraPinholeBrown();
		alg.setRadial(1, 2);
		assertArrayEquals(new double[]{1, 2}, alg.model.radial);
	}

	@Test void setTangential() {
		var alg = new CROperatorCameraPinholeBrown();
		alg.tangential = true;

		alg.setTangential(1.1, 1.2);
		checkTan(1.1, 1.2, alg);
		assertTrue(alg.tangential);

		// starting the terms at zero must not switch them off
		alg.setTangential(0, 0);
		checkTan(0, 0, alg);
		assertTrue(alg.tangential);
	}

	@Test void encode_decode() {
		// full: tangential + skew, two radial terms
		var src = new CameraPinholeBrown().fsetK(1.0, 2.0, 0.3, 4.0, 5.0, -1, -1)
				.fsetRadial(0.1, -0.2).fsetTangential(0.01, -0.02);

		var params = new double[11]; // fx,fy,cx,cy + 2 radial + t1,t2 + skew = 9, offset 1
		int end = CROperatorCameraPinholeBrown.encode(src, true, false, -1, params, 1);
		assertEquals(1 + 9, end);

		var found = new CameraPinholeBrown().fsetRadial(0, 0);
		int endD = CROperatorCameraPinholeBrown.decode(params, 1, found, true, false, -1);
		assertEquals(1 + 9, endD);

		assertCamEquals(src, found, true, false);
	}

	@Test void encode_decode_minimal() {
		for (boolean tangential : new boolean[]{false, true}) {
			for (boolean zeroSkew : new boolean[]{false, true}) {
				for (double aspect : new double[]{-1, 1.2}) {
					// no tangential, zero skew, single radial term
					var src = new CameraPinholeBrown().fsetK(1.0, 2.0, zeroSkew ? 0.0 : 0.1, 4.0, 5.0, -1, -1).fsetRadial(0.1);
					if (aspect > 0) {
						src.fy = src.fx/aspect;
					}
					if (tangential)
						src.fsetTangential(-0.12, 0.05);

					int dof = 8;
					if (!tangential)
						dof -= 2;
					if (zeroSkew)
						dof -= 1;
					if (aspect > 0)
						dof -= 1;

					var params = new double[dof];
					int end = CROperatorCameraPinholeBrown.encode(src, tangential, zeroSkew, aspect, params, 0);
					assertEquals(dof, end);

					var found = new CameraPinholeBrown().fsetRadial(0);
					CROperatorCameraPinholeBrown.decode(params, 0, found, tangential, zeroSkew, aspect);

					assertCamEquals(src, found, tangential, zeroSkew);
				}
			}
		}
	}

	@Test void encode_decode_config() {
		var config = new ConfigCalibrateBrown();
		config.tangential = true;
		config.zeroSkew = false;

		var src = new CameraPinholeBrown().fsetK(1.0, 2.0, 0.3, 4.0, 5.0, -1, -1)
				.fsetRadial(0.1, -0.2).fsetTangential(0.01, -0.02);

		var params = new double[9];
		CROperatorCameraPinholeBrown.encode(src, config, params, 0);

		var found = new CameraPinholeBrown().fsetRadial(0, 0);
		CROperatorCameraPinholeBrown.decode(params, 0, found, config);

		assertCamEquals(src, found, true, false);
	}

	@Test void encodeCamera_array() {
		var alg = new CROperatorCameraPinholeBrown();
		alg.zeroSkew = false;
		alg.tangential = true;
		alg.setK(1, 2, 0.3, 4, 5);
		alg.setRadial(0.1, -0.2);
		alg.setTangential(0.01, -0.02);

		var array = new CRArray<>(new double[12], 1);
		alg.encodeCamera(array); // instance -> static encodeCamera -> encode

		var found = new CameraPinholeBrown().fsetRadial(0, 0);
		CROperatorCameraPinholeBrown.decodeCamera(array, found, alg.tangential, alg.zeroSkew, alg.aspectRatio);

		assertCamEquals(alg.model, found, alg.tangential, alg.zeroSkew);
	}

	private static void assertCamEquals( CameraPinholeBrown a, CameraPinholeBrown b, boolean tangential, boolean zeroSkew ) {
		assertEquals(a.fx, b.fx);
		assertEquals(a.fy, b.fy);
		assertEquals(a.cx, b.cx);
		assertEquals(a.cy, b.cy);
		assertArrayEquals(a.radial, b.radial);
		if (tangential) {
			assertEquals(a.t1, b.t1);
			assertEquals(a.t2, b.t2);
		}
		if (!zeroSkew) {
			assertEquals(a.skew, b.skew);
		}
	}

	private void checkK( double fx, double fy, double skew, double cx, double cy, CROperatorCameraPinholeBrown alg ) {
		assertEquals(fx, alg.model.fx);
		assertEquals(fy, alg.model.fy);
		assertEquals(skew, alg.model.skew);
		assertEquals(cx, alg.model.cx);
		assertEquals(cy, alg.model.cy);
	}

	private void checkTan( double t1, double t2, CROperatorCameraPinholeBrown alg ) {
		assertEquals(t1, alg.model.t1);
		assertEquals(t2, alg.model.t2);
	}
}
