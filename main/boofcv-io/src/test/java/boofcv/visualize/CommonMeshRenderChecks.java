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

package boofcv.visualize;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.InterleavedU8;
import boofcv.struct.mesh.VertexMesh;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class CommonMeshRenderChecks extends BoofStandardJUnit {
	private final int defaultColor = 0xFFFFFF;

	public abstract MeshRender create();

	/// If it returns true then a mesh must be triangles
	public abstract boolean isRequireTriangles();

	@Test void checkDefaults() {
		MeshRender alg = create();
		assertFalse(alg.isCullBackFaces());
		assertFalse(alg.isForceColorizer());
		assertEquals(0xFFFFFF, alg.getDefaultColorRgb());
	}

	/// Render a simple shape. This effectively makes sure it doesn't crash and that it modified the image.
	@Test void allTogether() {
		// Manually define a simple mesh
		var mesh = new VertexMesh();
		mesh.vertexes.append(-1, -1, 10);
		mesh.vertexes.append(1, -1, 10);
		mesh.vertexes.append(1, 1, 10);
		mesh.vertexes.append(-1, 1, 10);
		mesh.faceVertexes.addAll(DogArray_I32.array(0, 1, 2, 3));
		mesh.faceOffsets.add(4);

		if (isRequireTriangles())
			mesh = mesh.toTriangles();

		MeshRender alg = create();
		alg.setDefaultColorRgb(defaultColor);

		var intrinsics = new CameraPinholeBrown();
		PerspectiveOps.createIntrinsic(300, 200, 90, -1, intrinsics);
		alg.setCamera(intrinsics);

		// Render
		alg.setMesh(mesh);
		alg.render();

		// The normal is pointing away and by default it should not cull backfaces
		assertTrue(countNotDefault(alg.getRenderedImage()) != 0);

		alg.setCullBackFaces(true);
		alg.setMesh(mesh);
		alg.render();
		assertEquals(0, countNotDefault(alg.getRenderedImage()));
	}

	/// Square made of two triangles. Each triangle uses a different solid color texture image
	@Test void multipleTextures() {
		VertexMesh mesh = texturedSquare(0, 1);

		MeshRender alg = create();
		var intrinsics = new CameraPinholeBrown();
		PerspectiveOps.createIntrinsic(300, 200, 90, -1, intrinsics);
		alg.setCamera(intrinsics);
		alg.setMesh(mesh);
		alg.setTextureImages(List.of(solidImage(0xFF0000), solidImage(0x0000FF)));
		alg.render();

		// The square spans pixels 135 to 165 along x and 85 to 115 along y
		assertEquals(0xFF0000, alg.getRenderedImage().get24(157, 92));
		assertEquals(0x0000FF, alg.getRenderedImage().get24(142, 107));
	}

	/// A single band texture is rendered as gray
	@Test void grayTexture() {
		VertexMesh mesh = texturedSquare(0, 0);

		// Varies along x, so reading a neighboring pixel as another band changes the color
		var texture = new InterleavedU8(4, 4, 1);
		for (int y = 0; y < texture.height; y++) {
			for (int x = 0; x < texture.width; x++) {
				texture.set(x, y, 0x40 + 0x20*x);
			}
		}

		MeshRender alg = create();
		var intrinsics = new CameraPinholeBrown();
		PerspectiveOps.createIntrinsic(300, 200, 90, -1, intrinsics);
		alg.setCamera(intrinsics);
		alg.setMesh(mesh);
		alg.setTextureImage(texture);
		alg.render();

		// Texture coordinate (0.5, 0.5) is halfway between x = 1 and 2
		assertEquals(0x707070, alg.getRenderedImage().get24(157, 92));
		assertEquals(0x707070, alg.getRenderedImage().get24(142, 107));
	}

	/// Square at z = 10 made of two triangles, with the specified texture image for each triangle
	private VertexMesh texturedSquare( int texture0, int texture1 ) {
		var mesh = new VertexMesh();
		mesh.vertexes.append(-1, -1, 10);
		mesh.vertexes.append(1, -1, 10);
		mesh.vertexes.append(1, 1, 10);
		mesh.vertexes.append(-1, 1, 10);
		mesh.faceVertexes.addAll(DogArray_I32.array(0, 1, 2, 0, 2, 3));
		mesh.faceOffsets.add(3);
		mesh.faceOffsets.add(6);
		for (int i = 0; i < 6; i++) {
			mesh.texture.append(0.5f, 0.5f);
		}
		mesh.faceTextures.addAll(DogArray_I32.array(texture0, texture1));
		return mesh;
	}

	private InterleavedU8 solidImage( int rgb ) {
		var image = new InterleavedU8(4, 4, 3);
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				image.set24(x, y, rgb);
			}
		}
		return image;
	}

	private int countNotDefault( InterleavedU8 renderedImage ) {
		int count = 0;
		for (int y = 0; y < renderedImage.height; y++) {
			for (int x = 0; x < renderedImage.width; x++) {
				if (renderedImage.get24(x, y) != defaultColor)
					count++;
			}
		}
		return count;
	}
}
