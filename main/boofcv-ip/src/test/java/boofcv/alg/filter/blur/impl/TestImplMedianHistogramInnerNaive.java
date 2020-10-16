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

package boofcv.alg.filter.blur.impl;

import boofcv.BoofTesting;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.GrowArray;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestImplMedianHistogramInnerNaive extends BoofStandardJUnit {

	@Test
	public void compareToSort() {
		GrayU8 image = new GrayU8(20,30);
		ImageMiscOps.fillUniform(image,new Random(234), 0, 100);

		GrayU8 found = new GrayU8( image.width , image.height );
		GrayU8 expected = new GrayU8( image.width , image.height );

		BoofTesting.checkSubImage(this, "compareToSort", true, image, found, expected);
	}

	public void compareToSort(GrayU8 image, GrayU8 found, GrayU8 expected) {
		for( int radius = 1; radius <= 3; radius++ ) 
		{
			ImplMedianHistogramInnerNaive.process(image,found,radius,radius,null,null);
			ImplMedianSortNaive.process(image,expected,radius,radius,(GrowArray)null);

			BoofTesting.assertEqualsInner(expected, found, 0, radius, radius, false);
		}
	}
}
