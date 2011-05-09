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

package gecv.alg.filter.basic;

import gecv.alg.drawing.impl.BasicDrawing_I8;
import gecv.struct.image.ImageUInt8;
import org.junit.Test;

import java.util.Random;

import static junit.framework.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestGrayImageOps {

	int width = 10;
	int height = 15;
	Random rand = new Random(234);

	@Test
	public void invert() {
		ImageUInt8 input = new ImageUInt8(width, height);
		BasicDrawing_I8.randomize(input, rand);

		ImageUInt8 output = GrayImageOps.invert(input, null);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				assertEquals(255 - input.get(x, y), output.get(x, y));
			}
		}
	}

	@Test
	public void brighten() {
		ImageUInt8 input = new ImageUInt8(width, height);
		BasicDrawing_I8.fill(input, 23);

		ImageUInt8 output = GrayImageOps.brighten(input, 10, null);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				assertEquals(33, output.get(x, y));
			}
		}

		// check to see how well it sets the ceiling
		output = GrayImageOps.brighten(output, 230, null);
		assertEquals(255, output.get(5, 6));

		// check it flooring to zero
		BasicDrawing_I8.fill(input, 23);
		output = GrayImageOps.brighten(input, -50, null);
		assertEquals(0, output.get(5, 6));
	}

	@Test
	public void stretch() {
		ImageUInt8 input = new ImageUInt8(width, height);
		BasicDrawing_I8.fill(input, 23);

		ImageUInt8 output = GrayImageOps.stretch(input, 2.5, 10, null);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				assertEquals(67, output.get(x, y));
			}
		}

		// check to see how well it sets the ceiling
		output = GrayImageOps.stretch(output, 4, 10, null);
		assertEquals(255, output.get(5, 6));

		// check it flooring to zero
		BasicDrawing_I8.fill(input, 23);
		output = GrayImageOps.stretch(input, -1, 2, null);
		assertEquals(0, output.get(5, 6));
	}
}
