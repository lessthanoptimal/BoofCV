/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static boofcv.alg.fiducial.qrcode.QrCode.ErrorLevel.*;


// TODO ECI Mode
// TODO Structure Appended

/**
 * Information for a detected QR Code.
 *
 * <p>Position Patterns (PP) have their vertices CCW order. The polygons are oriented such that the following sides are
 * paired: ppCorner[1,2] paired ppRight[3,0] and ppCorner[2,3] paired ppDown[0,1].</p>
 *
 * @author Peter Abeles
 */
public class QrCode implements Cloneable {

	/**
	 * Maximum possible version of a QR Code
	 */
	public static final int MAX_VERSION = 40;
	/**
	 * The QR code version after which and including version information is encoded into the QR code
	 */
	public static final int VERSION_ENCODED_AT = 7;

	public static final VersionInfo VERSION_INFO[] = new VersionInfo[MAX_VERSION+1];

	/**
	 * Location of data bits in the code qr for each version. Precomputed for speed.
	 */
	public static final List<Point2D_I32> LOCATION_BITS[] = new ArrayList[MAX_VERSION+1];

	/**
	 * The finder pattern that is composed of the 3 position patterns. Orientation of corners in each
	 * position pattern goes in clockwise direction (when viewed in an image, CCW mathematically) 0 = top left,
	 * 1 = top right, 2 = bottom right, 3 = bottom left.
	 */
	public Polygon2D_F64 ppRight = new Polygon2D_F64(4);
	public Polygon2D_F64 ppCorner = new Polygon2D_F64(4);
	public Polygon2D_F64 ppDown = new Polygon2D_F64(4);

	// locally computed binary threshold at each position pattern
	public double threshRight,threshCorner,threshDown;

	/** which version of QR code was found. 1 to 40*/
	public int version;

	/** Level of error correction */
	public ErrorLevel error;

	/**
	 * Which masking pattern is applied
	 */
	public QrCodeMaskPattern mask = null;

	/**
	 * Alignment pattern information
	 */
	public FastQueue<Alignment> alignment = new FastQueue<>(Alignment.class,true);

	/**
	 * Text encoding mode
	 */
	public Mode mode = Mode.UNKNOWN;

	/**
	 * The raw byte data encoded into the QR Code. data + ecc
	 */
	public byte[] rawbits = null;

	/**
	 * Raw byte data after error correction has been applied to it. Only contains the data portion.
	 */
	public byte[] corrected = null;

	/**
	 * If applicable the message is decoded into a sequence of characters.
	 */
	public String message = null;

	/**
	 * Specifies where the QR code parsing failed
	 */
	public Failure failureCause = null;

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

