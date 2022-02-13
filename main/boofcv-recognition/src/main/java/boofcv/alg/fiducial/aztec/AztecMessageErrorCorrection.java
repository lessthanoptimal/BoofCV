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
import boofcv.alg.fiducial.qrcode.ReedSolomonCodes_U16;
import boofcv.misc.BoofMiscOps;
import org.ddogleg.struct.DogArray_I16;

/**
 * Contains functions for computing error correction code words and applying error correction to a message
 *
 * @author Peter Abeles
 */
public abstract class AztecMessageErrorCorrection {
	// Which Galois Fields is used depends on bits per word.
	protected final ReedSolomonCodes_U16 ecc6 = new ReedSolomonCodes_U16(6, 67, 1);
	protected final ReedSolomonCodes_U16 ecc8 = new ReedSolomonCodes_U16(8, 301, 1);
	protected final ReedSolomonCodes_U16 ecc10 = new ReedSolomonCodes_U16(10, 1033, 1);
	protected final ReedSolomonCodes_U16 ecc12 = new ReedSolomonCodes_U16(12, 4201, 1);

	// The data portion of the message converted into a format ECC generation can understand
	protected final DogArray_I16 storageDataWords = new DogArray_I16();
	protected final DogArray_I16 storageEccWords = new DogArray_I16();

	/**
	 * Computes ECC words from {@link #storageDataWords} and stores them in {@link #storageEccWords}.
	 *
	 * @param wordBits Number of bits in a word
	 * @param maxMarkerWords Maximum number of words that can be stored in the marker
	 */
	public void computeEccWords( int wordBits, int maxMarkerWords ) {
		int actualEccWords = maxMarkerWords - storageDataWords.size;
		storageEccWords.resize(actualEccWords);

		// Compute ECC with the appropriate coefficients depending on the word size
		switch (wordBits) {
			case 6 -> computeEcc(ecc6, actualEccWords);
			case 8 -> computeEcc(ecc8, actualEccWords);
			case 10 -> computeEcc(ecc10, actualEccWords);
			case 12 -> computeEcc(ecc12, actualEccWords);
			default -> throw new RuntimeException("BUG!");
		}
	}

	private void computeEcc( ReedSolomonCodes_U16 ecc, int actualEccWords ) {
		ecc.generator(actualEccWords);
		ecc.computeECC(storageDataWords, storageEccWords);
	}

	/**
	 * Applies error correction to the raw data inside the marker and adds the results to the marker while updating
	 * metadata such as number of bit errors detected.
	 *
	 * @param marker (Input) Marker with raw data (Output) Marker with corrected data
	 * @return true if successful
	 */
	public boolean applyErrorCorrection( AztecCode marker ) {
		extractWordsFromMarker(marker);

		int bitErrors = applyEcc(marker.getCapacityWords(), marker.getWordBitCount());

		if (bitErrors < 0)
			return false;

		// Save number of detected errors
		marker.totalBitErrors = bitErrors;

		// Save the corrected data
		int wordBitCount = marker.getWordBitCount();
		int messageBits = storageDataWords.size*wordBitCount;
		marker.corrected = new byte[BoofMiscOps.bitToByteCount(messageBits)];
		PackedBits8 bits = PackedBits8.wrap(marker.corrected, 0);
		for (int i = 0; i < storageDataWords.size; i++) {
			int value = storageDataWords.get(i) & 0xFFFF;
			bits.append(value, wordBitCount, false);
		}

		return true;
	}

	/**
	 * Extract saved message data from the marker and split it into data and ecc words
	 */
	void extractWordsFromMarker( AztecCode marker ) {
		int wordBitCount = marker.getWordBitCount();
		PackedBits8 bits = PackedBits8.wrap(marker.rawbits, marker.getCapacityBits());

		// convert the rawbits into a format ECC can understand
		storageDataWords.resize(marker.messageWordCount);
		storageEccWords.resize(marker.getCapacityWords() - marker.messageWordCount);

		int locationBits = 0;
		for (int i = 0; i < storageDataWords.size; i++, locationBits += wordBitCount) {
			storageDataWords.data[i] = (short)bits.read(locationBits, wordBitCount, true);
		}
		for (int i = 0; i < storageEccWords.size; i++, locationBits += wordBitCount) {
			storageEccWords.data[i] = (short)bits.read(locationBits, wordBitCount, true);
		}
	}

	/**
	 * Applies error correction using the correct encoding.
	 *
	 * @return Number of bit errors or -1 if it failed
	 */
	int applyEcc( int capacityWords, int wordBitCount ) {
		return switch (wordBitCount) {
			case 6 -> applyEcc(capacityWords, ecc6);
			case 8 -> applyEcc(capacityWords, ecc8);
			case 10 -> applyEcc(capacityWords, ecc10);
			case 12 -> applyEcc(capacityWords, ecc12);
			default -> throw new RuntimeException("Unexpected word size");
		};
	}

	/**
	 * Applies error correction to data portion of rawbits, then copies the results into marker.corrected.
	 *
	 * @return Number of bit errors or -1 if it failed
	 */
	int applyEcc( int capacityWords, ReedSolomonCodes_U16 ecc ) {
		// TODO check for words with all 0 and all 1 and mark word as a known erasure

		// Apply error correction
		ecc.generator(capacityWords - storageDataWords.size);
		if (!ecc.correct(storageDataWords, storageEccWords)) {
			return -1;
		}

		return ecc.getTotalErrors();
	}
}
