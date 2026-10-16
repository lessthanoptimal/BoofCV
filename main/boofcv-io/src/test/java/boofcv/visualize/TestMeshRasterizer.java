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

import boofcv.struct.image.InterleavedU8;
import boofcv.struct.mesh.VertexMesh;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.shapes.Polygon2D_F32;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMeshRasterizer extends CommonMeshRenderChecks {
	@Test void computeBoundingBox() {
		var polygon = new Polygon2D_F64();
		polygon.vertexes.grow().setTo(-5, -1);
		polygon.vertexes.grow().setTo(-5, 100);
		polygon.vertexes.grow().setTo(90, 100);
		polygon.vertexes.grow().setTo(90, -1);

		// It should be bounded by the image
		var aabb = new Rectangle2D_I32();
		MeshRasterizer.computeBoundingBox(60, 50, polygon, aabb);
		assertEquals(0, aabb.x0);
		assertEquals(0, aabb.y0);
		assertEquals(60, aabb.x1);
		assertEquals(50, aabb.y1);

		// upper extent shouldn't be bounded by the image
		// Needs to handle exclusive upper extent properly
		MeshRasterizer.computeBoundingBox(200, 200, polygon, aabb);
		assertEquals(0, aabb.x0);
		assertEquals(0, aabb.y0);
		assertEquals(91, aabb.x1);
		assertEquals(101, aabb.y1);
	}

	@Test void boundingPixels() {
		var aabb = new Rectangle2D_I32();

		// Inside the image. Only pixel coordinates inside the box are included
		assertTrue(MeshRasterizer.boundingPixels(1.5, 2.2, 5.5, 2.4, 3.0, 7.9, 60, 50, aabb));
		assertEquals(2, aabb.x0);
		assertEquals(3, aabb.y0);
		assertEquals(6, aabb.x1);
		assertEquals(8, aabb.y1);

		// Vertexes exactly on a pixel coordinate include that pixel
		assertTrue(MeshRasterizer.boundingPixels(2, 3, 4, 3, 4, 5, 60, 50, aabb));
		assertEquals(2, aabb.x0);
		assertEquals(3, aabb.y0);
		assertEquals(5, aabb.x1);
		assertEquals(6, aabb.y1);

		// Clipped by the image
		assertTrue(MeshRasterizer.boundingPixels(-5, -1, 90, -1, 90, 100, 60, 50, aabb));
		assertEquals(0, aabb.x0);
		assertEquals(0, aabb.y0);
		assertEquals(60, aabb.x1);
		assertEquals(50, aabb.y1);

		// Between pixel coordinates along one axis
		assertFalse(MeshRasterizer.boundingPixels(2.1, 3, 2.9, 3, 2.5, 6, 60, 50, aabb));
		assertFalse(MeshRasterizer.boundingPixels(2, 3.1, 6, 3.2, 4, 3.9, 60, 50, aabb));

		// Entirely outside the image, including just outside where casting to int would round towards zero
		assertFalse(MeshRasterizer.boundingPixels(-3, 2, -0.5, 2, -1, 5, 60, 50, aabb));
		assertFalse(MeshRasterizer.boundingPixels(2, -3, 5, -0.5, 4, -1, 60, 50, aabb));
		assertFalse(MeshRasterizer.boundingPixels(59.5, 2, 70, 2, 65, 5, 60, 50, aabb));
		assertFalse(MeshRasterizer.boundingPixels(2, 49.5, 5, 60, 4, 55, 60, 50, aabb));
	}

	/// Tests the projection by having it fill in a known rectangle.
	@Test void projectSurfaceColor() {
		var alg = new MeshRasterizer();
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
		InterleavedU8 renderedImage = alg.getRenderedImage();
		int countDepth = 0;
		int countRgb = 0;
		for (int y = 0; y < alg.resolution.height; y++) {
			for (int x = 0; x < alg.resolution.width; x++) {
				if (alg.depthImage.get(x, y) == 10)
					countDepth++;
				if (renderedImage.get24(x, y) != 0xFFFFFF)
					countRgb++;
			}
		}

		assertEquals(600, countDepth);
		assertEquals(600, countRgb);
	}

	@Test void projectSurfaceTexture() {
		var alg = new MeshRasterizer() {
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
		InterleavedU8 renderedImage = alg.getRenderedImage();
		int countDepth = 0;
		int countRgb = 0;
		for (int y = 0; y < alg.resolution.height; y++) {
			for (int x = 0; x < alg.resolution.width; x++) {
				if (!Float.isNaN(alg.depthImage.get(x, y))) {
					countDepth++;
				}
				if (renderedImage.get24(x, y) != 0xFFFFFF)
					countRgb++;
			}
		}

		assertEquals(600, countDepth);
		assertEquals(600, countRgb);
	}

	/// Pixels inside a triangle must be the same as Intersection2D_F64.containsConvex(), including when
	/// vertexes lie exactly on pixel coordinates
	@Test void projectSurfaceTexture_sameAsContainsConvex() {
		var alg = new MeshRasterizer() {
			@Override int interpolateTextureRgb( float px, float py ) {return 1;}
		};
		alg.resolution.setTo(30, 25);

		var polygon = new Polygon2D_F64(3);
		var shapeInCamera = new DogArray<>(Point3D_F64::new);
		shapeInCamera.resize(3, ( p ) -> p.setTo(0, 0, 10));
		var polyTexture = new Polygon2D_F32(3);
		var point = new Point2D_F64();

		for (int trial = 0; trial < 200; trial++) {
			// Half the trials have vertexes on integer coordinates, the others on half pixels
			double step = trial%2 == 0 ? 1.0 : 0.5;
			for (int i = 0; i < 3; i++) {
				polygon.get(i).setTo(rand.nextInt(60)*step - 2, rand.nextInt(50)*step - 2);
			}

			alg.initializeImages();
			alg.projectSurfaceTexture(shapeInCamera, polygon, polyTexture);

			for (int y = 0; y < alg.resolution.height; y++) {
				for (int x = 0; x < alg.resolution.width; x++) {
					point.setTo(x, y);
					boolean expected = Intersection2D_F64.containsConvex(polygon, point);
					assertEquals(expected, alg.getRenderedImage().get24(x, y) != 0xFFFFFF);
				}
			}
		}
	}

	/// Rotate in a circle and check two handcrafted scenarios
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

			assertTrue(MeshRasterizer.isFrontVisible(mesh, 0, 0, pointCam));

			// This should fail
			mesh.reset();
			mesh.faceVertexes.add(0);
			mesh.vertexes.append(r*c, 2 + r*s, 2);
			mesh.faceNormals.add(0);
			mesh.normals.append((float)c, (float)s, 0);

			assertFalse(MeshRasterizer.isFrontVisible(mesh, 0, 0, pointCam));
		}
	}

	@Override public MeshRender create() {
		return new MeshRasterizer();
	}

	@Override public boolean isRequireTriangles() {
		return false;
	}
}
