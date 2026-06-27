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

import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.distort.Point2Transform3_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.InterleavedU8;
import boofcv.struct.mesh.VertexMesh;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

@SuppressWarnings("NullAway.Init")
public class MeshRayTracer {

	//--------------------------------------------------------------------------------------- Output
	/// Rendered range image. Each pixel is Euclidean distance from camera center to the surface
	/// along that pixel's ray. Pixels with no intersection are set to NaN.
	public final GrayF32 depthImage = new GrayF32(1, 1);

	/// Rendered color image, 3-band RGB.
	public final InterleavedU8 rgbImage = new InterleavedU8(1, 1, 3);

	//------------------------------------------------------------------------------------ Appearance
	/// Background color (0xRRGGBB) written to pixels with no intersection.
	public int defaultColorRgb = 0xFFFFFF;

	/// Returns the color of a face given its index in the original VertexMesh.
	public RenderMesh.SurfaceColor surfaceColor = which -> 0xFF0000;

	/// If false (default) triangles are visible from both sides.
	public boolean cullBackFaces = false;

	//------------------------------------------------------------------------------- Camera (rays)
	// Per-pixel unit pointing vectors in the CAMERA frame. Length = width*height.
	private double[] dirCamX = new double[0];
	private double[] dirCamY = new double[0];
	private double[] dirCamZ = new double[0];
	private int width, height;

	//---------------------------------------------------------------------------------- Mesh / BVH
	// Bounding Volume Hierarchy (MHV)
	// Triangle geometry, structure-of-arrays. v0 is the pivot vertex; e1=v1-v0, e2=v2-v0.
	// Triangle index == face index in the source VertexMesh (input is required to be triangles),
	// so no triangle->face side table is needed.
	private double[] V0X, V0Y, V0Z;
	private double[] E1X, E1Y, E1Z;
	private double[] E2X, E2Y, E2Z;
	// Triangle centroids, used only during BVH construction.
	private double[] cenX, cenY, cenZ;
	private int numTri;

	// Flattened BVH. An internal node has count==0 and its two children live at
	// [leftFirst, leftFirst+1]. A leaf has count>0 and owns triIdx[leftFirst .. leftFirst+count).
	private double[] bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ;
	private int[] bLeftFirst, bCount;
	private int[] triIdx;            // permuted triangle ordering referenced by leaves
	private int nodesUsed;
	private static final int LEAF_SIZE = 4;

	//--------------------------------------------------------------------------------- Current pose
	// Camera center in world coordinates and the rotation that takes a camera-frame direction to
	// world (= R^T where worldToView.R is world->view). Cached after render() for pixelTo3D().
	private final Point3D_F64 camCenter = new Point3D_F64();
	private double r00, r01, r02, r10, r11, r12, r20, r21, r22; // worldToView.R, row-major
	private final Se3_F64 worldToView = new Se3_F64();

	//------------------------------------------------------------------------------------ Tuning
	private static final double EPS_DET = 1e-12;  // reject near-degenerate det (grazing rays)
	private static final double EPS_T = 1e-9;     // reject hits at/behind the origin
	private static final int STACK_SIZE = 64;     // BVH depth bound; median split stays ~log2(N)

	// =====================================================================================
	//  Camera configuration
	// =====================================================================================

