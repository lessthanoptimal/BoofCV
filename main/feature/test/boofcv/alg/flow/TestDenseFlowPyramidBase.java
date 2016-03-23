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

package boofcv.alg.flow;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.pyramid.ImagePyramid;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDenseFlowPyramidBase {

	Random rand = new Random(234);

	@Test
	public void interpolateFlow() {
		Dummy alg = new Dummy(0.75,1,20);

		GrayF32 input = new GrayF32(5,7);
		GrayF32 output = new GrayF32(8,14);

		ImageMiscOps.fillUniform(input, rand, 0, 10);

		alg.interpolateFlowScale(input, output);

		// there should be no zero values.  This is a very crude test
		for( int i = 0; i < output.data.length; i++ ) {
			assertTrue( output.data[i] != 0 );
		}
	}

	@Test
	public void imageNormalization() {
		GrayF32 input1 = new GrayF32(5,7);
		GrayF32 input2 = new GrayF32(5,7);

		GrayF32 norm1 = new GrayF32(5,7);
		GrayF32 norm2 = new GrayF32(5,7);

		ImageMiscOps.fillUniform(input1, rand, 0, 10);
		ImageMiscOps.fillUniform(input2, rand, 0, 15);

		DenseFlowPyramidBase.imageNormalization(input1,input2,norm1,norm2);

		// should be normalized so that its values are between 0 and 1
		assertTrue(ImageStatistics.min(norm1) >= 0 );
		assertTrue(ImageStatistics.min(norm2) >= 0 );

		assertTrue(ImageStatistics.max(norm1) <= 1 );
		assertTrue(ImageStatistics.max(norm2) <= 1 );
	}

	public static class Dummy extends DenseFlowPyramidBase {

		public Dummy(double scale, double sigma, int maxLayers) {
			super(scale, sigma, maxLayers, FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED));
		}

		@Override
		public void process(ImagePyramid image1, ImagePyramid image2) {}
	}
}
