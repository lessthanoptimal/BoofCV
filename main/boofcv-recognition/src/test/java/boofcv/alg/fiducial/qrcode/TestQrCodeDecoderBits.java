/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestQrCodeDecoderBits {
	@Test
	public void applyErrorCorrection() {
		fail("implement");
	}

	@Test
	public void copyFromRawData() {
		fail("implement");
	}

	@Test
	public void decodeMessage() {
		fail("implement");
	}

	@Test
	public void alignToBytes() {
		assertEquals(0, QrCodeDecoderBits.alignToBytes(0));
		assertEquals(8, QrCodeDecoderBits.alignToBytes(1));
		assertEquals(8, QrCodeDecoderBits.alignToBytes(7));
		assertEquals(8, QrCodeDecoderBits.alignToBytes(8));
		assertEquals(16, QrCodeDecoderBits.alignToBytes(9));
	}

	@Test
	public void checkPaddingBytes() {
		fail("implement");
	}
}
