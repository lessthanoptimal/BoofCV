/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.orientation;

import boofcv.alg.feature.orientation.impl.ImplOrientationImageAverageIntegral;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.ii.IntegralImageOps;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestOrientationIntegralToImage {

	Random rand = new Random(234);

	@Test
	public void simpleTest() {
		ImageFloat32 input = new ImageFloat32(30,40);
		ImageFloat32 ii = new ImageFloat32(input.width,input.height);

		GImageMiscOps.fillUniform(input, rand, 0, 50);
		IntegralImageOps.transform(input, ii);

		ImplOrientationImageAverageIntegral o = new ImplOrientationImageAverageIntegral(5,2,2,1,ImageFloat32.class);

		OrientationIntegralToImage alg = new OrientationIntegralToImage(o,ImageFloat32.class);
		alg.setImage(input);
		double found = alg.compute(10,12);

		// now compute it directly from an integral image
		o.setImage(ii);
		double expected = alg.compute(10,12);

		assertEquals(expected,found,1e-8);
	}
}
