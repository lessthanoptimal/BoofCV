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

import boofcv.alg.fiducial.calib.squares.SquareEdge;
import boofcv.struct.image.GrayU8;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.UtilEjml;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestQrCodeDecoderImage {

	/**
	 * Run the entire algorithm on a rendered image but just care about the message
	 */
	@Test
	public void message_numeric() {
		String message = "";
		for (int i = 0; i < 20; i++) {
			QrCode expected = new QrCodeEncoder().setVersion(1).
					setError(QrCode.ErrorLevel.M).
					setMask(QrCodeMaskPattern.M011).
					addNumeric(message).fixate();

			QrCodeGeneratorImage generator = new QrCodeGeneratorImage(4);
			generator.render(expected);
			FastQueue<PositionPatternNode> pps = createPositionPatterns(generator);

//		ShowImages.showWindow(generator.gray,"QR Code", true);
//		BoofMiscOps.sleep(100000);

			QrCodeDecoderImage<GrayU8> decoder = new QrCodeDecoderImage<>(GrayU8.class);
			decoder.process(pps,generator.gray);

			assertEquals(1,decoder.successes.size());
			QrCode found = decoder.getFound().get(0);

			assertEquals(expected.version,found.version);
			assertEquals(expected.error,found.error);
			assertEquals(expected.mode,found.mode);
			assertEquals(message,new String(found.message));
			message += (i%10)+"";
		}
	}

	@Test
	public void message_alphanumeric() {
		String messages[] = new String[]{"","0","12","123","1234","12345","01234567ABCD%*+-./:"};

		for( String message : messages ) {
			QrCode expected = new QrCodeEncoder().setVersion(2).
					setError(QrCode.ErrorLevel.M).
					setMask(QrCodeMaskPattern.M011).
					addAlphanumeric(message).fixate();

			QrCodeGeneratorImage generator = new QrCodeGeneratorImage(4);
			generator.render(expected);
			FastQueue<PositionPatternNode> pps = createPositionPatterns(generator);

//		ShowImages.showWindow(generator.gray,"QR Code", true);
//		BoofMiscOps.sleep(100000);

			QrCodeDecoderImage<GrayU8> decoder = new QrCodeDecoderImage<>(GrayU8.class);
			decoder.process(pps, generator.gray);

			assertEquals(1, decoder.successes.size());
			QrCode found = decoder.getFound().get(0);

			assertEquals(expected.version, found.version);
			assertEquals(expected.error, found.error);
			assertEquals(expected.mode, found.mode);
			assertEquals(message, new String(found.message));
		}
	}

	@Test
	public void message_byte() {
		QrCode expected = new QrCodeEncoder().setVersion(2).
				setError(QrCode.ErrorLevel.M).
				setMask(QrCodeMaskPattern.M011).
				addBytes(new byte[]{0x50,0x70,0x34,0x2F}).fixate();

		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(4);
		generator.render(expected);
		FastQueue<PositionPatternNode> pps = createPositionPatterns(generator);

//		ShowImages.showWindow(generator.gray,"QR Code", true);
//		BoofMiscOps.sleep(100000);

		QrCodeDecoderImage<GrayU8> decoder = new QrCodeDecoderImage<>(GrayU8.class);
		decoder.process(pps,generator.gray);

		assertEquals(1,decoder.successes.size());
		QrCode found = decoder.getFound().get(0);

		assertEquals(expected.version,found.version);
		assertEquals(expected.error,found.error);
		assertEquals(expected.mode,found.mode);
		assertEquals("Pp4/",new String(found.message));
	}

	@Test
	public void message_kanji() {
		QrCode expected = new QrCodeEncoder().setVersion(2).
				setError(QrCode.ErrorLevel.M).
				setMask(QrCodeMaskPattern.M011).
				addKanji("阿ん鞠ぷへ≦Ｋ").fixate();

		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(4);
		generator.render(expected);
		FastQueue<PositionPatternNode> pps = createPositionPatterns(generator);

//		ShowImages.showWindow(generator.gray,"QR Code", true);
//		BoofMiscOps.sleep(100000);

		QrCodeDecoderImage<GrayU8> decoder = new QrCodeDecoderImage<>(GrayU8.class);
		decoder.process(pps,generator.gray);

		assertEquals(1,decoder.successes.size());
		QrCode found = decoder.getFound().get(0);

		assertEquals(expected.version,found.version);
		assertEquals(expected.error,found.error);
		assertEquals(expected.mode,found.mode);
		assertEquals("阿ん鞠ぷへ≦Ｋ",new String(found.message));
	}


	@Test
	public void message_multiple() {
		QrCode expected = new QrCodeEncoder().setVersion(3).
				setError(QrCode.ErrorLevel.M).
				setMask(QrCodeMaskPattern.M011).
				addKanji("阿ん鞠ぷへ≦Ｋ").addNumeric("1235").addAlphanumeric("AF").addBytes("efg").fixate();

		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(4);
		generator.render(expected);
		FastQueue<PositionPatternNode> pps = createPositionPatterns(generator);

//		ShowImages.showWindow(generator.gray,"QR Code", true);
//		BoofMiscOps.sleep(100000);

		QrCodeDecoderImage<GrayU8> decoder = new QrCodeDecoderImage<>(GrayU8.class);
		decoder.process(pps,generator.gray);

		assertEquals(1,decoder.successes.size());
		QrCode found = decoder.getFound().get(0);

		assertEquals(expected.version,found.version);
		assertEquals(expected.error,found.error);
		assertEquals(QrCode.Mode.KANJI,found.mode);
		assertEquals("阿ん鞠ぷへ≦Ｋ1235AFefg",new String(found.message));
	}
	/**
	 * Runs through the entire algorithm using a rendered image
	 */
	@Test
	public void full_simple() {
//		full_simple(7, QrCode.ErrorLevel.M, QrCodeMaskPattern.M010);

		for( QrCode.ErrorLevel error : QrCode.ErrorLevel.values() ) {
			for( QrCodeMaskPattern mask : QrCodeMaskPattern.values() ) {
				full_simple(1,error, mask);
				full_simple(2,error, mask);
				full_simple(7, error, mask);
				full_simple(20,error, mask);
				full_simple(40,error, mask);
			}
		}
	}

	public void full_simple(int version, QrCode.ErrorLevel error , QrCodeMaskPattern mask ) {
//		System.out.println("version "+version+" error "+error+" mask "+mask);
		QrCode expected = new QrCodeEncoder().setVersion(version).
				setError(error).
				setMask(mask).
				addNumeric("01234567").fixate();

		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(4);
		int border = generator.borderModule*4;
//		generator.renderData = false;
		generator.render(expected);

		FastQueue<PositionPatternNode> pps = createPositionPatterns(generator);

		QrCodeDecoderImage<GrayU8> decoder = new QrCodeDecoderImage<>(GrayU8.class);

//		ShowImages.showWindow(generator.gray,"QR Code");
//		BoofMiscOps.sleep(100000);

		decoder.process(pps,generator.gray);

		assertEquals(1,decoder.successes.size());
		QrCode found = decoder.getFound().get(0);

		// Check format info
		assertEquals(expected.error,found.error);
		assertTrue(expected.mask==found.mask);

		// check version info
		assertEquals(version,found.version);

		int numModules = QrCode.totalModules(version);

		// sanity check the position patterns
		for (int i = 0; i < 4; i++) {
			assertEquals(7*4,found.ppCorner.getSideLength(i),0.01);
			assertEquals(7*4,found.ppRight.getSideLength(i),0.01);
			assertEquals(7*4,found.ppDown.getSideLength(i),0.01);
		}
		assertTrue(found.ppCorner.get(0).distance(border,border) < 1e-4);
		assertTrue(found.ppRight.get(0).distance(border+(numModules-7)*4,border) < 1e-4);
		assertTrue(found.ppDown.get(0).distance(border,border+(numModules-7)*4) < 1e-4);

		// Check alignment patterns
		int alignment[] =  QrCode.VERSION_INFO[version].alignment;
		if( alignment.length == 0 ) {
			assertEquals(0,found.alignment.size);
		} else {
			assertEquals(found.alignment.size, alignment.length * alignment.length - 3);
			// TODO Check alignment pattern locations
		}

	}

	private FastQueue<PositionPatternNode> createPositionPatterns(QrCodeGeneratorImage generator) {
		FastQueue<PositionPatternNode> pps = new FastQueue<>(PositionPatternNode.class,true);

		pps.grow().square = generator.qr.ppCorner;
		pps.grow().square = generator.qr.ppRight;
		pps.grow().square = generator.qr.ppDown;

		for (int i = 0; i < pps.size; i++) {
			pps.get(i).grayThreshold = 125;
		}

		connect(pps.get(1),pps.get(0),3,1);
		connect(pps.get(2),pps.get(0),0,2);
		return pps;
	}

	@Test
	public void setPositionPatterns() {
		Polygon2D_F64 corner = new Polygon2D_F64(0,0, 2,0, 2,2, 0,2);
		Polygon2D_F64 right = new Polygon2D_F64(5,0, 7,0, 7,2, 5,2);
		Polygon2D_F64 bottom = new Polygon2D_F64(0,5, 2,5, 2,7, 0,7);

		PositionPatternNode n_corner = new PositionPatternNode();
		PositionPatternNode n_right = new PositionPatternNode();
		PositionPatternNode n_bottom = new PositionPatternNode();

		n_corner.square = corner;
		n_right.square = right;
		n_bottom.square = bottom;
		connect(n_right,n_corner,3,1);
		connect(n_bottom,n_corner,0,2);

		QrCode qr = new QrCode();
		QrCodeDecoderImage.setPositionPatterns(n_corner,1,2,qr);

		assertTrue(qr.ppCorner.get(0).distance(0,0) < UtilEjml.TEST_F64);
		assertTrue(qr.ppRight.get(0).distance(5,0)  < UtilEjml.TEST_F64);
		assertTrue(qr.ppDown.get(0).distance(0,5) < UtilEjml.TEST_F64);
	}

	private static void connect( PositionPatternNode a , PositionPatternNode b , int sideA , int sideB ) {
		SquareEdge e = new SquareEdge(a,b,3,1);
		a.edges[sideA] = b.edges[sideB] = e;
	}

	@Test
	public void rotateUntilAt() {
		Polygon2D_F64 square = new Polygon2D_F64(0,0, 2,0, 2,2, 0,2);
		assertTrue(square.isCCW());
		Polygon2D_F64 original = square.copy();

		QrCodeDecoderImage.rotateUntilAt(square,0,0);
		assertTrue(square.isIdentical(original, UtilEjml.TEST_F64));

		QrCodeDecoderImage.rotateUntilAt(square,1,0);
		assertTrue(square.isCCW());
		assertTrue(square.get(0).distance(2,0) < UtilEjml.TEST_F64);

		QrCodeDecoderImage.rotateUntilAt(square,0,1);
		assertTrue(square.isIdentical(original, UtilEjml.TEST_F64));
	}

	@Test
	public void computeBoundingBox() {
		QrCode qr = new QrCode();
		qr.ppCorner = new Polygon2D_F64(0,0, 1,0, 1,1, 0,1);
		qr.ppRight = new Polygon2D_F64(2,0, 3,0, 3,1, 2,1);
		qr.ppDown = new Polygon2D_F64(0,2, 1,2, 1,3, 0,3);

		QrCodeDecoderImage.computeBoundingBox(qr);

		assertTrue(qr.bounds.get(0).distance(0,0) < UtilEjml.TEST_F64);
		assertTrue(qr.bounds.get(1).distance(3,0) < UtilEjml.TEST_F64);
		assertTrue(qr.bounds.get(2).distance(3,3) < UtilEjml.TEST_F64);
		assertTrue(qr.bounds.get(3).distance(0,3) < UtilEjml.TEST_F64);
	}

	/**
	 * There was a bug where version 0 QR code was returned
	 */
	@Test
	public void extractVersionInfo_version0() {
		QrCodeDecoderImage<GrayU8> mock = new QrCodeDecoderImage(GrayU8.class){
			@Override
			int estimateVersionBySize(QrCode qr) {
				return 0;
			}

			@Override
			int decodeVersion() {
				return 8;
			}
		};

		QrCode qr = new QrCode();
		qr.version = 8;
		assertFalse(mock.extractVersionInfo(qr));
		assertEquals(-1,qr.version);
	}
}
