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

import boofcv.alg.fiducial.aztec.AztecCode.Encodings;
import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.alg.fiducial.qrcode.ReedSolomonCodes_U16;
import org.ddogleg.struct.DogArray_I16;
import org.ddogleg.struct.DogArray_I8;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Encodes the data message into binary data.
 *
 * @author Peter Abeles
 */
public class AztecEncoder {
	// generates ECC for different Galois Fields. Which one is used depends on how large the marker is
	ReedSolomonCodes_U16 ecc6 = new ReedSolomonCodes_U16(6, 67);
	ReedSolomonCodes_U16 ecc8 = new ReedSolomonCodes_U16(8, 301);
	ReedSolomonCodes_U16 ecc10 = new ReedSolomonCodes_U16(10, 1033);
	ReedSolomonCodes_U16 ecc12 = new ReedSolomonCodes_U16(12, 4201);

	// The data portion of the message converted into a format ECC generation can understand
	DogArray_I16 storageDataWords = new DogArray_I16();
	DogArray_I16 storageEccWords = new DogArray_I16();

	// maker which is going to have the encoding stored on it
	AztecCode workMarker = new AztecCode();

	// Workspace for encoding each segment
	PackedBits8 bits = new PackedBits8();

	/**
	 * Amount of error correction is determined by this number. ecc_words = errorCorrectionLength*code_words - 3.
	 * Larger values will require more data and larger markers, but do offer more fault tolerance.
	 *
	 * See G.2 in ISO document.
	 */
	public double errorCorrectionLength = 0.77;

	private final List<MessageSegment> segments = new ArrayList<>();

	public AztecEncoder() {reset();}

	public void reset() {
		workMarker.reset();
		// default to full since that's what is needed 99.99% of the time
		workMarker.structure = AztecCode.Structure.FULL;
		workMarker.dataLayers = -1;
		segments.clear();
	}

	public AztecEncoder setEcc( double fraction ) {
		errorCorrectionLength = fraction;
		return this;
	}

	public AztecEncoder setStructure( AztecCode.Structure structure ) {
		this.workMarker.structure = structure;
		return this;
	}

	public AztecEncoder setLayers( int numLayers ) {
		workMarker.dataLayers = numLayers;
		return this;
	}

	public AztecEncoder addUpper( String message ) {
		message = message.toUpperCase();
		var values = new DogArray_I8(message.length());
		for (int i = 0; i < message.length(); i++) {
			char c = message.charAt(i);
			if (c == ' ') {
				values.add(1);
				continue;
			}
			int value = c - 'A';
			if (value >= 26) {
				throw new IllegalArgumentException("Only space and letters in the alphabet are allowed, not '" + c + "'");
			}
			values.add((char)(value + 2));
		}
		segments.add(new MessageSegment(Encodings.UPPER, values, message));
		return this;
	}

	public AztecEncoder addLower( String message ) {
		message = message.toLowerCase();
		var values = new DogArray_I8(message.length());
		for (int i = 0; i < message.length(); i++) {
			char c = message.charAt(i);
			if (c == ' ') {
				values.add(1);
				continue;
			}
			int value = c - 'a';
			if (value >= 26) {
				throw new IllegalArgumentException("Only space and letters in the alphabet are allowed, not '" + c + "'");
			}
			values.add((char)(value + 2));
		}
		segments.add(new MessageSegment(Encodings.LOWER, values, message));
		return this;
	}

	public AztecEncoder addMixed( String message ) {
		byte[] ascii = message.getBytes(StandardCharsets.US_ASCII);
		var values = new DogArray_I8(ascii.length);
		for (int i = 0; i < ascii.length; i++) {
			int v = ascii[i]%0xFF;
			if (v == 32) {
				values.add(0);
			} else if (v >= 1 && v <= 13) {
				values.add((byte)(v + 1));
			} else if (v >= 27 && v <= 31) {
				values.add((byte)(v - 27 + 15));
			} else if (v == 64) {
				values.add(20);
			} else if (v >= 92 && v <= 96) {
				values.add((byte)(v - 92 + 21));
			} else if (v == 124) {
				values.add(25);
			} else if (v == 126) {
				values.add(26);
			} else if (v == 127) {
				values.add(27);
			} else {
				throw new IllegalArgumentException("Invalid ascii " + v);
			}
		}

		segments.add(new MessageSegment(Encodings.MIXED, values, message));
		return this;
	}