	static {
		// Manually entered from QR Code table. There's no simple equation for these magic numbers
		VERSION_INFO[1] = new VersionInfo(26, new int[0]);
		VERSION_INFO[1].add(L,26,19,1);
		VERSION_INFO[1].add(M,26,16,1);
		VERSION_INFO[1].add(Q,26,13,1);
		VERSION_INFO[1].add(H,26,9,1);

		VERSION_INFO[2] = new VersionInfo(44, new int[]{6,18});
		VERSION_INFO[2].add(L,44,34,1);
		VERSION_INFO[2].add(M,44,28,1);
		VERSION_INFO[2].add(Q,44,22,1);
		VERSION_INFO[2].add(H,44,16,1);

		VERSION_INFO[3] = new VersionInfo(70, new int[]{6,22});
		VERSION_INFO[3].add(L,70,55,1);
		VERSION_INFO[3].add(M,70,44,1);
		VERSION_INFO[3].add(Q,35,17,2);
		VERSION_INFO[3].add(H,35,13,2);

		VERSION_INFO[4] = new VersionInfo(100, new int[]{6,26});
		VERSION_INFO[4].add(L,100,80,1);
		VERSION_INFO[4].add(M,50,32,2);
		VERSION_INFO[4].add(Q,50,24,2);
		VERSION_INFO[4].add(H,25,9,4);

		VERSION_INFO[5] = new VersionInfo(134, new int[]{6,30});
		VERSION_INFO[5].add(L,134,108,1);
		VERSION_INFO[5].add(M,67,43,2);
		VERSION_INFO[5].add(Q,33,15,2);
		VERSION_INFO[5].add(H,33,11,2);

		VERSION_INFO[6] = new VersionInfo(172, new int[]{6,34});
		VERSION_INFO[6].add(L,86,68,2);
		VERSION_INFO[6].add(M,43,27,4);
		VERSION_INFO[6].add(Q,43,19,4);
		VERSION_INFO[6].add(H,43,15,4);

		VERSION_INFO[7] = new VersionInfo(196, new int[]{6,22,38});
		VERSION_INFO[7].add(L,98,78,2);
		VERSION_INFO[7].add(M,49,31,4);
		VERSION_INFO[7].add(Q,32,14,2);
		VERSION_INFO[7].add(H,39,13,4);

		VERSION_INFO[8] = new VersionInfo(242, new int[]{6,24,42});
		VERSION_INFO[8].add(L,121,97,2);
		VERSION_INFO[8].add(M,60,38,2);
		VERSION_INFO[8].add(Q,40,18,4);
		VERSION_INFO[8].add(H,40,14,4);

		VERSION_INFO[9] = new VersionInfo(292, new int[]{6,26,46});
		VERSION_INFO[9].add(L,146,116,2);
		VERSION_INFO[9].add(M,58,36,3);
		VERSION_INFO[9].add(Q,36,16,4);
		VERSION_INFO[9].add(H,36,12,4);

		VERSION_INFO[10] = new VersionInfo(346, new int[]{6,28,50});
		VERSION_INFO[10].add(L,86,68,2);
		VERSION_INFO[10].add(M,69,43,4);
		VERSION_INFO[10].add(Q,43,19,6);
		VERSION_INFO[10].add(H,43,15,6);

		VERSION_INFO[11] = new VersionInfo(404, new int[]{6,30,54});
		VERSION_INFO[11].add(L,101,81,4);
		VERSION_INFO[11].add(M,80,50,1);
		VERSION_INFO[11].add(Q,50,22,4);
		VERSION_INFO[11].add(H,36,12,3);

		VERSION_INFO[12] = new VersionInfo(466, new int[]{6,32,58});
		VERSION_INFO[12].add(L,116,92,2);
		VERSION_INFO[12].add(M,58,36,6);
		VERSION_INFO[12].add(Q,46,20,4);
		VERSION_INFO[12].add(H,42,14,7);

		VERSION_INFO[13] = new VersionInfo(532, new int[]{6,34,62});
		VERSION_INFO[13].add(L,133,107,4);
		VERSION_INFO[13].add(M,59,37,8);
		VERSION_INFO[13].add(Q,44,20,8);
		VERSION_INFO[13].add(H,33,11,12);

		VERSION_INFO[14] = new VersionInfo(581, new int[]{6,26,46,66});
		VERSION_INFO[14].add(L,145,115,3);
		VERSION_INFO[14].add(M,64,40,4);
		VERSION_INFO[14].add(Q,36,16,11);
		VERSION_INFO[14].add(H,36,12,11);

		VERSION_INFO[15] = new VersionInfo(655, new int[]{6,26,48,70});
		VERSION_INFO[15].add(L,109,87,5);
		VERSION_INFO[15].add(M,65,41,5);
		VERSION_INFO[15].add(Q,54,24,5);
		VERSION_INFO[15].add(H,36,12,11);

		VERSION_INFO[16] = new VersionInfo(733, new int[]{6,26,50,74});
		VERSION_INFO[16].add(L,122,98,5);
		VERSION_INFO[16].add(M,73,45,7);
		VERSION_INFO[16].add(Q,43,19,15);
		VERSION_INFO[16].add(H,45,15,3);

		VERSION_INFO[17] = new VersionInfo(815, new int[]{6,30,54,78});
		VERSION_INFO[17].add(L,135,107,1);
		VERSION_INFO[17].add(M,74,46,10);
		VERSION_INFO[17].add(Q,50,22,1);
		VERSION_INFO[17].add(H,42,14,2);

		VERSION_INFO[18] = new VersionInfo(901, new int[]{6,30,56,82});
		VERSION_INFO[18].add(L,150,120,5);
		VERSION_INFO[18].add(M,69,43,9);
		VERSION_INFO[18].add(Q,50,22,17);
		VERSION_INFO[18].add(H,42,14,2);

		VERSION_INFO[19] = new VersionInfo(991, new int[]{6,30,58,86});
		VERSION_INFO[19].add(L,141,113,3);
		VERSION_INFO[19].add(M,70,44,3);
		VERSION_INFO[19].add(Q,47,21,17);
		VERSION_INFO[19].add(H,39,13,9);

		VERSION_INFO[20] = new VersionInfo(1085, new int[]{6,34,62,90});
		VERSION_INFO[20].add(L,135,107,3);
		VERSION_INFO[20].add(M,67,41,3);
		VERSION_INFO[20].add(Q,54,24,15);
		VERSION_INFO[20].add(H,43,15,15);

		VERSION_INFO[21] = new VersionInfo(1156, new int[]{6,28,50,72,94});
		VERSION_INFO[21].add(L,144,116,4);
		VERSION_INFO[21].add(M,68,42,17);
		VERSION_INFO[21].add(Q,50,22,17);
		VERSION_INFO[21].add(H,46,16,19);

		VERSION_INFO[22] = new VersionInfo(1258, new int[]{6,26,50,74,98});
		VERSION_INFO[22].add(L,139,111,2);
		VERSION_INFO[22].add(M,74,46,17);
		VERSION_INFO[22].add(Q,54,24,7);
		VERSION_INFO[22].add(H,37,13,34);

		VERSION_INFO[23] = new VersionInfo(1364, new int[]{6,30,54,78,102});
		VERSION_INFO[23].add(L,151,121,4);
		VERSION_INFO[23].add(M,75,47,4);
		VERSION_INFO[23].add(Q,54,24,11);
		VERSION_INFO[23].add(H,45,15,16);


		VERSION_INFO[24] = new VersionInfo(1474, new int[]{6,28,54,80,106});
		VERSION_INFO[24].add(L,147,117,6);
		VERSION_INFO[24].add(M,73,45,6);
		VERSION_INFO[24].add(Q,54,24,11);
		VERSION_INFO[24].add(H,46,16,30);

		VERSION_INFO[25] = new VersionInfo(1588, new int[]{6,32,58,84,110});
		VERSION_INFO[25].add(L,132,106,8);
		VERSION_INFO[25].add(M,75,47,8);
		VERSION_INFO[25].add(Q,54,24,7);
		VERSION_INFO[25].add(H,45,15,22);

		VERSION_INFO[26] = new VersionInfo(1706, new int[]{6,30,58,86,114});
		VERSION_INFO[26].add(L,142,114,10);
		VERSION_INFO[26].add(M,74,46,19);
		VERSION_INFO[26].add(Q,50,22,28);
		VERSION_INFO[26].add(H,46,16,33);

		VERSION_INFO[27] = new VersionInfo(1828, new int[]{6,34,62,90,118});
		VERSION_INFO[27].add(L,152,122,8);
		VERSION_INFO[27].add(M,73,45,22);
		VERSION_INFO[27].add(Q,53,23,8);
		VERSION_INFO[27].add(H,45,15,12);

		VERSION_INFO[28] = new VersionInfo(1921, new int[]{6,26,50,74,98,122});
		VERSION_INFO[28].add(L,147,117,3);
		VERSION_INFO[28].add(M,73,45,3);
		VERSION_INFO[28].add(Q,54,24,4);
		VERSION_INFO[28].add(H,45,15,11);

		VERSION_INFO[29] = new VersionInfo(2051, new int[]{6,30,54,78,102,126});
		VERSION_INFO[29].add(L,146,116,7);
		VERSION_INFO[29].add(M,73,45,21);
		VERSION_INFO[29].add(Q,53,23,1);
		VERSION_INFO[29].add(H,45,15,19);

		VERSION_INFO[30] = new VersionInfo(2185, new int[]{6,26,52,78,104,130});
		VERSION_INFO[30].add(L,145,115,5);
		VERSION_INFO[30].add(M,75,47,19);
		VERSION_INFO[30].add(Q,54,24,15);
		VERSION_INFO[30].add(H,45,15,23);

		VERSION_INFO[31] = new VersionInfo(2323, new int[]{6,30,56,82,108,134});
		VERSION_INFO[31].add(L,145,115,13);
		VERSION_INFO[31].add(M,74,46,2);
		VERSION_INFO[31].add(Q,54,24,42);
		VERSION_INFO[31].add(H,45,15,23);

		VERSION_INFO[32] = new VersionInfo(2465, new int[]{6,34,60,86,112,138});
		VERSION_INFO[32].add(L,145,115,17);
		VERSION_INFO[32].add(M,74,46,10);
		VERSION_INFO[32].add(Q,54,24,10);
		VERSION_INFO[32].add(H,45,15,19);

		VERSION_INFO[33] = new VersionInfo(2611, new int[]{6,30,58,86,114,142});
		VERSION_INFO[33].add(L,145,115,17);
		VERSION_INFO[33].add(M,74,46,14);
		VERSION_INFO[33].add(Q,54,24,29);
		VERSION_INFO[33].add(H,45,15,11);

		VERSION_INFO[34] = new VersionInfo(2761, new int[]{6,34,62,90,118,146});
		VERSION_INFO[34].add(L,145,115,13);
		VERSION_INFO[34].add(M,74,46,14);
		VERSION_INFO[34].add(Q,54,24,44);
		VERSION_INFO[34].add(H,46,16,59);

		VERSION_INFO[35] = new VersionInfo(2876, new int[]{6,30,54,78,102,126,150});
		VERSION_INFO[35].add(L,151,121,12);
		VERSION_INFO[35].add(M,75,47,12);
		VERSION_INFO[35].add(Q,54,24,39);
		VERSION_INFO[35].add(H,45,15,22);

		VERSION_INFO[36] = new VersionInfo(3034, new int[]{6,24,50,76,102,128,154});
		VERSION_INFO[36].add(L,151,121,6);
		VERSION_INFO[36].add(M,75,47,6);
		VERSION_INFO[36].add(Q,54,24,46);
		VERSION_INFO[36].add(H,45,15,2);

		VERSION_INFO[37] = new VersionInfo(3196, new int[]{6,28,54,80,106,132,158});
		VERSION_INFO[37].add(L,152,122,17);
		VERSION_INFO[37].add(M,74,46,29);
		VERSION_INFO[37].add(Q,54,24,49);
		VERSION_INFO[37].add(H,45,15,24);

		VERSION_INFO[38] = new VersionInfo(3362, new int[]{6,32,58,84,110,136,162});
		VERSION_INFO[38].add(L,152,122,4);
		VERSION_INFO[38].add(M,74,46,13);
		VERSION_INFO[38].add(Q,54,24,48);
		VERSION_INFO[38].add(H,45,15,42);

		VERSION_INFO[39] = new VersionInfo(3532, new int[]{6,26,54,82,110,138,166});
		VERSION_INFO[39].add(L,147,117,20);
		VERSION_INFO[39].add(M,75,47,40);
		VERSION_INFO[39].add(Q,54,24,43);
		VERSION_INFO[39].add(H,45,15,10);

		VERSION_INFO[40] = new VersionInfo(3706, new int[]{6,30,58,86,114,142,170});
		VERSION_INFO[40].add(L,148,118,19);
		VERSION_INFO[40].add(M,75,47,18);
		VERSION_INFO[40].add(Q,54,24,34);
		VERSION_INFO[40].add(H,45,15,20);

		for (int version = 1; version <= MAX_VERSION; version++) {
			QrCodeCodeWordLocations mask = new QrCodeCodeWordLocations(version);

			LOCATION_BITS[version] = mask.bits;
		}
	}

