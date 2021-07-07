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

import boofcv.struct.geo.ScaleTranslateRotate2D;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestModelManagerScaleTranslateRotate2D extends BoofStandardJUnit {

	@Test void createModelInstance() {
		ModelManagerScaleTranslateRotate2D alg = new ModelManagerScaleTranslateRotate2D();

		assertTrue(alg.createModelInstance() != null);
	}

	@Test void copyModel() {
		ModelManagerScaleTranslateRotate2D alg = new ModelManagerScaleTranslateRotate2D();

		ScaleTranslateRotate2D model = new ScaleTranslateRotate2D(1, 2, 3, 4);
		ScaleTranslateRotate2D found = new ScaleTranslateRotate2D();

		alg.copyModel(model, found);

		assertEquals(model.theta, found.theta, 1e-8);
		assertEquals(model.scale, found.scale, 1e-8);
		assertEquals(model.transX, found.transX, 1e-8);
		assertEquals(model.transY, found.transY, 1e-8);
	}
}
