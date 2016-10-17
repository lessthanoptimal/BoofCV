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

package boofcv.abst.filter.blur;

import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestBlurStorageFilter {

	int width = 20;
	int height = 25;
	Random rand = new Random(234);

	Class[] imageTypes = new Class[]{GrayU8.class, GrayF32.class};

	@Test
	public void gaussian() {
		for( Class c : imageTypes ) {
			ImageGray input = GeneralizedImageOps.createSingleBand(c,width,height);
			ImageGray found = GeneralizedImageOps.createSingleBand(c,width,height);
			ImageGray expected = GeneralizedImageOps.createSingleBand(c,width,height);
			ImageGray storage = GeneralizedImageOps.createSingleBand(c,width,height);

			GImageMiscOps.fillUniform(input,rand,0,100);

			BlurStorageFilter alg = new BlurStorageFilter<>("gaussian",c,-1,2);

			GBlurImageOps.gaussian(input,found,-1,2,storage);

			alg.process(input,expected);

			BoofTesting.assertEquals(expected,found,1e-4);
		}
	}

	@Test
	public void mean() {
		for( Class c : imageTypes ) {
			ImageGray input = GeneralizedImageOps.createSingleBand(c,width,height);
			ImageGray found = GeneralizedImageOps.createSingleBand(c,width,height);
			ImageGray expected = GeneralizedImageOps.createSingleBand(c,width,height);
			ImageGray storage = GeneralizedImageOps.createSingleBand(c,width,height);

			GImageMiscOps.fillUniform(input,rand,0,100);

			BlurStorageFilter alg = new BlurStorageFilter<>("mean",c,2);

			GBlurImageOps.mean(input,found,2,storage);

			alg.process(input,expected);

			BoofTesting.assertEquals(expected,found,1e-4);
		}
	}

	@Test
	public void median() {
		for( Class c : imageTypes ) {
			ImageGray input = GeneralizedImageOps.createSingleBand(c,width,height);
			ImageGray found = GeneralizedImageOps.createSingleBand(c,width,height);
			ImageGray expected = GeneralizedImageOps.createSingleBand(c,width,height);

			GImageMiscOps.fillUniform(input,rand,0,100);

			BlurStorageFilter alg = new BlurStorageFilter<>("median",c,2);

			GBlurImageOps.median(input,found,2);

			alg.process(input,expected);

			BoofTesting.assertEquals(expected,found,1e-4);
		}
	}
}