	public QrCode() {
		reset();
	}

	public int getNumberOfModules() {
		return totalModules(version);
	}

	public int getNumberOfDataBytes() {
		return VERSION_INFO[version].totalDataBytes(error);
	}

	public static int totalModules(int version ) {
		return version*4+17;
	}

	/**
	 * Resets the QR-Code so that it's in its initial state.
	 */
	public void reset() {
		for (int i = 0; i < 4; i++) {
			ppCorner.get(i).set(0,0);
			ppDown.get(i).set(0,0);
			ppRight.get(i).set(0,0);
		}
		this.threshCorner = 0;
		this.threshDown = 0;
		this.threshRight = 0;
		version = -1;
		error = L;
		mask = QrCodeMaskPattern.M111;
		alignment.reset();
		mode = Mode.UNKNOWN;
		failureCause = Failure.NONE;
		rawbits = null;
		corrected = null;
		message = null;
	}

	@Override
	public QrCode clone() {
		QrCode c = new QrCode();
		c.set(this);
		return c;
	}

	/**
	 * Sets 'this' so that it's equivalent to 'o'.
	 * @param o The target object
	 */
	public void set( QrCode o ) {
		this.version = o.version;
		this.error = o.error;
		this.mask = o.mask;
		this.mode = o.mode;
		this.rawbits = o.rawbits == null ? null : o.rawbits.clone();
		this.corrected = o.corrected == null ? null : o.corrected.clone();
		this.message = o.message;
		this.threshCorner = o.threshCorner;
		this.threshDown = o.threshDown;
		this.threshRight = o.threshRight;
		this.ppCorner.set(o.ppCorner);
		this.ppDown.set(o.ppDown);
		this.ppRight.set(o.ppRight);
		this.failureCause = o.failureCause;
		this.bounds.set(o.bounds);
		this.alignment.reset();
		for (int i = 0; i < o.alignment.size; i++) {
			this.alignment.grow().set(o.alignment.get(i));
		}
		this.Hinv.set(o.Hinv);
	}

