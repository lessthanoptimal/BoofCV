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

package boofcv.alg.fiducial.aztec;

import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.alg.fiducial.qrcode.ReidSolomonCodes;
import org.ddogleg.struct.DogArray_I8;

/**
 * Encodes binary data for mode message and data message.
 *
 * @author Peter Abeles
 */
public class AztecEncoder {
	// Error correction for encoding the message mode
	ReidSolomonCodes eccMode = new ReidSolomonCodes(4, 19);

	// Storage for input and output when computing ECC bits
	DogArray_I8 eccInput = new DogArray_I8();
	DogArray_I8 eccOutput = new DogArray_I8();

	/**
	 * Encodes the binary data for the mode message for the provided marker. ECC is computed using Reid-Solomon
	 * encoding with 4-bit Galois Fields.
	 *
	 * @param marker (Input) Marker which is to be encoded
	 * @param bits (Output) Encoded binary data
	 */
	public void encodeModeMessage( AztecCode marker, PackedBits8 bits ) {
		bits.resize(0);

		switch (marker.structure) {
			case COMPACT -> {
				bits.append(marker.dataLayers - 1, 2, false);
				bits.append(marker.messageLength - 1, 6, false);
				eccInput.resize(2);
				eccMode.generatorAztec(5);
			}
			case FULL -> {
				bits.append(marker.dataLayers - 1, 5, false);
				bits.append(marker.messageLength - 1, 11, false);
				eccInput.resize(4);
				eccMode.generatorAztec(6);
			}
		}

		// Convert into 4-bit words for ECC
		for (int word = 0; word < eccInput.size; word++) {
			eccInput.data[word] = (byte)bits.read(word*4, 4, false);
		}
		eccMode.computeECC(eccInput, eccOutput);

		// Append resulting check words from ECC to bits data
		for (int word = 0; word < eccOutput.size; word++) {
			bits.append(eccOutput.get(word), 4, false);
		}
	}
}
