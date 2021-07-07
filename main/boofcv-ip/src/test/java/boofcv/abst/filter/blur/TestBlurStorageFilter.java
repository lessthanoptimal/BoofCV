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

package boofcv.abst.filter.blur;

import boofcv.BoofTesting;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */

@SuppressWarnings("rawtypes")
public class TestBlurStorageFilter extends BoofStandardJUnit {

	int width = 20;
	int height = 25;

	ImageType[] imageTypes = new ImageType[]{ImageType.single(GrayU8.class), ImageType.single(GrayF32.class)};

	@Test void gaussian() {
		for( ImageType c : imageTypes ) {
			ImageBase input = c.createImage(width,height);
			ImageBase found = c.createImage(width,height);
			ImageBase expected = c.createImage(width,height);
			ImageBase storage = c.createImage(width,height);

			GImageMiscOps.fillUniform(input,rand,0,100);

			BlurStorageFilter alg = new BlurStorageFilter<>("gaussian",c,-1.0,2,-1.0,2);

			GBlurImageOps.gaussian(input,found,-1,2,storage);

			alg.process(input,expected);

			BoofTesting.assertEquals(expected,found,1e-4);
		}
	}

	@Test void mean() {
		for( ImageType c : imageTypes ) {
			ImageBase input = c.createImage(width,height);
			ImageBase found = c.createImage(width,height);
			ImageBase expected = c.createImage(width,height);
			ImageBase storage = c.createImage(width,height);

			GImageMiscOps.fillUniform(input,rand,0,100);

			BlurStorageFilter alg = new BlurStorageFilter<>("mean",c,2);

			GBlurImageOps.mean(input,found,2,storage,null);

			alg.process(input,expected);

			BoofTesting.assertEquals(expected,found,1e-4);
		}
	}

	@Test void median() {
		for( ImageType c : imageTypes ) {
			ImageBase input = c.createImage(width,height);
			ImageBase found = c.createImage(width,height);
			ImageBase expected = c.createImage(width,height);

			GImageMiscOps.fillUniform(input,rand,0,100);

			BlurStorageFilter alg = new BlurStorageFilter<>("median",c,2);

			GBlurImageOps.median(input,found,2, 2,null);

			alg.process(input,expected);

			BoofTesting.assertEquals(expected,found,1e-4);
		}
	}
}
