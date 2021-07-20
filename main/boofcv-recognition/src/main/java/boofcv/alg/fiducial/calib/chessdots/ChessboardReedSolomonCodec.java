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

package boofcv.alg.fiducial.calib.chessdots;

import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.alg.fiducial.qrcode.ReidSolomonCodes;
import boofcv.misc.BoofMiscOps;
import georegression.struct.point.Point2D_I32;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray_I8;

/**
 * Encodes and decodes grid coordinates into a compact packet for use in calibration targets. A multiplier can be
 * added which will scale the coordinate values. Useful for when only some square are filled in. The same error
 * correction scheme used in QR codes is used here, but tweaked for the smaller packet size. A checksum was added
 * to detect corrupted packets. Between the ECC mechanism and checksum most errors are caught, but for small
 * packet sizes random data can be mistaken for a real target.
 *
 * Grid size is dynamically determined based on the maximum coordinate that's needed and the desired level of
 * error correction. If there are extra bits those are allocated for ECC or filled in with a known pattern.
 *
 * Words (8-bit data chunks) are laid out in a pattern that's designed to keep each individual word's bits as close
 * to eac other as possible. The error correction words on a per-word basis and not a per-bit basis. Dirt or damage
 * tends to be located within a small spatial region and you want to minimize the number of words it corrupts.
 *
 * TODO describe Reed-Solomon codes an how they are used.
 *
 * Packet Format:
 * <ul>
 *     <li>2-bits for multiplier (0=1,1=2,2=4,3=8)</li>
 *     <li>2-bits reserved for future use (must be 0)</li>
 *     <li>Encoded coordinates (row*max_length + col)</li>
 *     <li>4-bit checksum. xor based</li>
 * </ul>
 *
 * @author Peter Abeles
 */
public class ChessboardReedSolomonCodec {
	// TODO locate the bits spatially close to each for a single word so that local damage doesn't screw up
	//      multiple words

	/** Number of bits per word */
	public final int WORD_BITS = 8;
	/**
	 * Maximum number of words that can have an error in it.
	 * Change this value only if you really know what you are doing.
	 */
	@Getter @Setter double maxErrorFraction = 0.3;

	// Used to mask out bits not in the word
	int wordMask;
	/** The maximum allowed coordinate */
	@Getter int maxCoordinate;

	// Number of bits needed to encode a number
	int bitsPerNumber;

	// Number of bits in the data packet
	int messageBitCount;

	/** Length of the square grid the bits are encoded in */
	@Getter int gridBitLength;

	/** Multiplier that the coordinates are adjusted by */
	@Getter Multiplier multiplier;

	/** The resulting message that has been encoded */
	@Getter private final DogArray_I8 encodedPacket = new DogArray_I8();

	// Stores the encoded message
	protected final DogArray_I8 message = new DogArray_I8();
	// Stores error correction codes
	protected final DogArray_I8 ecc = new DogArray_I8();

	// Workspace that allowed you to construct a message one bit at a time
	protected final PackedBits8 bits = new PackedBits8();

	// Error correction algorithm. Primitive is taken from QR Code specification
	private final ReidSolomonCodes rscodes = new ReidSolomonCodes(WORD_BITS, 0b100011101);

	/**
	 * Computes how large of a grid will be needed to encode the coordinates + overhead. Pre-allocates any memory
	 * that will be needed
	 *
	 * @param multiplier Coordinate multiplier
	 * @param maxCoordinate The maximum coordinate that will be needed
	 */
	public void configure( Multiplier multiplier, int maxCoordinate ) {
		this.multiplier = multiplier;

		// Adjust the coordinate for scaling
		maxCoordinate = (int)Math.ceil(maxCoordinate/(double)multiplier.amount);
		this.maxCoordinate = maxCoordinate;

		// Compute how many bits it will take to encode the two coordinates
		bitsPerNumber = (int)Math.ceil(Math.log(maxCoordinate)/Math.log(2));
		messageBitCount = 2*bitsPerNumber;

		// Add overhead
		messageBitCount += 4; // (scale and reserved)
		messageBitCount += 4; // checksum at end

		// Compute number of words needed to save this message
		int dataWords = (int)Math.ceil(messageBitCount/(double)WORD_BITS);

		// Two words are needed to fix every word with an error. Multiple bit errors in a single word count
		// as a single error. See Singleton Bound
		int eccWords = (int)(2*Math.ceil(dataWords*maxErrorFraction));

		// Total number of bits that need to be encoded. message + ecc
		int packetBitCount = (dataWords + eccWords)*WORD_BITS;

		// How big the square needs to be to encode all this information
		gridBitLength = (int)Math.ceil(Math.sqrt(packetBitCount));

		// See if there are enough extra bits to increase the number of ECC words
		if (gridBitLength*gridBitLength - packetBitCount >= WORD_BITS) {
			int extraWords = (gridBitLength*gridBitLength - packetBitCount)/WORD_BITS;
			eccWords += extraWords;
		}

		// Compute the number of bytes to encode it all
		message.resize(dataWords);
		rscodes.generator(eccWords);

		// only save bits that are in the word
		wordMask = 0;
		for (int i = 0; i < WORD_BITS; i++) {
			wordMask |= 1 << i;
		}
//		System.out.println("squareLength=" + squareLength + " dataWords=" + dataWords + " eccWords=" + eccWords);
	}

