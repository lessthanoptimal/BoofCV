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

package boofcv.alg.fiducial.calib.ecocheck;

import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.alg.fiducial.qrcode.ReidSolomonCodes;
import boofcv.misc.BoofMiscOps;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray_I8;

/**
 * Encodes and decodes the marker ID and cell ID from an encoded cell in a calibration target. The maximum number
 * of possible markers and cells in a marker must be known in advance to decode and encode [1]. Validation
 * is done using a checksum and optional Reed Solomon error correction code (ECC) is provided to fix errors. The amount
 * of ECC is configurable. If ECC is turned on and there are extra bits, then those bits will be used for additional
 * error correction, if possible.
 *
 * <p>
 * The checksum was provided as a way to catch some invalid encoding and employs a simple xor strategy. It was found
 * that this checksum was needed even when ECC was turned on because for smaller messages it had an unacceptable
 * false positive rate. The number of bits for the checksum is {@link #checksumBitCount configurable}.
 * </p>
 *
 * <p>
 * The Reed Solomon encoding used in ECoCheck is the same as what's used in QR Codes. It can detect and fix
 * errors inside of 8-bit words. Two words are needed to detect and recover from an error in a single word. This
 * will increase the packet size as an encoded message has to now be contained inside of "word" sized chunks and
 * several extra words are required to fix errors. The amount of error correction can be
 * {@link #errorCorrectionLevel configured}.
 * </p>
 *
 * <p>
 * Number of bits used to encode the two ID numbers is dynamically determined based on their maximum value. The minimum
 * number of bits is computed using this formula: bit count = ceil(log(max_number)/log(2))
 * </p>
 *
 * Packet Format:
 * <ul>
 *     <li>N-bits: Marker ID</li>
 *     <li>M-bits: Cell ID</li>
 *     <li>K-bits: XOR Checksum</li>
 * </ul>
 *
 * [1] An earlier design was considered that did encode this information in the marker. It typically increased the bit
 * grid size by 1 but didn't remove the need to have prior information about the target to decode the target.
 *
 * @author Peter Abeles
 */
public class ECoCheckCodec {
	// Design Note: Encoding marker and cell ID as a single value saves at most a single bit in practical scenarios
	//              so it was decided to keep them separate to make the packet easier to understand.

	public final static int MAX_ECC_LEVEL = 9;
	public final static int MAX_CHECKSUM_BITS = 8;

	/** Number of bits per word */
	public final static int WORD_BITS = 8;

	/**
	 * Used to adjust the amount of error correction. Larger values mean more error correction. 0 = none. 9 = max.
	 */
	@Getter @Setter int errorCorrectionLevel = 3;
	// this is an integer and not a float to make configuring easier and more precise to the user

	/** Number of checksum bits. Max 8 */
	@Getter @Setter int checksumBitCount = 6;

	// Used to mask out bits not in the word
	int wordMask;

	// Number of bits in the data packet
	int messageBitCount;

	/** Number of bits required to encode a marker ID */
	@Getter int markerBitCount;
	/** Number of bits required to encode a cell ID */
	@Getter int cellBitCount;

	/** Length of the square grid the bits are encoded in */
	@Getter int gridBitLength;

	// Precomputed mask for checksum
	protected int checksumMask;

	// number of words needed to encode data portion of the message
	protected int dataWords;
	// number of words needed to encode ECC portion of the message
	protected int eccWords;

	// Values to use as padding when there are unused bits
	protected int paddingBits = 0;
	// NOTE: for some reason 0 bits produce a lot better results than all 1. maybe thresholding approach? FP corners?

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
	 * @param numUniqueMarkers Maximum number of unique markers required
	 * @param numUniqueCells Maximum number of unique cell IDs in a marker required
	 */
	public void configure( int numUniqueMarkers, int numUniqueCells ) {
		BoofMiscOps.checkTrue(checksumBitCount >= 0 && checksumBitCount <= MAX_CHECKSUM_BITS);
		BoofMiscOps.checkTrue(errorCorrectionLevel >= 0 && errorCorrectionLevel <= MAX_ECC_LEVEL);

		markerBitCount = numUniqueMarkers == 1 ? 0 : (int)Math.ceil(Math.log(numUniqueMarkers)/Math.log(2));
		cellBitCount = (int)Math.ceil(Math.log(numUniqueCells)/Math.log(2));

		// maker ID + cell ID + checksum
		messageBitCount = markerBitCount + cellBitCount + checksumBitCount;

		// Compute number of words needed to save this message
		dataWords = (int)Math.ceil(messageBitCount/(double)WORD_BITS);

		// Total number of bits that need to be encoded. message + ecc
		int packetBitCount;
		if (errorCorrectionLevel > 0) {
			// Two words are needed to fix every word with an error. Multiple bit errors in a single word count
			// as a single error. See Singleton Bound
			eccWords = (int)(2*Math.ceil(dataWords*errorCorrectionFraction()));
			packetBitCount = (dataWords + eccWords)*WORD_BITS;
		} else {
			// When there's no ECC there's no need to force the message length to align with a word size
			packetBitCount = messageBitCount;
			eccWords = 0;
		}

		// How big the square needs to be to encode all this information
		gridBitLength = (int)Math.ceil(Math.sqrt(packetBitCount));

		// See if there are enough extra bits to increase the number of ECC words
		if (errorCorrectionLevel > 0 && gridBitLength*gridBitLength - packetBitCount >= WORD_BITS) {
			int extraWords = (gridBitLength*gridBitLength - packetBitCount)/WORD_BITS;
			eccWords += extraWords;
		}

		// Compute the number of bytes to encode it all
		ecc.resize(eccWords);
		bits.resize(messageBitCount);
		rscodes.generator(eccWords);

		// only save bits that are in the word
		wordMask = BoofMiscOps.generateBitMask(WORD_BITS);
		checksumMask = BoofMiscOps.generateBitMask(checksumBitCount);
	}

