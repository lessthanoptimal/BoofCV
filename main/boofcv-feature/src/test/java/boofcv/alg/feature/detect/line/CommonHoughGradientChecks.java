/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.line.LineParametric2D_F32;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class CommonHoughGradientChecks extends BoofStandardJUnit {
	Class[] imageTypes = new Class[]{GrayS16.class, GrayS32.class,GrayF32.class};
	int width = 30;
	int height = 40;

	abstract HoughTransformGradient createAlgorithm( Class derivType );

	@Test
	void obviousLines() {
		for( Class imageType : imageTypes ) {
			obviousLines(imageType);
		}
	}

	private <D extends ImageGray<D>> void obviousLines(Class<D> derivType ) {
		GrayU8 binary = new GrayU8(width,height);
		D derivX = GeneralizedImageOps.createSingleBand(derivType, width, height);
		D derivY = GeneralizedImageOps.createSingleBand(derivType, width, height);

		for( int i = 0; i < height; i++ ) {
			binary.set(5, i, 1);
			GeneralizedImageOps.set(derivX,5,i,20);
		}

		HoughTransformGradient alg = createAlgorithm(derivType);

		alg.transform(derivX,derivY,binary);

		List<LineParametric2D_F32> lines =  alg.getLinesMerged();

		assertEquals(1,lines.size());

		LineParametric2D_F32 l = lines.get(0);
		assertEquals(l.p.x,5,0.1);
		// normalize the line for easier evaluation
		l.slope.x /= l.slope.norm();
		l.slope.y /= l.slope.norm();
		assertEquals(0,Math.abs(l.slope.x), 0.1);
		assertEquals(1,Math.abs(l.slope.y), 0.1);
	}
}
