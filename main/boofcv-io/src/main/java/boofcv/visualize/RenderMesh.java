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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.InterleavedU8;
import boofcv.struct.mesh.VertexMesh;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Set;

/**
 * Simple algorithm that renders a 3D mesh and computes a depth image. This rendering engine is fairly basic and makes
 * the following assumptions: each shape has a single color and all colors are opaque. What's configurable:
 *
 * <ul>
 *     <li>{@link #defaultColorRgba} Specifies what color the background is.</li>
 *     <li>{@link #surfaceColor} Function which returns the color of a shape. The shape's index is passed.</li>
 *     <li>{@link #intrinsics} Camera intrinsics. This must be set before use.</li>
 *     <li>{@link #worldToView} Transform from work to the current view.</li>
 * </ul>
 *
 * @author Peter Abeles
 */
public class RenderMesh implements VerbosePrint {
	/** What color background pixels are set to by default in RGBA. Default value is white */
	public @Getter @Setter int defaultColorRgba = 0xFFFFFF;

	/** Used to change what color a surface is. By default, it's red. */
	public @Getter @Setter SurfaceColor surfaceColor = ( surface ) -> 0xFF0000;

	/** Rendered depth image. Values with no depth information are set to NaN. */
	public @Getter final GrayF32 depthImage = new GrayF32(1, 1);

	/** Rendered color image. Pixels are in RGBA format. */
	public @Getter final InterleavedU8 rgbImage = new InterleavedU8(1, 1, 3);

	/** Pinhole camera model needed to go from depth image to 3D point */
	public @Getter final CameraPinhole intrinsics = new CameraPinhole();

	/** Transform from world (what the mesh is in) to the camera view */
	public @Getter final Se3_F64 worldToView = new Se3_F64();

	// Workspace variables
	private final Point3D_F64 camera = new Point3D_F64();
	private final Point2D_F64 point = new Point2D_F64();
	private final Polygon2D_F64 polygon = new Polygon2D_F64();
	final Rectangle2D_I32 aabb = new Rectangle2D_I32();

	@Nullable PrintStream verbose = null;

	/**
	 * Renders the mesh onto an image. Produces an RGB image and depth image. Must have configured
	 * {@link #intrinsics} already and set {@link #worldToView}.
	 *
	 * @param mesh The mesh that's going to be rendered.
	 */
	public void render( VertexMesh mesh ) {
		// Sanity check to see if intrinsics has been configured
		BoofMiscOps.checkTrue(intrinsics.width > 0 && intrinsics.height > 0, "Intrinsics not set");

		// Initialize output images
		initializeImages();

		final int width = intrinsics.width;
		final int height = intrinsics.height;
		final double fx = intrinsics.fx;
		final double fy = intrinsics.fy;
		final double cx = intrinsics.cx;
		final double cy = intrinsics.cy;

		int shapesRenderedCount = 0;

		for (int shapeIdx = 1; shapeIdx < mesh.offsets.size; shapeIdx++) {
			// First and last point in the polygon
			final int idx0 = mesh.offsets.get(shapeIdx - 1);
			final int idx1 = mesh.offsets.get(shapeIdx);

			// skip pathological case
			if (idx0 >= idx1)
				continue;

			// Project points on the shape onto the image and store in polygon
			polygon.vertexes.reset().reserve(idx1 - idx0);
			boolean behindCamera = false;
			for (int i = idx0; i < idx1; i++) {
				Point3D_F64 world = mesh.vertexes.getTemp(mesh.indexes.get(i));
				worldToView.transform(world, camera);

				// If any part is behind the camera skip it. While not ideal this keeps the code simple,
				// speeds it up a lot, and removes weird rendering artifacts
				if (camera.z <= 0) {
					behindCamera = true;
					break;
				}

				// normalized image coordinates
				double normX = camera.x/camera.z;
				double normY = camera.y/camera.z;

				// Project onto the image
				double pixelX = normX*fx + cx;
				double pixelY = normY*fy + cy;

				polygon.vertexes.grow().setTo(pixelX, pixelY);
			}

			// Skip if not visible
			if (behindCamera)
				continue;

			// Compute the pixels which might be able to see polygon
			computeBoundingBox(width, height, polygon, aabb);

			projectSurfaceOntoImage(mesh, polygon, shapeIdx-1);

			shapesRenderedCount++;
		}

		if (verbose != null ) verbose.println("total shapes rendered: " + shapesRenderedCount);
	}

