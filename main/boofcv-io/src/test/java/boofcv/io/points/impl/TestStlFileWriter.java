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

import boofcv.io.points.StlDataStructure;
import boofcv.struct.mesh.VertexMesh;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestStlFileWriter extends BoofStandardJUnit {
	/**
	 * Save a mesh then load it again and see if it gets the same results
	 */
	@Test void ascii_CompareToOriginal() throws IOException {
		StlDataStructure original = createTwoTriangle();

		var alg = new StlFileWriter();

		var output = new StringWriter();
		alg.writeAscii(original.toAccess(), original.name, output);

		var found = new StlDataStructure();
		var input = new BufferedReader(new StringReader(output.toString()));
		new StlFileReader().readAscii(input, found);

		checkEquivalent(original, found);
	}

	@Test void binary_CompareToOriginal() throws IOException {
		StlDataStructure original = createTwoTriangle();

		var alg = new StlFileWriter();

		var output = new ByteArrayOutputStream();
		alg.writeBinary(original.toAccess(), original.name, output);

		var found = new StlDataStructure();
		var input = new ByteArrayInputStream(output.toByteArray());
		new StlFileReader().readBinary(input, found);

		checkEquivalent(original, found);
	}

	/**
	 * See if it handles the case where there is more then 3 vertexes correctly
	 */
	@Test void ascii_HandleSquare() throws IOException {
		var alg = new StlFileWriter();
		var output = new StringWriter();
		alg.writeAscii(createSquare().toAccess(), "FooBar", output);

		var found = new StlDataStructure();
		var input = new BufferedReader(new StringReader(output.toString()));
		new StlFileReader().readAscii(input, found);

		var expectedNormal = new Vector3D_F64(0, 0, 1);
		assertEquals(2, found.size());
		assertEquals(0.0, expectedNormal.distance(found.normals.getTemp(0)), UtilEjml.TEST_F64);
		assertEquals(0.0, expectedNormal.distance(found.normals.getTemp(1)), UtilEjml.TEST_F64);
	}

	@Test void binary_HandleSquare() throws IOException {
		var alg = new StlFileWriter();
		var output = new ByteArrayOutputStream();
		alg.writeBinary(createSquare().toAccess(), "FooBar", output);

		var found = new StlDataStructure();
		var input = new ByteArrayInputStream(output.toByteArray());
		new StlFileReader().readBinary(input, found);

		var expectedNormal = new Vector3D_F64(0, 0, 1);
		assertEquals(2, found.size());
		assertEquals(0.0, expectedNormal.distance(found.normals.getTemp(0)), UtilEjml.TEST_F64);
		assertEquals(0.0, expectedNormal.distance(found.normals.getTemp(1)), UtilEjml.TEST_F64);
	}

	/**
	 * 3 points is a special case where it does a cross product
	 */
	@Test void computeNormal_Points3() {
		var vertexes = new DogArray<>(Point3D_F64::new);
		vertexes.grow().setTo(0, 0, 0);
		vertexes.grow().setTo(1, 0, 0);
		vertexes.grow().setTo(0, 1, 0);

		var alg = new StlFileWriter();
		assertTrue(alg.computeNormal(vertexes, 0));
		assertEquals(0.0, alg.normal.distance(0,0,1), UtilEjml.TEST_F64);

		// reverse the order
		vertexes.reset();
		vertexes.grow().setTo(0, 1, 0);
		vertexes.grow().setTo(1, 0, 0);
		vertexes.grow().setTo(0, 0, 0);
		assertTrue(alg.computeNormal(vertexes, 0));
		assertEquals(0.0, alg.normal.distance(0,0,-1), UtilEjml.TEST_F64);
	}

	/**
	 * 4 and more it fits a plane
	 */
	@Test void computeNormal_Points4() {
		var vertexes = new DogArray<>(Point3D_F64::new);
		vertexes.grow().setTo(0, 0, 0);
		vertexes.grow().setTo(1, 0, 0);
		vertexes.grow().setTo(1, 1, 0);
		vertexes.grow().setTo(0, 1, 0);

		var alg = new StlFileWriter();
		assertTrue(alg.computeNormal(vertexes, 0));
		assertEquals(0.0, alg.normal.distance(0,0,1), UtilEjml.TEST_F64);

		// reverse the order
		vertexes.reset();
		vertexes.grow().setTo(0, 1, 0);
		vertexes.grow().setTo(1, 1, 0);
		vertexes.grow().setTo(1, 0, 0);
		vertexes.grow().setTo(0, 0, 0);
		assertTrue(alg.computeNormal(vertexes, 0));
		assertEquals(0.0, alg.normal.distance(0,0,-1), UtilEjml.TEST_F64);
	}

	/**
	 * Checks to see if the two data structures are equivalent. Does not check to see if they are identical
	 * since some information (like two points being identical) will be lost when saving and reading
	 */
	private static void checkEquivalent( StlDataStructure original, StlDataStructure found ) {
		var orgN = new Vector3D_F64();
		var fndN = new Vector3D_F64();
		var orgF = new DogArray<>(Point3D_F64::new);
		var fndF = new DogArray<>(Point3D_F64::new);

		assertEquals(original.name, found.name);
		assertEquals(original.size(), found.size());
		for (int facetIdx = 0; facetIdx < original.size(); facetIdx++) {
			original.getFacet(facetIdx, orgN, orgF);
			found.getFacet(facetIdx, fndN, fndF);

			assertEquals(0.0, orgN.distance(fndN), UtilEjml.TEST_F64);
			for (int i = 0; i < 3; i++) {
				assertEquals(0.0, orgF.get(i).distance(fndF.get(i)), UtilEjml.TEST_F64);
			}
		}
	}

	/**
	 * STL data structure with two facets. They both reference the same points but in different order.
	 */
	private static StlDataStructure createTwoTriangle() {
		var mesh = new StlDataStructure();
		mesh.name = "FooBar";
		mesh.addFacet(
				new Point3D_F64(0, 0, 0),
				new Point3D_F64(1, 0, 0),
				new Point3D_F64(0, 1, 0),
				new Vector3D_F64(0, 0, 1));
		mesh.addFacet(2, 1, 0, new Vector3D_F64(0, 0, -1));
		return mesh;
	}

	private static VertexMesh createSquare() {
		var mesh = new VertexMesh();
		mesh.vertexes.append(0, 0, 0);
		mesh.vertexes.append(1, 0, 0);
		mesh.vertexes.append(1, 1, 0);
		mesh.vertexes.append(0, 1, 0);
		mesh.faceVertexes.addAll(new int[]{0, 1, 2, 3}, 0 , 4);
		mesh.faceOffsets.add(4);

		return mesh;
	}
}
