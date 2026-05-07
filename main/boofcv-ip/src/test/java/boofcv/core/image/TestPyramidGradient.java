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

package boofcv.core.image;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestPyramidGradient extends BoofStandardJUnit {
	@Test void update() {
		// dummy class. Just see if it's called enough times.
		var gradient = new ImageGradient<GrayF32, GrayF32>() {
			int count = 0;

			@Override public void setBorderType( BorderType type ) {}

			@Override public BorderType getBorderType() {return null;}

			@Override public int getBorder() {return 0;}

			@Override public ImageType<GrayF32> getDerivativeType() {return ImageType.SB_F32;}

			@Override public void process( GrayF32 inputImage, GrayF32 derivX, GrayF32 derivY ) {
				count++;
			}

			@Override public int divisor() {return 2;}

			@Override public ImageType<GrayF32> getInputType() {return ImageType.SB_F32;}
		};

		PyramidDiscrete<GrayF32> pyramid = createPyramid();
		var alg = new PyramidGradient<>(pyramid, gradient);

		alg.update(new GrayF32(200, 200));
		// Did it create the expected number of pyramid levels
		assertEquals(3, gradient.count);
		assertEquals(3, alg.derivX.length);
		assertEquals(3, alg.derivY.length);

		// Did it size all the levels correctly?
		for (int i = 0; i < 3; i++) {
			int width = alg.basePyramid.getWidth(i);
			int height = alg.basePyramid.getHeight(i);

			assertEquals(width, alg.derivX[i].width);
			assertEquals(height, alg.derivX[i].height);
			assertEquals(width, alg.derivY[i].width);
			assertEquals(height, alg.derivY[i].height);
		}
	}

	PyramidDiscrete<GrayF32> createPyramid() {
		var levels = new ConfigDiscreteLevels();
		levels.numLevelsRequested = 3;
		levels.minWidth = 5;
		levels.minHeight = 5;
		return FactoryPyramid.discreteGaussian(levels, -1, 2, true, ImageType.SB_F32);
	}
}
