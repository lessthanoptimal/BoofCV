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

import boofcv.alg.fiducial.aztec.AztecCode.Modes;
import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.misc.BoofMiscOps;
import org.ddogleg.struct.DogArray_I8;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Encodes the data message into binary data.
 *
 * @author Peter Abeles
 */
public class AztecEncoder extends AztecMessageErrorCorrection {
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
		segments.add(new MessageSegment(Modes.UPPER, values, message));
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
		segments.add(new MessageSegment(Modes.LOWER, values, message));
		return this;
	}

	public AztecEncoder addMixed( String message ) {
		byte[] ascii = message.getBytes(StandardCharsets.US_ASCII);
		var values = new DogArray_I8(ascii.length);
		for (int i = 0; i < ascii.length; i++) {
			int v = ascii[i]%0xFF;
			if (v == 32) {
				values.add(1);
			} else if (v >= 1 && v <= 13) {
				values.add((byte)(v + 1));
			} else if (v >= 27 && v <= 31) {
				values.add((byte)(v - 27 + 15));
			} else if (v == 64) {
				values.add(20);
			} else if (v == 92) {
				values.add(21);
			} else if (v >= 94 && v <= 96) {
				values.add((byte)(v - 94 + 22));
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

		segments.add(new MessageSegment(Modes.MIXED, values, message));
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
				if (a == '\r' && b == '\n') {
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
		segments.add(new MessageSegment(Modes.PUNCT, values, message));
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
		segments.add(new MessageSegment(Modes.DIGIT, values, message));
		return this;
	}

	public AztecEncoder addBytes( byte[] data, int offset, int length ) {
		var values = new DogArray_I8(length);
		values.size = length;
		System.arraycopy(data, offset, values.data, 0, length);
		segments.add(new MessageSegment(Modes.BYTE, values, new String(data, offset, length)));
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
		computeEccWords(workMarker.getWordBitCount(), workMarker.getCapacityWords());
		return copyIntoResults();
	}

	/**
	 * Encodes all the segments into the {@link #bits},
	 */
	void segmentsToEncodedBits() {
		Modes currentMode = Modes.UPPER;
		for (int segIdx = 0; segIdx < segments.size(); segIdx++) {
			MessageSegment m = segments.get(segIdx);
			// Switch into the new encoding
			boolean latched = transitionIntoMode(currentMode, m);

			// Write the data
			switch (m.encodingMode) {
				case UPPER -> append(m.data, 5);
				case LOWER -> append(m.data, 5);
				case MIXED -> append(m.data, 5);
				case PUNCT -> append(m.data, 5);
				case DIGIT -> append(m.data, 4);
				case BYTE -> appendByteArray(m.data);
				default -> throw new IllegalArgumentException("Encoding not yet supported: " + m.encodingMode);
			}

			workMarker.message += m.message;

			// Update the current mode
			if (latched) {
				currentMode = m.encodingMode;
			}
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

		// A word which is filled with all ones
		int ones = (1 << wordBitCount) - 1;
		int onesMinusOne = ones - 1;

		// Write all the data that fills an entire word first
		storageDataWords.reset();
		int bitLocation = 0;
		while (bitLocation + wordBitCount <= bits.size) {
			// Get the word's value
			short word = (short)bits.read(bitLocation, wordBitCount, true);

			// If a word is all zeros or all ones that's a special case. The
			// least-significant bit is zero to the opposition value and that bit
			// is punted to the next word
			if (word == 0) {
				storageDataWords.add(1);
				bitLocation += wordBitCount - 1;
			} else if (word == (short)ones) {
				storageDataWords.add(onesMinusOne);
				bitLocation += wordBitCount - 1;
			} else {
				storageDataWords.add(word);
				bitLocation += wordBitCount;
			}

			// Add the least significant bit again since it will be stripped away when decoded
			if (word == 1 || word == onesMinusOne) {
				bitLocation -= 1;
			}
		}

		// Last word is a special case
		if (bits.size > bitLocation) {
			// Read in the remaining bits
			int readBits = bits.size - bitLocation;
			int word = bits.read(bitLocation, readBits, true);

			// shift the data into the upper bits and fill the lower bits with all ones
			int remainder = wordBitCount - readBits;

			// pad it with 1's
			word = (word << remainder) | (0xFFFF >> (16 - remainder));
			// make sure it's not all 1's
			if (word == ones) {
				word = onesMinusOne;
			}
			storageDataWords.add(word);
		}

		// Record how many words are in the encoded message
		workMarker.messageWordCount = storageDataWords.size;

		if (workMarker.messageWordCount > workMarker.getCapacityWords())
			throw new RuntimeException("Encoded message is larger than the capacity. " +
					"Increase number of layers if manual or report a bug if automatic");
	}

	/**
	 * Selects the minim number of layers needed to store the data and error correction information
	 */
	void selectNumberOfLayers() {
		// See if the number of layers in the marker has been manually configured
		if (workMarker.dataLayers > 0) {
			return;
		}

		// Estimate the number of bits needed to encode the entire message based on the data and desired level
		// of error correction
		int targetBits = (int)(bits.size*(1.0 + errorCorrectionLength));

		// Determine the smallest marker size that can contain this data
		int maxLayers = workMarker.structure.maxDataLayers;

		// Sanity check to make sure this can be encoded
		int maxPossibleBits = workMarker.structure.getCapacityBits(maxLayers);
		if (targetBits > maxPossibleBits) {
			// See if the problem can be fixed by reducing error correction
			if (bits.size <= maxPossibleBits) {
				throw new IllegalArgumentException("Too large with ECC level. Try reducing amount of ECC");
			} else {
				throw new IllegalArgumentException("Too large to be encoded inside a single marker");
			}
		}

		// Select the smallest number of layers to store the data
		for (int layers = 1; layers <= maxLayers; layers++) {
			int capacityBits = workMarker.structure.getCapacityBits(layers);
			if (capacityBits >= targetBits) {
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
		bits.resize(0);
		for (int wordIdx = 0; wordIdx < storageDataWords.size; wordIdx++) {
			bits.append(storageDataWords.get(wordIdx) & 0xFFFF, wordBitCount, false);
		}

		// copy the message into "corrected"
		bits.size = storageDataWords.size*wordBitCount;
		// line above is done to make sure the last byte is a full word
		results.corrected = new byte[BoofMiscOps.bitToByteCount(bits.size)];
		System.arraycopy(bits.data, 0, results.corrected, 0, results.corrected.length);

		// Add ecc data to bits
		for (int word = 0; word < storageEccWords.size; word++) {
			bits.append(storageEccWords.get(word) & 0xFFFF, wordBitCount, false);
		}

		// Copy the entire encoded with ECC message into raw bits
		results.rawbits = new byte[BoofMiscOps.bitToByteCount(bits.size)];
		System.arraycopy(bits.data, 0, results.rawbits, 0, results.rawbits.length);

		return results;
	}

	/**
	 * Transitions from one mode into another.
	 *
	 * @return If it's latched to the new mode or false if not
	 */
	private boolean transitionIntoMode( Modes currentMode, MessageSegment m ) {
		boolean latched = true;
		// @formatter:off
		switch (currentMode) {
			case UPPER -> {
				switch (m.encodingMode) {
					case UPPER -> {}
					case LOWER -> append(28, 5);
					case MIXED -> append(29, 5);
					case DIGIT -> append(30, 5);
					case BYTE -> {append(31, 5); latched = false;}
					case PUNCT -> {
						if (m.data.size == 1) {
							append(0, 5); latched = false;
						} else {
							append(29, 5); // mixed
							append(30, 5); // punctuation-latched
						}
					}
					default -> throwUnsupported(currentMode, m.encodingMode);
				}
			}
			case LOWER -> {
				switch (m.encodingMode) {
					case LOWER -> {}
					case UPPER -> { append(28, 5); latched = false; }
					case MIXED -> append(29, 5);
					case DIGIT -> append(30, 5);
					case BYTE -> {append(31, 5); latched = false;}
					case PUNCT -> {
						if (m.data.size == 1) {
							append(0, 5); latched = false;
						} else {
							append(29, 5); // mixed
							append(30, 5); // punctuation-latched
						}
					}
					default -> throwUnsupported(currentMode, m.encodingMode);
				}
			}
			case MIXED -> {
				switch (m.encodingMode) {
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
					case BYTE -> {append(31, 5); latched = false;}
					default -> throwUnsupported(currentMode, m.encodingMode);
				}
			}
			// page 37
			case PUNCT -> {
				switch (m.encodingMode) {
					case PUNCT -> {}
					case LOWER -> { append(31, 5); append(28, 5); }
					case UPPER -> append(31, 5);
					case MIXED -> { append(31, 5); append(29, 5); }
					case DIGIT -> { append(31, 5); append(30, 5); }
					case BYTE -> { append(31, 5); append(31, 5); latched = false;}
					default -> throwUnsupported(currentMode, m.encodingMode);
				}
			}
			case DIGIT -> {
				switch (m.encodingMode) {
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
					case PUNCT -> {
						if (m.data.size == 1) {
							append(0, 4); latched = false;
						} else {
							append(14, 4); // upper
							append(29, 5); // mixed
							append(30, 5); // punctuation-latched
						}
					}
					case MIXED -> { append(14, 4); append(29, 5); }
					case BYTE -> { append(14, 4); append(31, 5); latched = false;}
					default -> throwUnsupported(currentMode, m.encodingMode);
				}
			}
			default -> throw new IllegalArgumentException("Unsupported. current="+currentMode+" encoding=" + m.encodingMode);
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

	/**
	 * Adds a byte array. This is a special case as it first specifies the length.
	 */
	private void appendByteArray( DogArray_I8 message ) {
		// If a smaller array encoding its length in the next 5-bits
		if (message.size < 32)
			bits.append(message.size, 5, false);
		else if (message.size < 0b0111_1111_1111 + 31) {
			// it's a longer array, so signal that with a 0 here
			bits.append(0, 5, false);
			// Then specify the actual length, minus 31
			bits.append(message.size - 31, 11, false);
		} else {
			throw new IllegalArgumentException("Message is too long to be encoded: " + message.size);
		}
		bits.append(message.data, message.size*8, false);
	}

	/** Convenience function for throwing an exception for incompatible transitions */
	private static int throwUnsupported( Modes src, Modes dst ) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Can't transition from " + src + " to " + dst);
	}

	public static class MessageSegment {
		public Modes encodingMode;
		public DogArray_I8 data;
		public String message;

		public MessageSegment( Modes encodingMode,
							   DogArray_I8 data,
							   String message ) {
			this.encodingMode = encodingMode;
			this.data = data;
			this.message = message;
		}
	}
}
