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

import boofcv.alg.fiducial.calib.squares.SquareEdge;
import boofcv.struct.image.GrayU8;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.UtilEjml;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestQrCodeDecoder {

	/**
	 * Runs through the entire algorithm using a rendered image
	 */
	@Test
	public void full_simple_ver2() {
		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(2,4);
		generator.generate("test message");

		FastQueue<PositionPatternNode> pps = new FastQueue<>(PositionPatternNode.class,true);

		pps.grow().square = generator.qr.ppCorner;
		pps.grow().square = generator.qr.ppRight;
		pps.grow().square = generator.qr.ppDown;

		connect(pps.get(1),pps.get(0),3,1);
		connect(pps.get(2),pps.get(0),0,2);

		QrCodeDecoder<GrayU8> decoder = new QrCodeDecoder<>(GrayU8.class);

//		ShowImages.showWindow(generator.gray,"QR Code");
//		BoofMiscOps.sleep(10000);

		decoder.process(pps,generator.gray);

		assertEquals(1,decoder.found.size);
		QrCode found = decoder.getFound().get(0);

		// sanity check the position patterns
		for (int i = 0; i < 4; i++) {
			assertEquals(7*4,found.ppCorner.getSideLength(i),0.01);
			assertEquals(7*4,found.ppRight.getSideLength(i),0.01);
			assertEquals(7*4,found.ppDown.getSideLength(i),0.01);
		}
		assertTrue(found.ppCorner.get(0).distance(0,0) < 1e-4);
		assertTrue(found.ppRight.get(0).distance((24-6)*4,0) < 1e-4);
		assertTrue(found.ppDown.get(0).distance(0,(24-6)*4) < 1e-4);

		// Check format info
		assertEquals(generator.qr.errorCorrection,found.errorCorrection);
		assertEquals(generator.qr.maskPattern,found.maskPattern);

		// check version info
		assertEquals(2,found.version);
	}

	@Ignore
	@Test
	public void full_simple_ver7() {
		fail("Implement");
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
		QrCodeDecoder.setPositionPatterns(n_corner,1,2,qr);

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

		QrCodeDecoder.rotateUntilAt(square,0,0);
		assertTrue(square.isIdentical(original, UtilEjml.TEST_F64));

		QrCodeDecoder.rotateUntilAt(square,1,0);
		assertTrue(square.isCCW());
		assertTrue(square.get(0).distance(2,0) < UtilEjml.TEST_F64);

		QrCodeDecoder.rotateUntilAt(square,0,1);
		assertTrue(square.isIdentical(original, UtilEjml.TEST_F64));
	}

	@Test
	public void computeBoundingBox() {
		QrCode qr = new QrCode();
		qr.ppCorner = new Polygon2D_F64(0,0, 1,0, 1,1, 0,1);
		qr.ppRight = new Polygon2D_F64(2,0, 3,0, 3,1, 2,1);
		qr.ppDown = new Polygon2D_F64(0,2, 1,2, 1,3, 0,3);

		QrCodeDecoder.computeBoundingBox(qr);

		assertTrue(qr.bounds.get(0).distance(0,0) < UtilEjml.TEST_F64);
		assertTrue(qr.bounds.get(1).distance(3,0) < UtilEjml.TEST_F64);
		assertTrue(qr.bounds.get(2).distance(3,3) < UtilEjml.TEST_F64);
		assertTrue(qr.bounds.get(3).distance(0,3) < UtilEjml.TEST_F64);
	}
}
