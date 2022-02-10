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

package boofcv.alg.fiducial.aztec;

import georegression.struct.shapes.Polygon2D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestAztecPyramid {
	@Test void alignCorners() {
		var pyramid = new AztecPyramid();
		// sanity check for when there's nothing to align
		pyramid.alignCorners();

		AztecPyramid.Layer layerA = pyramid.layers.grow();
		layerA.square.setTo(new Polygon2D_F64(0,0, 5,0, 5,5, 0,5));
		pyramid.alignCorners();

		// With two layers it should rotate the second
		AztecPyramid.Layer layerB = pyramid.layers.grow();
		layerB.square.setTo(new Polygon2D_F64(5,0, 5,5, 0,5, 0,0));
		pyramid.alignCorners();

		// make sure B was aligned to A
		assertTrue(layerB.square.get(0).isIdentical(0, 0, 0));
		assertTrue(layerA.isIdentical(layerB));
	}

	@Test void setTo() {
		var src = new AztecPyramid();
		var dst = new AztecPyramid();

		assertTrue(src.isIdentical(dst));

		AztecPyramid.Layer layerA = src.layers.grow();
		layerA.center.setTo(2,3);

		assertFalse(src.isIdentical(dst));
		dst.setTo(src);
		assertTrue(src.isIdentical(dst));

		AztecPyramid.Layer layerB = src.layers.grow();
		layerB.center.setTo(4,5);

		assertFalse(src.isIdentical(dst));
		dst.setTo(src);
		assertTrue(src.isIdentical(dst));
	}

	@Test void Layer_setTo() {
		var src = new AztecPyramid.Layer();
		src.center.setTo(5, 6);
		src.threshold = 3;
		src.square.get(2).setTo(9, 10);

		var dst = new AztecPyramid.Layer();
		assertFalse(dst.isIdentical(src)); // test negative first

		dst.setTo(src);

		// sanity check for is true
		assertEquals(0.0, dst.center.distance(5, 6), UtilEjml.TEST_F64);
		assertTrue(dst.isIdentical(src));
	}
}