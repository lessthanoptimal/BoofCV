/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

import boofcv.io.points.StlDataStructure;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestStlFileReader extends BoofStandardJUnit {
	@Test void ascii() throws IOException {
		String text = """
				solid cube_corner
				  facet normal 0.0 -1.0 0.0
					outer loop
					  vertex 0.0 0.0 0.0
					  vertex 1.0 0.0 0.0
					  vertex 0.0 0.0 1.0
					endloop
				  endfacet
				  facet normal 0.0 0.0 -1.0
					outer loop
					  vertex 0.0 0.0 0.0
					  vertex 0.0 1.0 0.0
					  vertex 1.0 0.0 0.0
					endloop
				  endfacet
				endsolid
				""";

		var alg = new StlFileReader();
		var found = new StlDataStructure();
		alg.readAscii(new BufferedReader(new StringReader(text)), found);

		checkResults(found);
	}

	@Test void binary() throws IOException {
		var labelBlock = new byte[80];

		byte[] textBytes = "cube_corner".getBytes(StandardCharsets.UTF_8);
		System.arraycopy(textBytes, 0, labelBlock, 0, textBytes.length);

		var byteStream = new ByteArrayOutputStream();
		var out = new DataOutputStream(byteStream);
		out.write(labelBlock);
		out.writeInt(2);
		write(out, 0.0f, -1.0f, 0.0f);
		write(out, 0.0f, 0.0f, 0.0f);
		write(out, 1.0f, 0.0f, 0.0f);
		write(out, 0.0f, 0.0f, 1.0f);

		write(out, 0.0f, 0.0f, -1.0f);
		write(out, 0.0f, 0.0f, 0.0f);
		write(out, 0.0f, 1.0f, 0.0f);
		write(out, 1.0f, 0.0f, 0.0f);

		out.writeShort(0);
		out.flush();

		var alg = new StlFileReader();
		var found = new StlDataStructure();
		alg.readBinary(new ByteArrayInputStream(byteStream.toByteArray()), found);

		checkResults(found);
	}

	private static void checkResults( StlDataStructure found ) {
		assertEquals("cube_corner", found.name);
		assertEquals(2, found.facetCount());

		var n = new Vector3D_F64();
		var vertexes = new DogArray<>(Point3D_F64::new);

		found.getFacet(0, n, vertexes);
		assertTrue(n.isIdentical(0.0, -1.0, 0.0));
		assertEquals(3, vertexes.size);
		assertTrue(vertexes.get(0).isIdentical(0.0, 0.0, 0.0));
		assertTrue(vertexes.get(1).isIdentical(1.0, 0.0, 0.0));
		assertTrue(vertexes.get(2).isIdentical(0.0, 0.0, 1.0));

		found.getFacet(1, n, vertexes);
		assertTrue(n.isIdentical(0.0, 0.0, -1.0));
		assertEquals(3, vertexes.size);
		assertTrue(vertexes.get(0).isIdentical(0.0, 0.0, 0.0));
		assertTrue(vertexes.get(1).isIdentical(0.0, 1.0, 0.0));
		assertTrue(vertexes.get(2).isIdentical(1.0, 0.0, 0.0));
	}

	void write( DataOutputStream out, float a, float b, float c ) throws IOException {
		out.writeFloat(a);
		out.writeFloat(b);
		out.writeFloat(c);
	}
}