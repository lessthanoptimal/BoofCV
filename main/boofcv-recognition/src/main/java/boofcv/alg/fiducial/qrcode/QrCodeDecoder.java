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

import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

/**
 * TODO document
 *
 * @author Peter Abeles
 */
public class QrCodeDecoder<T extends ImageGray<T>> {

	FastQueue<QrCode> found = new FastQueue<>(QrCode.class,true);

	SquareBitReader squareDecoder;
	PackedBits32 bits = new PackedBits32();

	// internal workspace
	Point2D_F64 grid = new Point2D_F64();

	public QrCodeDecoder( Class<T> imageType ) {
		squareDecoder = new SquareBitReader(imageType);
	}

	/**
	 *
	 * @param pps
	 * @param gray
	 */
	public void process(FastQueue<PositionPatternNode> pps , T gray ) {
		squareDecoder.setImage(gray);
		found.reset();

		for (int i = 0; i < pps.size; i++) {
			PositionPatternNode ppn = pps.get(i);

			for (int j = 3,k=0; k < 4; j=k,k++) {
				if( ppn.edges[j] != null && ppn.edges[k] != null ) {
					QrCode qr = found.grow();

					setPositionPatterns(ppn, j, k, qr);
					computeBoundingBox(qr);

					// Decode the entire marker now
					if( !decode(qr)) {
						found.removeTail();
					}
				}
			}
		}
	}

	static void setPositionPatterns(PositionPatternNode ppn,
									int cornerToRight, int cornerToDown,
									QrCode qr) {
		// copy the 3 position patterns over
		PositionPatternNode right = ppn.edges[cornerToRight].destination(ppn);
		PositionPatternNode down = ppn.edges[cornerToDown].destination(ppn);
		qr.ppRight.set( right.square );
		qr.ppCorner.set( ppn.square );
		qr.ppDown.set( down.square );

		qr.threshRight  = right.grayThreshold;
		qr.threshCorner = ppn.grayThreshold;
		qr.threshDown   = down.grayThreshold;

		// Put it into canonical orientation
		int indexR = right.findEdgeIndex(ppn);
		int indexD = down.findEdgeIndex(ppn);

		rotateUntilAt(qr.ppRight,indexR,3);
		rotateUntilAt(qr.ppCorner,cornerToRight,1);
		rotateUntilAt(qr.ppDown,indexD,0);
	}

	static void rotateUntilAt(Polygon2D_F64 square , int current , int desired ) {
		while( current != desired ) {
			UtilPolygons2D_F64.shiftDown(square);
			current = (current+1)%4;
		}
	}

	/**
	 * 3 or the 4 corners are from the position patterns. The 4th is extrapolated using the position pattern
	 * sides.
	 * @param qr
	 */
	static void computeBoundingBox(QrCode qr ) {
		qr.bounds.get(0).set(qr.ppCorner.get(0));
		qr.bounds.get(1).set(qr.ppRight.get(1));
		Intersection2D_F64.intersection(
				qr.ppRight.get(1),qr.ppRight.get(2),
				qr.ppDown.get(3),qr.ppDown.get(2),qr.bounds.get(2));
		qr.bounds.get(3).set(qr.ppDown.get(3));
	}

	private boolean decode( QrCode qr ) {
		if( !extractFormatInfo(qr) )
			return false;
		if( !extractVersionInfo(qr) )
			return false;

		return true;
	}

	/**
	 * Reads format info bits from the image and saves the results in qr
	 * @return true if successful or false if it failed
	 */
	private boolean extractFormatInfo(QrCode qr) {
		for (int i = 0; i < 2; i++) {
			// probably a better way to do this would be to go with the region that has the smallest
			// hamming distance
			if (i == 0)
				readFormatRegion0(qr);
			else
				readFormatRegion1(qr);
			int bits = this.bits.data[0] ^ QrCodePolynomialMath.FORMAT_MASK;
			int message;
			if (QrCodePolynomialMath.checkFormatBits(bits)) {
				message = bits >> 10;
			} else {
				message = QrCodePolynomialMath.correctFormatBits(bits);
			}
			if (message >= 0) {
				QrCodePolynomialMath.decodeFormatMessage(message, qr);
				return true;
			}
		}
		return false;
	}

