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

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.fiducial.qrcode.*;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.transform.homography.HomographyPointOps_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Given an image and a known location of a Micro QR Code, decode the marker.
 *
 * @author Peter Abeles
 */
public class MicroQrCodeDecoderImage<T extends ImageGray<T>> implements VerbosePrint {
	// Converts read bits into a message
	MicroQrCodeDecoderBits decoder;

	public boolean considerTransposed = true;

	DogArray<MicroQrCode> storageQR = new DogArray<>(MicroQrCode::new, MicroQrCode::reset);
	@Getter List<MicroQrCode> found = new ArrayList<>();
	@Getter List<MicroQrCode> failures = new ArrayList<>();

	// storage for read in bits from the grid
	PackedBits8 bits = new PackedBits8();

	// internal workspace
	QrCodeAlignmentPatternLocator<T> alignmentLocator;
	QrCodeBinaryGridReader<T> gridReader;

	// Storage for pixel intensity. There are N samples for each bit
	DogArray_F32 intensityBits = new DogArray_F32();

	@Nullable PrintStream verbose = null;

	/**
	 * @param forceEncoding Force the default encoding to be this. Null for default
	 */
	public MicroQrCodeDecoderImage( @Nullable String forceEncoding, String defaultEncoding, Class<T> imageType ) {
		decoder = new MicroQrCodeDecoderBits(forceEncoding, defaultEncoding);
		gridReader = new QrCodeBinaryGridReader<>(imageType);
		alignmentLocator = new QrCodeAlignmentPatternLocator<>(imageType);
	}

	/**
	 * Attempts to decode a marker at every found position pattern inside the image
	 *
	 * @param pps List of potential markers
	 * @param gray Original gray scale image
	 */
	public void process( List<PositionPatternNode> pps, T gray ) {
		gridReader.setImage(gray);
		storageQR.reset();
		found.clear();
		failures.clear();

		for (int i = 0; i < pps.size(); i++) {
			PositionPatternNode ppn = pps.get(i);

			MicroQrCode qr = storageQR.grow();
			qr.thresholdPP = ppn.grayThreshold;
			qr.pp.setTo(ppn.square);

			boolean success = false;

			// consider that the encoder is non-standard and rendered a transposed marker
			escape:for (int transposed = 0; transposed < (considerTransposed ? 2 : 1); transposed++) {
				if (transposed == 1)
					QrCodeDecoderImage.transposeCorners(qr.pp);

				// try different orientations. We don't know which one is valid
				for (int orientation = 0; orientation < 4; orientation++) {
					if (verbose != null) verbose.printf("idx=%d trans=%d orientation=%d pp=%s\n", i, transposed, orientation, qr.pp);
					// Decode the entire marker now
					if (decode(qr)) {
						found.add(qr);
						qr.bitsTransposed = transposed == 1;
						success = true;
						break escape;
					}

					// Try another orientation
					UtilPolygons2D_F64.shiftDown(qr.pp);
				}
			}


			if (!success) {
				failures.add(qr);
			}
		}
	}

	/**
	 * Decodes the message
	 *
	 * @return true if successful
	 */
	private boolean decode( MicroQrCode qr ) {
		qr.failureCause = QrCode.Failure.NONE;

		// Convert pixel values into format bit values
		if (!readFormatBitValues(qr)) {
			return false;
		}

		// Save the mapping from marker to pixel space as well as the location inside the image
		// Doing it here so if you look at a failed marker you know where it was
		qr.Hinv.setTo(gridReader.getTransformGrid().Hinv);

		if (!decodeFormatBitValues(qr)) {
			// Set the bounds to the position pattern since we don't know the version and can't infer the bounds
			qr.bounds.setTo(qr.pp);
			if (verbose != null) verbose.print("_ failed: reading format\n");
			qr.failureCause = QrCode.Failure.FORMAT;
			return false;
		}

		// Save where the marker is now that the version is known
		setBoundsOfMarker(qr);

		if (verbose != null) verbose.printf("valid: version=%d error=%s mask=%s\n", qr.version, qr.error, qr.mask);

		if (!readRawData(qr)) {
			if (verbose != null) verbose.print("_ failed: reading bits\n");
			qr.failureCause = QrCode.Failure.READING_BITS;
			return false;
		}

		if (!decoder.applyErrorCorrection(qr)) {
			if (verbose != null) verbose.print("_ failed: error correction\n");
			qr.failureCause = QrCode.Failure.ERROR_CORRECTION;
			return false;
		}

		if (!decoder.decodeMessage(qr)) {
			if (verbose != null) verbose.print("_ failed: decoding message\n");
			qr.failureCause = QrCode.Failure.DECODING_MESSAGE;
			return false;
		}

		if (verbose != null) verbose.printf("_ success: message='%s'\n", qr.message);

		return true;
	}

