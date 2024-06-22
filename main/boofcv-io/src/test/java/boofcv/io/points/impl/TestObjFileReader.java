/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.io.points.impl;

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

public class TestObjFileReader extends BoofStandardJUnit {
	/**
	 * Simple file with a comment and on face
	 */
	@Test void case0() throws IOException {
		String text = """
				# Simple Wavefront file
				v 0.0 0.0 0.0
				v 0.0 1.0 0.0
				v 1.0 0.0 0.0
				f 1 2 3
				""";

		var vertexes = new DogArray<>(Point3D_F64::new, Point3D_F64::zero);

		var reader = new DummyReader() {
			@Override protected void addVertex( double x, double y, double z ) {
				vertexes.grow().setTo(x, y, z);
			}

			@Override protected void addVertexNormal( double x, double y, double z ) {}

			@Override protected void addVertexTexture( double x, double y ) {}

			@Override protected void addFace( DogArray_I32 vertexes, int vertexCount ) {
				assertEquals(3, vertexes.size);
				assertTrue(vertexes.isEquals(0, 1, 2));
			}
		};
		reader.parse(new BufferedReader(new StringReader(text)));

		assertEquals(3, vertexes.size);
		assertEquals(0.0, vertexes.get(0).distance(0, 0, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, vertexes.get(1).distance(0, 1, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, vertexes.get(2).distance(1, 0, 0), UtilEjml.TEST_F64);
	}

	/**
	 * Simple file with vertex, texture, and normals
	 */
	@Test void case1() throws IOException {
		String text = """
				v 0.0 0.0 0.0
				v 0.0 1.0 0.0
				v 1.0 0.0 0.0
				vt 0.0 0.0
				vt 0.1 1.0
				vt 1.1 0.1
				vn 0.0 0.0 -1.0
				vn 0.0 -1.0 0.0
				vn -1.0 0.0 0.0
				f 1/1/1 2/2/2 3/3/3
				""";

		var vertexes = new DogArray<>(Point3D_F64::new, Point3D_F64::zero);
		var normals = new DogArray<>(Point3D_F64::new, Point3D_F64::zero);
		var textures = new DogArray<>(Point2D_F64::new, Point2D_F64::zero);

		var reader = new DummyReader() {
			@Override protected void addVertex( double x, double y, double z ) {
				vertexes.grow().setTo(x, y, z);
			}

			@Override protected void addVertexNormal( double x, double y, double z ) {normals.grow().setTo(x, y, z);}

			@Override protected void addVertexTexture( double x, double y ) {textures.grow().setTo(x, y);}

			@Override protected void addFace( DogArray_I32 indexes, int vertexCount ) {
				assertEquals(3, vertexCount);
				assertEquals(vertexCount*3, indexes.size);
				assertTrue(indexes.isEquals(0, 0, 0, 1, 1, 1, 2, 2, 2));
			}
		};
		reader.parse(new BufferedReader(new StringReader(text)));

		assertEquals(3, vertexes.size);
		assertEquals(0.0, vertexes.get(0).distance(0, 0, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, vertexes.get(1).distance(0, 1, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, vertexes.get(2).distance(1, 0, 0), UtilEjml.TEST_F64);
		assertEquals(3, normals.size);
		assertEquals(0.0, normals.get(0).distance(0, 0, -1), UtilEjml.TEST_F64);
		assertEquals(0.0, normals.get(1).distance(0, -1, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, normals.get(2).distance(-1, 0, 0), UtilEjml.TEST_F64);
		assertEquals(3, textures.size);
		assertEquals(0.0, textures.get(0).distance(0, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, textures.get(1).distance(0.1, 1), UtilEjml.TEST_F64);
		assertEquals(0.0, textures.get(2).distance(1.1, 0.1), UtilEjml.TEST_F64);
	}

	/**
	 * See if negative indexes are handled correct. Add two faces. Negative to be relative to current number
	 * of read in vertexes
	 */
	@Test void faceWithNegativeVertexes() throws IOException {
		String text = """
				# Simple Wavefront file
				v 0.0 0.0 0.0
				v 0.0 1.0 0.0
				v 1.0 0.0 0.0
				f -1 -2 -3
				v 0.0 0.0 0.0
				v 0.0 1.0 0.0
				v 1.0 0.0 0.0
				f -1 -2 -3
				""";

		var reader = new DummyReader() {
			int count = 0;

			@Override protected void addVertex( double x, double y, double z ) {}

			@Override protected void addVertexNormal( double x, double y, double z ) {}

			@Override protected void addVertexTexture( double x, double y ) {}

			@Override protected void addFace( DogArray_I32 vertexes, int vertexCount ) {
				assertEquals(3, vertexes.size);
				if (count == 0) {
					assertTrue(vertexes.isEquals(2, 1, 0));
				} else {
					assertTrue(vertexes.isEquals(5, 4, 3));
				}
				count++;
			}
		};

		reader.parse(new BufferedReader(new StringReader(text)));
		assertEquals(2, reader.count);
	}

	/** Does it handle the multi-line character correctly */
	@Test void multiLine() throws IOException {
		String text = """
				v 0.0 0.0 0.0
				v 0.0 1.0 0.0
				v 1.0 0.0 0.0
				f -1 -2 -3 \
				1 2 3
				v 0.0 0.0 0.0
				""";

		var reader = new DummyReader() {
			int vertexCount = 0;

			@Override protected void addVertex( double x, double y, double z ) {vertexCount++;}

			@Override protected void addVertexNormal( double x, double y, double z ) {}

			@Override protected void addVertexTexture( double x, double y ) {}

			@Override protected void addFace( DogArray_I32 vertexes, int vertexCount ) {
				assertEquals(6, vertexes.size);
				assertTrue(vertexes.isEquals(2, 1, 0, 0, 1, 2));
			}
		};

		reader.parse(new BufferedReader(new StringReader(text)));
		assertEquals(4, reader.vertexCount);
	}

	@Test void ensureIndex() {
		var reader = new DummyReader() {
			@Override protected void addVertex( double x, double y, double z ) {}

			@Override protected void addVertexNormal( double x, double y, double z ) {}

			@Override protected void addVertexTexture( double x, double y ) {}
		};

		// Vertexes are stored in 1-index but need to make sure it's converted to 0-index
		assertEquals(0, reader.ensureIndex(1));
		assertEquals(4, reader.ensureIndex(5));
		assertEquals(8, reader.ensureIndex(9));

		// Negative numbers count from end of array
		reader.vertexCount = 12;
		assertEquals(11, reader.ensureIndex(-1));
		assertEquals(7, reader.ensureIndex(-5));
	}

	static abstract class DummyReader extends ObjFileReader {
		@Override protected void addPoint( int vertex ) {
			fail("there are no points");
		}

		@Override protected void addLine( DogArray_I32 vertexes ) {
			fail("there are no lines");
		}

		@Override protected void addFace( DogArray_I32 vertexes, int vertexCount ) {
			fail("there are no faces");
		}
	}
}
