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
import boofcv.alg.fiducial.qrcode.ReedSolomonCodes_U8;
import boofcv.misc.BoofMiscOps;
import org.ddogleg.struct.DogArray_I8;

/**
 * Encodes and decodes binary data for mode message
 *
 * @author Peter Abeles
 */
public class AztecCodecMode {
	// Error correction for encoding the message mode
	ReedSolomonCodes_U8 rscodes = new ReedSolomonCodes_U8(4, 19);

	// Storage for input and output when computing ECC bits
	DogArray_I8 eccInput = new DogArray_I8();
	DogArray_I8 eccOutput = new DogArray_I8();

	/**
	 * Encodes the binary data for the mode message for the provided marker. ECC is computed using Reed-Solomon
	 * encoding with 4-bit Galois Fields.
	 *
	 * @param marker (Input) Marker which is to be encoded
	 * @param bits (Output) Encoded binary data
	 */
	public void encodeMode( AztecCode marker, PackedBits8 bits ) {
		bits.resize(0);

		switch (marker.structure) {
			case COMPACT -> {
				bits.append(marker.dataLayers - 1, 2, false);
				bits.append(marker.messageWordCount - 1, 6, false);
				eccInput.resize(2);
				rscodes.generatorAztec(5);
			}
			case FULL -> {
				bits.append(marker.dataLayers - 1, 5, false);
				bits.append(marker.messageWordCount - 1, 11, false);
				eccInput.resize(4);
				rscodes.generatorAztec(6);
			}
		}

		// Convert into 4-bit words for ECC
		for (int word = 0; word < eccInput.size; word++) {
			eccInput.data[word] = (byte)bits.read(word*4, 4, true);
		}
		rscodes.computeECC(eccInput, eccOutput);

		eccInput.printHex();
		eccOutput.printHex();
		System.out.println();

		// Append resulting check words from ECC to bits data
		for (int word = 0; word < eccOutput.size; word++) {
			bits.append(eccOutput.get(word), 4, false);
		}
	}

	/**
	 * Applies error correction to bits then decodes the values. Save results into marker. Returns true if successful
	 */
	public boolean decodeMode( PackedBits8 bits, AztecCode marker ) {
		if (!correctDataBits(bits, marker.structure))
			return false;

		switch (marker.structure) {
			case COMPACT -> {
				marker.dataLayers = bits.read(0, 2, true) + 1;
				marker.messageWordCount = bits.read(2, 6, true) + 1;
			}
			case FULL -> {
				marker.dataLayers = bits.read(0, 5, true) + 1;
				marker.messageWordCount = bits.read(5, 11, true) + 1;
			}
		}

		return true;
	}

	/**
	 * Corrects the data portion of the packet and re-write the input 'bits' array with just the corrected data.
	 *
	 * @param bits (Input) all bits, data and ECC. (Output) corrected data
	 * @param structure Which type of marker was encoded
	 * @return true if successful or false if it failed
	 */
	boolean correctDataBits( PackedBits8 bits, AztecCode.Structure structure ) {
		switch (structure) {
			case COMPACT -> {
				BoofMiscOps.checkEq(28, bits.size, "Invalid number of bits");
				eccInput.resize(2);
				eccOutput.resize(5);
			}
			case FULL -> {
				BoofMiscOps.checkEq(40, bits.size, "Invalid number of bits");
				eccInput.resize(4);
				eccOutput.resize(6);
			}
		}
		rscodes.generatorAztec(eccOutput.size);

		// Convert into 4-bit words for ECC
		int bitLocation = 0;
		for (int word = 0; word < eccInput.size; word++, bitLocation += 4) {
			eccInput.data[word] = (byte)bits.read(bitLocation, 4, true);
		}

		// Convert into 4-bit words for ECC
		for (int word = 0; word < eccOutput.size; word++, bitLocation += 4) {
			eccOutput.data[word] = (byte)bits.read(bitLocation, 4, true);
		}

		eccInput.printHex();
		eccOutput.printHex();
		System.out.println();
		if (!rscodes.correct(eccInput, eccOutput))
			return false;
		eccInput.printHex();
		System.out.println();

		// Copy results into bits. Just the data and no ECC
		bits.resize(0);
		for (int word = 0; word < eccInput.size; word++) {
			bits.append(eccInput.get(word), 4, false);
		}
		return true;
	}
}
