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

package boofcv.alg.fiducial.qrcode;

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Uses position pattern graph to find candidate QR Codes. From those it attempts to decode each QR Code.
 *
 * @author Peter Abeles
 */
public class QrCodeDecoderImage<T extends ImageGray<T>> {
	// used to compute error correction
	QrCodeDecoderBits decoder;

	/**
	 * Should it consider a QR code which has been encoded with a transposed bit pattern?
	 *
	 * @see boofcv.factory.fiducial.ConfigQrCode#considerTransposed
	 */
	public boolean considerTransposed = true;

	DogArray<QrCode> storageQR = new DogArray<>(QrCode::new);
	List<QrCode> successes = new ArrayList<>();
	List<QrCode> failures = new ArrayList<>();

	// storage for read in bits from the grid
	PackedBits8 bits = new PackedBits8();

	// internal workspace
	Point2D_F64 grid = new Point2D_F64();
	Polygon2D_F64 tempTranspose = new Polygon2D_F64();

	QrCodeAlignmentPatternLocator<T> alignmentLocator;
	QrCodeBinaryGridReader<T> gridReader;

	// Storage for pixel intensity. There are N samples for each bit
	DogArray_F32 intensityBits = new DogArray_F32();

	/**
	 * @param forceEncoding Force the default encoding to be this. Null for default
	 */
	public QrCodeDecoderImage( @Nullable String forceEncoding, Class<T> imageType ) {
		decoder = new QrCodeDecoderBits(forceEncoding);
		gridReader = new QrCodeBinaryGridReader<>(imageType);
		alignmentLocator = new QrCodeAlignmentPatternLocator<>(imageType);
	}

	/**
	 * Detects QR Codes inside image using position pattern graph
	 *
	 * @param pps position pattern graph
	 * @param gray Gray input image
	 */
	public void process( DogArray<PositionPatternNode> pps, T gray ) {
		gridReader.setImage(gray);
		storageQR.reset();
		successes.clear();
		failures.clear();

		for (int i = 0; i < pps.size; i++) {
			PositionPatternNode ppn = pps.get(i);

			for (int j = 3, k = 0; k < 4; j = k, k++) {
				if (ppn.edges[j] != null && ppn.edges[k] != null) {
					QrCode qr = storageQR.grow();
					qr.reset();

					setPositionPatterns(ppn, j, k, qr);
					computeBoundingBox(qr);

					// Decode the entire marker now
					if (decode(gray, qr)) {
						successes.add(qr);
					} else {
						// Consider the possibility that the QR code was encoded incorrectly with transposed bits
						boolean success = false;
						if (considerTransposed) {
							transposePositionPatterns(qr);
							success = decode(gray, qr);
						}

						if (success) {
							qr.bitsTransposed = true;
							successes.add(qr);
						} else {
							failures.add(qr);
						}
					}
				}
			}
		}
	}

	/**
	 * Transposes the orientation of position patterns. This will make it read the bits in a different order
	 * enabling it to read QR codes which were incorrectly encoded.
	 *
	 * NOTE: In theory this could be made a bit more efficient by only sampling the image once and transposing
	 * the read bits instead. This is much easier to implement.
	 */
	void transposePositionPatterns( QrCode qr ) {
		tempTranspose.setTo(qr.ppDown);
		qr.ppDown.setTo(qr.ppRight);
		qr.ppRight.setTo(tempTranspose);

		transposeCorners(qr.ppCorner);
		transposeCorners(qr.ppRight);
		transposeCorners(qr.ppDown);
	}

	/** Transposes the order of corners in the quadrilateral */
	private static void transposeCorners( Polygon2D_F64 c ) {
		double tmpX = c.get(1).x;
		double tmpY = c.get(1).y;

		c.get(1).setTo(c.get(3));
		c.get(3).setTo(tmpX, tmpY);
	}

	/**
	 * <p>Specifies transforms which can be used to change coordinates from distorted to undistorted and the opposite
	 * coordinates. The undistorted image is never explicitly created.</p>
	 *
	 * @param width Input image width. Used in sanity check only.
	 * @param height Input image height. Used in sanity check only.
	 * @param model distortion model. Null to remove a distortion model.
	 */
	public void setLensDistortion( int width, int height,
								   @Nullable LensDistortionNarrowFOV model ) {
		alignmentLocator.setLensDistortion(width, height, model);
		gridReader.setLensDistortion(width, height, model);
	}

