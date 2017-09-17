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
import georegression.struct.shapes.Polygon2D_F64;
import org.ejml.UtilEjml;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestQrCodeDecoder {

	@Test
	public void setPositionPatterns() {
		Polygon2D_F64 corner = new Polygon2D_F64(0,0, 2,0, 2,2, 0,2);
		Polygon2D_F64 right = new Polygon2D_F64(5,0, 7,0, 7,2, 5,2);
		Polygon2D_F64 bottom = new Polygon2D_F64(0,5, 2,5, 2,7, 0,7);

		PositionPatternNode n_corner = new PositionPatternNode();
		PositionPatternNode n_right = new PositionPatternNode();
		PositionPatternNode n_bottom = new PositionPatternNode();

		SquareEdge e_right = new SquareEdge();
		e_right.a = n_right; e_right.sideA = 3;
		e_right.b = n_corner; e_right.sideB = 1;

		SquareEdge e_bottom = new SquareEdge();
		e_bottom.a = n_bottom; e_bottom.sideA = 0;
		e_bottom.b = n_corner; e_bottom.sideB = 2;

		n_corner.square = corner;
		n_corner.edges[e_right.sideB] = e_right;
		n_corner.edges[e_bottom.sideB] = e_bottom;

		n_right.square = right;
		n_right.edges[e_right.sideA] = e_right;

		n_bottom.square = bottom;
		n_bottom.edges[e_bottom.sideA] = e_bottom;

		QrCode qr = new QrCode();
		QrCodeDecoder.setPositionPatterns(n_corner,e_right.sideB,e_bottom.sideB,qr);

		assertTrue(qr.ppCorner.get(0).distance(0,0) < UtilEjml.TEST_F64);
		assertTrue(qr.ppRight.get(0).distance(5,0)  < UtilEjml.TEST_F64);
		assertTrue(qr.ppDown.get(0).distance(0,5) < UtilEjml.TEST_F64);

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