	void initializeImages() {
		depthImage.reshape(intrinsics.width, intrinsics.height);
		rgbImage.reshape(intrinsics.width, intrinsics.height);
		ImageMiscOps.fill(rgbImage, defaultColorRgba);
		ImageMiscOps.fill(depthImage, Float.NaN);
	}

	/**
	 * Computes the AABB for the polygon inside the image.
	 *
	 * @param width (Input) image width
	 * @param height (Input) image height
	 * @param polygon (Input) projected polygon onto image
	 * @param aabb (Output) Found AABB clipped to be inside the image.
	 */
	static void computeBoundingBox( int width, int height, Polygon2D_F64 polygon, Rectangle2D_I32 aabb ) {
		UtilPolygons2D_F64.bounding(polygon, aabb);

		// Make sure the bounding box is within the image
		aabb.x0 = Math.max(0, aabb.x0);
		aabb.y0 = Math.max(0, aabb.y0);
		aabb.x1 = Math.min(width, aabb.x1);
		aabb.y1 = Math.min(height, aabb.y1);
	}

	/**
	 * Renders the polygon onto the image as a single color. The AABB that the polygon is contained inside
	 * is searched exhaustively. If the projected 2D polygon contains a pixels and the polygon is closer than
	 * the current depth of the pixel it is rendered there and the depth image is updated.
	 */
	void projectSurfaceOntoImage( VertexMesh mesh, Polygon2D_F64 polygon, int shapeIdx ) {
		// TODO temp hack. Best way is to find the distance to the 3D polygon at this point. Instead we will
		// use the depth of the first point.
		//
		// IDEA: Use a homography to map location on 2D polygon to 3D polygon, then rotate just the Z to get
		//       local depth on the surface.
		int vertexIndex = mesh.indexes.get(mesh.offsets.data[shapeIdx]);
		Point3D_F64 world = mesh.vertexes.getTemp(vertexIndex);
		worldToView.transform(world, camera);

		float depth = (float)camera.z;

		// TODO look at vertexes and get min/max depth. Use that to quickly reject pixels based on depth without
		//      convex intersection or computing the depth at that pixel on this surface

		// The entire surface will have one color
		int color = surfaceColor.surfaceRgb(shapeIdx);

		// Go through all pixels and see if the points are inside the polygon. If so
		for (int pixelY = aabb.y0; pixelY < aabb.y1; pixelY++) {
			for (int pixelX = aabb.x0; pixelX < aabb.x1; pixelX++) {
				// See if this is the closest point appearing at this pixel
				float pixelDepth = depthImage.unsafe_get(pixelX, pixelY);
				if (!Float.isNaN(pixelDepth) && depth >= pixelDepth) {
					continue;
				}

				point.setTo(pixelX, pixelY);
				if (!Intersection2D_F64.containsConvex(polygon, point))
					continue;

				// Update depth and image
				// Make sure the alpha channel is set to 100% in RGBA format
				depthImage.unsafe_set(pixelX, pixelY, depth);
				rgbImage.set24(pixelX, pixelY, color);
			}
		}
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		verbose = BoofMiscOps.addPrefix(this, out);
	}

	@FunctionalInterface
	public interface SurfaceColor {
		/**
		 * Returns RGB color of the specified surface
		 */
		int surfaceRgb( int which );
	}
}