	/**
	 * Encodes the coordinate and copies the results into 'encodedPacket'
	 *
	 * @param markerID (Input) The marker's unique ID
	 * @param cellID (input) The cell's unique ID inside the marker
	 * @param encodedPacket (Output) The encoded packet
	 */
	public void encode( int markerID, int cellID, PackedBits8 encodedPacket ) {
		// pre-allocate memory for encoded packet
		int elementsInGrid = gridBitLength*gridBitLength;
		encodedPacket.resize(elementsInGrid);

		// Build the packet
		bits.resize(0);

		// Specify the ID numbers
		bits.append(markerID, markerBitCount, false);
		bits.append(cellID, cellBitCount, false);

		// Compute a checksum. Checksum is needed since ECC won't catch everything in smaller packets
		int checkSum = computeCheckSum();
		bits.append(checkSum, checksumBitCount, true);

		// Sanity check
		BoofMiscOps.checkEq(bits.size, messageBitCount);

		// Concat the message and ecc data together
		encodedPacket.resize(0);
		encodedPacket.append(bits, bits.size);

		int bitsEncoded;
		if (errorCorrectionLevel > 0) {
			// Copy packet from the bits workspace into message
			message.setTo(bits.data, 0, dataWords);

			// Compute the error correction code
			rscodes.computeECC(message, ecc);

			// Copy ECC into the encoded packet. The message data is now implicitly filled with zeros to align with
			// a word
			System.arraycopy(ecc.data, 0, encodedPacket.data, dataWords, ecc.size);

			bitsEncoded = (dataWords + ecc.size)*WORD_BITS;
		} else {
			bitsEncoded = bits.size;
		}

		// Add padding to the remainder
		encodedPacket.size = bitsEncoded;
		for (int i = encodedPacket.size; i < elementsInGrid; i += 32) {
			encodedPacket.append(paddingBits, Math.min(32, elementsInGrid - i), false);
		}
	}

	/**
	 * Computes a k-bit checksum by xor the data portion of the packet
	 */
	private int computeCheckSum() {
		// See if the checksum is disabled
		if (checksumBitCount == 0)
			return 0;

		int checkSum = 0b1010_1011; // selected to avoid symmetry and being filled with all 0 or 1
		final int length = bits.length();
		for (int i = 0; i < length; i += checksumBitCount) {
			int amount = Math.min(checksumBitCount, length - i);
			checkSum ^= bits.read(i, amount, false);
		}
		return checkSum & checksumMask;
	}

	/**
	 * Decodes the read in bit array and converts it into a coordinate. If true is returned then it believes it
	 * is a valid packet and has corrected minor errors. This won't catch every single failure but should catch
	 * most of them.
	 *
	 * @param readBits (Input) Read in bits from an image
	 * @param cell (Output) Found marker and cell ID
	 * @return True is returned if it believes all errors have been fixed and the data is not corrupted.
	 */
	public boolean decode( PackedBits8 readBits, CellValue cell ) {
		BoofMiscOps.checkEq(readBits.size, gridBitLength*gridBitLength, "Unexpected array size");

		// Split up the incoming message into the message and ecc portions
		message.setTo(readBits.data, 0, dataWords);
		ecc.setTo(readBits.data, dataWords, eccWords);

		// Attempt to fix any errors
		if (!rscodes.correct(message, ecc)) {
			return false;
		}

		// Copy into bits array to make parsing easier
		System.arraycopy(message.data, 0, bits.data, 0, dataWords);

		// Extract the checksum
		bits.size = messageBitCount;
		int foundCheckSum = bits.read(bits.size - checksumBitCount, checksumBitCount, false);

		// Compute checksum, including padding bits
		bits.resize(bits.size - checksumBitCount);
		int expectedCheckSum = computeCheckSum();

		if (foundCheckSum != expectedCheckSum)
			return false;

		// Which scale factor was applied
		cell.markerID = bits.read(0, markerBitCount, true);
		cell.cellID = bits.read(markerBitCount, cellBitCount, true);

		return true;
	}

	/**
	 * Converts the level into a fraction
	 */
	public double errorCorrectionFraction() {
		return errorCorrectionLevel/(double)MAX_ECC_LEVEL;
	}
}
