/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.transform.ii.impl;

import boofcv.core.image.FactoryGeneralizedSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
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
	public void transform_F32() {
		ImageFloat32 a = new ImageFloat32(width,height);
		GeneralizedImageOps.randomize(a,rand,0,100);

		ImageFloat32 b = new ImageFloat32(width,height);
		ImplIntegralImageOps.transform(a,b);

		BoofTesting.checkSubImage(this,"checkResults",true,a,b);
	}

	@Test
	public void transform_U8() {
		ImageUInt8 a = new ImageUInt8(width,height);
		GeneralizedImageOps.randomize(a,rand,0,100);

		ImageSInt32 b = new ImageSInt32(width,height);
		ImplIntegralImageOps.transform(a,b);

		BoofTesting.checkSubImage(this,"checkResults",true,a,b);
	}

	public void checkResults(ImageSingleBand a, ImageSingleBand b) {

		GImageSingleBand aa = FactoryGeneralizedSingleBand.wrap(a);
		GImageSingleBand bb = FactoryGeneralizedSingleBand.wrap(b);

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
