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

import boofcv.alg.fiducial.aztec.AztecCode.Mode;
import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.misc.BoofMiscOps;
import lombok.Getter;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Objects;
import java.util.Set;

/**
 * Converts a raw binary stream read from the image and converts it into a String message.
 *
 * @author Peter Abeles
 */
public class AztecDecoder extends AztecMessageErrorCorrection implements VerbosePrint {
	//------------------ state of decoder -----------------
	// Specifies the encoding mode for the active character set
	Mode current = Mode.UPPER;
	// If not latched (shift mode) then it will switch back to this mode after 1 character
	@Nullable AztecCode.Mode shiftMode = null;
	// if lacked it won't revert to the old mode
	boolean latched;
	// Results of decoded
	StringBuilder workString = new StringBuilder();

	@Nullable PrintStream verbose = null;

	/** True if it failed when doing error correction */
	@Getter boolean failedECC;

	/**
	 * Extracts the message from this marker.
	 *
	 * @param marker Marker which is to be decoded
	 * @return true if successful
	 */
	public boolean process( AztecCode marker ) {
		// Sanity check that the marker has been set up correctly
		BoofMiscOps.checkTrue(marker.dataLayers >= 1);
		Objects.requireNonNull(marker.rawbits);

		// Apply error correction to the message
		failedECC = false;
		if (!applyErrorCorrection(marker)) {
			failedECC = true;
			if (verbose != null) verbose.println("ECC failed");
			return false;
		}

		// Remove padding from the encoded bits
		PackedBits8 paddedBits = PackedBits8.wrap(marker.corrected, marker.messageWordCount*marker.getWordBitCount());
		var bits = new PackedBits8();
		if (!removeExtraBits(marker.getWordBitCount(), marker.messageWordCount, paddedBits, bits))
			return false;

		return bitsToMessage(marker, bits);
	}

	/**
	 * Removes extra bits in raw stream caused by padding being added to avoid a word that's all zero or ones.
	 */
	boolean removeExtraBits( int wordBitCount, int messageWordCount, PackedBits8 bitsExtras, PackedBits8 bits ) {
		// sanity check to make sure only whole words are inside the input bit array
		BoofMiscOps.checkTrue(bitsExtras.size%wordBitCount == 0);

		int numWords = bitsExtras.size/wordBitCount;

		// a word with all ones
		int ones = (1 << wordBitCount) - 1;

		// Remove extra 0 and 1 added to data stream
		int onesMinusOne = (1 << wordBitCount) - 2;
		for (int i = 0; i < numWords; i++) {
			int value = bitsExtras.read(i*wordBitCount, wordBitCount, true);
			if (value == 1 || value == onesMinusOne) {
				bits.append(value >> 1, wordBitCount - 1, false);
			} else if (i < messageWordCount && (value == 0 || value == ones)) {
				if (verbose != null) verbose.println("invalid message word. All zeros or ones");
				// These are illegal values in the raw message. In all likelihood the message is very small
				// and filled with all zeros
				return false;
			} else {
				bits.append(value, wordBitCount, false);
			}
		}
		return true;
	}

	/**
	 * Converts the corrected bits into a message.
	 *
	 * @return true if successful
	 */
	boolean bitsToMessage( AztecCode marker, PackedBits8 bits ) {
		// Reset the state
		latched = false;
		shiftMode = null;
		workString.delete(0, workString.length());
		current = Mode.UPPER;

		int location = 0;
		while (location + current.wordSize <= bits.size) {
			if (current == Mode.BYTE) {
				// Reading raw bytes is a special case
				int length = bits.read(location, 5, true);
				location += 5;
				if (length == 0) {
					length = bits.read(location, 11, true) + 31;
					location += 11;
				}
				if (verbose != null) verbose.println("current=" + current + " length=" + length);
				for (int i = 0; i < length; i++) {
					workString.append((char)bits.read(location, 8, true));
					location += 8;
				}
				// it always returns to the previous mode it was in before entering byte mode
				current = Objects.requireNonNull(shiftMode);
				shiftMode = null;
			} else {
				int value = bits.read(location, current.wordSize, true);
				if (verbose != null) verbose.println("current=" + current + " latched=" + latched + " value=" + value);

				location += current.wordSize;
				latched = true;
				Mode previous = current;
				boolean success = switch (current) {
					case UPPER -> handleUpper(value);
					case LOWER -> handleLower(value);
					case MIXED -> handleMixed(value);
					case PUNCT -> handlePunct(value);
					case DIGIT -> handleDigit(value);
					default -> {
						if (verbose != null) verbose.println("Unhandled mode: " + current);
						yield false;
					}
				};
				if (!success)
					return false;

				if (shiftMode != null) {
					current = shiftMode;
				}
				shiftMode = latched ? null : previous;
			}
		}

		marker.message = workString.toString();

		return true;
	}

