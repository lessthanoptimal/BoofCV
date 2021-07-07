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

package boofcv.alg.feature.detect.line;

import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.line.LineParametric2D_F32;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class CommonHoughBinaryChecks extends BoofStandardJUnit {
	int width = 30;
	int height = 40;

	abstract HoughTransformBinary createAlgorithm();

	/**
	 * See if it can detect an obvious line in the image
	 */
	@Test void obviousLines() {
		GrayU8 image = new GrayU8(width,height);

		for( int i = 0; i < height; i++ ) {
			image.set(5,i,1);
		}

		HoughTransformBinary alg = createAlgorithm();

		alg.transform(image);

		List<LineParametric2D_F32> lines =  alg.getLinesMerged();

		assertTrue(lines.size() > 0);

		for( int i = 0; i < lines.size(); i++ ) {
			LineParametric2D_F32 l = lines.get(i);
			assertEquals(5,l.p.x,0.1);
			assertEquals(0,Math.abs(l.slope.x),1e-4);
			assertEquals(1,Math.abs(l.slope.y),0.1);
		}
	}
}
