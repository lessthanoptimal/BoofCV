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

package boofcv.abst.filter.blur;

import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestBlurStorageFilter {

	int width = 20;
	int height = 25;

	@Test
	public void gaussian() {
		ImageUInt8 input = new ImageUInt8(width,height);
		ImageUInt8 found = new ImageUInt8(width,height);
		ImageUInt8 expected = new ImageUInt8(width,height);
		ImageUInt8 storage = new ImageUInt8(width,height);

		BlurStorageFilter<ImageUInt8> alg = new BlurStorageFilter<ImageUInt8>("gaussian",ImageUInt8.class,-1,2);

		BlurImageOps.gaussian(input,found,-1,2,storage);

		alg.process(input,expected);

		BoofTesting.assertEquals(expected,found,1e-4);
	}

	@Test
	public void mean() {
		ImageUInt8 input = new ImageUInt8(width,height);
		ImageUInt8 found = new ImageUInt8(width,height);
		ImageUInt8 expected = new ImageUInt8(width,height);
		ImageUInt8 storage = new ImageUInt8(width,height);

		BlurStorageFilter<ImageUInt8> alg = new BlurStorageFilter<ImageUInt8>("mean",ImageUInt8.class,2);

		BlurImageOps.mean(input, found, 2, storage);

		alg.process(input,expected);

		BoofTesting.assertEquals(expected,found,1e-4);
	}

	@Test
	public void median() {
		ImageUInt8 input = new ImageUInt8(width,height);
		ImageUInt8 found = new ImageUInt8(width,height);
		ImageUInt8 expected = new ImageUInt8(width,height);

		BlurStorageFilter<ImageUInt8> alg = new BlurStorageFilter<ImageUInt8>("median",ImageUInt8.class,2);

		BlurImageOps.median(input, found, 2);

		alg.process(input,expected);

		BoofTesting.assertEquals(expected,found,1e-4);
	}
}