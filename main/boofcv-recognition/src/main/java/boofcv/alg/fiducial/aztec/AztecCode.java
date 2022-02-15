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

import georegression.struct.homography.Homography2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import lombok.Getter;

/**
 * Information on a detected Aztec Code
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class AztecCode {
	/** Number of layers or rings outside the locator pattern that data is encoded on */
	public int dataLayers = 0;

	/** Number of code words used to encode the message. Code words have variable bit count. */
	public int messageWordCount = 0;

	/** The raw byte data encoded into the QR Code. data + ecc */
	public byte[] rawbits;

	/** Raw byte data after error correction has been applied to it. Only contains the data portion */
	public byte[] corrected;

	/** The decoded message */
	public String message = "";

	/** Which Structure does it have. Determines shape of locator pattern and maximum number of data layers. */
	public Structure structure = Structure.COMPACT;

	/** At what stage did decoding fail at */
	public Failure failure = Failure.NONE;

	/**
	 * True if the marker was incorrectly encoded or is being viewed in a mirror because the bits locations are
	 * transposed.
	 */
	public boolean transposed;

	/** Number of bit errors detected when apply error correction to the message */
	public int totalBitErrors;

	/** Locations of extern contours around the squares in a locator pattern. */
	public final AztecPyramid locator = new AztecPyramid();

	/**
	 * Approximate bounding box of the marker. Note that the corners are not directly measured but have to be
	 * inferred from other fixed structures.
	 *
	 * Order: top-left = 0. Top-right = 1, Bottom-Right = 2, Bottom-Left = 3.
	 */
	public Polygon2D_F64 bounds = new Polygon2D_F64(4);

	/** A homography transform from grid bit coordinates into image pixels. */
	public Homography2D_F64 Hinv = new Homography2D_F64();

	/** Number of squares (data bits) wide the marker is */
	public int getMarkerWidthSquares() {
		int withoutGrid = getLocatorWidthSquares() + 6 + dataLayers*4;
		if (structure == Structure.COMPACT || dataLayers <= 4)
			return withoutGrid;

		int radius = 9 + dataLayers*2;
		int gridRingCount = radius/16;
		return withoutGrid + 1 + gridRingCount*2 - 1;
	}

	/** Number of rings in the locator pattern */
	public int getLocatorRingCount() {
		return switch (structure) {
			case COMPACT -> 2;
			case FULL -> 3;
		};
	}

	/** Number of squares in locator pattern */
	public int getLocatorWidthSquares() {
		return (getLocatorRingCount() - 1)*4 + 1;
	}

	/** Returns the maximum number of bits that can be encoded. Data and ECC combined */
	public int getCapacityBits() {
		return getCapacityWords()*getWordBitCount();
	}

	/** Returns the maximum number of codewords that can be stored in the marker. */
	public int getCapacityWords() {
		return structure.getCodewords(dataLayers);
	}

	/** Returns number bits in a code word */
	public int getWordBitCount() {
		return getWordBitCount(dataLayers);
	}

	public static int getWordBitCount( int dataLayers ) {
		if (dataLayers < 1)
			throw new RuntimeException("Invalid number of layers. layers=" + dataLayers);
		if (dataLayers <= 2)
			return 6;
		else if (dataLayers <= 8)
			return 8;
		else if (dataLayers <= 22)
			return 10;
		else
			return 12;
	}

	/** Returns ratio of words used for error correction over words used to store data */
	public double getCorrectionLevel() {
		int wordsErrorCorrection = getCapacityWords() - messageWordCount;
		return wordsErrorCorrection/(double)messageWordCount;
	}

	@SuppressWarnings({"NullAway"})
	public void reset() {
		dataLayers = 0;
		structure = Structure.COMPACT;
		message = "";
		rawbits = null;
		corrected = null;
		failure = Failure.NONE;
		transposed = false;
		totalBitErrors = 0;
		locator.reset();
		bounds.zero();
		Hinv.reset();
	}

	@SuppressWarnings({"NullAway"})
	public AztecCode setTo( AztecCode src ) {
		dataLayers = src.dataLayers;
		messageWordCount = src.messageWordCount;
		message = src.message;
		rawbits = src.rawbits == null ? null : src.rawbits.clone();
		corrected = src.corrected == null ? null : src.corrected.clone();
		failure = src.failure;
		structure = src.structure;
		transposed = src.transposed;
		totalBitErrors = src.totalBitErrors;
		locator.setTo(src.locator);
		bounds.setTo(src.bounds);
		Hinv.setTo(src.Hinv);
		return this;
	}

	public AztecCode copy() {
		return new AztecCode().setTo(this);
	}

	/** Which symbol structure is used */
	public enum Structure {
		COMPACT(new int[]{17, 40, 51, 76}),
		FULL(new int[]{21, 48, 60, 88, 120, 156, 196, 240, 230, 272,
				316, 364, 416, 470, 528, 588, 652, 720, 790, 864, 940,
				1020, 920, 992, 1066, 1144, 1224, 1306, 1392, 1480, 1570, 1664});

		Structure( int[] codewords ) {
			this.maxDataLayers = codewords.length;
			this.codewords = codewords;
		}

		/** Maximum number of data layers */
		@Getter final int maxDataLayers;

		// stores number of codewords that can be saved in a marker with this many layers-1.
		private final int[] codewords;

		/** Returns number of codewords available at this level */
		public int getCodewords( int level ) {
			return codewords[level - 1];
		}

		public int getCapacityBits( int level ) {
			return getCodewords(level)*getWordBitCount(level);
		}
	}

	/** Specifies which encoding is currently active in the data stream. */
	public enum Modes {
		UPPER(5),
		LOWER(5),
		MIXED(5),
		PUNCT(5),
		DIGIT(4),
		FNC1(5),
		ECI(8),
		BYTE(8);

		Modes( int wordSize ) {
			this.wordSize = wordSize;
		}

		@Getter final int wordSize;
	}

	/** At what stage did it fail at? */
	public enum Failure {
		/** No failure */
		NONE,
		/** Reconstruction resulted in a very odd shape and this is most likely noise */
		IMPROBABLE,
		/** Orientation could not be determined */
		ORIENTATION,
		/** Error correction failed when decoding the mode */
		MODE_ECC,
		/** Error correction failed when decoding the message */
		MESSAGE_ECC,
		/** Could not parse the message */
		MESSAGE_PARSE
	}
}
