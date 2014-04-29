/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.pyramid.ImagePyramid;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestDenseFlowPyramidBase {

	Random rand = new Random(234);

	@Test
	public void interpolateFlow() {
		Dummy alg = new Dummy(0.75,1,20);

		ImageFloat32 input = new ImageFloat32(5,7);
		ImageFloat32 output = new ImageFloat32(8,14);

		ImageMiscOps.fillUniform(input, rand, 0, 10);

		alg.interpolateFlowScale(input, output);

		// there should be no zero values.  This is a very crude test
		for( int i = 0; i < output.data.length; i++ ) {
			assertTrue( output.data[i] != 0 );
		}
	}

	@Test
	public void imageNormalization() {
		fail("Implement");
	}

	public static class Dummy extends DenseFlowPyramidBase {

		public Dummy(double scale, double sigma, int maxLayers) {
			super(scale, sigma, maxLayers, FactoryInterpolation.bilinearPixelS(ImageFloat32.class));
		}

		@Override
		public void process(ImagePyramid image1, ImagePyramid image2) {}
	}
}
