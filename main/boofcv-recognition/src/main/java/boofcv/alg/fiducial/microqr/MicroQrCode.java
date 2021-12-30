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

package boofcv.alg.fiducial.microqr;

import boofcv.alg.fiducial.qrcode.QrCode;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static boofcv.alg.fiducial.microqr.MicroQrCode.ErrorLevel.*;

/**
 * Information about a detected Micro QR Code.
 *
 * @author Peter Abeles
 */
public class MicroQrCode {

	/** Bits that format information is XOR using */
	public static final int FORMAT_XOR = 0b100010001000101;

	/** Maximum possible version of a Micro QR Code */
	public static final int MAX_VERSION = 4;

	public static final VersionInfo[] VERSION_INFO = new VersionInfo[MAX_VERSION + 1];

	/**
	 * Version of the marker. This can be from 1 to 4.
	 */
	public int version = -1;

	public ErrorLevel error = ErrorLevel.L;

	/** Location of position pattern in the image */
	public Polygon2D_F64 pp = new Polygon2D_F64(4);

	/** Text encoding mode */
	public QrCode.Mode mode = QrCode.Mode.UNKNOWN;

	/** Which mask is applied */
	public MicroQrCodeMaskPattern mask = MicroQrCodeMaskPattern.M00;

	/** The raw byte data encoded into the QR Code. data + ecc */
	public byte[] rawbits;

	/** Raw byte data after error correction has been applied to it. Only contains the data portion */
	public byte[] corrected;

	/**
	 * If applicable the message is decoded into a sequence of characters.
	 */
	public String message = "";

	/**
	 * Approximate bounding box for QR-Code. The bottom right corner is estimated by intersecting lines
	 * and should not be used in SFM applications.
	 *
	 * Order: top-left = 0. Top-right = 1, Bottom-Right = 2, Bottom-Left = 3.
	 */
	public Polygon2D_F64 bounds = new Polygon2D_F64(4);

	/**
	 * A homography transform from grid bit coordinates into image pixels.
	 */
	public Homography2D_F64 Hinv = new Homography2D_F64();

	/**
	 * True if the QR code was incorrectly encoded and the bits are transposed. If this is true then the position
	 * patterns are stored in a transposed order. Bounds will not be affected.
	 */
	public boolean bitsTransposed;

	/** Number of zero bits used to endicate end of message */
	public int terminatorBits() {
		return 1 + version*2;
	}

	public int encodeFormatBits() {
		int bits = encodeVersionAndECC();
		bits |= mask.bits << 3;

		// TODO apply mask
		// TODO apply ECC

		return bits;
	}

	public boolean decodeFormatBits() {
		return true;
	}

	/** Computes the 3-bit format which encodes marker version and ECC level */
	public int encodeVersionAndECC() {
		return switch (version) {
			case 1 -> 0b000;
			case 2 -> switch (error) {
				case L -> 0b001;
				case M -> 0b010;
				default -> throw new IllegalArgumentException("Only L and M supported for version 2");
			};
			case 3 -> switch (error) {
				case L -> 0b011;
				case M -> 0b100;
				default -> throw new IllegalArgumentException("Only L and M supported for version 3");
			};
			case 4 -> switch (error) {
				case L -> 0b101;
				case M -> 0b110;
				case Q -> 0b111;
				default -> throw new IllegalArgumentException("Only L and M supported for version 3");
			};
			default -> throw new IllegalArgumentException("Only version 1 to 4 exist for Micro QR Code");
		};
	}

	public void decodeVersionAndECC( int code ) {
		switch (code) {
			case 0b000 -> version = 1;
			case 0b001, 0b010 -> version = 2;
			case 0b011, 0b100 -> version = 3;
			default -> version = 4;
		}

		switch (version) {
			case 2 -> error = code == 0b001 ? L : M;
			case 3 -> error = code == 0b011 ? L : M;
			case 4 -> error = switch (code & 0b011) {
				case 1 -> L;
				case 2 -> M;
				case 3 -> Q;
				default -> throw new IllegalArgumentException("Invalid code");
			};
		}
	}

