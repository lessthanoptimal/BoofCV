/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.microqr;

import boofcv.alg.fiducial.qrcode.PackedBits32;
import boofcv.alg.fiducial.qrcode.QrGeneratorBase;

/**
 * Generates an image of a Micro QR Code.
 *
 * @author Peter Abeles
 */
public class MicroQrCodeGenerator extends QrGeneratorBase {
	public MicroQrCodeGenerator render( MicroQrCode qr ) {
		numModules = MicroQrCode.totalModules(qr.version);
		moduleWidth = markerWidth/numModules;

		render.init();

		positionPattern(0, 0, qr.pp);

		timingPattern(7*moduleWidth, 0, moduleWidth, 0, numModules - 7);
		timingPattern(0, 7*moduleWidth, 0, moduleWidth, numModules - 7);

		formatInformation(qr);

		// TODO render data bits

		qr.bounds.set(0, 0, 0);
		qr.bounds.set(1, markerWidth, 0);
		qr.bounds.set(2, markerWidth, markerWidth);
		qr.bounds.set(3, 0, markerWidth);

		return this;
	}

	/** Renders format bits */
	private void formatInformation( MicroQrCode qr ) {
		PackedBits32 bits = formatInformationBits(qr);

		for (int i = 0; i < 15; i++) {
			if (bits.get(i) == 0) {
				continue;
			}
			if (i < 8) {
				square(i + 1, 8);
			} else {
				square(8, 15 - i);
			}
		}
	}

	static PackedBits32 formatInformationBits( MicroQrCode qr ) {
		var bits = new PackedBits32(15);
		bits.data[0] = qr.encodeFormatBits();
		bits.data[0] ^= MicroQrCode.FORMAT_MASK;
		return bits;
	}
}