	public AztecEncoder addPunctuation( String message ) {
		var values = new DogArray_I8(message.length());
		for (int i = 0; i < message.length(); i++) {
			char a = message.charAt(i);
			// some punctuations are two characters
			if (i + 1 < message.length()) {
				char b = message.charAt(i + 1);
				boolean matched = true;
				if (a == '\n' && b == '\r') {
					values.add(2);
				} else if (b == ' ') {
					if (a == '.') {
						values.add(3);
					} else if (a == ',') {
						values.add(4);
					} else if (a == ':') {
						values.add(5);
					} else {
						matched = false;
					}
				} else {
					matched = false;
				}

				// jump to next character and iterate
				if (matched) {
					i++;
					continue;
				}
			}
			if (a == 13) {
				values.add(1);
			} else if (a >= 33 && a <= 47) {
				values.add(a - 33 + 6);
			} else if (a >= 58 && a <= 59) {
				values.add(a - 58 + 21);
			} else if (a >= 60 && a <= 63) {
				values.add(a - 60 + 23);
			} else if (a == 91) {
				values.add(27);
			} else if (a == 93) {
				values.add(28);
			} else if (a == 123) {
				values.add(29);
			} else if (a == 125) {
				values.add(30);
			} else {
				throw new IllegalArgumentException("Invalid ascii " + (int)a);
			}
		}
		segments.add(new MessageSegment(Encodings.PUNCT, values, message));
		return this;
	}

	public AztecEncoder addDigit( String message ) {
		var values = new DogArray_I8(message.length());
		for (int i = 0; i < message.length(); i++) {
			char c = message.charAt(i);
			if (c == ' ') {
				values.add(1);
			} else if (c >= 48 && c <= 57) {
				values.add(c - 48 + 2);
			} else if (c == ',') {
				values.add(12);
			} else if (c == '.') {
				values.add(13);
			}
		}
		segments.add(new MessageSegment(Encodings.DIGIT, values, message));
		return this;
	}

	/**
	 * Encodes into binary data all the segments, computes ECC, and constructs the final marker. Anything
	 * which is not explicitly specified will have a reasonable value auto selected.
	 *
	 * @return Returns a new marker.
	 */
	public AztecCode fixate() {
		segmentsToEncodedBits();
		selectNumberOfLayers();
		bitsToWords();
		computeErrorCorrectionWords();
		return copyIntoResults();
	}

	/**
	 * Encodes all the segments into the {@link #bits},
	 */
	private void segmentsToEncodedBits() {
		Encodings currentEncoding = Encodings.UPPER;
		for (int segIdx = 0; segIdx < segments.size(); segIdx++) {
			MessageSegment m = segments.get(segIdx);
			// Switch into the new encoding
			boolean latched = transitionIntoEncoding(currentEncoding, m);

			// Write the data
			switch (m.encoding) {
				case UPPER -> append(m.data, 5);
				case LOWER -> append(m.data, 5);
				case MIXED -> append(m.data, 5);
				case PUNCT -> append(m.data, 5);
				case DIGIT -> append(m.data, 4);
				default -> throw new IllegalArgumentException("Encoding not yet supported: " + m.encoding);
			}

			workMarker.message += m.message;

			// Update the current encoding
			if (latched) {
				currentEncoding = m.encoding;
			}
		}
	}

	/**
	 * Compute the value of error correction words. The number of words depends on the message size and requested
	 * amount of correction.
	 */
	private void computeErrorCorrectionWords() {
		// it will use all possible code words in the marker for error correction
		int maxMarkerWords = workMarker.structure.getCodewords(workMarker.dataLayers);
		int actualEccWords = maxMarkerWords - storageDataWords.size;
		storageEccWords.resize(actualEccWords);

		// Compute ECC with the appropriate coefficients depending on the word size
		switch (workMarker.getWordBitCount()) {
			case 6 -> computeEcc(ecc6, actualEccWords);
			case 8 -> computeEcc(ecc8, actualEccWords);
			case 10 -> computeEcc(ecc10, actualEccWords);
			case 12 -> computeEcc(ecc12, actualEccWords);
			default -> throw new RuntimeException("BUG!");
		}
	}

