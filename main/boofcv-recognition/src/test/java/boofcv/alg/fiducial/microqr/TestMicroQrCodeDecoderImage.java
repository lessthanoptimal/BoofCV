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

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.fiducial.microqr.MicroQrCode.ErrorLevel;
import boofcv.alg.fiducial.qrcode.PackedBits32;
import boofcv.alg.fiducial.qrcode.PositionPatternNode;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPolygons2D_F64;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import static boofcv.alg.fiducial.microqr.MicroQrCodeMaskPattern.M00;
import static boofcv.alg.fiducial.microqr.MicroQrCodeMaskPattern.M01;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestMicroQrCodeDecoderImage extends BoofStandardJUnit {
	/**
	 * Render then decode images with all possible versions, masks, and error correction levels.
	 */
	@Test void simple_all_configs() {
		String message = "01234";

		var engine = new FiducialImageEngine();
		engine.configure(0, 100);
		var generator = new MicroQrCodeGenerator();
		generator.markerWidth = 100;
		generator.setRender(engine);

		var alg = new MicroQrCodeDecoderImage<>(null, "", GrayU8.class);
		var patterns = new DogArray<>(PositionPatternNode::new);

//		alg.setVerbose(System.out, null);

		for (int version = 1; version <= 4; version++) {
			ErrorLevel[] errors = MicroQrCode.allowedErrorCorrection(version);
			for (ErrorLevel error : errors) {
				// Generate the QR code
				var encoder = new MicroQrCodeEncoder();
//				encoder.setVerbose(System.out, null);
				MicroQrCode qr = encoder.setVersion(version).setError(error).setMask(M00).addNumeric(message).fixate();
				generator.render(qr);

				// Set up the "detected" position pattern
				patterns.reset();
				PositionPatternNode pp = patterns.grow();
				pp.square = qr.pp;
				pp.grayThreshold = (double)(engine.getWhite()/2);

//				ShowImages.showWindow(engine.getGray(), "Title");
//				BoofMiscOps.sleep(10_000);

				// Process the image
//				alg.decoder.setVerbose(System.out, null);
				alg.process(patterns.toList(), engine.getGray());

				// Check results
				assertEquals(1, alg.found.size());
				assertEquals(0, alg.failures.size());
				assertEquals(version, alg.found.get(0).version);
				assertEquals(message, alg.found.get(0).message);
			}
		}
	}

	/**
	 * Rotate the square and see if it can always decode it
	 */
	@Test void simple_rotateSquare() {
		MicroQrCode qr = new MicroQrCodeEncoder().addAutomatic("123LK").fixate();
		GrayU8 image = render(qr);

		var patterns = new DogArray<>(PositionPatternNode::new);
		PositionPatternNode pp = addPositionPattern(qr, patterns);

		var alg = new MicroQrCodeDecoderImage<>(null, "", GrayU8.class);
		for (int i = 0; i < 4; i++) {
			alg.process(patterns.toList(), image);
			assertEquals(1, alg.found.size());
			assertEquals(0, alg.failures.size());
			assertEquals("123LK", alg.found.get(0).message);

			// Rotate the polygon so that it now needs to compensate for that
			UtilPolygons2D_F64.shiftDown(pp.square);
		}
	}

	/**
	 * Decode a marker with byte encoded data
	 */
	@Test void decode_byte() {
		MicroQrCode qr = new MicroQrCodeEncoder().addBytes("123LK%$63").fixate();
		GrayU8 image = render(qr);

		var patterns = new DogArray<>(PositionPatternNode::new);
		addPositionPattern(qr, patterns);

		var alg = new MicroQrCodeDecoderImage<>(null, "", GrayU8.class);
		alg.process(patterns.toList(), image);

		assertEquals(1, alg.found.size());
		assertEquals(0, alg.failures.size());
		assertEquals("123LK%$63", alg.found.get(0).message);
	}

	/** Transposes the input image and sees if it can still be decoded by default */
	@Test void transposed() {
		MicroQrCode qr = new MicroQrCodeEncoder().addAutomatic("123LK").fixate();
		GrayU8 image = render(qr);
		GrayU8 imageTransposed = image.createSameShape();
		ImageMiscOps.transpose(image, imageTransposed);

		var patterns = new DogArray<>(PositionPatternNode::new);
		addPositionPattern(qr, patterns);

		var alg = new MicroQrCodeDecoderImage<>(null, "", GrayU8.class);

		alg.process(patterns.toList(), imageTransposed);
		assertEquals(1, alg.found.size());
		assertEquals(0, alg.failures.size());
		assertEquals("123LK", alg.found.get(0).message);

		// It should fail now since it won't consider a transposed marker
		alg.considerTransposed = false;
		alg.process(patterns.toList(), imageTransposed);
		assertEquals(0, alg.found.size());
		assertEquals(1, alg.failures.size());
	}

	@Test void readFormatBitValues() {
		String message = "12399";
		MicroQrCode qr = new MicroQrCodeEncoder().setVersion(2).setError(ErrorLevel.L).setMask(M01).addNumeric(message).fixate();

		GrayU8 image = render(qr);

		var alg = new MicroQrCodeDecoderImage<>(null, "", GrayU8.class);
		alg.gridReader.setImage(image);
		alg.readFormatBitValues(qr);

		assertEquals(15, alg.bits.size);

		// Compare to directly computed values
		var expected = new PackedBits32();
		expected.resize(15);
		expected.data[0] = qr.encodeFormatBits() ^ MicroQrCode.FORMAT_MASK;
		for (int i = 9; i < 15; i++) {
			assertEquals(expected.get(i), alg.bits.get(i));
		}
	}

	private GrayU8 render( MicroQrCode qr ) {
		var engine = new FiducialImageEngine();
		engine.configure(0, 100);
		var generator = new MicroQrCodeGenerator();
		generator.markerWidth = 100;
		generator.setRender(engine);

		generator.render(qr);
		return engine.getGray();
	}

	private PositionPatternNode addPositionPattern( MicroQrCode qr, DogArray<PositionPatternNode> patterns ) {
		PositionPatternNode pp = patterns.grow();
		pp.square = qr.pp;
		pp.grayThreshold = 127;
		return pp;
	}
}
