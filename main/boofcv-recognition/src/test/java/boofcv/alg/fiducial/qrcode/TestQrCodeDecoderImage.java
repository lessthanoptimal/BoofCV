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

import boofcv.alg.fiducial.calib.squares.SquareEdge;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestQrCodeDecoderImage extends BoofStandardJUnit {

	@Test void withLensDistortion() {
		// render a distorted image
		var helper = new QrCodeDistortedChecks();

		helper.render();

		// find location of position patterns and create graph
		var pps = new DogArray<>(PositionPatternNode::new);

		pps.grow().square = new Polygon2D_F64(4);
		pps.grow().square = new Polygon2D_F64(4);
		pps.grow().square = new Polygon2D_F64(4);

		helper.setLocation(pps.get(0).square, pps.get(1).square, pps.get(2).square);
		for (int i = 0; i < pps.size; i++) {
			pps.get(i).grayThreshold = 125;
		}
		// these numbers were found by sketching the QR code
		connect(pps.get(2), pps.get(1), 3, 1);
		connect(pps.get(0), pps.get(1), 0, 2);

		// Should fail when run on distorted image
		var decoder = new QrCodeDecoderImage<>(null, GrayF32.class);
		decoder.process(pps, helper.image);

		assertEquals(0, decoder.successes.size());

		// now tell it how to undistort the image
		decoder.setLensDistortion(helper.image.width, helper.image.height, helper.distortion);
		for (int i = 0; i < pps.size; i++) {
			helper.distToUndist(pps.get(i).square);
		}
		decoder.process(pps, helper.image);

		assertEquals(1, decoder.successes.size());
		QrCode found = decoder.getFound().get(0);
		assertEquals(found.message, "123");
	}

	@Test void transposed() {
		String message = "TRANSPOSED";
		QrCode expected = new QrCodeEncoder().addAlphanumeric(message).fixate();

		var generator = new QrCodeGeneratorImage(4);
		generator.render(expected);
		DogArray<PositionPatternNode> pps = createPositionPatterns(generator);

		// transpose the image. This happens when an encoderer doesn't get the coordinate systems correct
		GrayU8 transposedImage = ImageMiscOps.transpose(generator.getGray(), null);

		// It should succeed since the flag is on by default
		var alg = new QrCodeDecoderImage<>(null, GrayU8.class);

		alg.process(pps, transposedImage);
		assertEquals(1, alg.successes.size());
		assertEquals(message, alg.getFound().get(0).message);

		// fail when you turn it off
		alg.considerTransposed = false;
		alg.process(pps, transposedImage);
		assertEquals(0, alg.successes.size());
	}

	@Test void transposePositionPatterns() {
		var qr = new QrCode();
		qr.ppCorner = new Polygon2D_F64(0, 0, 1, 0, 1, 1, 0, 1);
		qr.ppRight = new Polygon2D_F64(2, 0, 3, 0, 3, 1, 2, 1);
		qr.ppDown = new Polygon2D_F64(0, 2, 1, 2, 1, 3, 0, 3);

		new QrCodeDecoderImage<>(null, GrayU8.class).transposePositionPatterns(qr);

		assertEquals(0.0, qr.ppCorner.get(1).distance(0, 1), UtilEjml.TEST_F64);
		assertEquals(0.0, qr.ppRight.get(0).distance(0, 2), UtilEjml.TEST_F64);
		assertEquals(0.0, qr.ppRight.get(1).distance(0, 3), UtilEjml.TEST_F64);
		assertEquals(0.0, qr.ppDown.get(0).distance(2, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, qr.ppDown.get(1).distance(2, 1), UtilEjml.TEST_F64);
	}

	/**
	 * Run the entire algorithm on a rendered image but just care about the message
	 */
	@Test void message_numeric() {
		String message = "";
		for (int i = 0; i < 20; i++) {
			QrCode expected = new QrCodeEncoder().setVersion(1).
					setError(QrCode.ErrorLevel.M).
					setMask(QrCodeMaskPattern.M011).
					addNumeric(message).fixate();

			var generator = new QrCodeGeneratorImage(4);
			generator.render(expected);
			DogArray<PositionPatternNode> pps = createPositionPatterns(generator);

			var decoder = new QrCodeDecoderImage<>(null, GrayU8.class);
			decoder.process(pps, generator.getGray());

			assertEquals(1, decoder.successes.size());
			QrCode found = decoder.getFound().get(0);

			assertEquals(expected.version, found.version);
			assertEquals(expected.error, found.error);
			assertEquals(expected.mode, found.mode);
			assertEquals(message, found.message);
			message += (i%10) + "";
		}
	}

	@Test void message_alphanumeric() {
		String[] messages = new String[]{"", "0", "12", "123", "1234", "12345", "01234567ABCD%*+-./:"};

		for (String message : messages) {
			QrCode expected = new QrCodeEncoder().setVersion(2).
					setError(QrCode.ErrorLevel.M).
					setMask(QrCodeMaskPattern.M011).
					addAlphanumeric(message).fixate();

			var generator = new QrCodeGeneratorImage(4);
			generator.render(expected);
			DogArray<PositionPatternNode> pps = createPositionPatterns(generator);

//		ShowImages.showWindow(generator.getGray(),"QR Code", true);
//		BoofMiscOps.sleep(100000);

			var decoder = new QrCodeDecoderImage<>(null, GrayU8.class);
			decoder.process(pps, generator.getGray());

			assertEquals(1, decoder.successes.size());
			QrCode found = decoder.getFound().get(0);

			assertEquals(expected.version, found.version);
			assertEquals(expected.error, found.error);
			assertEquals(expected.mode, found.mode);
			assertEquals(message, found.message);
		}
	}

	@Test void message_byte() {
		QrCode expected = new QrCodeEncoder().setVersion(2).
				setError(QrCode.ErrorLevel.M).
				setMask(QrCodeMaskPattern.M011).
				addBytes(new byte[]{0x50, 0x70, 0x34, 0x2F}).fixate();

		var generator = new QrCodeGeneratorImage(4);
		generator.render(expected);
		DogArray<PositionPatternNode> pps = createPositionPatterns(generator);

//		ShowImages.showWindow(generator.getGray(),"QR Code", true);
//		BoofMiscOps.sleep(100000);

		var decoder = new QrCodeDecoderImage<>(null, GrayU8.class);
		decoder.process(pps, generator.getGray());

		assertEquals(1, decoder.successes.size());
		QrCode found = decoder.getFound().get(0);

		assertEquals(expected.version, found.version);
		assertEquals(expected.error, found.error);
		assertEquals(expected.mode, found.mode);
		assertEquals("Pp4/", found.message);
	}

	@Test void message_kanji() {
		QrCode expected = new QrCodeEncoder().setVersion(2).
				setError(QrCode.ErrorLevel.M).
				setMask(QrCodeMaskPattern.M011).
				addKanji("阿ん鞠ぷへ≦Ｋ").fixate();

		var generator = new QrCodeGeneratorImage(4);
		generator.render(expected);
		DogArray<PositionPatternNode> pps = createPositionPatterns(generator);

//		ShowImages.showWindow(generator.getGray(),"QR Code", true);
//		BoofMiscOps.sleep(100000);

		var decoder = new QrCodeDecoderImage<>(null, GrayU8.class);
		decoder.process(pps, generator.getGray());

		assertEquals(1, decoder.successes.size());
		QrCode found = decoder.getFound().get(0);

		assertEquals(expected.version, found.version);
		assertEquals(expected.error, found.error);
		assertEquals(expected.mode, found.mode);
		assertEquals("阿ん鞠ぷへ≦Ｋ", found.message);
	}

	@Test void message_multiple() {
		QrCode expected = new QrCodeEncoder().setVersion(3).
				setError(QrCode.ErrorLevel.M).
				setMask(QrCodeMaskPattern.M011).
				addKanji("阿ん鞠ぷへ≦Ｋ").addNumeric("1235").addAlphanumeric("AF").addBytes("efg").fixate();

		var generator = new QrCodeGeneratorImage(4);
		generator.render(expected);
		DogArray<PositionPatternNode> pps = createPositionPatterns(generator);

//		ShowImages.showWindow(generator.getGray(),"QR Code", true);
//		BoofMiscOps.sleep(100000);

		var decoder = new QrCodeDecoderImage<>(null, GrayU8.class);
		decoder.process(pps, generator.getGray());

		assertEquals(1, decoder.successes.size());
		QrCode found = decoder.getFound().get(0);

		assertEquals(expected.version, found.version);
		assertEquals(expected.error, found.error);
		assertEquals(QrCode.Mode.MIXED, found.mode);
		assertEquals("阿ん鞠ぷへ≦Ｋ1235AFefg", found.message);
	}

	/**
	 * Runs through the entire algorithm using a rendered image
	 */
	@Test void full_simple() {
//		full_simple(7, QrCode.ErrorLevel.M, QrCodeMaskPattern.M010);

		for (QrCode.ErrorLevel error : QrCode.ErrorLevel.values()) {
			for (QrCodeMaskPattern mask : QrCodeMaskPattern.values()) {
				full_simple(1, error, mask);
				full_simple(2, error, mask);
				full_simple(7, error, mask);
				full_simple(20, error, mask);
				full_simple(40, error, mask);
			}
		}
	}

	public void full_simple( int version, QrCode.ErrorLevel error, QrCodeMaskPattern mask ) {
//		System.out.println("version "+version+" error "+error+" mask "+mask);
		QrCode expected = new QrCodeEncoder().setVersion(version).
				setError(error).
				setMask(mask).
				addNumeric("01234567").fixate();

		var generator = new QrCodeGeneratorImage(4);
		int border = generator.borderModule*4;
//		generator.renderData = false;
		generator.render(expected);

		DogArray<PositionPatternNode> pps = createPositionPatterns(generator);

		var decoder = new QrCodeDecoderImage<>(null, GrayU8.class);

//		ShowImages.showWindow(generator.getGray(),"QR Code");
//		BoofMiscOps.sleep(100000);

		decoder.process(pps, generator.getGray());

		assertEquals(1, decoder.successes.size());
		QrCode found = decoder.getFound().get(0);

		// Check format info
		assertEquals(expected.error, found.error);
		assertSame(expected.mask, found.mask);

		// check version info
		assertEquals(version, found.version);

		int numModules = QrCode.totalModules(version);

		// sanity check the position patterns
		for (int i = 0; i < 4; i++) {
			assertEquals(7*4, found.ppCorner.getSideLength(i), 0.01);
			assertEquals(7*4, found.ppRight.getSideLength(i), 0.01);
			assertEquals(7*4, found.ppDown.getSideLength(i), 0.01);
		}
		assertTrue(found.ppCorner.get(0).distance(border, border) < 1e-4);
		assertTrue(found.ppRight.get(0).distance(border + (numModules - 7)*4, border) < 1e-4);
		assertTrue(found.ppDown.get(0).distance(border, border + (numModules - 7)*4) < 1e-4);

		// Check alignment patterns
		int[] alignment = QrCode.VERSION_INFO[version].alignment;
		if (alignment.length == 0) {
			assertEquals(0, found.alignment.size);
		} else {
			assertEquals(found.alignment.size, alignment.length*alignment.length - 3);
			// TODO Check alignment pattern locations
		}
	}

	private DogArray<PositionPatternNode> createPositionPatterns( QrCodeGeneratorImage generator ) {
		var pps = new DogArray<>(PositionPatternNode::new);

		pps.grow().square = generator.qr.ppCorner;
		pps.grow().square = generator.qr.ppRight;
		pps.grow().square = generator.qr.ppDown;

		for (int i = 0; i < pps.size; i++) {
			pps.get(i).grayThreshold = 125;
		}

		connect(pps.get(1), pps.get(0), 3, 1);
		connect(pps.get(2), pps.get(0), 0, 2);
		return pps;
	}

	@Test void setPositionPatterns() {
		var corner = new Polygon2D_F64(0, 0, 2, 0, 2, 2, 0, 2);
		var right = new Polygon2D_F64(5, 0, 7, 0, 7, 2, 5, 2);
		var bottom = new Polygon2D_F64(0, 5, 2, 5, 2, 7, 0, 7);

		var n_corner = new PositionPatternNode();
		var n_right = new PositionPatternNode();
		var n_bottom = new PositionPatternNode();

		n_corner.square = corner;
		n_right.square = right;
		n_bottom.square = bottom;
		connect(n_right, n_corner, 3, 1);
		connect(n_bottom, n_corner, 0, 2);

		QrCode qr = new QrCode();
		QrCodeDecoderImage.setPositionPatterns(n_corner, 1, 2, qr);

		assertTrue(qr.ppCorner.get(0).distance(0, 0) < UtilEjml.TEST_F64);
		assertTrue(qr.ppRight.get(0).distance(5, 0) < UtilEjml.TEST_F64);
		assertTrue(qr.ppDown.get(0).distance(0, 5) < UtilEjml.TEST_F64);
	}

	private static void connect( PositionPatternNode a, PositionPatternNode b, int sideA, int sideB ) {
		SquareEdge e = new SquareEdge(a, b, 3, 1);
		a.edges[sideA] = b.edges[sideB] = e;
	}

	@Test void rotateUntilAt() {
		var square = new Polygon2D_F64(0, 0, 2, 0, 2, 2, 0, 2);
		assertTrue(square.isCCW());
		Polygon2D_F64 original = square.copy();

		QrCodeDecoderImage.rotateUntilAt(square, 0, 0);
		assertTrue(square.isIdentical(original, UtilEjml.TEST_F64));

		QrCodeDecoderImage.rotateUntilAt(square, 1, 0);
		assertTrue(square.isCCW());
		assertTrue(square.get(0).distance(2, 0) < UtilEjml.TEST_F64);

		QrCodeDecoderImage.rotateUntilAt(square, 0, 1);
		assertTrue(square.isIdentical(original, UtilEjml.TEST_F64));
	}

	@Test void computeBoundingBox() {
		QrCode qr = new QrCode();
		qr.ppCorner = new Polygon2D_F64(0, 0, 1, 0, 1, 1, 0, 1);
		qr.ppRight = new Polygon2D_F64(2, 0, 3, 0, 3, 1, 2, 1);
		qr.ppDown = new Polygon2D_F64(0, 2, 1, 2, 1, 3, 0, 3);

		QrCodeDecoderImage.computeBoundingBox(qr);

		assertTrue(qr.bounds.get(0).distance(0, 0) < UtilEjml.TEST_F64);
		assertTrue(qr.bounds.get(1).distance(3, 0) < UtilEjml.TEST_F64);
		assertTrue(qr.bounds.get(2).distance(3, 3) < UtilEjml.TEST_F64);
		assertTrue(qr.bounds.get(3).distance(0, 3) < UtilEjml.TEST_F64);
	}

	/**
	 * There was a bug where version 0 QR code was returned
	 */
	@Test void extractVersionInfo_version0() {
		var mock = new QrCodeDecoderImage<>(EciEncoding.UTF8, GrayU8.class) {
			@Override int estimateVersionBySize( QrCode qr ) {return 0;}

			@Override int decodeVersion() {return 8;}
		};

		QrCode qr = new QrCode();
		qr.version = 8;
		assertFalse(mock.extractVersionInfo(qr));
		assertEquals(-1, qr.version);
	}

	/**
	 * Check to see if the correct number of bits are read and that the correct grid coordinates have been selected
	 */
	@Test void readFormatRegion0() {
		var reader = new MockReader();
		var alg = new QrCodeDecoderImage<>(EciEncoding.UTF8, GrayU8.class);
		alg.gridReader = reader;

		alg.readFormatRegion0(new QrCode());
		assertEquals(15, reader.bitsRead.size());

		// From QR code specification
		for (int row = 0; row < 5; row++) {
			assertTrue(reader.containsPoint(row, 8));
		}
		for (int row = 7; row < 9; row++) {
			assertTrue(reader.containsPoint(row, 8));
		}
		assertTrue(reader.containsPoint(8, 7));
		for (int col = 0; col < 6; col++) {
			assertTrue(reader.containsPoint(8, col));
		}
	}

	/**
	 * Check to see if the correct number of bits are read and that the correct grid coordinates have been selected
	 */
	@Test void readFormatRegion1() {
		var reader = new MockReader();
		var alg = new QrCodeDecoderImage<>(EciEncoding.UTF8, GrayU8.class);
		alg.gridReader = reader;

		alg.readFormatRegion1(new QrCode());
		assertEquals(15, reader.bitsRead.size());
		for (int col = 0; col < 8; col++) {
			assertTrue(reader.containsPoint(8, col - 1));
		}
		for (int row = 0; row < 7; row++) {
			assertTrue(reader.containsPoint(row, 8));
		}
	}

	private static class MockReader extends QrCodeBinaryGridReader<GrayU8> {
		List<Point2D_I32> bitsRead = new ArrayList<>();

		public MockReader() {super(GrayU8.class);}

		/**
		 * Save which bits were read and always return 1
		 */
		@Override public int readBit( int row, int col ) {
			bitsRead.add(new Point2D_I32(row, col));
			return 1;
		}

		// We don't want this to do anything
		@Override public void setSquare( Polygon2D_F64 square, float threshold ) {}

		boolean containsPoint( int row, int col ) {
			for (Point2D_I32 p : bitsRead) {
				if (p.x == row && p.y == col)
					return true;
			}
			return false;
		}
	}
}