	static void setPositionPatterns( PositionPatternNode ppn,
									 int cornerToRight, int cornerToDown,
									 QrCode qr ) {
		// copy the 3 position patterns over
		PositionPatternNode right = ppn.edges[cornerToRight].destination(ppn);
		PositionPatternNode down = ppn.edges[cornerToDown].destination(ppn);
		qr.ppRight.setTo(right.square);
		qr.ppCorner.setTo(ppn.square);
		qr.ppDown.setTo(down.square);

		qr.threshRight = right.grayThreshold;
		qr.threshCorner = ppn.grayThreshold;
		qr.threshDown = down.grayThreshold;

		// Put it into canonical orientation
		int indexR = right.findEdgeIndex(ppn);
		int indexD = down.findEdgeIndex(ppn);

		rotateUntilAt(qr.ppRight, indexR, 3);
		rotateUntilAt(qr.ppCorner, cornerToRight, 1);
		rotateUntilAt(qr.ppDown, indexD, 0);
	}

	static void rotateUntilAt( Polygon2D_F64 square, int current, int desired ) {
		while (current != desired) {
			UtilPolygons2D_F64.shiftDown(square);
			current = (current + 1)%4;
		}
	}

	/**
	 * 3 or the 4 corners are from the position patterns. The 4th is extrapolated using the position pattern
	 * sides.
	 */
	static void computeBoundingBox( QrCode qr ) {
		qr.bounds.get(0).setTo(qr.ppCorner.get(0));
		qr.bounds.get(1).setTo(qr.ppRight.get(1));
		Intersection2D_F64.intersection(
				qr.ppRight.get(1), qr.ppRight.get(2),
				qr.ppDown.get(3), qr.ppDown.get(2), qr.bounds.get(2));
		qr.bounds.get(3).setTo(qr.ppDown.get(3));
	}

	private boolean decode( T gray, QrCode qr ) {
		if (!extractFormatInfo(qr)) {
			qr.failureCause = QrCode.Failure.FORMAT;
			return false;
		}
		if (!extractVersionInfo(qr)) {
			qr.failureCause = QrCode.Failure.VERSION;
			return false;
		}
		if (!alignmentLocator.process(gray, qr)) {
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
			if (i > 0) {
				boolean removed = gridReader.getTransformGrid().removeFeatureWithLargestError();
				if (!removed) {
					break;
				}
			}

			gridReader.getTransformGrid().computeTransform();
			qr.failureCause = QrCode.Failure.NONE;
			if (!readRawData(qr)) {
				qr.failureCause = QrCode.Failure.READING_BITS;
//				System.out.println("failed trial "+i+" "+qr.failureCause);
				continue;
			}
			if (!decoder.applyErrorCorrection(qr)) {
				qr.failureCause = QrCode.Failure.ERROR_CORRECTION;
//				System.out.println("failed trial "+i+" "+qr.failureCause);
				continue;
			}

			success = true;
			break;
		}

		if (success) {
			// if it can error the errors that means it has all the bits correct
			// that's why decode is outside of the loop above
			if (!decoder.decodeMessage(qr)) {
				// error enum is set internally so that it can be more specific
//				System.out.println("failed trial "+i+" "+qr.failureCause);
				success = false;
			}
		}


//		System.out.println("success "+success+" v "+qr.version+" mask "+qr.mask+" error "+qr.error);
		qr.Hinv.setTo(gridReader.getTransformGrid().Hinv);
		return success;
	}

