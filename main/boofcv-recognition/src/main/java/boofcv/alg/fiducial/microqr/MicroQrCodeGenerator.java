/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.fiducial.qrcode.QrCodeCodeWordLocations;
import boofcv.alg.fiducial.qrcode.QrGeneratorBase;
import georegression.struct.point.Point2D_I32;

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

		if (renderData) {
			if (qr.rawbits.length != MicroQrCode.VERSION_INFO[qr.version].codewords)
				throw new RuntimeException("Unexpected length of raw data.");

			// mark which modules can store data
			bitLocations = QrCodeCodeWordLocations.microqr(qr.version).bits;

			int numBytes = bitLocations.size()/8;
			if (numBytes != qr.rawbits.length)
				throw new RuntimeException("Egads. unexpected length of qrcode raw data");

			// Render the output data
			renderData(qr.mask, qr.rawbits);
		}

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

	/**
	 * Renders the raw data bit output while applying the selected mask
	 */
	private void renderData(MicroQrCodeMaskPattern mask, byte[] rawbits) {
		int count = 0;

		int length = bitLocations.size() - bitLocations.size()%8;
		while (count < length) {
			int bits = rawbits[count/8] & 0xFF;

			int N = Math.min(8, bitLocations.size() - count);

			for (int i = 0; i < N; i++) {
				Point2D_I32 coor = bitLocations.get(count + i);
				int value = mask.apply(coor.y, coor.x, ((bits >> i) & 0x01));
//				int value = ((bits >> i ) & 0x01);
				if (value > 0) {
					square(coor.y, coor.x);
				}
			}
			count += 8;
		}
	}
}
