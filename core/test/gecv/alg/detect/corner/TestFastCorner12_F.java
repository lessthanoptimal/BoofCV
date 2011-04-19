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

package gecv.alg.detect.corner;

import gecv.struct.image.ImageFloat32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestFastCorner12_F {
	private int[] offsets;

	int width = 7;
	float centerVal = 100;

	ImageFloat32 img;

	public TestFastCorner12_F() {

		int center = 3 * width + 3;

		offsets = new int[16];
		offsets[0] = center - 3;
		offsets[1] = center - 3 - width;
		offsets[2] = center - 2 - 2 * width;
		offsets[3] = center - 1 - 3 * width;
		offsets[4] = center - 3 * width;
		offsets[5] = center + 1 - 3 * width;
		offsets[6] = center + 2 - 2 * width;
		offsets[7] = center + 3 - width;
		offsets[8] = center + 3;
		offsets[9] = center + 3 + width;
		offsets[10] = center + 2 + 2 * width;
		offsets[11] = center + 1 + 3 * width;
		offsets[12] = center + 3 * width;
		offsets[13] = center - 1 + 3 * width;
		offsets[14] = center - 2 + 2 * width;
		offsets[15] = center - 3 + width;

		img = new ImageFloat32(width, width);
	}

	/**
	 * Create a set of synthetic images and see if it correctly identifies them as corners.
	 */
	@Test
	public void testPositive() {
		FastCorner12_F corner = new FastCorner12_F(img, 20, 12);

		// pixels in circle are lower than threshold
		for (int i = 0; i < 15; i++) {
			setSynthetic(img, i, 12, (centerVal - 50));

			corner.setInput(img);
			corner.process();

			assertEquals(1, countNonZero(corner.getIntensity()));
			assertEquals(1, corner.getNumFeatures());
			// feature intensity should be positive and more than zero
			float intensity = corner.getIntensity().data[corner.getFeatures()[0]];
			assertTrue(intensity > 0);
		}

		// pixels in circle are higher than threshold
		for (int i = 0; i < 15; i++) {
			setSynthetic(img, i, 12, (centerVal + 50));

			corner.process();

			assertEquals(1, countNonZero(corner.getIntensity()));
			assertEquals(1, corner.getNumFeatures());
			assertTrue(corner.getIntensity().data[corner.getFeatures()[0]] > 0);
		}

		// longer than needed
		for (int i = 0; i < 15; i++) {
			setSynthetic(img, i, 13, (centerVal + 50));

			corner.process();

			assertEquals(1, countNonZero(corner.getIntensity()));
			assertEquals(1, corner.getNumFeatures());
			assertTrue(corner.getIntensity().data[corner.getFeatures()[0]] > 0);
		}

	}

	private static int countNonZero(ImageFloat32 img) {
		float[] data = img.data;

		int ret = 0;
		for (float aData : data) {
			if (aData != 0)
				ret++;
		}
		return ret;
	}

	/**
	 * See if it classifies a circle that is too short
	 */
	@Test
	public void testNegativeShort() {
		FastCorner12_F corner = new FastCorner12_F(img, 20, 12);

		for (int i = 0; i < 15; i++) {
			setSynthetic(img, i, 11, (centerVal + 50));

			corner.process();

			assertEquals(0, countNonZero(corner.getIntensity()));
			assertEquals(0, corner.getNumFeatures());
		}
	}

	/**
	 * Both pixels that are too high and low, but exceed the threshold are mixed
	 */
	@Test
	public void testNegativeMixed() {
		FastCorner12_F corner = new FastCorner12_F(img, 20, 12);

		for (int i = 0; i < 15; i++) {
			setSynthetic(img, i, 12, (centerVal + 50));

			img.data[offsets[(i + 7) % offsets.length]] = centerVal - 50;

			corner.process();

			assertEquals(0, countNonZero(corner.getIntensity()));
		}
	}

	private void setSynthetic(ImageFloat32 img, int start, int length, float outerVal) {
		float data[] = img.data;

		int endA = start + length;
		int endB;

		if (endA > offsets.length) {
			endB = endA - offsets.length;
			endA = offsets.length;
		} else {
			endB = 0;
		}

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < width; j++) {
				img.set(i, j, centerVal);
			}
		}

		for (int i = start; i < endA; i++) {
			data[offsets[i]] = outerVal;
		}

		for (int i = 0; i < endB; i++) {
			data[offsets[i]] = outerVal;
		}
	}

	@Test
	public void testSubImage() {
		fail("implement");
	}

}