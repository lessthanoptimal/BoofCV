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

package boofcv.alg.segmentation.cc;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestConnectedTwoRowSpeckleFiller_F32 extends CommonConnectedTwoRowSpeckleFiller<GrayF32> {
	public TestConnectedTwoRowSpeckleFiller_F32() {
		super(ImageType.SB_F32);
	}

	/** Check row labeling on a normal situation without any edge cases */
	@Test void labelRow() {
		int width = 10;
		float[] row = new float[]{1, 2, 3, 4, 4, 4, 4, 5, 5, 2, 3, 4, 6, 7};
		int[] labels = new int[width];
		int[] counts = new int[width];
		int[] locations = new int[width];

		int regions = ConnectedTwoRowSpeckleFiller_F32.labelRow(row, 1, width, labels, counts, locations, 10.0f, 0.9f);

		assertEquals(6, regions);
		assertArrayEquals(new int[]{0, 1, 2, 2, 2, 2, 3, 3, 4, 5}, labels);
		assertArrayEquals(new int[]{1, 1, 4, 2, 1, 1, 0, 0, 0, 0}, counts);
		assertArrayEquals(new int[]{0, 1, 2, 6, 8, 9, 0, 0, 0, 0}, locations);
	}

	/** See if it handles elements equal to the fill value correctly */
	@Test void labelRow_FillValue() {
		int width = 10;
		float[] row = new float[]{1, 4, 3, 3, 4, 4, 4, 5, 5, 2, 3, 4, 6, 7};
		int[] labels = new int[width];
		int[] counts = new int[width];
		int[] locations = new int[width];

		int regions = ConnectedTwoRowSpeckleFiller_F32.labelRow(row, 1, width, labels, counts, locations, 4.0f, 0.9f);

		assertEquals(4, regions);
		assertArrayEquals(new int[]{-1, 0, 0, -1, -1, -1, 1, 1, 2, 3}, labels);
		assertArrayEquals(new int[]{2, 2, 1, 1, 0, 0, 0, 0, 0, 0}, counts);
		assertArrayEquals(new int[]{1, 6, 8, 9, 0, 0, 0, 0, 0, 0}, locations);
	}

	@Override protected ConnectedTwoRowSpeckleFiller<GrayF32> createTwoRow() {
		return new ConnectedTwoRowSpeckleFiller_F32();
	}

	@Override protected Object createArray( double... values ) {
		float[] out = new float[values.length];
		for (int i = 0; i < values.length; i++) {
			out[i] = (float)values[i];
		}
		return out;
	}
}