	/**
	 * Converts the raw data stores in 'bits' into words which are written to the marker.
	 * Words which are all zeros or ones are handled as a special case for image processing
	 * reasons.
	 */
	void bitsToWords() {
		// Convert the binary data into a format ECC computation can understand
		int wordBitCount = workMarker.getWordBitCount();

		// compute value of all ones in a word
		int ones = 0b11_1111;
		for (int i = 6; i < wordBitCount; i++) {
			ones |= 1 << i;
		}

		storageDataWords.reset();
		int bitLocation = 0;
		while (bitLocation < bits.size) {
			// Make sure it doesn't read past the last bit
			int readBits = Math.min(bits.size - bitLocation, wordBitCount);

			// Get the word's value
			short word = (short)bits.read(bitLocation, readBits, true);

			// If a word is all zeros or all ones that's a special case. The
			// least-significant bit is zero to the opposition value and that bit
			// is punted to the next word
			if (word == 0 && readBits == wordBitCount) {
				storageDataWords.add((short)0b0000_0000_0000_0001);
				bitLocation += readBits - 1;
			} else if (word == (short)ones) {
				storageDataWords.add(ones & 0xFFFE);
				bitLocation += readBits - 1;
			} else {
				storageDataWords.add(word);
				bitLocation += readBits;
			}
		}

		// Record how many words are in the encoded message
		workMarker.messageWordCount = storageDataWords.size;
	}

	/**
	 * Selects the minim number of layers needed to store the data and error correction information
	 */
	void selectNumberOfLayers() {
		// number of data code words
		int dataWords = storageDataWords.size;

		// Compute number of error correction words
		int eccWords = (int)Math.ceil(errorCorrectionLength*dataWords - 3);

		int minimumTotalWords = dataWords + eccWords;

		// If the number of layers has already been selected, make sure it can store all the data
		if (workMarker.dataLayers > 0) {
			if (workMarker.structure.getCodewords(workMarker.dataLayers) < minimumTotalWords)
				throw new RuntimeException("Selected number of layers is insufficient");
			return;
		}
		// Determine the smallest marker size that can contain this data
		int maxLayers = workMarker.structure.maxDataLayers;

		// Sanity check to make sure this can be encoded
		int maxPossibleWords = workMarker.structure.getCodewords(maxLayers - 1);
		if (minimumTotalWords > maxPossibleWords) {
			// See if the problem can be fixed by reducing error correction
			if (dataWords <= maxPossibleWords) {
				throw new IllegalArgumentException("Too large with ECC level. Try reducing amount of ECC");
			} else {
				throw new IllegalArgumentException("Too large to be encoded inside a single marker");
			}
		}

		// Select the smallest number of layers to store the data
		for (int layers = 1; layers <= maxLayers; layers++) {
			int numWords = workMarker.structure.getCodewords(layers);
			if (numWords >= minimumTotalWords) {
				workMarker.dataLayers = layers;
				break;
			}
		}
	}

	/** Copies data and other parameters into a new marker */
	AztecCode copyIntoResults() {
		AztecCode results = new AztecCode().setTo(workMarker);

		// To make it easier to process we will write the data words into bits, which internally
		// uses a byte array
		int wordBitCount = workMarker.getWordBitCount();
		int bitCount = bits.size;
		bits.resize(0);
		for (int wordIdx = 0; wordIdx < storageDataWords.size; wordIdx++) {
			// the last word will get corrupted if this isn't taken in account
			int amount = Math.min(bitCount - wordIdx*wordBitCount, wordBitCount);
			bits.append(storageDataWords.get(wordIdx) & 0xFFFF, amount, false);
		}

		// copy the message into "corrected"
		bits.size = storageDataWords.size*wordBitCount;
		// line above is done to make sure the last byte is a full word
		results.corrected = new byte[bits.size/8 + (bits.size%8 == 0 ? 0 : 1)];
		System.arraycopy(bits.data, 0, results.corrected, 0, results.corrected.length);

		// Add ecc data to bits
		for (int word = 0; word < storageEccWords.size; word++) {
			bits.append(storageEccWords.get(word) & 0xFFFF, wordBitCount, false);
		}

		// Copy the entire encoded with ECC message into raw bits
		results.rawbits = new byte[bits.size/8 + (bits.size%8 == 0 ? 0 : 1)];
		System.arraycopy(bits.data, 0, results.rawbits, 0, results.rawbits.length);

		return results;
	}

