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

package boofcv.alg.fiducial.microqr;

import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCode.Mode;
import boofcv.alg.fiducial.qrcode.QrCodeCodeWordLocations;
import boofcv.alg.fiducial.qrcode.QrCodePolynomialMath;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ejml.dense.fixed.CommonOps_DDF3;

import java.util.*;

import static boofcv.alg.fiducial.microqr.MicroQrCode.ErrorLevel.*;

/**
 * Information about a detected Micro QR Code.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"MutablePublicArray", "NullAway.Init"})
public class MicroQrCode implements Cloneable {

	/** Mask that's applied to format information when encoding */
	public static final int FORMAT_MASK = 0b100010001000101;

	/** Maximum possible version of a Micro QR Code */
	public static final int MAX_VERSION = 4;

	/** Specifies characteristics of each version */
	public static final VersionInfo[] VERSION_INFO = new VersionInfo[MAX_VERSION + 1];

	/** Location of data bits in the code qr for each version. Precomputed for speed. */
	public static final List<Point2D_I32>[] LOCATION_BITS = new ArrayList[MAX_VERSION + 1];

	/**
	 * Version of the marker. This can be from 1 to 4.
	 */
	public int version = -1;

	public ErrorLevel error = ErrorLevel.L;

	/** Location of position pattern (pp) in the image */
	public Polygon2D_F64 pp = new Polygon2D_F64(4);

	/** Locally computed threshold around the pp */
	public double thresholdPP;

	/** Text encoding mode */
	public Mode mode = Mode.UNKNOWN;

	/** Which mask is applied */
	public MicroQrCodeMaskPattern mask = MicroQrCodeMaskPattern.M00;

	/**
	 * Which string encoding was used decoding a {@link Mode#BYTE} message. If there is no BYTE encoding then
	 * this will be an empty string. If there are multiple independent BYTE segments then this will
	 * be the first one encountered.
	 */
	public String byteEncoding = "";

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

	/** Number of bit errors detected when apply error correction to the message */
	public int totalBitErrors;

	/** Specifies where the QR code parsing failed */
	public QrCode.Failure failureCause = QrCode.Failure.NONE;

	/** Specifies the subset of allowed mode for Micro QR Codes. */
	public static final Mode[] allowedModes = new Mode[]{
			Mode.NUMERIC, Mode.ALPHANUMERIC, Mode.BYTE, Mode.KANJI};

	public MicroQrCode() {
		reset();
	}

	/**
	 * Resets class variables into their initial state
	 */
	@SuppressWarnings({"NullAway"})
	public void reset() {
		for (int i = 0; i < 4; i++) {
			pp.get(i).setTo(0, 0);
			bounds.get(i).setTo(0, 0);
		}
		thresholdPP = 0;
		version = -1;
		error = MicroQrCode.ErrorLevel.L;
		mask = MicroQrCodeMaskPattern.M00;
		mode = Mode.UNKNOWN;
		byteEncoding = "";
		rawbits = null;
		corrected = null;
		message = null;
		bitsTransposed = false;
		CommonOps_DDF3.setIdentity(Hinv);
		totalBitErrors = 0;
		failureCause = QrCode.Failure.NONE;
	}

	/**
	 * Sets 'this' so that it's equivalent to 'o'.
	 *
	 * @param o The target object
	 */
	@SuppressWarnings("NullAway")
	public MicroQrCode setTo( MicroQrCode o ) {
		this.version = o.version;
		this.error = o.error;
		this.mask = o.mask;
		this.mode = o.mode;
		this.rawbits = o.rawbits == null ? null : o.rawbits.clone();
		this.corrected = o.corrected == null ? null : o.corrected.clone();
		this.message = o.message;
		this.bitsTransposed = o.bitsTransposed;
		this.pp.setTo(o.pp);
		this.thresholdPP = o.thresholdPP;
		this.failureCause = o.failureCause;
		this.bounds.setTo(o.bounds);
		this.Hinv.setTo(o.Hinv);
		this.totalBitErrors = o.totalBitErrors;
		return this;
	}

	@Override public MicroQrCode clone() {
		return new MicroQrCode().setTo(this);
	}

	/** Number of zero bits used to indicate end of message */
	public int terminatorBits() {
		if (version < 1)
			throw new IllegalArgumentException("Invalid version");
		return 1 + version*2;
	}

	/**
	 * Encoded format information with ECC. Does NOT apply the mask.
	 */
	public int encodeFormatBits() {
		int bits = encodeVersionAndEccLevel() << 2;
		bits |= mask.bits;
		return QrCodePolynomialMath.encodeFormatBits(bits);
	}

	/**
	 * Decodes format bits and updates the QR Code
	 *
	 * @param bits Format bits without mask
	 * @return true if error correction didn't have a fault
	 */
	public boolean decodeFormatBits( int bits ) {
		int corrected = QrCodePolynomialMath.correctFormatBits(bits);
		if (corrected < 0)
			return false;
		mask = MicroQrCodeMaskPattern.lookupMask(corrected & 0x03);
		return decodeVersionAndECC((corrected >> 2) & 0x07);
	}

	/** Computes the 3-bit format which encodes marker version and ECC level */
	public int encodeVersionAndEccLevel() {
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
				default -> throw new IllegalArgumentException("Unsupported error level " + error);
			};
			default -> throw new IllegalArgumentException("Only version 1 to 4 exist for Micro QR Code");
		};
	}

	/**
	 * Decodes the version of error level
	 *
	 * @return true if the code is valid
	 */
	public boolean decodeVersionAndECC( int code ) {
		switch (code & 0x07) {
			case 0b000 -> version = 1;
			case 0b001, 0b010 -> version = 2;
			case 0b011, 0b100 -> version = 3;
			default -> version = 4;
		}

		switch (version) {
			case 1 -> error = DETECT;
			case 2 -> error = code == 0b001 ? L : M;
			case 3 -> error = code == 0b011 ? L : M;
			case 4 -> {
				switch (code & 0b011) {
					case 1 -> error = L;
					case 2 -> error = M;
					case 3 -> error = Q;
					default -> {
						return false;
					}
				}
			}
		}
		return true;
	}

	/** Returns number of modules in the marker, i.e. number of squares wide */
	public int getNumberOfModules() {
		return totalModules(version);
	}

	/** Maximum number of data bits this marker can store */
	public int getMaxDataBits() {
		return maxDataBits(version, error);
	}

	static {
		// Manually entered from QR Code table. There's no simple equation for these magic numbers
		VERSION_INFO[1] = new VersionInfo(5);
		VERSION_INFO[1].add(DETECT, 3);

		VERSION_INFO[2] = new VersionInfo(10);
		VERSION_INFO[2].add(L, 5);
		VERSION_INFO[2].add(M, 4);

		VERSION_INFO[3] = new VersionInfo(17);
		VERSION_INFO[3].add(L, 11);
		VERSION_INFO[3].add(M, 9);

		VERSION_INFO[4] = new VersionInfo(24);
		VERSION_INFO[4].add(L, 16);
		VERSION_INFO[4].add(M, 14);
		VERSION_INFO[4].add(Q, 10);

		for (int version = 1; version <= MAX_VERSION; version++) {
			LOCATION_BITS[version] = QrCodeCodeWordLocations.microqr(version).bits;
		}
	}

	/** Number of modules (squares) across the marker is based on its version */
	public static int totalModules( int version ) {
		return version*2 + 9;
	}

	/** Returns number of data bits which can be encoded */
	public static int maxDataBits( int version, ErrorLevel level ) {
		// If this error level isn't supported return 0 since it can't encode any data
		if (!VERSION_INFO[version].isErrorSupported(level)) {
			return 0;
		}
		int bits = VERSION_INFO[version].levels(level).dataCodewords*8;

		// last data code word is 4 bits in these cases
		if (version == 1 || version == 3) {
			bits -= 4;
		}

		return bits;
	}

	/**
	 * Returns which error correction is allowed at a version.
	 */
	public static ErrorLevel[] allowedErrorCorrection( int version ) {
		// Order here from most correction to least. When it automatically selects error correction this is
		// important
		return switch (version) {
			case 1 -> new ErrorLevel[]{ErrorLevel.DETECT};
			case 2, 3 -> new ErrorLevel[]{ErrorLevel.M, ErrorLevel.L};
			case 4 -> new ErrorLevel[]{ErrorLevel.Q, ErrorLevel.M, ErrorLevel.L};
			default -> throw new IllegalArgumentException("Illegal version=" + version);
		};
	}

	/** Number of bits used to indicate the mode */
	public static int modeIndicatorBitCount( int version ) {
		if (version < 1 || version > 4)
			throw new IllegalArgumentException("Invalid version " + version);
		return version - 1;
	}

	public static Mode[] allowedModes( int version ) {
		return switch (version) {
			case 1 -> new Mode[]{Mode.NUMERIC};
			case 2 -> new Mode[]{Mode.NUMERIC, Mode.ALPHANUMERIC};
			default -> Mode.values();
		};
	}

	/**
	 * Looks up an allowed mode by value. Returns UNKNOWN if the value is out of range
	 */
	public static Mode valueToMode( int value ) {
		if (value < 0 || value > 3)
			return Mode.UNKNOWN;
		return allowedModes[value];
	}

	/** Returns number of data code words */
	public int getNumberOfDataCodeWords() {
		return VERSION_INFO[version].levels(error).dataCodewords;
	}

	/** Returns number of error code words */
	public int getNumberOfErrorCodeWords() {
		return VERSION_INFO[version].codewords - VERSION_INFO[version].levels(error).dataCodewords;
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
	 * Specifies information about the data in this marker
	 */
	public static class DataInfo {
		/** Number of data codewords */
		final public int dataCodewords;

		public DataInfo( int dataCodewords ) {
			this.dataCodewords = dataCodewords;
		}
	}

	public static class VersionInfo {
		// total number of codewords available
		final public int codewords;

		// information for each error correction level
		final public Map<ErrorLevel, DataInfo> levels = new HashMap<>();

		public VersionInfo( int codewords ) {
			this.codewords = codewords;
		}

		/**
		 * Returns true if you can use this level of error correction for this version
		 */
		public boolean isErrorSupported( ErrorLevel level ) {
			return levels.containsKey(level);
		}

		public DataInfo levels( ErrorLevel level ) {
			return Objects.requireNonNull(levels.get(level),
					"ErrorLevel " + level.name() + " not supported for this version");
		}

		public void add( ErrorLevel level, int dataCodewords ) {
			levels.put(level, new DataInfo(dataCodewords));
		}
	}
}
