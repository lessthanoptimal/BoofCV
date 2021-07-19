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

import boofcv.alg.fiducial.qrcode.ReidSolomonCodes;
import georegression.struct.point.Point2D_I32;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray_I8;

/**
 * 2-bits for multiplier (0=1,1=2,2=4,3=8)
 * remaining bits are split in two for row/col number
 *
 * @author Peter Abeles
 */
public class ChessDotsMessageEncoderDecoder {
	// TODO locate the bits spatially close to each for a single word so that local damage doesn't screw up
	//      multiple words

	/** Number of bits per word */
	public final int WORD_BITS = 8;
	/**
	 * Maximum number of words that can have an error in it.
	 * Change this value only if you really know what you are doing.
	 */
	@Getter @Setter double maxErrorFraction = 0.3;

	int wordMask;
	int maxCoordinate;

	int squareLength;
	Multiplier multiplier;

	@Getter private final DogArray_I8 rawData = new DogArray_I8();

	// Stores the encoded message
	protected final DogArray_I8 message = new DogArray_I8();
	// Stores error correction codes
	protected final DogArray_I8 ecc = new DogArray_I8();

	// Error correction algorithm
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
		int dataBits = 2*(int)Math.ceil(Math.log(maxCoordinate)/Math.log(2));

		// Add overhead
		dataBits += 2;

		int dataWords = (int)Math.ceil(dataBits/(double)WORD_BITS);

		// Two words are needed to fix every word with an error. Multiple bit errors in a single word count
		// as a single error. See Singleton Bound
		int eccWords = (int)(2*Math.ceil(dataWords*maxErrorFraction));

		dataBits = (dataWords + eccWords)*WORD_BITS;

		// How big the square needs to be to encode all this information
		squareLength = (int)Math.ceil(Math.sqrt(dataBits));

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

	public void encode( int row, int col ) {
		int scale = multiplier.amount;
		if (row%scale != 0 || col%scale != 0)
			throw new IllegalArgumentException("Coordinate is not evenly divisible by scale factor");
		row /= scale;
		col /= scale;

		// encode it in a row-major format
		long number = row*(long)maxCoordinate + col;

		// First two bits will be the multiplier
		number = multiplier.ordinal() | (number << 2);

		message.fill((byte)0);

		// Convert the number into bytes
		for (int i = 0; i < message.size; i++) {
			message.data[i] = (byte)((number >> (WORD_BITS*i)) & wordMask);
		}

		// Compute the error correction code
		rscodes.computeECC(message, ecc);

		// Copy into output array
		rawData.resize(message.size + ecc.size);
		System.arraycopy(message.data, 0, rawData.data, 0, message.size);
		System.arraycopy(ecc.data, 0, rawData.data, message.size, ecc.size);

//		System.out.println("raw.size=" + rawData.size + " ecc.size=" + ecc.size + " number=" + number);
	}

	public boolean decode( DogArray_I8 bits, Point2D_I32 coordinate ) {
		// Split up the incoming message into the message and ecc portions
		message.setTo(bits.data, 0, message.size);
		ecc.setTo(bits.data, message.size, ecc.size);

		// Attempt to fix any errors
		if (!rscodes.correct(message, ecc)) {
			return false;
		}

		// Extract encoded data from the message
		long value = 0;
		for (int i = 0; i < message.size; i++) {
			value |= (long)(message.data[i] & 0xFF) << (WORD_BITS*i);
		}

		// Which scale factor was applied
		int m = (int)(value & 0x03);
		int scale = Multiplier.values()[m].amount;

		// Remove the header
		value >>= 2;

		// Extract the coordinate
		coordinate.x = (int)(scale*(value%maxCoordinate)); // column
		coordinate.y = (int)(scale*(value/maxCoordinate)); // row

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
