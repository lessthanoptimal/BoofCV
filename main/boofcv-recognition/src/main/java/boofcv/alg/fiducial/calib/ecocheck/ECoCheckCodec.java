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
 * of possible markers and cells in a marker must be known in advance to decode and encode [1]. Error correction
 * is provided bye Reed Solomon encoding and the amount of error correction is configurable. If there is extra
 * words available in the data grid then the amount of error correction is increased.
 *
 * <p>
 * Words (8-bit data chunks) are laid out in a pattern that's designed to keep each individual word's bits as close
 * to eac other as possible. The error correction words on a per-word basis and not a per-bit basis. Dirt or damage
 * tends to be located within a small spatial region and you want to minimize the number of words it corrupts.
 * <p>
 *
 * TODO describe Reed-Solomon codes an how they are used.
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
 * TODO merge marker ID and cell ID into a single number
 *
 * [1] An earlier design was considered that did encode this information in the marker. It typically increased the bit
 * grid size by 1 but didn't remove the need to have prior information about the target to decode the target.
 *
 * @author Peter Abeles
 */
public class ECoCheckCodec {
	// TODO locate the bits spatially close to each for a single word so that local damage doesn't screw up
	//      multiple words

	/**
	 * Number of bits per word
	 */
	public final int WORD_BITS = 8;

	/**
	 * Used to adjust the amount of error correction. Larger values mean more error correction. 0 = non. 10 = max.
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

	/**
	 * Length of the square grid the bits are encoded in
	 */
	@Getter int gridBitLength;

	// Precomputed mask for checksum
	protected int checksumMask;

	// number of bits needed to make data aligned with word side
	protected int paddingCount;
	protected int paddingBits;

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
		BoofMiscOps.checkTrue(checksumBitCount >= 0 && checksumBitCount <= 8);

		markerBitCount = numUniqueMarkers == 1 ? 0 : (int)Math.ceil(Math.log(numUniqueMarkers)/Math.log(2));
		cellBitCount = (int)Math.ceil(Math.log(numUniqueCells)/Math.log(2));

		// maker ID + cell ID + checksum
		messageBitCount = markerBitCount + cellBitCount + checksumBitCount;

		// Compute number of words needed to save this message
		int dataWords = (int)Math.ceil(messageBitCount/(double)WORD_BITS);

		// Two words are needed to fix every word with an error. Multiple bit errors in a single word count
		// as a single error. See Singleton Bound
		int eccWords = (int)(2*Math.ceil(dataWords*errorCorrectionFraction()));

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
		ecc.resize(eccWords);
		bits.resize(dataWords*WORD_BITS);
		message.resize(dataWords);
		rscodes.generator(eccWords);

		// Compute number of bits to align data with word size
		paddingCount = messageBitCount%WORD_BITS == 0 ? 0 : WORD_BITS-(messageBitCount%WORD_BITS);

		// only save bits that are in the word
		wordMask = BoofMiscOps.generateBitMask(WORD_BITS);
		checksumMask = BoofMiscOps.generateBitMask(checksumBitCount);
		// All padding bits are 1
		paddingBits = BoofMiscOps.generateBitMask(paddingCount);
	}

	/**
	 * Encodes the coordinate and copies the results into 'encodedPacket'
	 *
	 * @param markerID (Input) The marker's unique ID
	 * @param cellID (input) The cell's unique ID inside the marker
	 * @param encodedPacket (Output) The encoded packet
	 */
	public void encode( int markerID, int cellID, PackedBits8 encodedPacket ) {
		// Build the packet
		bits.resize(0);

		// Specify the ID numbers
		bits.append(markerID, markerBitCount, false);
		bits.append(cellID, cellBitCount, false);

		// Compute a checksum. ECC doesn't catch everything in targets this small
		int checkSum = computeCheckSum();
		bits.append(checkSum, checksumBitCount, true);

		// Sanity check
		BoofMiscOps.checkEq(bits.size, messageBitCount);

		// Add padding to align message with words
		bits.append(paddingBits, paddingCount, false);

		// Copy packet from the bits workspace into message
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
	 * Computes a k-bit checksum by xor the data portion of the packet
	 */
	private int computeCheckSum() {
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
		message.setTo(readBits.data, 0, message.size);
		ecc.setTo(readBits.data, message.size, ecc.size);

		// Attempt to fix any errors
		if (!rscodes.correct(message, ecc)) {
			return false;
		}

		// Copy into bits array to make parsing easier
		System.arraycopy(message.data, 0, bits.data, 0, message.size);

		// Reject if padding bits are not the expected value
		if (paddingCount > 0) {
			bits.size = messageBitCount + paddingCount;
			int padding = bits.read(messageBitCount, paddingCount, false);
			if (padding != paddingBits)
				return false;
		}

		bits.size = messageBitCount;
		int foundCheckSum = bits.read(markerBitCount + cellBitCount, checksumBitCount, false);
		bits.resize(markerBitCount + cellBitCount);
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
		return errorCorrectionLevel/10.0;
	}
}
