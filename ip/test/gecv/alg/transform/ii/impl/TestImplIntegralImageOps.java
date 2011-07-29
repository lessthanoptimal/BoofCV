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

package gecv.alg.transform.ii.impl;

import gecv.core.image.FactorySingleBandImage;
import gecv.core.image.GeneralizedImageOps;
import gecv.core.image.SingleBandImage;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt32;
import gecv.struct.image.ImageUInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestImplIntegralImageOps {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;

	@Test
	public void testFloat32() {
		ImageFloat32 a = new ImageFloat32(width,height);
		GeneralizedImageOps.randomize(a,rand,0,100);

		ImageFloat32 b = new ImageFloat32(width,height);
		ImplIntegralImageOps.process(a,b);

		GecvTesting.checkSubImage(this,"checkResults",true,a,b);
	}

	@Test
	public void testUInt8() {
		ImageUInt8 a = new ImageUInt8(width,height);
		GeneralizedImageOps.randomize(a,rand,0,100);

		ImageSInt32 b = new ImageSInt32(width,height);
		ImplIntegralImageOps.process(a,b);

		GecvTesting.checkSubImage(this,"checkResults",true,a,b);
	}

	public void checkResults(ImageBase a, ImageBase b) {

		SingleBandImage aa = FactorySingleBandImage.wrap(a);
		SingleBandImage bb = FactorySingleBandImage.wrap(b);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				double total = 0;

				for( int i = 0; i <= y; i++ ) {
					for( int j = 0; j <= x; j++ ) {
						total += aa.get(j,i).doubleValue();
					}
				}

				assertEquals(x+" "+y,total,bb.get(x,y).doubleValue(),1e-1);
			}
		}
	}
}
