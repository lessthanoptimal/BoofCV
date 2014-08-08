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

package boofcv.abst.filter.binary;

import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestGlobalEntropyBinaryFilter {
	Random rand = new Random(234);

	@Test
	public void compare() {
		Class imageTypes[] = new Class[]{ImageUInt8.class,ImageFloat32.class};

		for( Class type : imageTypes ) {

			ImageSingleBand input = GeneralizedImageOps.createSingleBand(type, 30, 40);
			ImageUInt8 found = new ImageUInt8(30,40);
			ImageUInt8 expected = new ImageUInt8(30,40);

			GImageMiscOps.fillUniform(input, rand, 0, 200);

			GlobalEntropyBinaryFilter alg = new GlobalEntropyBinaryFilter(0,256,true, ImageType.single(type));

			alg.process(input,found);
			double threshold = GThresholdImageOps.computeEntropy(input,0,256);
			GThresholdImageOps.threshold(input,expected,threshold,true);

			BoofTesting.assertEquals(found, expected, 0);
		}
	}
}