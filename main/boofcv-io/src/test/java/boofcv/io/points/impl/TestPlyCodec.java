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

import boofcv.alg.cloud.PointCloudReader;
import boofcv.alg.cloud.PointCloudWriter;
import boofcv.struct.Point3dRgbI_F64;
import boofcv.struct.mesh.VertexMesh;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestPlyCodec extends BoofStandardJUnit {
	@Test void encode_decode_3D_ascii() throws IOException {
		var expected = new ArrayList<Point3D_F64>();
		for (int i = 0; i < 10; i++) {
			expected.add(new Point3D_F64(i*123.45, i - 1.01, i + 2.34));
		}

		var found = new DogArray<>(Point3D_F64::new);

		var output = new StringWriter();
		PlyCodec.saveCloudAscii(PointCloudReader.wrapF64(expected), false, output);
		var input = new ByteArrayInputStream(output.toString().getBytes(UTF_8));
		PlyCodec.readCloud(input, PointCloudWriter.wrapF64(found));

		assertEquals(expected.size(), found.size);
		for (int i = 0; i < found.size; i++) {
			assertEquals(0.0, found.get(i).distance(expected.get(i)), UtilEjml.TEST_F64);
		}
	}

	@Test void encode_decode_3DRGB_ascii() throws IOException {
		List<Point3dRgbI_F64> expected = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			int r = (10*i) & 0xFF;
			int g = (28*i) & 0xFF;
			int b = (58*i) & 0xFF;

			int rgb = r << 16 | g << 16 | b;

			expected.add(new Point3dRgbI_F64(i*123.45, i - 1.01, i + 2.34, rgb));
		}

		DogArray<Point3dRgbI_F64> found = new DogArray<>(Point3dRgbI_F64::new);

		var output = new StringWriter();
		PlyCodec.saveCloudAscii(PointCloudReader.wrapF64RGB(expected), true, output);
		var input = new ByteArrayInputStream(output.toString().getBytes(UTF_8));
		PlyCodec.readCloud(input, PointCloudWriter.wrapF64RGB(found));

		assertEquals(expected.size(), found.size);
		for (int i = 0; i < found.size; i++) {
			assertEquals(0.0, found.get(i).distance(expected.get(i)), UtilEjml.TEST_F64);
		}
	}

	@Test void encode_decode_3D_binary() throws IOException {
		List<Point3D_F64> expected = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			expected.add(new Point3D_F64(i*123.45, i - 1.01, i + 2.34));
		}

		for (boolean asFloat : new boolean[]{true, false}) {
			DogArray<Point3D_F64> found = new DogArray<>(Point3D_F64::new);

			var output = new ByteArrayOutputStream();
			PlyCodec.saveCloudBinary(PointCloudReader.wrapF64(expected), ByteOrder.BIG_ENDIAN, false, asFloat, output);
			var input = new ByteArrayInputStream(output.toByteArray());
			PlyCodec.readCloud(input, PointCloudWriter.wrapF64(found));

			assertEquals(expected.size(), found.size);
			double tol = asFloat ? UtilEjml.TEST_F32 : UtilEjml.TEST_F64;
			for (int i = 0; i < found.size; i++) {
				assertEquals(0.0, found.get(i).distance(expected.get(i)), tol);
			}
		}
	}

	@Test void encode_decode_3DRGB_binary() throws IOException {
		List<Point3dRgbI_F64> expected = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			int r = (10*i) & 0xFF;
			int g = (28*i) & 0xFF;
			int b = (58*i) & 0xFF;

			int rgb = r << 16 | g << 16 | b;

			expected.add(new Point3dRgbI_F64(i*123.45, i - 1.01, i + 2.34, rgb));
		}

		for (boolean asFloat : new boolean[]{true, false}) {
			DogArray<Point3dRgbI_F64> found = new DogArray<>(Point3dRgbI_F64::new);

			var output = new ByteArrayOutputStream();
			PlyCodec.saveCloudBinary(PointCloudReader.wrapF64RGB(expected), ByteOrder.BIG_ENDIAN, false, asFloat, output);
			var input = new ByteArrayInputStream(output.toByteArray());
			PlyCodec.readCloud(input, PointCloudWriter.wrapF64RGB(found));

			assertEquals(expected.size(), found.size);
			double tol = asFloat ? UtilEjml.TEST_F32 : UtilEjml.TEST_F64;
			for (int i = 0; i < found.size; i++) {
				assertEquals(0.0, found.get(i).distance(expected.get(i)), tol);
			}
		}
	}

	@Test void encode_decode_mesh_binary() throws IOException {
		for (var endian : new ByteOrder[]{ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN}) {
			var mesh = new VertexMesh();
			mesh.textureName = "foo";
			mesh.faceOffsets.add(0);
			int numVertexes = 10;
			for (int i = 0; i < numVertexes; i++) {
				mesh.vertexes.append(i, 2, 3);
				mesh.normals.append(i, 2, 3);

				// bound indexes to ensure they are in the valid range
				mesh.faceVertexes.add((i*3)%numVertexes);
				mesh.faceVertexes.add((i*3 + 1)%numVertexes);
				mesh.faceVertexes.add((i*3 + 2)%numVertexes);
				mesh.faceOffsets.add(mesh.faceVertexes.size);

				for (int idxPoly = 0; idxPoly < 3; idxPoly++) {
					mesh.texture.append(1, idxPoly);
				}
			}
			var colors = new DogArray_I32();
			for (int i = 0; i < mesh.vertexes.size(); i++) {
				colors.add(i + 5);
			}

			var output = new ByteArrayOutputStream();
			PlyCodec.saveMeshBinary(mesh, colors, endian, true, output);

			var foundMesh = new VertexMesh();
			var foundColors = new DogArray_I32();

			var input = new ByteArrayInputStream(output.toByteArray());
			PlyCodec.readMesh(input, foundMesh, foundColors);

			assertEquals(mesh.vertexes.size(), mesh.vertexes.size());
			assertEquals(mesh.normals.size(), mesh.normals.size());
			assertEquals(mesh.texture.size(), mesh.texture.size());
			assertTrue(mesh.faceVertexes.isEquals(foundMesh.faceVertexes));
			assertTrue(mesh.faceOffsets.isEquals(foundMesh.faceOffsets));
			assertTrue(colors.isEquals(foundColors));

			for (int i = 0; i < mesh.vertexes.size(); i++) {
				Point3D_F64 expected = mesh.vertexes.getTemp(i);
				Point3D_F64 found = foundMesh.vertexes.getTemp(i);
				assertEquals(0.0, expected.distance(found));
				assertTrue(mesh.normals.getTemp(i).isIdentical(foundMesh.normals.getTemp(i), 1e-4f));
			}

			for (int i = 0; i < mesh.vertexes.size(); i++) {
				assertEquals(mesh.faceVertexes.get(i), foundMesh.faceVertexes.get(i));
			}

			for (int i = 0; i < mesh.texture.size(); i++) {
				assertEquals(0.0f, mesh.texture.getTemp(i).distance(mesh.texture.getTemp(i)), UtilEjml.TEST_F32);
			}
		}
	}

	/**
	 * Makes sure it saves and can read the texture map image name
	 */
	@Test void textureName() throws IOException {
		var mesh = new VertexMesh();
		mesh.textureName = "foo";

		var output = new ByteArrayOutputStream();
		PlyCodec.saveMeshBinary(mesh, null, ByteOrder.BIG_ENDIAN, true, output);

		var input = new ByteArrayInputStream(output.toByteArray());
		var foundMesh = new VertexMesh();
		PlyCodec.readMesh(input, foundMesh, new DogArray_I32());

		assertEquals("foo", foundMesh.textureName);
	}
}
