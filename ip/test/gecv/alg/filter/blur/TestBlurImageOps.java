/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.blur;

import gecv.alg.filter.blur.impl.ImplMedianSortNaive;
import gecv.core.image.GeneralizedImageOps;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageUInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestBlurImageOps {

	Random rand = new Random(234);

	int width = 20;
	int height = 30;

	@Test
	public void mean() {
		fail("implement");
	}

	@Test
	public void median_U8() {

		ImageUInt8 input = new ImageUInt8(width,height);
		ImageUInt8 found = new ImageUInt8(width,height);
		ImageUInt8 expected = new ImageUInt8(width,height);

		GeneralizedImageOps.randomize(input,rand,0,20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImplMedianSortNaive.process(input,expected,radius,null);
			BlurImageOps.median(input,found,radius);

			GecvTesting.assertEquals(expected,found,0);
		}
	}

	@Test
	public void median_F32() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);

		GeneralizedImageOps.randomize(input,rand,0,20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImplMedianSortNaive.process(input,expected,radius,null);
			BlurImageOps.median(input,found,radius);

			GecvTesting.assertEquals(expected,found,0,1e-4);
		}
	}

	@Test
	public void gaussian() {
		fail("implement");
	}
}
