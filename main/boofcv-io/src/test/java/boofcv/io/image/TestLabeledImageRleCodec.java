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

package boofcv.io.image;

import boofcv.BoofTesting;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayS32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

class TestLabeledImageRleCodec extends BoofStandardJUnit {
	@Test void encode_decode() throws IOException {
		GrayS32 expected = new GrayS32(20, 30);
		ImageMiscOps.fillRectangle(expected, 1, 10, 8, 6, 7);
		ImageMiscOps.fillRectangle(expected, 2, 0, 1, 2, 7);
		ImageMiscOps.fillRectangle(expected, 3, 10, 25, 12, 5);

		GrayS32 found = new GrayS32(1, 1);

		var output = new ByteArrayOutputStream();
		LabeledImageRleCodec.encode(expected, output);

		var input = new ByteArrayInputStream(output.toByteArray());
		LabeledImageRleCodec.decode(input, found);

		BoofTesting.assertEquals(expected, found, 1e-8);
	}
}