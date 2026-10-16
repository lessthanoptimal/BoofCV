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

import boofcv.io.UtilIO;
import boofcv.struct.mesh.VertexMesh;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F32;
import org.apache.commons.io.FileUtils;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestObjLoadFileSystem extends BoofStandardJUnit {
	private final static String fileObj = "file.obj";
	private final static String fileMtl = "file.mtl";

	File workDir;

	@BeforeEach public void setup() {
		try {
			workDir = Files.createTempDirectory("obj").toFile();
		} catch (IOException ignore) {
		}
	}

	@AfterEach public void teardown() {
		UtilIO.deleteRecursive(workDir);
	}

	/**
	 * No texture mapping and everything is contained in a single file
	 */
	@Test void noMaterials() {
		String text = """
				v 0.0 0.0 0.0
				v 0.0 1.0 0.0
				v 1.0 0.0 0.0
				f 1 2 3
				""";
		save(fileObj, text);

		var mesh = new VertexMesh();
		mesh.rgb.add(1); // test to see if it calls reset

		new ObjLoadFromFiles().load(new File(workDir, fileObj), mesh);

		assertEquals(3, mesh.vertexes.size());
		assertEquals(1, mesh.size());
		assertEquals(0, mesh.rgb.size());
		assertTrue(mesh.textureNames.isEmpty());
		assertTrue(mesh.faceTextures.isEmpty());
	}

	/** Each material has its own texture and all faces are in one mesh */
	@Test void multipleMaterials() {
		String textObj = """
				mtllib file.mtl
				v 0.0 0.0 0.0
				v 0.0 1.0 0.0
				v 1.0 0.0 0.0
				v 1.0 1.0 0.0
				usemtl a
				f 1 2 3
				usemtl b
				f 1 3 4
				usemtl a
				f 2 3 4
				""";
		save(fileObj, textObj);

		String textMtl = """
				newmtl a
				map_Kd a.jpg
				newmtl b
				map_Kd b.jpg
				""";
		save(fileMtl, textMtl);

		var mesh = new VertexMesh();
		new ObjLoadFromFiles().load(new File(workDir, fileObj), mesh);

		assertEquals(4, mesh.vertexes.size());
		assertEquals(3, mesh.size());
		assertEquals(List.of("a.jpg", "b.jpg"), mesh.textureNames);
		assertTrue(mesh.faceTextures.isEquals(0, 1, 0));
	}

	/** Materials which share a texture or have no texture */
	@Test void sharedAndMissingTextures() {
		String textObj = """
				mtllib file.mtl
				v 0.0 0.0 0.0
				v 0.0 1.0 0.0
				v 1.0 0.0 0.0
				usemtl b
				f 1 2 3
				usemtl a
				f 1 2 3
				usemtl c
				f 1 2 3
				usemtl d
				f 1 2 3
				""";
		save(fileObj, textObj);

		// 'c' has the same texture as 'a' and 'd' has no texture
		String textMtl = """
				newmtl a
				map_Kd a.jpg
				newmtl b
				map_Kd b.jpg
				newmtl c
				map_Kd a.jpg
				newmtl d
				""";
		save(fileMtl, textMtl);

		var mesh = new VertexMesh();
		new ObjLoadFromFiles().load(new File(workDir, fileObj), mesh);

		assertEquals(List.of("b.jpg", "a.jpg"), mesh.textureNames);
		assertTrue(mesh.faceTextures.isEquals(0, 1, 1, 0));

		// A warning is printed for the material without a texture
		assertTrue(err.used);
		err.used = false;
	}

	/** Saves a mesh with multiple textures using ObjFileCodec then reads it back in */
	@Test void saveThenLoad_multipleTextures() throws IOException {
		// Texture coordinates for each vertex in a face, the same as a PLY file
		saveThenLoad(createTexturedMesh());

		// Texture coordinate and normal indexes are specified
		VertexMesh mesh = createTexturedMesh();
		mesh.faceVertexTextures.addAll(DogArray_I32.array(2, 1, 0, 0, 1, 2));
		mesh.normals.append(0, 0, 1);
		mesh.normals.append(0, 0, -1);
		mesh.faceVertexNormals.addAll(DogArray_I32.array(1, 0, 1, 0, 1, 0));
		saveThenLoad(mesh);
	}

	private void saveThenLoad( VertexMesh expected ) throws IOException {
		try (var writer = new FileWriter(new File(workDir, fileObj), StandardCharsets.UTF_8)) {
			ObjFileCodec.save(expected, writer);
		}
		try (var writer = new FileWriter(new File(workDir, "red.mtl"), StandardCharsets.UTF_8)) {
			ObjFileCodec.saveMtl(expected.textureNames, writer);
		}

		var found = new VertexMesh();
		new ObjLoadFromFiles().load(new File(workDir, fileObj), found);

		assertEquals(expected.vertexes.size(), found.vertexes.size());
		assertTrue(expected.faceVertexes.isEquals(found.faceVertexes));
		assertTrue(expected.faceOffsets.isEquals(found.faceOffsets));
		assertTrue(expected.faceVertexNormals.isEquals(found.faceVertexNormals));

		// Textures are indexed in the order they are first used, so compare the texture name of each face
		assertEquals(expected.textureNames.size(), found.textureNames.size());
		for (int face = 0; face < expected.size(); face++) {
			assertEquals(expected.textureNames.get(expected.getFaceTexture(face)),
					found.textureNames.get(found.getFaceTexture(face)));
		}

		// Compare the texture coordinates of each face, since indexes are not saved the same way
		var expectedCoors = new DogArray<>(Point2D_F32::new);
		var foundCoors = new DogArray<>(Point2D_F32::new);
		for (int face = 0; face < expected.size(); face++) {
			expected.getTexture(face, expectedCoors);
			found.getTexture(face, foundCoors);
			assertEquals(expectedCoors.size, foundCoors.size);
			for (int i = 0; i < expectedCoors.size; i++) {
				assertEquals(0.0, expectedCoors.get(i).distance(foundCoors.get(i)), 1e-5);
			}
		}
	}

	/** Two triangles which use different textures and have a texture coordinate for each vertex in a face */
	private static VertexMesh createTexturedMesh() {
		var mesh = new VertexMesh();
		mesh.vertexes.append(0, 0, 0);
		mesh.vertexes.append(0, 1, 0);
		mesh.vertexes.append(1, 0, 0);
		mesh.vertexes.append(1, 1, 0);
		mesh.faceVertexes.addAll(DogArray_I32.array(0, 1, 2, 1, 2, 3));
		mesh.faceOffsets.add(3);
		mesh.faceOffsets.add(6);
		for (int i = 0; i < 6; i++) {
			mesh.texture.append(i*0.1f, 1.0f - i*0.1f);
		}
		mesh.textureNames.add("red.jpg");
		mesh.textureNames.add("blue.jpg");
		mesh.faceTextures.addAll(DogArray_I32.array(1, 0));
		return mesh;
	}

	private void save( String fileName, String text ) {
		try {
			FileUtils.writeStringToFile(new File(workDir, fileName), text, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