	/**
	 * Specify the bounds using the position pattern and corner estimates using the homography
	 */
	private void setBoundsOfMarker( MicroQrCode qr ) {
		int N = qr.getNumberOfModules();
		qr.bounds.get(0).setTo(qr.pp.get(0));
		HomographyPointOps_F64.transform(qr.Hinv, N, 0, qr.bounds.get(1));
		HomographyPointOps_F64.transform(qr.Hinv, N, N, qr.bounds.get(2));
		HomographyPointOps_F64.transform(qr.Hinv, 0, N, qr.bounds.get(3));
	}

	private boolean decodeFormatBitValues( MicroQrCode qr ) {
		// Decode the read in bits
		int bitField = this.bits.read(0, 15, false);
		// All zeros and All ones are typical failure modes for reading all white or black regions
		if (bitField == 0 || (bitField & 0b01111111_11111111) == 0b01111111_11111111)
			return false;
		bitField ^= MicroQrCode.FORMAT_MASK;

		return qr.decodeFormatBits(bitField);
	}

	boolean readFormatBitValues( MicroQrCode qr ) {
		gridReader.setSquare(qr.pp, (float)qr.thresholdPP);

		bits.resize(15);
		bits.zero();

		for (int i = 0; i < 8; i++) {
			read(i, i + 1, 8);
		}
		for (int i = 0; i < 7; i++) {
			read(i + 8, 8, 7 - i);
		}

		return true;
	}

	boolean readRawData( MicroQrCode qr ) {
		MicroQrCode.VersionInfo info = MicroQrCode.VERSION_INFO[qr.version];

		// Get the location of each bit
		List<Point2D_I32> locationBits = MicroQrCode.LOCATION_BITS[qr.version];

		qr.rawbits = new byte[info.codewords];

		readBitIntensityValues(locationBits);
		bitIntensityToBitValue(qr, locationBits);

		// copy over the results
		System.arraycopy(bits.data, 0, qr.rawbits, 0, qr.rawbits.length);

		return true;
	}

	/**
	 * Sample the pixel intensity values around each data bit
	 */
	void readBitIntensityValues( List<Point2D_I32> locationBits ) {
		intensityBits.reserve(locationBits.size()*5);
		intensityBits.reset();

		for (int bitIndex = 0; bitIndex < locationBits.size(); bitIndex++) {
			Point2D_I32 b = locationBits.get(bitIndex);
			gridReader.readBitIntensity(b.y, b.x, intensityBits);
		}
	}

	/**
	 * Use the previously sampled pixel intensity values to determine the bit values for the message
	 */
	void bitIntensityToBitValue( MicroQrCode qr, List<Point2D_I32> locationBits ) {
		// Handle the situation where the last data word is 4-bits only. We will force the upper 4 bits to be zero
		int numDataBits = qr.getMaxDataBits();
		if (numDataBits%8 == 0) {
			numDataBits = Integer.MAX_VALUE; // don't do anything when full word
		}

		bits.resize(locationBits.size());
		bits.zero();

		float threshold = (float)qr.thresholdPP;
		for (int intensityIndex = 0; intensityIndex < intensityBits.size; ) {
			int bitIndex = intensityIndex/5;

			Point2D_I32 b = locationBits.get(bitIndex);

			// Black bits are encoded as 1
			int votes = 0;
			votes += intensityBits.data[intensityIndex++] < threshold ? 1 : 0;
			votes += intensityBits.data[intensityIndex++] < threshold ? 1 : 0;
			votes += intensityBits.data[intensityIndex++] < threshold ? 1 : 0;
			votes += intensityBits.data[intensityIndex++] < threshold ? 1 : 0;
			votes += intensityBits.data[intensityIndex++] < threshold ? 1 : 0;

			int bit = votes >= 3 ? 1 : 0;

			// Skip over those bits as the word is only 4-bits not 8-bits
			if (bitIndex >= numDataBits)
				bitIndex += 4;
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
	 * <p>Specifies transforms which can be used to change coordinates from distorted to undistorted and the opposite
	 * coordinates. The undistorted image is never explicitly created.</p>
	 *
	 * @param width Input image width. Used in sanity check only.
	 * @param height Input image height. Used in sanity check only.
	 * @param model distortion model. Null to remove a distortion model.
	 */
	public void setLensDistortion( int width, int height, @Nullable LensDistortionNarrowFOV model ) {
		alignmentLocator.setLensDistortion(width, height, model);
		gridReader.setLensDistortion(width, height, model);
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(out, configuration, decoder);
	}
}
