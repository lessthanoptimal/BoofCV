/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

import static boofcv.alg.fiducial.qrcode.QrCodeMaskPattern.*;


// TODO Structure Appended

/**
 * Information for a detected QR Code.
 *
 * <p>Position Patterns (PP) have their vertices CCW order. The polygons are oriented such that the following sides are
 * paired: ppCorner[1,2] paired ppRight[3,0] and ppCorner[2,3] paired ppDown[0,1].</p>
 *
 * @author Peter Abeles
 */
public class QrCode {
	/**
	 * The finder pattern that is composed of the 3 position patterns.
	 */
	public Polygon2D_F64 ppRight = new Polygon2D_F64(4);
	public Polygon2D_F64 ppCorner = new Polygon2D_F64(4);
	public Polygon2D_F64 ppDown = new Polygon2D_F64(4);

	// locally computed binary threshold at each position pattern
	public double threshRight,threshCorner,threshDown;

	/** which version of QR code was found. 1 to 40*/
	public int version;

	/** Level of error correction */
	ErrorCorrectionLevel errorCorrection;
	/**
	 * 3 byte value indicating the mask pattern used in the QR code
	 */
	public int maskPattern;

	/**
	 * Alignment pattern information
	 */
	public FastQueue<Alignment> alignment = new FastQueue<>(Alignment.class,true);

	/**
	 * Text encoding mode
	 */
	public Mode mode;

	/**
	 * The raw byte data encoded into the QR Code
	 */
	public byte[] dataRaw = new byte[0];

	/**
	 * Approximate bounding box for QR-Code. The bottom right corner is estimated by intersecting lines
	 * and should not be used in SFM applications.
	 *
	 * Order: top-left = 0. Top-right = 1, Bottom-Right = 2, Bottom-Left = 3.
	 */
	public Polygon2D_F64 bounds = new Polygon2D_F64(4);

	public QrCode() {
		reset();
	}

	public QrCodeMaskPattern lookupMask() {
		switch( maskPattern ) {
			case 0b000: return M000;
			case 0b001: return M001;
			case 0b010: return M010;
			case 0b011: return M011;
			case 0b100: return M100;
			case 0b101: return M101;
			case 0b110: return M110;
			case 0b111: return M111;
			default: throw new RuntimeException("Unknown mask: "+maskPattern);
		}
	}

	public void reset() {
		for (int i = 0; i < 4; i++) {
			ppCorner.get(i).set(0,0);
			ppDown.get(i).set(0,0);
			ppRight.get(i).set(0,0);
		}

		version = 1;
		errorCorrection = ErrorCorrectionLevel.L;
		maskPattern = 0b101;
		alignment.reset();
		mode = Mode.ALPHANUMERIC;
	}

	public enum ErrorCorrectionLevel {
		L(0b01),
		M(0b00),
		Q(0b11),
		H(0b10);

		ErrorCorrectionLevel(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		public static ErrorCorrectionLevel lookup( int value ) {
			switch( value ) {
				case 0b01:return L;
				case 0b00:return M;
				case 0b11:return Q;
				case 0b10:return H;
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
	}

	public enum Mode {
		ECI,
		NUMERIC,
		ALPHANUMERIC,
		BYTE,
		KANJI
	}
}