	/**
	 * Reads format info bits from the image and saves the results in qr
	 *
	 * @return true if successful or false if it failed
	 */
	private boolean extractFormatInfo( QrCode qr ) {
		for (int i = 0; i < 2; i++) {
			// probably a better way to do this would be to go with the region that has the smallest
			// hamming distance
			if (i == 0)
				readFormatRegion0(qr);
			else
				readFormatRegion1(qr);
			int bitField = this.bits.read(0, 15, false);
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
	final boolean readFormatRegion0( QrCode qr ) {
		// set the coordinate system to the closest pp to reduce position errors
		gridReader.setSquare(qr.ppCorner, (float)qr.threshCorner);

		bits.resize(15);
		bits.zero();
		for (int i = 0; i < 6; i++) {
			read(i, i, 8);
		}

		read(6, 7, 8);
		read(7, 8, 8);
		read(8, 8, 7);

		for (int i = 0; i < 6; i++) {
			read(9 + i, 8, 5 - i);
		}

		return true;
	}

	/**
	 * Read the format bits on the right and bottom patterns
	 */
	final boolean readFormatRegion1( QrCode qr ) {
		// set the coordinate system to the closest pp to reduce position errors
		gridReader.setSquare(qr.ppRight, (float)qr.threshRight);

		bits.resize(15);
		bits.zero();
		for (int i = 0; i < 8; i++) {
			read(i, 8, 6 - i);
		}

		gridReader.setSquare(qr.ppDown, (float)qr.threshDown);

		for (int i = 0; i < 7; i++) {
			read(i + 8, i, 8);
		}

		return true;
	}

	/**
	 * Read the raw data from input memory
	 */
	private boolean readRawData( QrCode qr ) {
		QrCode.VersionInfo info = QrCode.VERSION_INFO[qr.version];

		qr.rawbits = new byte[info.codewords];

		// predeclare memory
		bits.resize(info.codewords*8);

		// Get the location of each bit
		List<Point2D_I32> locationBits = QrCode.LOCATION_BITS[qr.version];

		// read the pixel intensity values at each bit while computing a threshold for the
		// lower right corner region
		qr.threshDownRight = readBitIntensityAndThresholdDownRight(qr, locationBits);

		// Convert the sampled intensity at each bit into a binary number
		bitIntensityToBitValue(qr, locationBits);

		// copy over the results
		System.arraycopy(bits.data, 0, qr.rawbits, 0, qr.rawbits.length);

		return true;
	}

	/**
	 * Samples pixel intensity around the center of each bit. Multiple points are measured to reduce the amount of
	 * damage a single noisy pixel can cause.
	 *
	 * At the same time a threshold is computing for binarization from the pixel intensity values in the lower
	 * right corner. It assumes the distribution of white and black squares is about equal.
	 */
	private float readBitIntensityAndThresholdDownRight( QrCode qr, List<Point2D_I32> locationBits ) {
		// Sample the image intensity around each bit
		int numModules = qr.getNumberOfModules();
		intensityBits.reserve(locationBits.size()*5);
		intensityBits.reset();

		// end at bits.size instead of locationBits.size because location might point to useless bits
		int start = Math.max(8, numModules - 10);

		// sum of pixel intensity in lower right
		float sumLowerRight = 0.0f;
		// Number of points which contrinute to the dum
		int total = 0;

		// measure the intensity around each bit's location
		for (int bitIndex = 0; bitIndex < bits.size; bitIndex++) {
			Point2D_I32 b = locationBits.get(bitIndex);
			gridReader.readBitIntensity(b.y, b.x, intensityBits);

			// only consider points in the lower right corner
			if (b.x < start && b.y < start)
				continue;

			total += QrCodeBinaryGridReader.BIT_INTENSITY_SAMPLES;
			for (int i = intensityBits.size - QrCodeBinaryGridReader.BIT_INTENSITY_SAMPLES; i < intensityBits.size; i++) {
				sumLowerRight += intensityBits.data[i];
			}
		}

		// simple average for the threshold. Could be improved with Otsu, but be more expensive
		return sumLowerRight/total;
	}

	/**
	 * Takes the previously measured intensity at each bit and converts it into a binary value. The threshold used
	 * is computed by applying bilinear interpolation to the 4-thresholds computed at each corner of the QR code.
	 * This provides
	 */
	private void bitIntensityToBitValue( QrCode qr, List<Point2D_I32> locationBits ) {
		float gridSize = qr.getNumberOfModules() - 1.0f;
		float threshold00 = (float)qr.threshCorner;
		float threshold01 = (float)qr.threshRight;
		float threshold10 = (float)qr.threshDown;
		float threshold11 = (float)qr.threshDownRight;

		for (int intensityIndex = 0; intensityIndex < intensityBits.size; ) {
			int bitIndex = intensityIndex/5;

			Point2D_I32 b = locationBits.get(bitIndex);

			float bx = b.x/gridSize;
			float by = b.y/gridSize;

			// Compute threshold by performing bilinear interpolation between local thresholds at each corner
			float threshold = 0.0f;
			threshold += (1.0f - bx)*(1.0f - by)*threshold00;
			threshold += bx*(1.0f - by)*threshold01;
			threshold += bx*by*threshold11;
			threshold += (1.0f - bx)*by*threshold10;

			int votes = 0;
			votes += intensityBits.data[intensityIndex++] < threshold ? 1 : 0;
			votes += intensityBits.data[intensityIndex++] < threshold ? 1 : 0;
			votes += intensityBits.data[intensityIndex++] < threshold ? 1 : 0;
			votes += intensityBits.data[intensityIndex++] < threshold ? 1 : 0;
			votes += intensityBits.data[intensityIndex++] < threshold ? 1 : 0;

			int bit = votes >= 3 ? 1 : 0;

			bits.set(bitIndex, qr.mask.apply(b.y, b.x, bit));
		}
	}

	/**
	 * Reads a bit from the image.
	 *
	 * @param bit Index the bit will be written to
	 * @param row row in qr code grid
	 * @param col column in qr code grid
	 */
	private void read( int bit, int row, int col ) {
		int value = gridReader.readBit(row, col);
		if (value == -1) {
			// The requested region is outside the image. A partial QR code can be read so let's just
			// assign it a value of zero and let error correction handle this
			value = 0;
		}
		bits.set(bit, value);
	}

	/**
	 * Determine the QR code's version. For QR codes version < 7 it can be determined using the marker's size alone.
	 * Otherwise the version is read from the image itself
	 *
	 * @return true if version was successfully extracted or false if it failed
	 */
	boolean extractVersionInfo( QrCode qr ) {
		int version = estimateVersionBySize(qr);

		// For version 7 and beyond use the version which has been encoded into the qr code
		if (version >= QrCode.VERSION_ENCODED_AT) {
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
			} else if (version0 != version1) {
				version = -1;
			} else {
				version = version0;
			}
		} else if (version <= 0) {
			version = -1;
		}

		qr.version = version;
		return version >= 1 && version <= QrCode.MAX_VERSION;
	}

	/**
	 * Decode version information from read in bits
	 *
	 * @return The found version or -1 if it failed
	 */
	int decodeVersion() {
		int bitField = this.bits.read(0, 18, false);
		int message;
		// see if there's any errors
		if (QrCodePolynomialMath.checkVersionBits(bitField)) {
			message = bitField >> 12;
		} else {
			message = QrCodePolynomialMath.correctVersionBits(bitField);
		}
		// sanity check results
		if (message > QrCode.MAX_VERSION || message < QrCode.VERSION_ENCODED_AT)
			return -1;

		return message;
	}

	/**
	 * Attempts to estimate the qr-code's version based on distance between position patterns.
	 * If it can't estimate it based on distance return -1
	 */
	int estimateVersionBySize( QrCode qr ) {
		// Just need the homography for this corner square square
		gridReader.setMarkerUnknownVersion(qr, 0);

		// Compute location of position patterns relative to corner PP
		gridReader.imageToGrid(qr.ppRight.get(0), grid);

		// see if pp is miss aligned. Probably not a flat surface
		// or they don't belong to the same qr code
		if (Math.abs(grid.y/grid.x) >= 0.3)
			return -1;

		double versionX = ((grid.x + 7) - 17)/4;

		gridReader.imageToGrid(qr.ppDown.get(0), grid);

		if (Math.abs(grid.x/grid.y) >= 0.3)
			return -1;

		double versionY = ((grid.y + 7) - 17)/4;

		// see if they are in agreement
		if (Math.abs(versionX - versionY)/Math.max(versionX, versionY) > 0.4)
			return -1;

		return (int)((versionX + versionY)/2.0 + 0.5);
	}

	/**
	 * Reads the version bits near the right position pattern
	 */
	private boolean readVersionRegion0( QrCode qr ) {
		// set the coordinate system to the closest pp to reduce position errors
		gridReader.setSquare(qr.ppRight, (float)qr.threshRight);

		bits.resize(18);
		bits.zero();
		for (int i = 0; i < 18; i++) {
			int row = i/3;
			int col = i%3;
			read(i, row, col - 4);
		}
//		System.out.println(" decoder version region 0 =  "+Integer.toBinaryString(bits.data[0]));


		return true;
	}

	/**
	 * Reads the version bits near the bottom position pattern
	 */
	private boolean readVersionRegion1( QrCode qr ) {
		// set the coordinate system to the closest pp to reduce position errors
		gridReader.setSquare(qr.ppDown, (float)qr.threshDown);

		bits.resize(18);
		bits.zero();
		for (int i = 0; i < 18; i++) {
			int row = i%3;
			int col = i/3;
			read(i, row - 4, col);
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
