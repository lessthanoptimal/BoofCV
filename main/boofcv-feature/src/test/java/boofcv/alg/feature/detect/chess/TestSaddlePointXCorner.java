/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.chess;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestSaddlePointXCorner {
	@Test
	void basic() {
		GrayF32 input = new GrayF32(100,100);
		GImageMiscOps.fillRectangle(input,255,0,0,50,50);
		GImageMiscOps.fillRectangle(input,255,50,50,50,50);

		SaddlePointXCorner alg = new SaddlePointXCorner();
		alg.setImage(input);
		alg.setRadius(8);

		for (int dy = -2; dy <= 2; dy++) {
			for (int dx = -2; dx <= 2; dx++) {
				dx = dy = 2;
//				System.out.println("dx = "+dx+" dy = "+dy);
				assertTrue(alg.process(50+dx,50+dy));

				assertEquals(50,alg.getSaddleX(), 0.8);
				assertEquals(50,alg.getSaddleY(), 0.8);
			}
		}
	}
}