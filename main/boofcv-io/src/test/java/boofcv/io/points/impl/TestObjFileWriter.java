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
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestObjFileWriter extends BoofStandardJUnit {
	/**
	 * Create a OBJ file then see if it can be read
	 */
	@Test void encodeThenDecode() throws IOException {
		var output = new StringWriter();
		var alg = new ObjFileWriter(output);
		alg.addVertex(1, 0, 0);
		alg.addVertex(0, 1, 0);
		alg.addVertex(0, 0, 1);
		alg.addTextureVertex(1, 0);
		alg.addTextureVertex(0, 1);
		alg.addTextureVertex(1, 1);
		alg.addFace(DogArray_I32.array(0, 1, 2), 1);
		alg.addComment("Foo Bar");
		alg.addVertex(2, 0, 0);
		alg.addVertex(0, 2, 0);
		alg.addVertex(0, 0, 2);
		alg.addTextureVertex(2, 0);
		alg.addTextureVertex(0, 2);
		alg.addTextureVertex(2, 2);
		alg.addFace(null, 0);

		String text = output.toString();
		var reader = new TestObjFileReader.DummyReader() {
			int countVertex = 0;
			int countTexture = 0;
			int countFaces = 0;

			@Override protected void addVertex( double x, double y, double z ) {
				double expected = countVertex < 3 ? 1.0 : 2.0;
				double found = switch (countVertex%3) {
					case 0 -> x;
					case 1 -> y;
					case 2 -> z;
					default -> throw new RuntimeException();
				};
				assertEquals(expected, found);
				countVertex++;
			}

			@Override protected void addVertexNormal( double x, double y, double z ) {}

			@Override protected void addVertexTexture( double x, double y ) {
				double expected = countTexture < 3 ? 1.0 : 2.0;
				double found = switch (countTexture%3) {
					case 0 -> x;
					case 1 -> y;
					case 2 -> expected;
					default -> throw new RuntimeException();
				};
				assertEquals(expected, found);
				countTexture++;
			}

			@Override protected void addFace( DogArray_I32 vertexes, int vertexCount ) {
				switch (countVertex) {
					case 0 -> assertTrue(vertexes.isEquals(0, 1, 2));
					case 1 -> assertTrue(vertexes.isEquals(3, 4, 5));
				}
				countFaces++;
			}
		};
		reader.parse(new BufferedReader(new StringReader(text)));
		assertEquals(6, reader.countVertex);
		assertEquals(2, reader.countFaces);
	}
}
