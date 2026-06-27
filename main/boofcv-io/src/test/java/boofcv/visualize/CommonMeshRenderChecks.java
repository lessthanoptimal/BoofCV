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
