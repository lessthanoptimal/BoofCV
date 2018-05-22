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

import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO document
 *
 * @author Peter Abeles
 */
	// TODO change QrCode to use GrowQueue's so that data can be recycled
	// TODO better support for damaged qr codes with missing finder patterns.
public class QrCodeDecoderImage<T extends ImageGray<T>> {

	// used to compute error correction
	QrCodeDecoderBits decoder = new QrCodeDecoderBits();

	FastQueue<QrCode> storageQR = new FastQueue<>(QrCode.class,true);
	List<QrCode> successes = new ArrayList<>();
	List<QrCode> failures = new ArrayList<>();

	// storage for read in bits from the grid
	PackedBits8 bits = new PackedBits8();

	// internal workspace
	Point2D_F64 grid = new Point2D_F64();

	QrCodeAlignmentPatternLocator<T> alignmentLocator;
	QrCodeBinaryGridReader<T> gridReader;

	public QrCodeDecoderImage(Class<T> imageType ) {
		gridReader = new QrCodeBinaryGridReader<>(imageType);
		alignmentLocator = new QrCodeAlignmentPatternLocator<>(imageType);
	}

	/**
	 *
	 * @param pps
	 * @param gray
	 */
	public void process(FastQueue<PositionPatternNode> pps , T gray ) {
		gridReader.setImage(gray);
		storageQR.reset();
		successes.clear();
		failures.clear();

		for (int i = 0; i < pps.size; i++) {
			PositionPatternNode ppn = pps.get(i);

			for (int j = 3,k=0; k < 4; j=k,k++) {
				if( ppn.edges[j] != null && ppn.edges[k] != null ) {
					QrCode qr = storageQR.grow();
					qr.reset();

					setPositionPatterns(ppn, j, k, qr);
					computeBoundingBox(qr);

					// Decode the entire marker now
					if( decode(gray,qr)) {
						successes.add(qr);
					} else {
						failures.add(qr);
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

	private boolean decode( T gray , QrCode qr ) {
		if( !extractFormatInfo(qr) ) {
			qr.failureCause = QrCode.Failure.FORMAT;
			return false;
		}
		if( !extractVersionInfo(qr) ) {
			qr.failureCause = QrCode.Failure.VERSION;
			return false;
		}
		if( !alignmentLocator.process(gray,qr )) {
			qr.failureCause = QrCode.Failure.ALIGNMENT;
			return false;
		}

		// First try using all the features then remove features if they have large errors and might
		// have incorrectly localized
		boolean success = false;
		gridReader.setMarker(qr);
		gridReader.getTransformGrid().addAllFeatures(qr);
		// by default it removes outside corners. This works most of the time
		for (int i = 0; i < 6; i++) {
			if( i > 0 ) {
				boolean removed = gridReader.getTransformGrid().removeFeatureWithLargestError();
				if( !removed ) {
					break;
				}
			}

			gridReader.getTransformGrid().computeTransform();
			qr.failureCause = QrCode.Failure.NONE;
			if( !readRawData(qr) ) {
				qr.failureCause = QrCode.Failure.READING_BITS;
//				System.out.println("failed trial "+i+" "+qr.failureCause);
				continue;
			}
			if( !decoder.applyErrorCorrection(qr)) {
				qr.failureCause = QrCode.Failure.ERROR_CORRECTION;
//				System.out.println("failed trial "+i+" "+qr.failureCause);
				continue;
			}

			success = true;
			break;
		}

		if( success ) {
			// if it can error the errors that means it has all the bits correct
			// that's why decode is outside of the loop above
			if( !decoder.decodeMessage(qr) ) {
				// error enum is set internally so that it can be more specific
//				System.out.println("failed trial "+i+" "+qr.failureCause);
				success = false;
			}
		}


//		System.out.println("success "+success+" v "+qr.version+" mask "+qr.mask+" error "+qr.error);
		qr.Hinv.set(gridReader.getTransformGrid().Hinv);
		return success;
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
			int bitField = this.bits.read(0,15,false);
			bitField ^= QrCodePolynomialMath.FORMAT_MASK;

			int message;
			if (QrCodePolynomialMath.checkFormatBits(bitField)) {
				message = bitField >> 10;
			} else {
				message = QrCodePolynomialMath.correctFormatBits(bitField);
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
		gridReader.setSquare(qr.ppCorner,(float)qr.threshCorner);

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
//		if( qr.ppRight.get(0).distance(988.8,268.3) < 30 )
//			System.out.println("tjere");
//		System.out.println(qr.ppRight.get(0));

		// set the coordinate system to the closest pp to reduce position errors
		gridReader.setSquare(qr.ppRight,(float)qr.threshRight);

		bits.resize(15);
		bits.zero();
		for (int i = 0; i < 8; i++) {
			read(i,8,6-i);
		}

		gridReader.setSquare(qr.ppDown,(float)qr.threshDown);

		for (int i = 0; i < 6; i++) {
			read(i+8,i,8);
		}

		return true;
	}

	/**
	 * Read the raw data from input memory
	 */
	private boolean readRawData( QrCode qr) {
		QrCode.VersionInfo info = QrCode.VERSION_INFO[qr.version];

		qr.rawbits = new byte[info.codewords];

		// predeclare memory
		bits.resize(info.codewords*8);

		// read bits from memory
		List<Point2D_I32> locationBits =  QrCode.LOCATION_BITS[qr.version];
		// end at bits.size instead of locationBits.size because location might point to useless bits
		for (int i = 0; i < bits.size; i++ ) {
			Point2D_I32 b = locationBits.get(i);
			readDataMatrix(i,b.y,b.x, qr.mask);
		}

//		System.out.println("Version "+qr.version);
//		System.out.println("bits8.size "+bits8.size+"  locationBits "+locationBits.size());
//		bits8.print();

		// copy over the results
		System.arraycopy(bits.data,0,qr.rawbits,0,qr.rawbits.length);

		return true;
	}

	/**
	 * Reads a bit from the image.
	 * @param bit Index the bit will be written to
	 * @param row row in qr code grid
	 * @param col column in qr code grid
	 */
	private void read(int bit , int row , int col ) {
		int value = gridReader.readBit(row,col);
		if( value == -1 ) {
			// The requested region is outside the image. A partial QR code can be read so let's just
			// assign it a value of zero and let error correction handle this
			value = 0;
		}
		bits.set(bit,value);
	}

	private void readDataMatrix(int bit , int row , int col , QrCodeMaskPattern mask ) {
		int value = gridReader.readBit(row,col);
		if( value == -1 ) {
			// The requested region is outside the image. A partial QR code can be read so let's just
			// assign it a value of zero and let error correction handle this
			value = 0;
		}
		bits.set(bit,mask.apply(row,col,value));
	}

	/**
	 * Determine the QR code's version. For QR codes version < 7 it can be determined using the marker's size alone.
	 * Otherwise the version is read from the image itself
	 * @return true if version was successfully extracted or false if it failed
	 */
	boolean extractVersionInfo(QrCode qr) {
		int version = estimateVersionBySize(qr);

		// For version 7 and beyond use the version which has been encoded into the qr code
		if( version >= QrCode.VERSION_ENCODED_AT) {
			readVersionRegion0(qr);
			int version0 = decodeVersion();
			readVersionRegion1(qr);
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
		} else if( version <= 0 ) {
			version = -1;
		}

		qr.version = version;
		return version >= 1 && version <= QrCode.MAX_VERSION;
	}

	/**
	 * Decode version information from read in bits
	 * @return The found version or -1 if it failed
	 */
	int decodeVersion() {
		int bitField = this.bits.read(0,18,false);
		int message;
		// see if there's any errors
		if (QrCodePolynomialMath.checkVersionBits(bitField)) {
			message = bitField >> 12;
		} else {
			message = QrCodePolynomialMath.correctVersionBits(bitField);
		}
		// sanity check results
		if( message > QrCode.MAX_VERSION || message < QrCode.VERSION_ENCODED_AT)
			return -1;

		return message;
	}

	/**
	 * Attempts to estimate the qr-code's version based on distance between position patterns.
	 * If it can't estimate it based on distance return -1
	 */
	int estimateVersionBySize( QrCode qr ) {
		// Just need the homography for this corner square square
		gridReader.setSquare(qr.ppCorner,0);

		// Compute location of position patterns relative to corner PP
		gridReader.imageToGrid(qr.ppRight.get(0),grid);

		// see if pp is miss aligned. Probably not a flat surface
		// or they don't belong to the same qr code
		if( Math.abs(grid.y) >= 1 )
			return -1;

		double versionX = ((grid.x+7)-17)/4;

		gridReader.imageToGrid(qr.ppDown.get(0),grid);

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
		gridReader.setSquare(qr.ppRight, (float) qr.threshRight);

		bits.resize(18);
		bits.zero();
		for (int i = 0; i < 18; i++) {
			int row = i/3;
			int col = i%3;
			read(i,row,col-4);
		}
//		System.out.println(" decoder version region 0 =  "+Integer.toBinaryString(bits.data[0]));


		return true;
	}

	/**
	 * Reads the version bits near the bottom position pattern
	 */
	private boolean readVersionRegion1(QrCode qr) {
		// set the coordinate system to the closest pp to reduce position errors
		gridReader.setSquare(qr.ppDown, (float) qr.threshDown);

		bits.resize(18);
		bits.zero();
		for (int i = 0; i < 18; i++) {
			int row = i%3;
			int col = i/3;
			read(i,row-4,col);
		}

//		System.out.println(" decoder version region 1 =  "+Integer.toBinaryString(bits.data[0]));

		return true;
	}

	public QrCodeAlignmentPatternLocator<T> getAlignmentLocator() {
		return alignmentLocator;
	}

	public List<QrCode> getFound() {
		return successes;
	}

	public List<QrCode> getFailures() {
		return failures;
	}
}