	boolean handleUpper( int value ) {
		if (value == 0) {
			current = Mode.PUNCT;
			latched = false;
		} else if (value == 1) {
			workString.append(' ');
		} else if (value <= 27) {
			workString.append((char)('A' + (value - 2)));
		} else {
			switch (value) {
				case 28 -> current = Mode.LOWER;
				case 29 -> current = Mode.MIXED;
				case 30 -> current = Mode.DIGIT;
				case 31 -> {
					current = Mode.BYTE;
					latched = false;
				}
			}
		}
		return true;
	}

	boolean handleLower( int value ) {
		if (value == 0) {
			current = Mode.PUNCT;
			latched = false;
		} else if (value == 1) {
			workString.append(' ');
		} else if (value <= 27) {
			workString.append((char)('a' + (value - 2)));
		} else {
			switch (value) {
				case 28 -> {
					current = Mode.UPPER;
					latched = false;
				}
				case 29 -> current = Mode.MIXED;
				case 30 -> current = Mode.DIGIT;
				case 31 -> {
					current = Mode.BYTE;
					latched = false;
				}
			}
		}
		return true;
	}

	boolean handleMixed( int value ) {
		if (value == 0) {
			current = Mode.PUNCT;
			latched = false;
		} else if (value == 1) {
			workString.append(' ');
		} else if (value <= 14) {
			workString.append((char)(value - 1));
		} else if (value <= 19) {
			workString.append((char)(value - 19 + 27));
		} else if (value == 20) {
			workString.append('@');
		} else if (value == 21) {
			workString.append('\\');
		} else if (value <= 24) {
			workString.append((char)(value - 22 + 94));
		} else {
			switch (value) {
				case 25 -> workString.append('|');
				case 26 -> workString.append('~');
				case 27 -> workString.append((char)127);
				case 28 -> current = Mode.LOWER;
				case 29 -> current = Mode.UPPER;
				case 30 -> current = Mode.PUNCT;
				case 31 -> {
					current = Mode.BYTE;
					latched = false;
				}
			}
		}
		return true;
	}

	boolean handlePunct( int value ) {
		if (value == 0) {
			if (verbose != null) verbose.println("FlG(n) encountered");
			return false;
		}
		switch (value) {
			case 1 -> workString.append('\r');
			case 2 -> workString.append("\r\n");
			case 3 -> workString.append(". ");
			case 4 -> workString.append(", ");
			case 5 -> workString.append(": ");
			case 27 -> workString.append('[');
			case 28 -> workString.append(']');
			case 29 -> workString.append('{');
			case 30 -> workString.append('}');
			case 31 -> current = Mode.UPPER;
			default -> {
				if (value <= 20) {
					workString.append((char)(value - 6 + 33));
				} else if (value <= 26) {
					workString.append((char)(value - 21 + 58));
				}
			}
		}
		return true;
	}

	boolean handleDigit( int value ) {
		if (value == 0) {
			current = Mode.PUNCT;
			latched = false;
		} else if (value == 1) {
			workString.append(' ');
		} else if (value <= 11) {
			workString.append((char)(value - 2 + 48));
		} else {
			switch (value) {
				case 12 -> workString.append(',');
				case 13 -> workString.append('.');
				case 14 -> current = Mode.UPPER;
				case 15 -> {
					current = Mode.UPPER;
					latched = false;
				}
			}
		}
		return true;
	}

	boolean handleByte( int value ) {
		workString.append((char)value);
		return true;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> set ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