	/**
	 * Error correction level
	 */
	public enum ErrorLevel {
		/**
		 * Error correction of about 7%
		 */
		L(0b01),
		/**
		 * Error correction of about 15%
		 */
		M(0b00),
		/**
		 * Error correction of about 25%
		 */
		Q(0b11),
		/**
		 * Error correction of about 30%
		 */
		H(0b10);

		ErrorLevel(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		public static ErrorLevel lookup(int value ) {
			switch( value ) {
				case 0b01:return L;
				case 0b00:return M;
				case 0b11:return Q;
				case 0b10:return H;
			}
			throw new IllegalArgumentException("Unknown");
		}

		public static ErrorLevel lookup(String letter) {
			switch( letter ) {
				case "L":return L;
				case "M":return M;
				case "Q":return Q;
				case "H":return H;
			}
			throw new IllegalArgumentException("Unknown");
		}

		int value;
	}

	/**
	 * Information related to a specific alignment pattern.  The center coordinate is stored.
	 */
	public static class Alignment
	{
		/**
		 * Pixel coordinate of this alignment pattern's center
		 */
		public Point2D_F64 pixel = new Point2D_F64();

		/**
		 * Center grid coordinate of alignment pattern.
		 */
		public int moduleX,moduleY;

		/**
		 * The found grid coordinate
		 */
		Point2D_F64 moduleFound = new Point2D_F64();

