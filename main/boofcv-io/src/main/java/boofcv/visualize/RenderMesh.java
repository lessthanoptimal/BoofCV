/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.InterleavedU8;
import boofcv.struct.mesh.VertexMesh;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import lombok.Getter;

/**
 * Renders a 3D mesh and computes a depth image.
 *
 * TODO describe how to use. i.e. set up intrinsic
 * TODO add ability to set color of mesh
 * TODO ste brightness using dot product or something
 * TODO describe algorithm more
 *
 * @author Peter Abeles
 */
public class RenderMesh {
	/** What color background pixels are set to by default in RGBA. Default value is transparent black */
	public int defaultColorRgba = 0x00000000;

	/** Rendered depth image */
	public @Getter final GrayF32 depthImage = new GrayF32(1, 1);

	/** Rendered color image. Pixels are in RGBA format. */
	public @Getter final InterleavedU8 rgbImage = new InterleavedU8(1, 1, 4);

	/** Pinhole camera model needed to go from depth image to 3D point */
	public @Getter final CameraPinhole intrinsics = new CameraPinhole();

	/** Transform from world (what the mesh is in) to the camera view */
	public Se3_F64 worldToView = new Se3_F64();

	// Workspace variables
	Point3D_F64 camera = new Point3D_F64();
	Point2D_F64 point = new Point2D_F64();
	Polygon2D_F64 polygon = new Polygon2D_F64();
	Rectangle2D_I32 aabb = new Rectangle2D_I32();

	public void render( VertexMesh mesh ) {
		// Sanity check to see if intrinsics has been configured
		BoofMiscOps.checkTrue(intrinsics.width > 0 && intrinsics.height > 0);

		// Initialize output images
		depthImage.reshape(intrinsics.width, intrinsics.height);
		rgbImage.reshape(intrinsics.width, intrinsics.height);
		ImageMiscOps.fill(rgbImage, defaultColorRgba);
		ImageMiscOps.fill(depthImage, Float.NaN);

		final int width = intrinsics.width;
		final int height = intrinsics.height;
		final double fx = intrinsics.fx;
		final double fy = intrinsics.fy;
		final double cx = intrinsics.cx;
		final double cy = intrinsics.cy;

		for (int shapeIdx = 1; shapeIdx < mesh.offsets.size; shapeIdx++) {
			// First and last point in the polygon
			int idx0 = mesh.offsets.get(shapeIdx - 1);
			int idx1 = mesh.offsets.get(shapeIdx);

			// skip pathological case
			if (idx0 >= idx1)
				continue;

			// Project points on the shape onto the image and store in polygon
			polygon.vertexes.reset().reserve(idx1 - idx0);
			int countBehind = 0;
			for (int i = idx0; i < idx1; i++) {
				Point3D_F64 world = mesh.vertexes.getTemp(mesh.indexes.get(i));
				worldToView.transform(world, camera);

				// If the entire mesh is behind the camera then it can be skipped
				if (camera.z <= 0)
					countBehind++;

				// normalized image coordinates
				double normX = camera.x/camera.z;
				double normY = camera.y/camera.z;

				// Project onto the image
				double pixelX = normX*fx + cx;
				double pixelY = normY*fy + cy;

				polygon.vertexes.grow().setTo(pixelX, pixelY);
			}

			// Skip if not visible
			if (countBehind == polygon.size())
				continue;

			// Compute the pixels which might be able to see polygon
			computeBoundingBox(width, height, polygon, aabb);

			projectSurfaceOntoImage(mesh, polygon, idx0);
		}
	}

	private static void computeBoundingBox( int width, int height, Polygon2D_F64 polygon, Rectangle2D_I32 aabb ) {
		// Find the pixel bounds that contain this polygon
		double x0 = polygon.vertexes.get(0).x;
		double y0 = polygon.vertexes.get(0).y;
		double x1 = x0;
		double y1 = y0;

		for (int i = 1; i < polygon.size(); i++) {
			Point2D_F64 p = polygon.get(i);
			if (p.x < x0)
				x0 = p.x;
			else if (p.x > x1)
				x1 = p.x;

			if (p.y < y0)
				y0 = p.y;
			else if (p.y > y1)
				y1 = p.y;
		}

		// Make sure the bounding box is within the image
		aabb.x0 = (int)Math.max(0, x0);
		aabb.y0 = (int)Math.max(0, y0);
		aabb.x1 = (int)Math.ceil(Math.min(width, x1 + 1));
		aabb.y1 = (int)Math.ceil(Math.min(height, y1 + 1));
	}

	private void projectSurfaceOntoImage( VertexMesh mesh, Polygon2D_F64 polygon, int idx0 ) {
		// TODO temp hack. Best way is to find the distance to the 3D polygon at this point. Instead we will
		// use the depth of the first point
		Point3D_F64 world = mesh.vertexes.getTemp(mesh.indexes.get(idx0));
		worldToView.transform(world, camera);

		float depth = (float)camera.z;

		// hard code color for now.
		int color = 0xFFFF0000;

		// Go through all pixels and see if the points are inside the polygon. If so
		for (int pixelY = aabb.y0; pixelY < aabb.y1; pixelY++) {
			for (int pixelX = aabb.x0; pixelX < aabb.x1; pixelX++) {
				point.setTo(pixelX, pixelY);
				if (!Intersection2D_F64.containsConvex(polygon, point))
					continue;

				// See if this is the closest point appearing at this pixel
				float pixelDepth = depthImage.unsafe_get(pixelX, pixelY);
				if (!Float.isNaN(pixelDepth) && depth > pixelDepth) {
					continue;
				}

				// Update depth and image
				// Make sure the alpha channel is set to 100% in RGBA format
				depthImage.unsafe_set(pixelX, pixelY, depth);
				rgbImage.set32(pixelX, pixelY, color);
			}
		}
	}
}
