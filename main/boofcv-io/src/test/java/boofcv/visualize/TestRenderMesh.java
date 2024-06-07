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

package boofcv.visualize;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.mesh.VertexMesh;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestRenderMesh extends BoofStandardJUnit {
	/**
	 * Render a simple shape. This effectively makes sure it doesn't crash and that it modified the image.
	 */
	@Test void allTogether() {
		// Manually define a simple mesh
		var mesh = new VertexMesh();
		mesh.vertexes.append(-1, -1, 10);
		mesh.vertexes.append(1, -1, 10);
		mesh.vertexes.append(1, 1, 10);
		mesh.vertexes.append(-1, 1, 10);
		mesh.indexes.addAll(DogArray_I32.array(0, 1, 2, 3));
		mesh.offsets.add(4);

		// Configure
		var alg = new RenderMesh();
		PerspectiveOps.createIntrinsic(300, 200, 90, -1, alg.intrinsics);

		// Render
		alg.render(mesh);

		// See if it did anything
		int count = 0;
		for (int y = 0; y < alg.intrinsics.height; y++) {
			for (int x = 0; x < alg.intrinsics.width; x++) {
				if (alg.rgbImage.get24(x,y) != 0xFFFFFF)
					count++;
			}
		}

		assertTrue(count != 0);
	}

	@Test void computeBoundingBox() {
		var polygon = new Polygon2D_F64();
		polygon.vertexes.grow().setTo(-5, -1);
		polygon.vertexes.grow().setTo(-5, 100);
		polygon.vertexes.grow().setTo(90, 100);
		polygon.vertexes.grow().setTo(90, -1);

		// It should be bounded by the image
		var aabb = new Rectangle2D_I32();
		RenderMesh.computeBoundingBox(60, 50, polygon, aabb);
		assertEquals(0, aabb.x0);
		assertEquals(0, aabb.y0);
		assertEquals(60, aabb.x1);
		assertEquals(50, aabb.y1);

		// upper extent shouldn't be bounded by the image
		// Needs to handle exclusive upper extent properly
		RenderMesh.computeBoundingBox(200, 200, polygon, aabb);
		assertEquals(0, aabb.x0);
		assertEquals(0, aabb.y0);
		assertEquals(91, aabb.x1);
		assertEquals(101, aabb.y1);
	}

	/**
	 * Tests the projection by having it fill in a known rectangle. The AABB is larger than needed. One pixel
	 * is given a depth closer than the polygon and isn't filled in.
	 */
	@Test void projectSurfaceColor() {
		var alg = new RenderMesh();
		alg.intrinsics.fsetShape(100, 120);
		alg.initializeImages();
		alg.aabb.setTo(10, 15, 50, 60);

		// Polygon of projected shape on to the image. Make is an AABB, but smaller than the one above
		var polygon = new Polygon2D_F64();
		polygon.vertexes.grow().setTo(10, 15);
		polygon.vertexes.grow().setTo(40, 15);
		polygon.vertexes.grow().setTo(40, 35);
		polygon.vertexes.grow().setTo(10, 35);

		// The mesh is used to get the depth of the shape being examined
		var mesh = new VertexMesh();
		mesh.vertexes.append(0, 0, 10);
		mesh.indexes.add(0);
		mesh.offsets.add(1);

		// Set one pixel inside the projected region to be closer than the mesh
		alg.depthImage.set(15, 25, 1);

		// Perform the projection
		alg.projectSurfaceColor(mesh, polygon, 0);

		// Verify by counting the number of projected points
		int countDepth = 0;
		int countRgb = 0;
		for (int y = 0; y < alg.intrinsics.height; y++) {
			for (int x = 0; x < alg.intrinsics.width; x++) {
				if (alg.depthImage.get(x,y) == 10)
					countDepth++;
				if (alg.rgbImage.get24(x,y) != 0xFFFFFF)
					countRgb++;
			}
		}

		assertEquals(599, countDepth);
		assertEquals(599, countRgb);
	}

	@Test void projectSurfaceTexture() {
		fail("implement");
	}
}