		/**
		 * Threshold value selected at this alignment pattern
		 */
		public double threshold;

		public void set( Alignment o ){
			this.pixel.set(o.pixel);
			this.moduleX = o.moduleX;
			this.moduleY = o.moduleY;
			this.moduleFound.set(o.moduleFound);
		}
	}

	public static class VersionInfo {

		// total number of codewords available
		final public int codewords;

		// location of alignment patterns
		final public int alignment[];

		// information for each error correction level
		final public Map<ErrorLevel,BlockInfo> levels = new HashMap<>();

		public VersionInfo(int codewords , int alignment[] ) {
			this.codewords = codewords;
			this.alignment = alignment;
		}

		public void add(ErrorLevel level , int codeWords, int dataCodewords , int eccBlocks ) {
			levels.put( level , new BlockInfo(codeWords,dataCodewords, eccBlocks));
		}

		/**
		 * Returns the total number of bytes in the data area that can be stored in at this version and error
		 * correction level.
		 * @param error Error correction level
		 * @return total bytes that can be stored.
		 */
		public int totalDataBytes( ErrorLevel error ) {
			BlockInfo b = levels.get(error);
			int remaining = codewords - b.codewords*b.blocks;
			int blocksB = remaining/(b.codewords+1);

			return b.dataCodewords*b.blocks + (b.dataCodewords+1)*blocksB;
		}
	}

