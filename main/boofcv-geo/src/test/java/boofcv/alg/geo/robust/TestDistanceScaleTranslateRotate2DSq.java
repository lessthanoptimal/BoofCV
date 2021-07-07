/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.ScaleTranslateRotate2D;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDistanceScaleTranslateRotate2DSq extends BoofStandardJUnit {
	@Test void perfect() {
		var model = new ScaleTranslateRotate2D(0.2, 1.5, -2, 3);

		AssociatedPair a = apply(-5, 4, model);

		var alg = new DistanceScaleTranslateRotate2DSq();
		alg.setModel(model);

		assertEquals(0, alg.distance(a), 1e-8);
	}

	@Test void noisy() {
		var model = new ScaleTranslateRotate2D(0.2, 1.5, -2, 3);

		AssociatedPair a = apply(-5, 4, model);
		a.p2.x += 3.5;

		var alg = new DistanceScaleTranslateRotate2DSq();
		alg.setModel(model);

		assertEquals(3.5*3.5, alg.distance(a), 1e-8);
	}

	@Test void multiple() {
		var model = new ScaleTranslateRotate2D(0.2, 1.5, -2, 3);

		AssociatedPair a = apply(-5, 4, model);
		a.p2.x += 3.5;
		AssociatedPair b = apply(2.15, 2, model);

		List<AssociatedPair> obs = new ArrayList<>();
		obs.add(a);
		obs.add(b);

		var alg = new DistanceScaleTranslateRotate2DSq();
		alg.setModel(model);
		double[] found = new double[2];
		alg.distances(obs, found);

		assertEquals(3.5*3.5, found[0], 1e-8);
		assertEquals(0, found[1], 1e-8);
	}

	public static AssociatedPair apply( double x, double y, ScaleTranslateRotate2D model ) {
		var p = new AssociatedPair();
		p.p1.setTo(x, y);

		double c = Math.cos(model.theta);
		double s = Math.sin(model.theta);

		p.p2.x = (x*c - y*s)*model.scale + model.transX;
		p.p2.y = (x*s + y*c)*model.scale + model.transY;

		return p;
	}
}
