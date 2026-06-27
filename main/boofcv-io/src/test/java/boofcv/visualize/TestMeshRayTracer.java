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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests for the internal ray-tracing primitives of MeshRayTracer.
///
/// NOTE: requires intersectTri() and the Hit class to be package-private (like trace/rayBoxEntry/
/// sampleTexture/bilerp already are).
public class TestMeshRayTracer extends CommonMeshRenderChecks {
	@Override public MeshRender create() {
		return new MeshRayTracer();
	}

	@Override public boolean isRequireTriangles() {
		return true;
	}

	/// Closest-hit traversal over a built BVH. Two stacked triangles verify it returns the nearer
	/// one, plus a couple of clean misses.
	@Test void trace() {
		// Two triangles covering (0,0): the near one at z=10, the far one at z=20.
		var mesh = new VertexMesh();
		mesh.vertexes.append(-1, -1, 10);
		mesh.vertexes.append(3, -1, 10);
		mesh.vertexes.append(-1, 3, 10);
		mesh.vertexes.append(-1, -1, 20);
		mesh.vertexes.append(3, -1, 20);
		mesh.vertexes.append(-1, 3, 20);
		for (int i = 0; i < 6; i++) mesh.faceVertexes.add(i);
		mesh.faceOffsets.add(3);
		mesh.faceOffsets.add(6);

		var alg = new MeshRayTracer();
		alg.setMesh(mesh);

		// Straight down +z from the origin: must hit the NEAR triangle (index 0) at range 10.
		var hit = new MeshRayTracer.Hit();
		hit.t = Double.POSITIVE_INFINITY;
		hit.tri = -1;
		alg.trace(0, 0, 0, 0, 0, 1, new int[64], hit);
		assertEquals(0, hit.tri);
		assertEquals(10.0, hit.t, 1e-8);

		// Pointing away from the mesh: no hit.
		var behind = new MeshRayTracer.Hit();
		behind.t = Double.POSITIVE_INFINITY;
		behind.tri = -1;
		alg.trace(0, 0, 0, 0, 0, -1, new int[64], behind);
		assertEquals(-1, behind.tri);

		// Travelling in the z=0 plane never reaches the triangles: no hit (also exercises the
		// zero-direction-component path in the slab test).
		var sideways = new MeshRayTracer.Hit();
		sideways.t = Double.POSITIVE_INFINITY;
		sideways.tri = -1;
		alg.trace(0, 0, 0, 1, 0, 0, new int[64], sideways);
		assertEquals(-1, sideways.tri);
	}

	/// Ray/AABB slab test against a known axis-aligned box. The single triangle below has
	/// AABB = [1,3] x [1,3] x [5,9], so node 0 (a leaf) carries exactly that box.
	@Test void rayBoxEntry() {
		var alg = new MeshRayTracer();
		alg.setMesh(singleTriangle(1, 1, 5, 3, 1, 5, 1, 3, 9));

		// Inverse-direction component the implementation uses when a direction component is 0.
		double big = 1.0/1e-300;

		// +z ray entering through the front face at z=5, starting at z=0 -> entry range 5.
		assertEquals(5.0, alg.rayBoxEntry(0, 2, 2, 0, big, big, 1.0, Double.POSITIVE_INFINITY), 1e-6);

		// Origin already inside the box -> entry clamped to 0.
		assertEquals(0.0, alg.rayBoxEntry(0, 2, 2, 7, big, big, 1.0, Double.POSITIVE_INFINITY), 1e-6);

		// Pointing away in z -> miss.
		assertEquals(Double.POSITIVE_INFINITY,
				alg.rayBoxEntry(0, 2, 2, 0, big, big, -1.0, Double.POSITIVE_INFINITY));

		// Would enter at range 5 but the best hit so far is closer (2) -> pruned.
		assertEquals(Double.POSITIVE_INFINITY, alg.rayBoxEntry(0, 2, 2, 0, big, big, 1.0, 2.0));

		// Laterally offset so the ray never crosses the box in x -> miss.
		assertEquals(Double.POSITIVE_INFINITY,
				alg.rayBoxEntry(0, 10, 2, 0, big, big, 1.0, Double.POSITIVE_INFINITY));
	}

	/// Moller-Trumbore against the canonical right triangle (0,0)-(1,0)-(0,1) at z=10. With this
	/// triangle the barycentric weights equal the (x,y) of the hit, which makes every expectation
	/// hand-checkable.
	@Test void intersectTri() {
		var alg = new MeshRayTracer();
		alg.setMesh(singleTriangle(0, 0, 10, 1, 0, 10, 0, 1, 10));

		// +z ray through an interior point: hit at range 10 with u=v=0.25.
		var hit = new MeshRayTracer.Hit();
		hit.t = Double.POSITIVE_INFINITY;
		hit.tri = -1;
		alg.intersectTri(0, 0.25, 0.25, 0, 0, 0, 1, hit);
		assertEquals(0, hit.tri);
		assertEquals(10.0, hit.t, 1e-8);
		assertEquals(0.25, hit.u, 1e-8);
		assertEquals(0.25, hit.v, 1e-8);

		// Same ray but the triangle is behind the origin's direction -> no update.
		var behind = new MeshRayTracer.Hit();
		behind.t = Double.POSITIVE_INFINITY;
		behind.tri = -1;
		alg.intersectTri(0, 0.25, 0.25, 0, 0, 0, -1, behind);
		assertEquals(-1, behind.tri);

		// Ray crosses the triangle's plane outside the triangle (x+y > 1) -> no update.
		var outside = new MeshRayTracer.Hit();
		outside.t = Double.POSITIVE_INFINITY;
		outside.tri = -1;
		alg.intersectTri(0, 0.8, 0.8, 0, 0, 0, 1, outside);
		assertEquals(-1, outside.tri);

		// A nearer existing hit must not be overwritten by a farther intersection.
		var nearer = new MeshRayTracer.Hit();
		nearer.t = 5.0;
		nearer.tri = 99;
		alg.intersectTri(0, 0.25, 0.25, 0, 0, 0, 1, nearer);
		assertEquals(99, nearer.tri);
		assertEquals(5.0, nearer.t);

		// Default is two-sided, so the back face was hit above. Turning on culling rejects it.
		alg.setCullBackFaces(true);
		var culled = new MeshRayTracer.Hit();
		culled.t = Double.POSITIVE_INFINITY;
		culled.tri = -1;
		alg.intersectTri(0, 0.25, 0.25, 0, 0, 0, 1, culled);
		assertEquals(-1, culled.tri);
	}