	/// General camera model that supports > 180 FOV
	public void setCamera( Point2Transform3_F64 pixelToPointing, int width, int height ) {
		allocateRays(width, height);
		var p = new Point3D_F64();
		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++, i++) {
				pixelToPointing.compute(x, y, p);
				store(i, p.x, p.y, p.z);
			}
		}
	}

	/// Convenience for narrow-FOV models that expose pixel -> normalized image coordinates
	public void setCameraNarrow( Point2Transform2_F64 pixelToNorm, int width, int height ) {
		allocateRays(width, height);
		var n = new georegression.struct.point.Point2D_F64();
		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++, i++) {
				pixelToNorm.compute(x, y, n);
				store(i, n.x, n.y, 1.0);
			}
		}
	}

	private void allocateRays( int width, int height ) {
		this.width = width;
		this.height = height;
		int n = width*height;
		if (dirCamX.length < n) {
			dirCamX = new double[n];
			dirCamY = new double[n];
			dirCamZ = new double[n];
		}
	}

	// Normalize defensively; a model may return a non-unit vector. Unit length keeps t == range.
	private void store( int i, double x, double y, double z ) {
		double norm = Math.sqrt(x*x + y*y + z*z);
		if (norm == 0) norm = 1;
		dirCamX[i] = x/norm;
		dirCamY[i] = y/norm;
		dirCamZ[i] = z/norm;
	}

	// =====================================================================================
	//  Pose
	// =====================================================================================

	public void setWorldToView( Se3_F64 worldToView ) {
		this.worldToView.setTo(worldToView);
	}

	// =====================================================================================
	//  Mesh ingest + BVH build  (heavy, camera-independent precompute)
	// =====================================================================================

	/// Verifies the mesh is triangulated, copies its triangles into the internal cache-friendly
	/// layout, and builds the BVH. This is the heavy, camera-independent precompute; do it once per
	/// mesh. Triangulate beforehand with [VertexMesh#toTriangles()].
	///
	/// @throws IllegalArgumentException if any face is not a triangle.
	public void setMesh( VertexMesh mesh ) {
		numTri = mesh.size();
		allocateTriangles(numTri);

		// Faces are exactly 3 corners, so triangle f owns corners [3f, 3f+3) and triangle index == f.
		// Keep every triangle (including any degenerate ones) so this identity holds; degenerate
		// triangles are harmless and get rejected at trace time by EPS_DET.
		var v = new Point3D_F64();
		for (int f = 0; f < numTri; f++) {
			int c = mesh.faceOffsets.get(f); // first corner of this face
			if (mesh.faceOffsets.get(f + 1) - c != 3)
				throw new IllegalArgumentException("All faces must be triangles. Call mesh.toTriangles() first.");

			mesh.vertexes.getCopy(vertexIndex(mesh, c), v);     double ax = v.x, ay = v.y, az = v.z;
			mesh.vertexes.getCopy(vertexIndex(mesh, c + 1), v); double bx = v.x, by = v.y, bz = v.z;
			mesh.vertexes.getCopy(vertexIndex(mesh, c + 2), v); double cx = v.x, cy = v.y, cz = v.z;

			V0X[f] = ax;        V0Y[f] = ay;        V0Z[f] = az;
			E1X[f] = bx - ax;   E1Y[f] = by - ay;   E1Z[f] = bz - az;
			E2X[f] = cx - ax;   E2Y[f] = cy - ay;   E2Z[f] = cz - az;
			cenX[f] = (ax + bx + cx)/3.0;
			cenY[f] = (ay + by + cy)/3.0;
			cenZ[f] = (az + bz + cz)/3.0;
		}

		buildBvh();
	}

	// Resolves a corner to its vertex-pool index, honoring the implicit (empty faceVertexes) mode.
	private static int vertexIndex( VertexMesh mesh, int corner ) {
		return mesh.faceVertexes.isEmpty() ? corner : mesh.faceVertexes.get(corner);
	}

	private void allocateTriangles( int n ) {
		V0X = new double[n]; V0Y = new double[n]; V0Z = new double[n];
		E1X = new double[n]; E1Y = new double[n]; E1Z = new double[n];
		E2X = new double[n]; E2Y = new double[n]; E2Z = new double[n];
		cenX = new double[n]; cenY = new double[n]; cenZ = new double[n];
		triIdx = new int[n];
		for (int i = 0; i < n; i++) triIdx[i] = i;

		int maxNodes = Math.max(1, 2*n);
		bMinX = new double[maxNodes]; bMinY = new double[maxNodes]; bMinZ = new double[maxNodes];
		bMaxX = new double[maxNodes]; bMaxY = new double[maxNodes]; bMaxZ = new double[maxNodes];
		bLeftFirst = new int[maxNodes];
		bCount = new int[maxNodes];
	}

	private void buildBvh() {
		if (numTri == 0) { nodesUsed = 0; return; }
		nodesUsed = 1;          // root is node 0; children allocated contiguously from 1
		bLeftFirst[0] = 0;
		bCount[0] = numTri;
		subdivide(0);
	}

	private void subdivide( int node ) {
		int first = bLeftFirst[node];
		int count = bCount[node];
		computeNodeBounds(node, first, count);

		if (count <= LEAF_SIZE) return; // leaf: leftFirst/count already correct

		// Split axis = widest extent of the node AABB.
		double ex = bMaxX[node] - bMinX[node];
		double ey = bMaxY[node] - bMinY[node];
		double ez = bMaxZ[node] - bMinZ[node];
		int axis = (ex >= ey && ex >= ez) ? 0 : (ey >= ez ? 1 : 2);

		// Object-median split via quickselect on the centroid along the chosen axis. This keeps the
		// tree balanced (depth ~log2 N) and never produces an empty side.
		int mid = first + count/2;
		quickselect(first, first + count - 1, mid, axis);
		int leftCount = mid - first;

		int left = nodesUsed++;
		int right = nodesUsed++;
		bLeftFirst[left] = first;       bCount[left] = leftCount;
		bLeftFirst[right] = mid;        bCount[right] = count - leftCount;
		bLeftFirst[node] = left;        bCount[node] = 0; // mark internal

		subdivide(left);
		subdivide(right);
	}

	private void computeNodeBounds( int node, int first, int count ) {
		double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
		double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
		for (int i = 0; i < count; i++) {
			int tri = triIdx[first + i];
			double ax = V0X[tri], ay = V0Y[tri], az = V0Z[tri];
			double bx = ax + E1X[tri], by = ay + E1Y[tri], bz = az + E1Z[tri];
			double cx = ax + E2X[tri], cy = ay + E2Y[tri], cz = az + E2Z[tri];
			minX = min3(minX, ax, bx, cx); maxX = max3(maxX, ax, bx, cx);
			minY = min3(minY, ay, by, cy); maxY = max3(maxY, ay, by, cy);
			minZ = min3(minZ, az, bz, cz); maxZ = max3(maxZ, az, bz, cz);
		}
		bMinX[node] = minX; bMinY[node] = minY; bMinZ[node] = minZ;
		bMaxX[node] = maxX; bMaxY[node] = maxY; bMaxZ[node] = maxZ;
	}

	// Hoare-style quickselect: partition triIdx[lo..hi] so position k holds its sorted element and
	// everything left of k has a smaller-or-equal centroid on the given axis.
	private void quickselect( int lo, int hi, int k, int axis ) {
		while (lo < hi) {
			double pivot = centroid(triIdx[(lo + hi) >>> 1], axis);
			int i = lo, j = hi;
			while (i <= j) {
				while (centroid(triIdx[i], axis) < pivot) i++;
				while (centroid(triIdx[j], axis) > pivot) j--;
				if (i <= j) { int tmp = triIdx[i]; triIdx[i] = triIdx[j]; triIdx[j] = tmp; i++; j--; }
			}
			if (k <= j) hi = j;
			else if (k >= i) lo = i;
			else break;
		}
	}

	private double centroid( int tri, int axis ) {
		return axis == 0 ? cenX[tri] : axis == 1 ? cenY[tri] : cenZ[tri];
	}

	// =====================================================================================
	//  Render
	// =====================================================================================

	public void render() {
		if (numTri == 0) throw new IllegalStateException("Mesh not set");
		if (width <= 0 || height <= 0) throw new IllegalStateException("Camera not set");

		depthImage.reshape(width, height);
		rgbImage.reshape(width, height);

		// Camera center in world: the point that maps to the view origin.
		worldToView.transformReverse(new Point3D_F64(0, 0, 0), camCenter);

		// Cache R (world->view). World direction of a camera-frame ray is R^T * dirCam.
		double[] R = worldToView.R.data;
		r00 = R[0]; r01 = R[1]; r02 = R[2];
		r10 = R[3]; r11 = R[4]; r12 = R[5];
		r20 = R[6]; r21 = R[7]; r22 = R[8];

		final double ox = camCenter.x, oy = camCenter.y, oz = camCenter.z;

		BoofConcurrency.loopFor(0, height, y -> {
			int[] stack = new int[STACK_SIZE];
			Hit hit = new Hit();
			int row = y*width;
			for (int x = 0; x < width; x++) {
				int pix = row + x;
				double dcx = dirCamX[pix], dcy = dirCamY[pix], dcz = dirCamZ[pix];

				// R^T * dcam  (rotation is isometric, so the world direction stays unit -> t is range)
				double dx = r00*dcx + r10*dcy + r20*dcz;
				double dy = r01*dcx + r11*dcy + r21*dcz;
				double dz = r02*dcx + r12*dcy + r22*dcz;

				hit.t = Double.POSITIVE_INFINITY;
				hit.tri = -1;
				trace(ox, oy, oz, dx, dy, dz, stack, hit);

				if (hit.tri >= 0) {
					depthImage.unsafe_set(x, y, (float)hit.t);
					rgbImage.set24(x, y, surfaceColor.surfaceRgb(hit.tri)); // tri index == face index
				} else {
					depthImage.unsafe_set(x, y, Float.NaN);
					rgbImage.set24(x, y, defaultColorRgb);
				}
			}
		});
	}

	// Closest-hit traversal. Descends the nearer child first and prunes any node whose entry
	// distance exceeds the best hit found so far.
	private void trace( double ox, double oy, double oz,
	                    double dx, double dy, double dz,
	                    int[] stack, Hit hit ) {
		// Avoid 0*inf NaN in the slab test by nudging exact-zero components.
		double idx = 1.0/(dx != 0 ? dx : 1e-300);
		double idy = 1.0/(dy != 0 ? dy : 1e-300);
		double idz = 1.0/(dz != 0 ? dz : 1e-300);

		int sp = 0;
		stack[sp++] = 0; // root
		while (sp > 0) {
			int node = stack[--sp];
			if (bCount[node] > 0) {
				int first = bLeftFirst[node];
				int end = first + bCount[node];
				for (int i = first; i < end; i++)
					intersectTri(triIdx[i], ox, oy, oz, dx, dy, dz, hit);
				continue;
			}
			int left = bLeftFirst[node];
			int right = left + 1;
			double tL = rayBoxEntry(left, ox, oy, oz, idx, idy, idz, hit.t);
			double tR = rayBoxEntry(right, ox, oy, oz, idx, idy, idz, hit.t);

			// Push farther first so the nearer child is processed next (better pruning).
			if (tL <= tR) {
				if (tR != Double.POSITIVE_INFINITY) stack[sp++] = right;
				if (tL != Double.POSITIVE_INFINITY) stack[sp++] = left;
			} else {
				if (tL != Double.POSITIVE_INFINITY) stack[sp++] = left;
				if (tR != Double.POSITIVE_INFINITY) stack[sp++] = right;
			}
		}
	}

	// Slab test. Returns the entry distance (clamped to >=0) if the ray hits the box within [0,bestT),
	// otherwise +inf. invD components are precomputed.
	private double rayBoxEntry( int node, double ox, double oy, double oz,
	                            double idx, double idy, double idz, double bestT ) {
		double t1 = (bMinX[node] - ox)*idx, t2 = (bMaxX[node] - ox)*idx;
		double tmin = Math.min(t1, t2), tmax = Math.max(t1, t2);

		t1 = (bMinY[node] - oy)*idy; t2 = (bMaxY[node] - oy)*idy;
		tmin = Math.max(tmin, Math.min(t1, t2));
		tmax = Math.min(tmax, Math.max(t1, t2));

		t1 = (bMinZ[node] - oz)*idz; t2 = (bMaxZ[node] - oz)*idz;
		tmin = Math.max(tmin, Math.min(t1, t2));
		tmax = Math.min(tmax, Math.max(t1, t2));

		if (tmax >= Math.max(tmin, 0.0) && tmin < bestT)
			return Math.max(tmin, 0.0);
		return Double.POSITIVE_INFINITY;
	}

	// Moller-Trumbore. Updates hit if this triangle is a closer valid intersection.
	// Two-sided unless cullBackFaces is set.
	private void intersectTri( int tri, double ox, double oy, double oz,
	                           double dx, double dy, double dz, Hit hit ) {
		double e1x = E1X[tri], e1y = E1Y[tri], e1z = E1Z[tri];
		double e2x = E2X[tri], e2y = E2Y[tri], e2z = E2Z[tri];

		// pvec = D x e2
		double px = dy*e2z - dz*e2y;
		double py = dz*e2x - dx*e2z;
		double pz = dx*e2y - dy*e2x;

		double det = e1x*px + e1y*py + e1z*pz;
		// Degenerate triangles are always rejected. When cullBackFaces is set we additionally reject
		// back faces (det <= 0). 'cullBackFaces' is constant across the render, so this branch is
		// fully predicted; it changes which surfaces are visible, not throughput. Which side is
		// "front" depends on vertex winding -- meshes with inconsistent winding (e.g. disparity
		// derived) will show holes when culled, which is why two-sided is the default.
		if (cullBackFaces) {
			if (det < EPS_DET) return;                   // reject back faces and degenerate
		} else {
			if (det > -EPS_DET && det < EPS_DET) return; // reject degenerate only
		}
		double invDet = 1.0/det;

		// tvec = O - v0
		double tx = ox - V0X[tri];
		double ty = oy - V0Y[tri];
		double tz = oz - V0Z[tri];

		double u = (tx*px + ty*py + tz*pz)*invDet;
		if (u < 0.0 || u > 1.0) return;

		// qvec = tvec x e1
		double qx = ty*e1z - tz*e1y;
		double qy = tz*e1x - tx*e1z;
		double qz = tx*e1y - ty*e1x;

		double v = (dx*qx + dy*qy + dz*qz)*invDet;
		if (v < 0.0 || u + v > 1.0) return;

		double t = (e2x*qx + e2y*qy + e2z*qz)*invDet;
		if (t > EPS_T && t < hit.t) {
			hit.t = t;
			hit.tri = tri;
		}
	}

	// =====================================================================================
	//  Downstream helper
	// =====================================================================================

	/// Reconstructs the world-frame 3D point seen at a pixel using the rendered range image and the
	/// pose from the last render(). Returns false if that pixel had no intersection. This is exact
	/// for any FOV because it reuses the same unit ray that produced the range.
	public boolean pixelTo3D( int x, int y, Point3D_F64 out ) {
		float range = depthImage.unsafe_get(x, y);
		if (Float.isNaN(range)) return false;
		int pix = y*width + x;
		double dcx = dirCamX[pix], dcy = dirCamY[pix], dcz = dirCamZ[pix];
		double dx = r00*dcx + r10*dcy + r20*dcz;
		double dy = r01*dcx + r11*dcy + r21*dcz;
		double dz = r02*dcx + r12*dcy + r22*dcz;
		out.x = camCenter.x + range*dx;
		out.y = camCenter.y + range*dy;
		out.z = camCenter.z + range*dz;
		return true;
	}

	// =====================================================================================
	//  Small helpers / types
	// =====================================================================================

	private static double min3( double a, double b, double c, double d ) {
		return Math.min(a, Math.min(b, Math.min(c, d)));
	}
	private static double max3( double a, double b, double c, double d ) {
		return Math.max(a, Math.max(b, Math.max(c, d)));
	}

	/// Mutable closest-hit record reused per row to avoid allocation.
	private static final class Hit {
		double t;
		int tri;
	}
}