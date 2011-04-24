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

package gecv.alg.interpolate.impl;

import gecv.core.image.UtilImageFloat32;
import gecv.struct.image.ImageFloat32;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestBilinearRegion_F32 {

	Random rand = new Random(0xff34);

	int width = 320;
	int height = 240;

	/**
	 * Compare region against the value returned by get BilinearPixel_F32
	 */
	@Test
	public void region() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0, 100);

		GecvTesting.checkSubImage(this, "region", false, img);
	}

	public void region(ImageFloat32 img) {
		BilinearPixel_F32 interpPt = new BilinearPixel_F32();
		BilinearRegion_F32 interp = new BilinearRegion_F32();
		interp.setImage(img);
		interpPt.setImage(img);

		int width = 10;
		int height = 5;
		float data[] = new float[50];

		float tl_x = 2.11f;
		float tl_y = 5.23f;

		interp.region(tl_x, tl_y, data, width, height);

		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				assertEquals(interpPt.get(x + tl_x, y + tl_y), data[i++], 1e-4);
			}
		}
	}
}