	/// Texture-coordinate interpolation + bilinear sampling. The triangle's corner texcoords are
	/// (0,0),(1,0),(0,1), which makes the interpolated coordinate equal (u,v); a 2x2 texture then
	/// lets integer texel hits and one midpoint be checked exactly.
	@Test void sampleTexture() {
		var mesh = singleTriangle(0, 0, 10, 1, 0, 10, 0, 1, 10);
		mesh.texture.append(0, 0);
		mesh.texture.append(1, 0);
		mesh.texture.append(0, 1);

		// 2x2 RGB texture with a distinct color per texel.
		var tex = new InterleavedU8(2, 2, 3);
		tex.set24(0, 0, 0xFF0000); // red
		tex.set24(1, 0, 0x00FF00); // green
		tex.set24(0, 1, 0x0000FF); // blue
		tex.set24(1, 1, 0xFFFFFF); // white

		var alg = new MeshRayTracer();
		alg.setMesh(mesh);
		alg.setTextureImage(tex);

		// (u,v) -> texcoord (u,v) -> texel (u*(W-1), (1-v)*(H-1)). Integer coords -> exact texel.
		assertEquals(0xFF0000, alg.sampleTexture(0, 0.0, 1.0)); // texel (0,0)
		assertEquals(0x00FF00, alg.sampleTexture(0, 1.0, 1.0)); // texel (1,0)
		assertEquals(0x0000FF, alg.sampleTexture(0, 0.0, 0.0)); // texel (0,1)
		assertEquals(0xFFFFFF, alg.sampleTexture(0, 1.0, 0.0)); // texel (1,1)

		// Halfway between red (255,0,0) and green (0,255,0): each blends to 128.
		assertEquals(0x808000, alg.sampleTexture(0, 0.5, 1.0));
	}

	/// Bilinear blend of four packed bytes. Indices and band offset are supplied explicitly.
	@Test void bilerp() {
		// Single-band layout: corners at indices 0..3.
		byte[] d = {0, 100, (byte)200, 40};

		// Four corners (fx,fy in {0,1}) return the corresponding corner exactly.
		assertEquals(0, MeshRayTracer.bilerp(d, 0, 1, 2, 3, 0, 0f, 0f));
		assertEquals(100, MeshRayTracer.bilerp(d, 0, 1, 2, 3, 0, 1f, 0f));
		assertEquals(200, MeshRayTracer.bilerp(d, 0, 1, 2, 3, 0, 0f, 1f));
		assertEquals(40, MeshRayTracer.bilerp(d, 0, 1, 2, 3, 0, 1f, 1f));

		// Edge midpoints.
		assertEquals(50, MeshRayTracer.bilerp(d, 0, 1, 2, 3, 0, 0.5f, 0f));  // (0+100)/2
		assertEquals(100, MeshRayTracer.bilerp(d, 0, 1, 2, 3, 0, 0f, 0.5f)); // (0+200)/2

		// Center: average of (0,100,200,40) = 85.
		assertEquals(85, MeshRayTracer.bilerp(d, 0, 1, 2, 3, 0, 0.5f, 0.5f));

		// (byte)200 must be read unsigned.
		assertEquals(200, MeshRayTracer.bilerp(d, 2, 2, 2, 2, 0, 0.3f, 0.7f));

		// Band offset is honored: read band 1 with a 2-band interleaved layout.
		byte[] d2 = {9, 10, 9, 20, 9, 30, 9, 40};
		assertEquals(10, MeshRayTracer.bilerp(d2, 0, 2, 4, 6, 1, 0f, 0f));
		assertEquals(25, MeshRayTracer.bilerp(d2, 0, 2, 4, 6, 1, 0.5f, 0.5f)); // (10+20+30+40)/4
	}

	/// A mesh with a single triangle from three (x,y,z) corners.
	private static VertexMesh singleTriangle( double ax, double ay, double az,
	                                          double bx, double by, double bz,
	                                          double cx, double cy, double cz ) {
		var m = new VertexMesh();
		m.vertexes.append(ax, ay, az);
		m.vertexes.append(bx, by, bz);
		m.vertexes.append(cx, cy, cz);
		m.faceVertexes.add(0);
		m.faceVertexes.add(1);
		m.faceVertexes.add(2);
		m.faceOffsets.add(3);
		return m;
	}
}
