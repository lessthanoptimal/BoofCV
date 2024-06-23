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

import boofcv.io.UtilIO;
import boofcv.struct.mesh.VertexMesh;
import boofcv.testing.BoofStandardJUnit;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

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
				f 1 2 3
				""";
		save(fileObj, text);

		var alg = new ObjLoadFromFiles();
		alg.load(new File(workDir, fileObj), null);
		Map<String, VertexMesh> found = alg.getShapeToMesh();
		assertEquals(1, found.size());

		VertexMesh mesh = found.get("");

		assertEquals(2, mesh.vertexes.size());
		assertEquals(1, mesh.size());
	}

	@Test void multipleMaterials() {
		createMultipleMaterialFiles();

		var alg = new ObjLoadFromFiles();
		alg.load(new File(workDir, fileObj), null);
		Map<String, VertexMesh> found = alg.getShapeToMesh();
		assertEquals(2, found.size());

		VertexMesh mesh1 = found.get("a");
		assertEquals("a.jpg", mesh1.textureName);
		assertEquals(2, mesh1.vertexes.size());
		assertEquals(0, mesh1.faceNormals.size());


		VertexMesh mesh2 = found.get("b");
		assertEquals("b.jpg", mesh2.textureName);
		assertEquals(2, mesh2.vertexes.size());
		assertEquals(1, mesh2.size());
		assertEquals(0, mesh2.vertexes.getTemp(0).distance(1.0, 0, 0));
		assertEquals(0, mesh2.vertexes.getTemp(1).distance(0.0, 2, 0));
	}

	@Test void singleMesh_noMaterial() {
		String text = """
				v 0.0 0.0 0.0
				v 0.0 1.0 0.0
				f 1 2 3
				""";
		save(fileObj, text);

		var mesh = new VertexMesh();
		mesh.rgb.add(1); // test to see if it calls reset

		var alg = new ObjLoadFromFiles();
		alg.load(new File(workDir, fileObj), mesh);
		Map<String, VertexMesh> found = alg.getShapeToMesh();

		assertFalse(alg.isIgnoredMaterial());
		assertEquals(1, found.size());
		assertSame(mesh, found.get(""));

		assertEquals(2, mesh.vertexes.size());
		assertEquals(1, mesh.size());
		assertEquals(0, mesh.rgb.size());
	}

	@Test void singleMesh_multipleMaterial() {
		createMultipleMaterialFiles();

		var mesh = new VertexMesh();
		mesh.rgb.add(1); // test to see if it calls reset

		var alg = new ObjLoadFromFiles();
		alg.load(new File(workDir, fileObj), mesh);
		Map<String, VertexMesh> found = alg.getShapeToMesh();
		assertTrue(alg.isIgnoredMaterial());

		assertEquals(1, found.size());
		assertSame(mesh, found.get("a"));

		assertEquals(2, mesh.vertexes.size());
		assertEquals(0, mesh.vertexes.getTemp(0).distance(0.0, 0, 0));
		assertEquals(0, mesh.vertexes.getTemp(1).distance(0.0, 1, 0));
		assertEquals(0, mesh.size());
		assertEquals(0, mesh.rgb.size());
	}

	private void save( String fileName, String text ) {
		try {
			FileUtils.writeStringToFile(new File(workDir, fileName), text, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void createMultipleMaterialFiles() {
		String textObj = """
				mtllib file.mtl
				usemtl a
				v 0.0 0.0 0.0
				v 0.0 1.0 0.0
				usemtl b
				v 1.0 0.0 0.0
				v 0.0 2.0 0.0
				f 1 2 3
				""";
		save(fileObj, textObj);

		String textMtl = """
				newmtl a
				map_Kd a.jpg
				newmtl b
				map_Kd b.jpg
				""";
		save(fileMtl, textMtl);
	}
}
