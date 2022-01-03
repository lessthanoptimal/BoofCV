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
import boofcv.alg.fiducial.qrcode.PositionPatternNode;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPolygons2D_F64;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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

		var alg = new MicroQrCodeDecoderImage<>(null, GrayU8.class);
		var patterns = new DogArray<>(PositionPatternNode::new);

//		alg.setVerbose(System.out, null);

		for (int version = 1; version <= 4; version++) {
			ErrorLevel[] errors = MicroQrCode.allowedErrorCorrection(version);
			for (ErrorLevel error : errors) {
				// Generate the QR code
				var encoder = new MicroQrCodeEncoder();
//				encoder.setVerbose(System.out, null);
				MicroQrCode qr = encoder.setVersion(version).
						setError(error).
						setMask(MicroQrCodeMaskPattern.M00).
						addNumeric(message).fixate();
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
				alg.process(patterns, engine.getGray());

				// Check results
				assertEquals(1, alg.successes.size());
				assertEquals(0, alg.failures.size());
				assertEquals(version, alg.successes.get(0).version);
				assertEquals(message, alg.successes.get(0).message);
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

		var alg = new MicroQrCodeDecoderImage<>(null, GrayU8.class);
		for (int i = 0; i < 4; i++) {
			alg.process(patterns, image);
			assertEquals(1, alg.successes.size());
			assertEquals(0, alg.failures.size());
			assertEquals("123LK", alg.successes.get(0).message);

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
		PositionPatternNode pp = addPositionPattern(qr, patterns);

		var alg = new MicroQrCodeDecoderImage<>(null, GrayU8.class);
		alg.process(patterns, image);

		assertEquals(1, alg.successes.size());
		assertEquals(0, alg.failures.size());
		assertEquals("123LK%$63", alg.successes.get(0).message);
	}

	@Test void withLensDistortion() {
		fail("Implement");
	}

	@Test void transposed() {
		fail("Implement");
	}

	@Test void readFormatBitValues() {
		fail("Implement");
	}

	@Test void bitIntensityToBitValue() {
		fail("Implement");
	}

	private GrayU8 render(MicroQrCode qr) {
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