	/**
	 * Reads the format bits near the corner position pattern
	 */
	private boolean readFormatRegion0(QrCode qr) {
		// set the coordinate system to the closest pp to reduce position errors
		if( !squareDecoder.setSquare(qr.ppCorner,(float)qr.threshCorner) )
			return false;

		bits.resize(15);
		bits.zero();
		for (int i = 0; i < 6; i++) {
			read(i,i,8);
		}

		read(6,7,8);
		read(7,8,8);
		read(8,8,7);

		for (int i = 0; i < 6; i++) {
			read(9+i,8,5-i);
		}

		return true;
	}

	/**
	 * Read the format bits on the right and bottom patterns
	 */
	private boolean readFormatRegion1(QrCode qr) {
		// set the coordinate system to the closest pp to reduce position errors
		if( !squareDecoder.setSquare(qr.ppRight,(float)qr.threshRight) )
			return false;

		bits.resize(15);
		for (int i = 0; i < 8; i++) {
			read(i,8,6-i);
		}

		if( !squareDecoder.setSquare(qr.ppDown,(float)qr.threshDown) )
			return false;

		for (int i = 0; i < 6; i++) {
			read(i+8,i,8);
		}

		return true;
	}

	private void read( int bit , int row , int col ) {
		int value = squareDecoder.read(row,col);
		if( value == -1 ) {
			// The requested region is outside the image. A partial QR code can be read so let's just
			// assign it a value of zero and let error correction handle this
			value = 0;
		}
		bits.set(bit,value);
	}

	private boolean extractVersionInfo(QrCode qr) {
		int version = estimateVersionBySize(qr);

		// Fpr version 7 and beyond use the version which has been encoded into the qr code
		if( version >= 7 ) {
			readFormatRegion0(qr);
			int version0 = decodeVersion();
			readFormatRegion1(qr);
			int version1 = decodeVersion();

			if (version0 < 1 && version1 < 1) { // both decodings failed
				version = -1;
			} else if (version0 < 1) { // one failed so use the good one
				version = version1;
			} else if (version1 < 1) {
				version = version0;
			} else if( version0 != version1 ){
				version = -1;
			} else {
				version = version0;
			}
		}
		qr.version = version;
		return version != -1;
	}

	private int decodeVersion() {
		int bits = this.bits.data[0];
		int message;
		if (QrCodePolynomialMath.checkVersionBits(bits)) {
			message = bits >> 12;
		} else {
			message = QrCodePolynomialMath.correctVersionBits(bits);
		}
		return message;
	}

	/**
	 * Attempts to estimate the qr-code's version based on distance between position patterns.
	 * If it can't estimate it based on distance return -1
	 */
	private int estimateVersionBySize( QrCode qr ) {
		// Just need the homography for this corner square square
		if( !squareDecoder.setSquare(qr.ppCorner,0) )
			return -1;

		// Compute location of position patterns relative to corner PP
		squareDecoder.imageToGrid(qr.ppRight.get(0),grid);

		// see if pp is miss aligned. Probably not a flat surface
		// or they don't belong to the same qr code
		if( Math.abs(grid.y) >= 1 )
			return -1;

		double versionX = ((grid.x+7)-17)/4;

		squareDecoder.imageToGrid(qr.ppDown.get(0),grid);

		if( Math.abs(grid.x) >= 1 )
			return -1;

		double versionY = ((grid.y+7)-17)/4;

		// see if they are in agreement
		if( Math.abs(versionX-versionY) > 1.5 )
			return -1;

		return (int)((versionX+versionY)/2.0 + 0.5);
	}

	/**
	 * Reads the version bits near the right position pattern
	 */
	private boolean readVersionRegion0(QrCode qr) {
		// set the coordinate system to the closest pp to reduce position errors
		if (!squareDecoder.setSquare(qr.ppRight, (float) qr.threshRight))
			return false;

		bits.resize(18);
		for (int i = 0; i < 18; i++) {
			int row = i/3;
			int col = i%3;
			read(i,row,col-4);
		}

		return true;
	}

	/**
	 * Reads the version bits near the bottom position pattern
	 */
	private boolean readVersionRegion1(QrCode qr) {
		// set the coordinate system to the closest pp to reduce position errors
		if (!squareDecoder.setSquare(qr.ppDown, (float) qr.threshRight))
			return false;

		bits.resize(18);
		for (int i = 0; i < 18; i++) {
			int row = i/3;
			int col = i%3;
			read(i,row-4,col);
		}

		return true;
	}

	public FastQueue<QrCode> getFound() {
		return found;
	}
}