	/**
	 * Encodes the coordinate and copies the results into 'encodedPacket'
	 *
	 * @param row (input) Row number
	 * @param col (input) Column number
	 * @param encodedPacket (Output) The encoded packet
	 */
	public void encode( int row, int col, PackedBits8 encodedPacket ) {
		int scale = multiplier.amount;
		if (row%scale != 0 || col%scale != 0)
			throw new IllegalArgumentException("Coordinate is not evenly divisible by scale factor. row=" + row + " col=" + col);
		row /= scale;
		col /= scale;
		if (row >= maxCoordinate)
			throw new IllegalArgumentException("Row is larger than max. row=" + row + " max=" + maxCoordinate);
		if (col >= maxCoordinate)
			throw new IllegalArgumentException("Column is larger than max. col=" + col + " max=" + maxCoordinate);

		// Build the packet
		bits.resize(0);
		bits.append(multiplier.ordinal(), 4, false);
		bits.append(row, bitsPerNumber, false);
		bits.append(col, bitsPerNumber, false);

		// Compute a checksum. ECC doesn't catch everything in targets this small
		int checkSum = computeCheckSum();
		bits.append(checkSum, 4, false);

		// Sanity check
		BoofMiscOps.checkEq(bits.arrayLength(), message.size);

		// Work from workspace to the message
		message.setTo(bits.data, 0, message.size);

		// Compute the error correction code
		rscodes.computeECC(message, ecc);

		// Allocate enough bits for every element in the data grid
		encodedPacket.resize(gridBitLength*gridBitLength);

		// Copy into output array
		System.arraycopy(message.data, 0, encodedPacket.data, 0, message.size);
		System.arraycopy(ecc.data, 0, encodedPacket.data, message.size, ecc.size);

		// Fill in extra bits with a known pattern. This is ignored when decoding
		for (int i = (message.size + ecc.size)*WORD_BITS; i < encodedPacket.size; i++) {
			encodedPacket.set(i, i%2);
		}
	}

	/**
	 * Computes a 4-bit checksum by xor the data portion of the packet
	 */
	private int computeCheckSum() {
		int checkSum = 0b1010;
		int length = bits.arrayLength();
		for (int i = 0; i < length; i++) {
			int v = bits.data[i] & 0xFF;
			// If at the end, zero out extra bits at the end
			if ((i + 1)*8 > bits.size) {
				int unused = (i + 1)*8 - bits.size;
				v = (((byte)(v << unused)) >> unused) & 0xFF;
			}

			checkSum ^= v >> 4;
			checkSum ^= v & 0x0F;
		}
		return checkSum;
	}

	/**
	 * Decodes the read in bit array and converts it into a coordinate. If true is returned then it believes it
	 * is a valid packet and has corrected minor errors. This won't catch every single failure but should catch
	 * most of them.
	 *
	 * @param readBits (Input) Read in bits from an image
	 * @param coordinate (Output) Found coordinates
	 * @return True is returned if it believes all errors have been fixed and the data is not corrupted.
	 */
	public boolean decode( PackedBits8 readBits, Point2D_I32 coordinate ) {
		BoofMiscOps.checkEq(readBits.size, gridBitLength*gridBitLength, "Unexpected array size");
		// Split up the incoming message into the message and ecc portions
		message.setTo(readBits.data, 0, message.size);
		ecc.setTo(readBits.data, message.size, ecc.size);

		// Attempt to fix any errors
		if (!rscodes.correct(message, ecc)) {
			return false;
		}

		// Copy into bits array to make parsing easier
		System.arraycopy(message.data, 0, bits.data, 0, message.size);
		bits.size = messageBitCount;
		int foundCheckSum = bits.read(bits.size - 4, 4, true);
		bits.resize(bits.size - 4);
		int expectedCheckSum = computeCheckSum();

		if (foundCheckSum != expectedCheckSum)
			return false;

		// Which scale factor was applied
		int m = bits.read(0, 4, true);
		if (m >= 4)
			return false;
		int scale = Multiplier.values()[m].amount;

		// Extract the coordinate
		coordinate.y = scale*(bits.read(4, bitsPerNumber, true)); // row
		coordinate.x = scale*(bits.read(4 + bitsPerNumber, bitsPerNumber, true)); // column

		// See if the coordinate is invalid
		return coordinate.x >= 0 && coordinate.y >= 0 && coordinate.y < maxCoordinate;
	}

	/**
	 * Specifies how often coordinates are printed on the grid and how to convert the encoded coordinate back to
	 * the original scale
	 */
	public enum Multiplier {
		LEVEL_0(1),
		LEVEL_1(2),
		LEVEL_2(4),
		LEVEL_3(8);

		/** How much the coordinate is multiplied by */
		@Getter final int amount;

		Multiplier( int amount ) {
			this.amount = amount;
		}
	}
}