	/**
	 * Returns the encoded mode as specified for each version. Number of bits needed to encode also varies.
	 */
	public int modeEncoding() {
		return switch (version) {
			case 1 -> 0;
			case 2 -> switch (mode) {
				case NUMERIC -> 0;
				case ALPHANUMERIC -> 1;
				default -> throw new IllegalArgumentException("Version 2 does not support " + mode);
			};
			case 3, 4 -> switch (mode) {
				case NUMERIC -> 0b00;
				case ALPHANUMERIC -> 0b01;
				case BYTE -> 0b10;
				case KANJI -> 0b11;
				default -> throw new IllegalArgumentException("Unknown " + mode);
			};

			default -> throw new IllegalArgumentException("Illegal version");
		};
	}

	static {
		// Manually entered from QR Code table. There's no simple equation for these magic numbers
		VERSION_INFO[1] = new VersionInfo(5);
		VERSION_INFO[1].add(DETECT, 5, 3);

		VERSION_INFO[2] = new VersionInfo(10);
		VERSION_INFO[2].add(L, 10, 5);
		VERSION_INFO[2].add(M, 10, 4);

		VERSION_INFO[3] = new VersionInfo(17);
		VERSION_INFO[3].add(L, 17, 11);
		VERSION_INFO[3].add(M, 17, 9);

		VERSION_INFO[4] = new VersionInfo(24);
		VERSION_INFO[4].add(L, 24, 16);
		VERSION_INFO[4].add(M, 24, 14);
		VERSION_INFO[4].add(Q, 24, 10);
	}

	public static int totalModules( int version ) {
		return version*2 + 11;
	}

	/** Error correction level */
	public enum ErrorLevel {
		/** Error detection only */
		DETECT,
		/** Error correction of about 7% */
		L,
		/** Error correction of about 15% */
		M,
		/** Error correction of about 25% */
		Q;

		public static ErrorLevel lookup( String letter ) {
			return switch (letter) {
				case "L" -> L;
				case "M" -> M;
				case "Q" -> Q;
				default -> throw new IllegalArgumentException("Unknown");
			};
		}
	}

	/**
	 * Specifies the format for a data block. Storage for data and ECC plus the number of blocks of each size.
	 */
	public static class BlockInfo {
		/** Code words per block */
		final public int codewords;

		/** Number of data codewords */
		final public int dataCodewords;

		public BlockInfo( int codewords, int dataCodewords ) {
			this.codewords = codewords;
			this.dataCodewords = dataCodewords;
		}
	}

	public static class VersionInfo {
		// total number of codewords available
		final public int codewords;

		// information for each error correction level
		final public Map<ErrorLevel, BlockInfo> levels = new HashMap<>();

		public VersionInfo( int codewords ) {
			this.codewords = codewords;
		}

		public void add( ErrorLevel level, int codeWords, int dataCodewords ) {
			levels.put(level, new BlockInfo(codeWords, dataCodewords));
		}

		/**
		 * Returns the total number of bytes in the data area that can be stored in at this version and error
		 * correction level.
		 *
		 * @param error Error correction level
		 * @return total bytes that can be stored.
		 */
		public int totalDataBytes( ErrorLevel error ) {
			BlockInfo b = Objects.requireNonNull(levels.get(error));
			int remaining = codewords - b.codewords;
			int blocksB = remaining/(b.codewords + 1);

			return b.dataCodewords + (b.dataCodewords + 1)*blocksB;
		}
	}

	/**
	 * The encoding mode. A QR Code can be encoded with multiple modes. The most complex character set is what the
	 * final mode is set to when decoding a QR code.
	 */
	public enum Mode {
		/** Place holder */
		UNKNOWN(-1),
		NUMERIC(0b0001),
		ALPHANUMERIC(0b0010),
		BYTE(0b0100),
		KANJI(0b1000);

		final int bits;

		Mode( int bits ) {
			this.bits = bits;
		}

		public static Mode lookup( int bits ) {
			if (NUMERIC.bits == bits)
				return NUMERIC;
			else if (ALPHANUMERIC.bits == bits)
				return ALPHANUMERIC;
			else if (BYTE.bits == bits)
				return BYTE;
			else if (KANJI.bits == bits)
				return KANJI;
			else
				return UNKNOWN;
		}

		public static @Nullable Mode lookup( String name ) {
			name = name.toUpperCase();
			Mode[] modes = values();
			for (int modeIdx = 0; modeIdx < modes.length; modeIdx++) {
				Mode m = modes[modeIdx];
				if (m.toString().equals(name))
					return m;
			}
			return null;
		}
	}
}
