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

package boofcv.struct.gss;

import boofcv.alg.transform.gss.ScaleSpacePyramid;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestScaleSpacePyramid {

	Random rand = new Random(234);
	int width = 50;
	int height = 60;

	/**
	 * This is a hard class to test.  It can't be compared directly against {@link GaussianScaleSpace}
	 * because they won't produce exactly the same results and visually it looks a bit different.  The
	 * math has been inspected a couple of times.
	 *
	 * So all this test does is see if it will process an image without blowing up.
	 */
	@Test
	public void reallyStupidTest() {
		ScaleSpacePyramid<ImageFloat32> ss = new ScaleSpacePyramid<ImageFloat32>(ImageFloat32.class,1,2,3,4);

		ImageFloat32 input = new ImageFloat32(width,height);
		GeneralizedImageOps.randomize(input,rand,0,100);

		ss.setImage(input);

		assertEquals(4,ss.getNumLayers());

		for( int i = 0; i < 4; i++ ) {
			assertTrue(GeneralizedImageOps.sum(ss.getLayer(i))>0);
		}
	}
}
