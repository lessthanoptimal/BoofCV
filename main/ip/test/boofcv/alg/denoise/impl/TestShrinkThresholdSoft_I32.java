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

package boofcv.alg.denoise.impl;

import boofcv.alg.denoise.ShrinkThresholdRule;
import boofcv.alg.denoise.wavelet.ShrinkThresholdSoft_I32;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestShrinkThresholdSoft_I32 {
	int width = 10;
	int height = 20;

	@Test
	public void basicTest() {
		performBasicSoftTest(new GrayS32(width,height),new ShrinkThresholdSoft_I32());
	}

	public static <T extends ImageGray> void performBasicSoftTest(T image , ShrinkThresholdRule<T> rule ) {
		final int height = image.height;
		final int width = image.width;

		GImageGray a = FactoryGImageGray.wrap(image);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				a.set(x,y,x);
			}
		}

		rule.process(image,4);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				if( x < 4 ) {
					assertEquals(0,a.get(x,y).floatValue(),1e-4);
				} else {
					assertEquals(x-4,a.get(x,y).floatValue(),1e-4);
				}
			}
		}
	}
}
