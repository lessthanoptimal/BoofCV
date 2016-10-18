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

package boofcv.alg.sfm.robust;

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.sfm.ScaleTranslate2D;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestGenerateScaleTranslate2D {
	@Test
	public void perfect() {
		ScaleTranslate2D model = new ScaleTranslate2D(1.5,-2,3);

		AssociatedPair a = TestDistanceScaleTranslate2DSq.apply(-5, 4, model);
		AssociatedPair b = TestDistanceScaleTranslate2DSq.apply(2,3,model);

		List<AssociatedPair> obs = new ArrayList<>();
		obs.add(a);
		obs.add(b);

		ScaleTranslate2D found = new ScaleTranslate2D();
		GenerateScaleTranslate2D alg = new GenerateScaleTranslate2D();
		assertTrue(alg.generate(obs,found));

		assertEquals(model.transX, found.transX, 1e-8);
		assertEquals(model.transY, found.transY, 1e-8);
		assertEquals(model.scale, found.scale, 1e-8);
	}

	/**
	 * Both points are at 0,0.  Scale can't be resolved
	 */
	@Test
	public void pathological_noscale() {
		ScaleTranslate2D model = new ScaleTranslate2D(1.5,-2,3);

		AssociatedPair a = TestDistanceScaleTranslate2DSq.apply(0, 0, model);
		AssociatedPair b = TestDistanceScaleTranslate2DSq.apply(0,0,model);

		List<AssociatedPair> obs = new ArrayList<>();
		obs.add(a);
		obs.add(b);

		ScaleTranslate2D found = new ScaleTranslate2D();
		GenerateScaleTranslate2D alg = new GenerateScaleTranslate2D();
		assertFalse(alg.generate(obs, found));
	}

	/**
	 * Possible pathological case.  One point is at zero.
	 */
	@Test
	public void onePointAtZero_a() {
		ScaleTranslate2D model = new ScaleTranslate2D(1.5,-2,3);

		AssociatedPair a = TestDistanceScaleTranslate2DSq.apply(0, 0, model);
		AssociatedPair b = TestDistanceScaleTranslate2DSq.apply(2,3,model);

		List<AssociatedPair> obs = new ArrayList<>();
		obs.add(a);
		obs.add(b);

		ScaleTranslate2D found = new ScaleTranslate2D();
		GenerateScaleTranslate2D alg = new GenerateScaleTranslate2D();
		assertTrue(alg.generate(obs,found));

		assertEquals(model.transX, found.transX, 1e-8);
		assertEquals(model.transY, found.transY, 1e-8);
		assertEquals(model.scale, found.scale, 1e-8);
	}

	@Test
	public void getMinimumPoints() {
		GenerateScaleTranslate2D alg = new GenerateScaleTranslate2D();
		assertEquals(2,alg.getMinimumPoints());
	}
}