	private void computeEcc( ReedSolomonCodes_U16 ecc, int actualEccWords ) {
		ecc.generatorAztec(actualEccWords);
		ecc.computeECC(storageDataWords, storageEccWords);
	}

	/**
	 * Transitions from one encoding into another.
	 *
	 * @return If it's latched to the new encoding or false if not
	 */
	private boolean transitionIntoEncoding( Encodings currentEncoding, MessageSegment m ) {
		boolean latched = true;
		// @formatter:off
		switch (currentEncoding) {
			case UPPER -> {
				switch (m.encoding) {
					case UPPER -> {}
					case LOWER -> append(28, 5);
					case MIXED -> append(29, 5);
					case DIGIT -> append(30, 5);
					case BYTE -> append(31, 5);
					case PUNCT -> { append(0, 5); latched = false; }
					default -> throwUnsupported(currentEncoding, m.encoding);
				}
			}
			case LOWER -> {
				switch (m.encoding) {
					case LOWER -> {}
					case UPPER -> { append(28, 5); latched = false; }
					case MIXED -> append(29, 5);
					case DIGIT -> append(30, 5);
					case BYTE -> append(31, 5);
					case PUNCT -> { append(0, 5); latched = false; }
					default -> throwUnsupported(currentEncoding, m.encoding);
				}
			}
			case MIXED -> {
				switch (m.encoding) {
					case MIXED -> {}
					case LOWER -> append(28, 5);
					case UPPER -> append(29, 5);
					case PUNCT -> {
						if (m.data.size == 1) {
							append(0, 5); // shift
							latched = false;
						} else {
							append(30, 5); // latch
						}
					}
					case DIGIT -> { append(29, 5); append(30, 5); }
					case BYTE -> append(31, 5);
					default -> throwUnsupported(currentEncoding, m.encoding);
				}
			}
			// page 37
			case PUNCT -> {
				switch (m.encoding) {
					case PUNCT -> {}
					case LOWER -> { append(31, 5); append(28, 5); }
					case UPPER -> append(31, 5);
					case MIXED -> { append(31, 5); append(29, 5); }
					case DIGIT -> { append(31, 5); append(30, 5); }
					case BYTE -> { append(31, 5); append(31, 5); }
					default -> throwUnsupported(currentEncoding, m.encoding);
				}
			}
			case DIGIT -> {
				switch (m.encoding) {
					case DIGIT -> {}
					case LOWER -> { append(14, 4); append(28, 5); }
					case UPPER -> {
						// if it's a single digit don't latch it
						if (m.data.size == 1) {
							latched = false;
							append(15, 4);
						} else {
							append(14, 4);
						}
					}
					case PUNCT -> { append(0, 4); latched = false; }
					case MIXED -> { append(14, 4); append(29, 5); }
					case BYTE -> { append(14, 4); append(31, 5); }
					default -> throwUnsupported(currentEncoding, m.encoding);
				}
			}
			default -> throw new IllegalArgumentException("Unsupported encoding " + m.encoding);
		}
		// @formatter:on
		return latched;
	}

	/**
	 * Adds the specified data to bits.
	 *
	 * @param value Value being written
	 * @param wordBits Number of bits to write
	 */
	private void append( int value, int wordBits ) {
		bits.append(value, wordBits, false);
	}

	/**
	 * Copies the array into bits.
	 *
	 * @param message message to be copied
	 * @param wordBits How many bits in each byte it should write
	 */
	private void append( DogArray_I8 message, int wordBits ) {
		for (int i = 0; i < message.size; i++) {
			bits.append(message.data[i] & 0xFF, wordBits, false);
		}
	}

	/** Convenience function for throwing an exception for incompatible transitions */
	private static int throwUnsupported( Encodings src, Encodings dst ) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Can't transition from " + src + " to " + dst);
	}

	public static class MessageSegment {
		public Encodings encoding;
		public DogArray_I8 data;
		public String message;

		public MessageSegment( Encodings encoding,
							   DogArray_I8 data,
							   String message ) {
			this.encoding = encoding;
			this.data = data;
			this.message = message;
		}
	}
}
