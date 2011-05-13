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

import gecv.alg.drawing.impl.ImageInitialization_F32;
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

	int regionWidth;
	int regionHeight;
	float tl_x;
	float tl_y;

	/**
	 * Tell it to copy a region in the center
	 */
	@Test
	public void checkCenter() {
		checkRegion(10, 15, 2.11f, 5.23f);
	}

	/**
	 * See if it handles edge conditions gracefully
	 */
	@Test
	public void checkBottomRightEdge() {
		checkRegion(10, 15, width - 10, height - 15);
	}

	/**
	 * Compare region against the value returned by get BilinearPixel_F32
	 */
	public void checkRegion(int regionWidth, int regionHeight, float x, float y) {
		ImageFloat32 img = new ImageFloat32(width, height);
		ImageInitialization_F32.randomize(img, rand, 0, 100);

		this.regionWidth = regionWidth;
		this.regionHeight = regionHeight;
		this.tl_x = x;
		this.tl_y = y;
		GecvTesting.checkSubImage(this, "region", false, img);
	}

	public void region(ImageFloat32 img) {
		BilinearPixel_F32 interpPt = new BilinearPixel_F32();
		BilinearRectangle_F32 interp = new BilinearRectangle_F32();
		interp.setImage(img);
		interpPt.setImage(img);

		float data[] = new float[regionWidth * regionHeight];

		interp.region(tl_x, tl_y, data, regionWidth, regionHeight);

		int i = 0;
		for (int y = 0; y < regionHeight; y++) {
			for (int x = 0; x < regionWidth; x++) {
				assertEquals(interpPt.get(x + tl_x, y + tl_y), data[i++], 1e-4);
			}
		}
	}
}
