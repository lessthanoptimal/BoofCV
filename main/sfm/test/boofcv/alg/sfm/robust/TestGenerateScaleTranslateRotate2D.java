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
import boofcv.struct.sfm.ScaleTranslateRotate2D;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestGenerateScaleTranslateRotate2D {

	Random rand = new Random(234);

	@Test
	public void perfect() {
		for( int i = 0; i < 100; i++ ) {
			double theta = rand.nextDouble()*Math.PI*2-Math.PI;
			double scale = rand.nextDouble()*5+0.1;

			ScaleTranslateRotate2D model = new ScaleTranslateRotate2D(theta,scale,-2,3);

			AssociatedPair a = TestDistanceScaleTranslateRotate2DSq.apply(-5, 4, model);
			AssociatedPair b = TestDistanceScaleTranslateRotate2DSq.apply(2,3,model);
			AssociatedPair c = TestDistanceScaleTranslateRotate2DSq.apply(-3,2,model);

			List<AssociatedPair> obs = new ArrayList<>();
			obs.add(a);
			obs.add(b);
			obs.add(c);

			ScaleTranslateRotate2D found = new ScaleTranslateRotate2D();
			GenerateScaleTranslateRotate2D alg = new GenerateScaleTranslateRotate2D();
			assertTrue(alg.generate(obs,found));

			assertEquals(model.transX, found.transX, 1e-8);
			assertEquals(model.transY, found.transY, 1e-8);
			assertEquals(model.scale, found.scale, 1e-8);
			assertEquals(model.theta, found.theta, 1e-8);
		}
	}

	@Test
	public void getMinimumPoints() {
		GenerateScaleTranslateRotate2D alg = new GenerateScaleTranslateRotate2D();
		assertEquals(3,alg.getMinimumPoints());
	}
}
