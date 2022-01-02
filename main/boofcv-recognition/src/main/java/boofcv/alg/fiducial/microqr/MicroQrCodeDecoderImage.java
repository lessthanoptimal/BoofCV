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

import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.alg.fiducial.qrcode.PositionPatternNode;
import boofcv.alg.fiducial.qrcode.QrCodeAlignmentPatternLocator;
import boofcv.alg.fiducial.qrcode.QrCodeBinaryGridReader;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Given an image and a known location of a Micro QR Code, decode the marker.
 *
 * @author Peter Abeles
 */
public class MicroQrCodeDecoderImage<T extends ImageGray<T>> {
	// Converts read bits into a message
	MicroQrCodeDecoderBits decoder;

	public boolean considerTransposed = true;

	DogArray<MicroQrCode> storageQR = new DogArray<>(MicroQrCode::new, MicroQrCode::reset);
	List<MicroQrCode> successes = new ArrayList<>();
	List<MicroQrCode> failures = new ArrayList<>();

	// storage for read in bits from the grid
	PackedBits8 bits = new PackedBits8();

	// internal workspace
	Point2D_F64 grid = new Point2D_F64();

	QrCodeAlignmentPatternLocator<T> alignmentLocator;
	QrCodeBinaryGridReader<T> gridReader;

	// Storage for pixel intensity. There are N samples for each bit
	DogArray_F32 intensityBits = new DogArray_F32();

	/**
	 * @param forceEncoding Force the default encoding to be this. Null for default
	 */
	public MicroQrCodeDecoderImage( @Nullable String forceEncoding, Class<T> imageType ) {
		decoder = new MicroQrCodeDecoderBits(forceEncoding);
		gridReader = new QrCodeBinaryGridReader<>(imageType);
		alignmentLocator = new QrCodeAlignmentPatternLocator<>(imageType);
	}

	/**
	 * Attempts to decode a marker at every found position pattern inside the image
	 *
	 * @param pps List of potential markers
	 * @param gray Original gray scale image
	 */
	public void process( DogArray<PositionPatternNode> pps, T gray ) {
		gridReader.setImage(gray);
		storageQR.reset();
		successes.clear();
		failures.clear();

		for (int i = 0; i < pps.size; i++) {
			PositionPatternNode ppn = pps.get(i);

			MicroQrCode qr = storageQR.grow();

			// TODO define coordinate system

			// TODO use homography to set bounding box

			// Decode the entire marker now
			if (decode(gray, qr)) {
				successes.add(qr);
			} else {
				failures.add(qr);
			}
		}
	}

	private boolean decode( T gray, MicroQrCode qr ) {
		// TODO consider transpose inside of here. No need to read bits twice

		// TODO read format information

		return true;
	}
}
