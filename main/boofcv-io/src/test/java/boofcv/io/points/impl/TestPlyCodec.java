/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.*;

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
		for (int numTextures : new int[]{1, 3}) {
			for (var endian : new ByteOrder[]{ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN}) {
				for (boolean floatType : new boolean[]{true, false}) {
					VertexMesh mesh = createMesh(numTextures);
					DogArray_I32 colors = createColors(mesh);

					var output = new ByteArrayOutputStream();
					PlyCodec.saveMeshBinary(mesh, colors, endian, floatType, output);

					// Make sure it encoded it as the correct type
					var header = output.toString(UTF_8);
					assertTrue(header.contains("property list uchar " + (floatType ? "float" : "double") + " texcoord"));

					var foundMesh = new VertexMesh();
					PlyCodec.readMesh(new ByteArrayInputStream(output.toByteArray()), foundMesh);
					checkSameMesh(mesh, colors, foundMesh);
				}
			}
		}
	}

	@Test void encode_decode_mesh_ascii() throws IOException {
		for (int numTextures : new int[]{1, 3}) {
			VertexMesh mesh = createMesh(numTextures);
			DogArray_I32 colors = createColors(mesh);

			var output = new StringWriter();
			PlyCodec.saveMeshAscii(mesh, colors, output);

			var foundMesh = new VertexMesh();
			PlyCodec.readMesh(new ByteArrayInputStream(output.toString().getBytes(UTF_8)), foundMesh);
			checkSameMesh(mesh, colors, foundMesh);
		}
	}

	/// Creates a textured triangle mesh. If there's more than one texture, each face specifies its texture
	private VertexMesh createMesh( int numTextures ) {
		var mesh = new VertexMesh();
		for (int i = 0; i < numTextures; i++) {
			mesh.textureNames.add("foo" + i + ".jpg");
		}
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

			if (numTextures > 1)
				mesh.faceTextures.add(i%numTextures);
		}
		return mesh;
	}

	private DogArray_I32 createColors( VertexMesh mesh ) {
		var colors = new DogArray_I32();
		for (int i = 0; i < mesh.vertexes.size(); i++) {
			colors.add(i + 5);
		}
		return colors;
	}

	private void checkSameMesh( VertexMesh expected, DogArray_I32 colors, VertexMesh found ) {
		assertEquals(expected.vertexes.size(), found.vertexes.size());
		assertEquals(expected.normals.size(), found.normals.size());
		assertEquals(expected.texture.size(), found.texture.size());
		assertTrue(expected.faceVertexes.isEquals(found.faceVertexes));
		assertTrue(expected.faceOffsets.isEquals(found.faceOffsets));
		assertTrue(colors.isEquals(found.rgb));
		assertEquals(expected.textureNames, found.textureNames);
		assertTrue(expected.faceTextures.isEquals(found.faceTextures));

		for (int i = 0; i < expected.vertexes.size(); i++) {
			assertEquals(0.0, expected.vertexes.getTemp(i).distance(found.vertexes.getTemp(i)));
			assertTrue(expected.normals.getTemp(i).isIdentical(found.normals.getTemp(i), 1e-4f));
		}

		for (int i = 0; i < expected.texture.size(); i++) {
			assertEquals(0.0f, expected.texture.getTemp(i).distance(found.texture.getTemp(i)), UtilEjml.TEST_F32);
		}
	}

	/// If per-face textures are not specified then every face uses the first texture
	@Test void wrapMeshForWriting_faceTexture() {
		VertexMesh mesh = createMesh(2);
		mesh.faceTextures.reset();
		assertEquals(0, PlyCodec.wrapMeshForWriting(mesh, null).getFaceTexture(1));
	}

	/// Makes sure it saves and can read the texture map image name
	@Test void textureName() throws IOException {
		var mesh = new VertexMesh();
		mesh.textureNames.add("foo");

		var output = new ByteArrayOutputStream();
		PlyCodec.saveMeshBinary(mesh, null, ByteOrder.BIG_ENDIAN, true, output);

		var input = new ByteArrayInputStream(output.toByteArray());
		var foundMesh = new VertexMesh();
		PlyCodec.readMesh(input, foundMesh);

		assertEquals(List.of("foo"), foundMesh.textureNames);
	}

	/// Per-face texture indexes are only saved if there are texture coordinates and more than one texture
	@Test void isFaceTextures() {
		assertFalse(PlyCodec.isFaceTextures(true, List.of()));
		assertFalse(PlyCodec.isFaceTextures(true, List.of("a")));
		assertTrue(PlyCodec.isFaceTextures(true, List.of("a", "b")));
		assertFalse(PlyCodec.isFaceTextures(false, List.of("a", "b")));
	}

	/// Metashape adds a scalar texnumber to each face. The faces after it still line up
	@Test void faceScalarProperty_binary() throws IOException {
		var bytes = new ByteArrayOutputStream();
		bytes.write(("""
				ply
				format binary_little_endian 1.0
				element vertex 4
				property float x
				property float y
				property float z
				element face 2
				property list uchar int vertex_indices
				property list uchar float texcoord
				property int texnumber
				end_header
				""").getBytes(UTF_8));
		var bb = java.nio.ByteBuffer.allocate(4*3*4 + 2*(1 + 3*4 + 1 + 6*4 + 4)).order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < 4; i++) {
			bb.putFloat(i).putFloat(i + 10).putFloat(i + 20);
		}
		int[][] faces = {{0, 1, 2}, {1, 3, 2}};
		for (int idxFace = 0; idxFace < faces.length; idxFace++) {
			int[] face = faces[idxFace];
			bb.put((byte)3).putInt(face[0]).putInt(face[1]).putInt(face[2]);
			bb.put((byte)6);
			for (int i = 0; i < 6; i++)
				bb.putFloat(0.1f*i);
			bb.putInt(idxFace);
		}
		bytes.write(bb.array());

		VertexMesh mesh = checkTwoFaces(new ByteArrayInputStream(bytes.toByteArray()), faces);
		assertTrue(mesh.faceTextures.isEquals(0, 1));
	}

	@Test void faceScalarProperty_ascii() throws IOException {
		String text = """
				ply
				format ascii 1.0
				element vertex 4
				property float x
				property float y
				property float z
				element face 2
				property list uchar int vertex_indices
				property int texnumber
				end_header
				0 10 20
				1 11 21
				2 12 22
				3 13 23
				3 0 1 2 0
				3 1 3 2 1
				""";
		VertexMesh mesh = checkTwoFaces(new ByteArrayInputStream(text.getBytes(UTF_8)), new int[][]{{0, 1, 2}, {1, 3, 2}});
		assertTrue(mesh.faceTextures.isEquals(0, 1));
	}

	/// Metashape lists every texture file in a comment and adds texnumber to faces
	@Test void readHeader_textures() throws IOException {
		String text = """
				ply
				format ascii 1.0
				comment TextureFile a.jpg
				comment TextureFile b.jpg
				element vertex 4
				property float x
				property float y
				property float z
				element face 2
				property list uchar int vertex_indices
				property list uchar float texcoord
				property int texnumber
				end_header
				""";
		var header = new PlyCodec.Header();
		PlyCodec.readHeader(new ByteArrayInputStream(text.getBytes(UTF_8)), header);

		assertEquals(List.of("a.jpg", "b.jpg"), header.textureNames);
		assertEquals(3, header.properties.size());
		assertEquals("texnumber", header.properties.get(2).label);
	}

	@Test void checkVertexIndex() throws IOException {
		PlyCodec.checkVertexIndex(0, 3);
		PlyCodec.checkVertexIndex(2, 3);
		assertThrows(IOException.class, () -> PlyCodec.checkVertexIndex(-1, 3));
		assertThrows(IOException.class, () -> PlyCodec.checkVertexIndex(3, 3));
	}

	/// An element the reader does not use, placed between the vertexes and faces, an unknown list on the faces,
	/// and the sized type names. All are skipped and the faces still line up
	@Test void unknownElementsAndLists_binary() throws IOException {
		var bytes = new ByteArrayOutputStream();
		bytes.write(("""
				ply
				format binary_little_endian 1.0
				element vertex 4
				property float32 x
				property float32 y
				property float32 z
				property uint8 quality
				element material 2
				property list uint8 int32 ids
				property float64 shininess
				element face 2
				property list uint8 int32 vertex_indices
				property list uint8 uint16 flags
				property int32 texnumber
				end_header
				""").getBytes(UTF_8));
		var bb = java.nio.ByteBuffer.allocate(1000).order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < 4; i++) {
			bb.putFloat(i).putFloat(i + 10).putFloat(i + 20).put((byte)9);
		}
		// material: a list of 3 ints then a double, and an empty list then a double
		bb.put((byte)3).putInt(1).putInt(2).putInt(3).putDouble(0.5);
		bb.put((byte)0).putDouble(0.25);
		int[][] faces = {{0, 1, 2}, {1, 3, 2}};
		for (int idxFace = 0; idxFace < faces.length; idxFace++) {
			int[] face = faces[idxFace];
			bb.put((byte)3).putInt(face[0]).putInt(face[1]).putInt(face[2]);
			bb.put((byte)2).putShort((short)5).putShort((short)6);
			bb.putInt(idxFace + 7);
		}
		bytes.write(bb.array(), 0, bb.position());

		VertexMesh mesh = checkTwoFaces(new ByteArrayInputStream(bytes.toByteArray()), faces);
		assertTrue(mesh.faceTextures.isEquals(7, 8));
	}

	@Test void unknownElementsAndLists_ascii() throws IOException {
		String text = """
				ply
				format ascii 1.0
				element vertex 4
				property float32 x
				property float32 y
				property float32 z
				element material 2
				property list uint8 int32 ids
				property float64 shininess
				element face 2
				property list uint8 int32 vertex_indices
				property list uint8 uint16 flags
				end_header
				0 10 20
				1 11 21
				2 12 22
				3 13 23
				3 1 2 3 0.5
				0 0.25
				3 0 1 2 2 5 6
				3 1 3 2 2 5 6
				""";
		checkTwoFaces(new ByteArrayInputStream(text.getBytes(UTF_8)), new int[][]{{0, 1, 2}, {1, 3, 2}});
	}

	private VertexMesh checkTwoFaces( ByteArrayInputStream input, int[][] expected ) throws IOException {
		var mesh = new VertexMesh();
		PlyCodec.readMesh(input, mesh);
		assertEquals(4, mesh.vertexes.size());
		assertEquals(3.0, mesh.vertexes.getTemp(3).x, UtilEjml.TEST_F64);
		assertEquals(23.0, mesh.vertexes.getTemp(3).z, UtilEjml.TEST_F64);
		assertEquals(expected.length, mesh.size());
		for (int face = 0; face < expected.length; face++) {
			int offset = mesh.faceOffsets.get(face);
			for (int i = 0; i < 3; i++) {
				assertEquals(expected[face][i], mesh.faceVertexes.get(offset + i));
			}
		}
		return mesh;
	}
}