	/**
	 * Specifies the format for a data block. Storage for data and ECC plus the number of blocks of each size.
	 */
	public static class BlockInfo {

		/**
		 * Code words per block
		 */
		final public int codewords;

		/**
		 * Number of data codewords
		 */
		final public int dataCodewords;

		/**
		 * Number of error data blocks of this size. There can be one other set of blocks which will store codewords
		 * + 1 and dataCodewords + 1.
		 */
		final public int blocks;

		public BlockInfo(int codewords, int dataCodewords, int blocks) {
			this.codewords = codewords;
			this.dataCodewords = dataCodewords;
			this.blocks = blocks;
		}
	}

	/**
	 * The encoding mode. A QR Code can be encoded with multiple modes. The most complex character set is what the
	 * final mode is set to when decoding a QR code.
	 */
	public enum Mode {
		UNKNOWN(-1),
		NUMERIC(0b0001),
		ALPHANUMERIC(0b0010),
		BYTE(0b0100),
		KANJI(0b1000),
		ECI(0b0111),
		STRUCTURE_APPENDED(0b0011),
		FNC1_FIRST(0b0101),
		FNC1_SECOND(0b1001);

		int bits;

		Mode(int bits) {
			this.bits = bits;
		}

		public static Mode lookup( int bits ) {
			if( NUMERIC.bits == bits )
				return NUMERIC;
			else if( ALPHANUMERIC.bits == bits )
				return ALPHANUMERIC;
			else if( BYTE.bits == bits )
				return BYTE;
			else if( KANJI.bits == bits )
				return KANJI;
			else if( ECI.bits == bits )
				return ECI;
			else if( STRUCTURE_APPENDED.bits == bits )
				return STRUCTURE_APPENDED;
			else if( FNC1_FIRST.bits == bits )
				return FNC1_FIRST;
			else if( FNC1_SECOND.bits == bits )
				return FNC1_SECOND;
			else
				return UNKNOWN;
		}

		public static Mode lookup( String name ) {
			name = name.toUpperCase();
			for( Mode m : values() ) {
				if( m.toString().equals(name))
					return m;
			}
			return null;
		}
	}

	/**
	 * Specifies the step at which decoding failed
	 */
	public enum Failure {
		NONE,
		FORMAT,
		VERSION,
		ALIGNMENT,
		READING_BITS,
		ERROR_CORRECTION,
		UNKNOWN_MODE,
		READING_PADDING,
		MESSAGE_OVERFLOW,
		DECODING_MESSAGE,
		KANJI_UNAVAILABLE,
		JIS_UNAVAILABLE
	}
}
