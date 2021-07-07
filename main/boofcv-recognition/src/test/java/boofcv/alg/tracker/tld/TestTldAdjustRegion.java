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

package boofcv.alg.tracker.tld;

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.ScaleTranslate2D;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.shapes.Rectangle2D_F64;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTldAdjustRegion extends BoofStandardJUnit {

	@Test void process() {
		ScaleTranslate2D motion = new ScaleTranslate2D(1.5,2,3);

		DogArray<AssociatedPair> pairs = new DogArray<>(AssociatedPair::new);

		for( int i = 0; i < 200; i++ ) {
			AssociatedPair p = pairs.grow();
			p.p1.x = rand.nextGaussian()*2;
			p.p1.y = rand.nextGaussian()*2;

			p.p2.x = p.p1.x*motion.scale + motion.transX;
			p.p2.y = p.p1.y*motion.scale + motion.transY;
		}

		Rectangle2D_F64 rect = new Rectangle2D_F64(10,20,30,40);

		TldAdjustRegion alg = new TldAdjustRegion(30);
		alg.init(300,400);

		assertTrue(alg.process(pairs, rect));

		assertEquals(17, rect.p0.x , 1e-8);
		assertEquals(33, rect.p0.y, 1e-8);
		assertEquals(47, rect.p1.x, 1e-8);
		assertEquals(63, rect.p1.y, 1e-8);
	}

	@Test void adjustRectangle() {
		TldAdjustRegion alg = new TldAdjustRegion(50);

		Rectangle2D_F64 rect = new Rectangle2D_F64(10,20,30,40);
		ScaleTranslate2D motion = new ScaleTranslate2D(1.5,2,3);

		alg.adjustRectangle(rect,motion);

		assertEquals(17, rect.p0.x , 1e-8);
		assertEquals(33, rect.p0.y, 1e-8);
		assertEquals(47, rect.p1.x, 1e-8);
		assertEquals(63, rect.p1.y, 1e-8);
	}
}
