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
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.mesh.VertexMesh;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.shapes.Polygon2D_F32;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import org.ddogleg.struct.DogArray;
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
		mesh.faceVertexes.addAll(DogArray_I32.array(0, 1, 2, 3));
		mesh.faceOffsets.add(4);

		// Configure
		var alg = new RenderMesh();

		// turn off checking with normals to simply this test
		alg.setCheckFaceNormal(false);
		var intrinsics = new CameraPinholeBrown();
		PerspectiveOps.createIntrinsic(300, 200, 90, -1, intrinsics);
		alg.setCamera(intrinsics);

		// Render
		alg.render(mesh);

		// See if it did anything
		int count = 0;
		for (int y = 0; y < alg.resolution.height; y++) {
			for (int x = 0; x < alg.resolution.width; x++) {
				if (alg.rgbImage.get24(x, y) != 0xFFFFFF)
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
	 * Tests the projection by having it fill in a known rectangle.
	 */
	@Test void projectSurfaceColor() {
		var alg = new RenderMesh();
		alg.resolution.setTo(100, 120);
		alg.initializeImages();

		// Polygon of projected shape on to the image. Make is an AABB, but smaller than the one above
		var polygon = new Polygon2D_F64();
		polygon.vertexes.grow().setTo(10, 15);
		polygon.vertexes.grow().setTo(40, 15);
		polygon.vertexes.grow().setTo(40, 35);
		polygon.vertexes.grow().setTo(10, 35);

		var shapeInCamera = new DogArray<>(Point3D_F64::new);
		shapeInCamera.resize(polygon.size());
		shapeInCamera.get(0).setTo(0, 0, 10); // the depth will be 10 for the shape

		// Perform the projection
		alg.projectSurfaceColor(shapeInCamera, polygon, 0);

		// Verify by counting the number of projected points
		int countDepth = 0;
		int countRgb = 0;
		for (int y = 0; y < alg.resolution.height; y++) {
			for (int x = 0; x < alg.resolution.width; x++) {
				if (alg.depthImage.get(x, y) == 10)
					countDepth++;
				if (alg.rgbImage.get24(x, y) != 0xFFFFFF)
					countRgb++;
			}
		}

		assertEquals(600, countDepth);
		assertEquals(600, countRgb);
	}

	@Test void projectSurfaceTexture() {
		var alg = new RenderMesh() {
			@Override int interpolateTextureRgb( float px, float py ) {
				// return some arbitrary color
				return 1;
			}
		};
		alg.resolution.setTo(100, 120);
		alg.initializeImages();

		// Polygon of projected shape on to the image. Make is an AABB, but smaller than the one above
		var polygon = new Polygon2D_F64();
		polygon.vertexes.grow().setTo(10, 15);
		polygon.vertexes.grow().setTo(40, 15);
		polygon.vertexes.grow().setTo(40, 35);
		polygon.vertexes.grow().setTo(10, 35);

		var shapeInCamera = new DogArray<>(Point3D_F64::new);
		shapeInCamera.resize(polygon.size(), ( p ) -> p.setTo(0, 0, 10));
		// All points will have a depth of 10

		// Create texture with reasonable coordinates. Doesn't really matter what they are
		var polyTexture = new Polygon2D_F32();
		for (int i = 0; i < polygon.size(); i++) {
			Point2D_F64 p = polygon.get(i);
			polyTexture.vertexes.grow().setTo((float)p.x/50, (float)p.y/50);
		}

		// Perform the projection
		alg.projectSurfaceTexture(shapeInCamera, polygon, polyTexture);

		// Verify by counting the number of projected points
		int countDepth = 0;
		int countRgb = 0;
		for (int y = 0; y < alg.resolution.height; y++) {
			for (int x = 0; x < alg.resolution.width; x++) {
				if (!Float.isNaN(alg.depthImage.get(x, y))) {
					countDepth++;
				}
				if (alg.rgbImage.get24(x, y) != 0xFFFFFF)
					countRgb++;
			}
		}

		assertEquals(600, countDepth);
		assertEquals(600, countRgb);
	}

	/**
	 * Rotate in a circle and check two handcrafted scenarios
	 */
	@Test void isFrontVisible() {
		var mesh = new VertexMesh();

		double r = 5;

		var pointCam = new Point3D_F64(0, 2, 2);

		// Should pass all of these
		for (int i = 0; i < 30; i++) {
			double yaw = Math.PI*i/15;

			double c = Math.cos(yaw);
			double s = Math.sin(yaw);

			// This should pass
			mesh.reset();
			mesh.faceNormals.add(0);
			mesh.normals.append((float)-c, (float)-s, 0);
			mesh.faceVertexes.add(0);
			mesh.vertexes.append(r*c, 2 + r*s, 2);

			assertTrue(RenderMesh.isFrontVisible(mesh, 0, 0, pointCam));

			// This should fail
			mesh.reset();
			mesh.faceVertexes.add(0);
			mesh.vertexes.append(r*c, 2 + r*s, 2);
			mesh.faceNormals.add(0);
			mesh.normals.append((float)c, (float)s, 0);

			assertFalse(RenderMesh.isFrontVisible(mesh, 0, 0, pointCam));
		}
	}
}
