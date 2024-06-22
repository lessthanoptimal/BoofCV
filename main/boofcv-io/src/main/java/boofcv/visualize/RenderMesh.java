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

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.image.InterleavedU8;
import boofcv.struct.mesh.VertexMesh;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Polygon2D_F32;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;
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
 *     <li>{@link #setCamera(CameraPinholeBrown)} This must be set before use.</li>
 *     <li>{@link #worldToView} Transform from work to the current view.</li>
 * </ul>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class RenderMesh implements VerbosePrint {
	/** What color background pixels are set to by default in RGBA. Default value is white */
	public @Getter @Setter int defaultColorRgba = 0xFFFFFF;

	/** Used to change what color a surface is. By default, it's red. */
	public @Getter @Setter SurfaceColor surfaceColor = ( surface ) -> 0xFF0000;

	/** Rendered depth image. Values with no depth information are set to NaN. */
	public @Getter final GrayF32 depthImage = new GrayF32(1, 1);

	/** Rendered color image. Pixels are in RGBA format. */
	public @Getter final InterleavedU8 rgbImage = new InterleavedU8(1, 1, 3);

	/** Transform from world (what the mesh is in) to the camera view */
	public @Getter final Se3_F64 worldToView = new Se3_F64();

	/** If true then a polygon will only be rendered if the surface normal is pointed towards the camera */
	public @Getter @Setter boolean checkFaceNormal = false;

	/** If true it will always use the colorizer, even if there is texture information */
	public @Getter @Setter boolean forceColorizer = false;

	// Image for texture mapping
	private InterleavedU8 textureImage = new InterleavedU8(1, 1, 3);
	private InterpolatePixelMB<InterleavedU8> textureInterp = FactoryInterpolation.bilinearPixelMB(textureImage, BorderType.EXTENDED);
	private float[] textureValues = new float[3];

	// Intrinsic camera model
	protected Point2Transform2_F64 pixelToNorm;
	protected Point2Transform2_F64 normToPixel;
	protected ImageDimension resolution = new ImageDimension();

	//---------- Workspace variables
	private final Point3D_F64 camera = new Point3D_F64();
	private final Point2D_F64 point = new Point2D_F64();

	// mesh in camera reference frame
	private final DogArray<Point3D_F64> meshCam = new DogArray<>(Point3D_F64::new);
	// Mesh projected onto the image
	private final Polygon2D_F64 polygonProj = new Polygon2D_F64();
	// Vertex of polygon in the texture image
	private final Polygon2D_F32 polygonTex = new Polygon2D_F32();
	// Axis aligned bonding box
	final Rectangle2D_I32 aabb = new Rectangle2D_I32();
	// Workspace for a sub-triangle in the polygon
	private final Polygon2D_F64 workTri = new Polygon2D_F64(3);

	@Nullable PrintStream verbose = null;

	public void setTextureImage( InterleavedU8 textureImage ) {
		this.textureImage = textureImage;
		textureInterp.setImage(textureImage);
		textureValues = new float[textureImage.numBands];
	}

	/**
	 * Specifies a pinhole camera from its fov and image shape.
	 *
	 * @param hfov Horizontal field of view. Degrees.
	 */
	public void setCamera( double hfov, int width, int height ) {
		var model = new CameraPinhole();
		PerspectiveOps.createIntrinsic(width, height, hfov, -1, model);
		var factory = new LensDistortionPinhole(model);
		setCamera(factory, model.width, model.height);
	}

	/**
	 * Specifies the intrinsic camera model
	 */
	public void setCamera( CameraPinholeBrown model ) {
		var factory = new LensDistortionBrown(model);
		setCamera(factory, model.width, model.height);
	}

	/**
	 * Specifies the intrinsic camera model
	 */
	public void setCamera( LensDistortionNarrowFOV factory, int width, int height ) {
		setCamera(factory.undistort_F64(true, false),
				factory.distort_F64(false, true),
				width, height);
	}

	/**
	 * Specifies the intrinsic camera model
	 */
	public void setCamera( Point2Transform2_F64 pixelToNorm,
						   Point2Transform2_F64 normToPixel,
						   int width, int height ) {
		this.pixelToNorm = pixelToNorm;
		this.normToPixel = normToPixel;
		this.resolution.setTo(width, height);
	}

	/**
	 * Renders the mesh onto an image. Produces an RGB image and depth image. Must have configured
	 * {@link #setCamera(CameraPinholeBrown)} already and set {@link #worldToView}.
	 *
	 * @param mesh The mesh that's going to be rendered.
	 */
	public void render( VertexMesh mesh ) {
		// Sanity check to see if intrinsics has been configured
		BoofMiscOps.checkTrue(resolution.width > 0 && resolution.height > 0, "Intrinsics not set");

		// Make sure there are normals if it's configured to use them
		if (checkFaceNormal && mesh.faceNormals.size() == 0)
			mesh.computeFaceNormals();

		// Initialize output images
		initializeImages();

		// Keep track of how many meshes were rendered
		int shapesRenderedCount = 0;

		var worldCamera = new Point3D_F64();
		worldToView.transformReverse(worldCamera, worldCamera);

		// Decide if it should texture map or user a per shape color
		final boolean useColorizer = forceColorizer || mesh.texture.size() == 0;

		for (int shapeIdx = 1; shapeIdx < mesh.faceOffsets.size; shapeIdx++) {
			// First and last point in the polygon
			final int idx0 = mesh.faceOffsets.get(shapeIdx - 1);
			final int idx1 = mesh.faceOffsets.get(shapeIdx);

			// skip pathological case
			if (idx0 >= idx1)
				continue;

			// Project points on the shape onto the image and store in polygon
			polygonProj.vertexes.reset().reserve(idx1 - idx0);
			meshCam.reset().reserve(idx1 - idx0);

			if (!useColorizer) {
				mesh.getTexture(shapeIdx - 1, polygonTex.vertexes);
			}

			// Prune using normal vector
			if (checkFaceNormal) {
				if (!isFrontVisible(mesh, shapeIdx - 1, idx0, worldCamera)) continue;
			}

			boolean behindCamera = false;
			for (int i = idx0; i < idx1; i++) {
				Point3D_F64 world = mesh.vertexes.getTemp(mesh.faceVertexes.get(i));
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

				// Compute pixel coordinates of this observation
				normToPixel.compute(normX, normY, polygonProj.vertexes.grow());
				meshCam.grow().setTo(camera);
			}

			// Skip if not visible
			if (behindCamera)
				continue;

			if (useColorizer) {
				projectSurfaceColor(meshCam, polygonProj, shapeIdx - 1);
			} else {
				projectSurfaceTexture(meshCam, polygonProj, polygonTex);
			}

			shapesRenderedCount++;
		}

		if (verbose != null) verbose.println("total shapes rendered: " + shapesRenderedCount);
	}

	/**
	 * Use the normal vector to see if the front of the mesh is visible. If it's not visible we can skip it
	 *
	 * @param worldCamera Location of the camera in current view in world coordinates
	 * @return true if visible
	 */
	static boolean isFrontVisible( VertexMesh mesh, int shapeIdx, int idx0, Point3D_F64 worldCamera ) {
		// Get normal in world coordinates
		Point3D_F32 normal = mesh.getFaceNormalTmp(shapeIdx);

		// vector from the camera to a vertex
		Point3D_F64 v1 = mesh.vertexes.getTemp(mesh.faceVertexes.get(idx0));
		v1.x -= worldCamera.x;
		v1.y -= worldCamera.y;
		v1.z -= worldCamera.z;

		// compute the dot product
		double dot = v1.x*normal.x + v1.y*normal.y + v1.z*normal.z;

		// Don't render if we are viewing it from behind
		return dot < 0.0;
	}

	void initializeImages() {
		depthImage.reshape(resolution.width, resolution.height);
		rgbImage.reshape(resolution.width, resolution.height);
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
	void projectSurfaceColor( FastAccess<Point3D_F64> mesh, Polygon2D_F64 polyProj, int shapeIdx ) {
		// TODO compute the depth at each pixel
		float depth = (float)mesh.get(0).z;

		// TODO look at vertexes and get min/max depth. Use that to quickly reject pixels based on depth without
		//      convex intersection or computing the depth at that pixel on this surface

		// The entire surface will have one color
		int color = surfaceColor.surfaceRgb(shapeIdx);

		computeBoundingBox(resolution.width, resolution.height, polyProj, aabb);

		// Go through all pixels and see if the points are inside the polygon. If so
		for (int pixelY = aabb.y0; pixelY < aabb.y1; pixelY++) {
			for (int pixelX = aabb.x0; pixelX < aabb.x1; pixelX++) {
				// See if this is the closest point appearing at this pixel
				float pixelDepth = depthImage.unsafe_get(pixelX, pixelY);
				if (!Float.isNaN(pixelDepth) && depth >= pixelDepth) {
					continue;
				}

				point.setTo(pixelX, pixelY);
				if (!Intersection2D_F64.containsConvex(polyProj, point))
					continue;

				// Update depth and image
				// Make sure the alpha channel is set to 100% in RGBA format
				depthImage.unsafe_set(pixelX, pixelY, depth);
				rgbImage.set24(pixelX, pixelY, color);
			}
		}
	}

	/**
	 * Projection with texture mapping. Breaks the polygon up into triangles and uses Barycentric coordinates to
	 * map pixels to textured mapped coordinates.
	 *
	 * @param mesh 3D location of vertexes in the mesh in the view's coordinate system
	 * @param polyProj Projected pixels of mesh
	 * @param polyText Texture coordinates of the mesh
	 */
	void projectSurfaceTexture( FastAccess<Point3D_F64> mesh, Polygon2D_F64 polyProj, Polygon2D_F32 polyText ) {

		// Scale factor to normalize image pixels from 0 to 1.0
		float scale = Math.max(resolution.width, resolution.height);

		// If the mesh has more than 3 sides, break it up into triangles using the first vertex as a pivot
		// This works because the mesh has to be convex
		for (int vertC = 2; vertC < polyProj.size(); vertC++) {
			int vertA = 0;
			int vertB = vertC - 1;

			// Get depth at each vertex
			float Z0 = (float)mesh.get(vertA).z;
			float Z1 = (float)mesh.get(vertB).z;
			float Z2 = (float)mesh.get(vertC).z;

			// Scale the pixel coordinates for numerical issues
			float ax = (float)polyProj.get(vertA).x/scale;
			float ay = (float)polyProj.get(vertA).y/scale;
			float bx = (float)polyProj.get(vertB).x/scale;
			float by = (float)polyProj.get(vertB).y/scale;
			float cx = (float)polyProj.get(vertC).x/scale;
			float cy = (float)polyProj.get(vertC).y/scale;

			float area = edgeFunction(ax, ay, bx, by, cx, cy);

			// Compute coordinate on texture image
			Point2D_F32 t0 = polyText.get(vertA);
			Point2D_F32 t1 = polyText.get(vertB);
			Point2D_F32 t2 = polyText.get(vertC);

			// Do the polygon intersection with the triangle in question only
			workTri.get(0).setTo(polyProj.get(vertA));
			workTri.get(1).setTo(polyProj.get(vertB));
			workTri.get(2).setTo(polyProj.get(vertC));

			// TODO look at vertexes and get min/max depth. Use that to quickly reject pixels based on depth without
			//      convex intersection or computing the depth at that pixel on this surface

			computeBoundingBox(resolution.width, resolution.height, workTri, aabb);

			// Go through all pixels and see if the points are inside the polygon. If so
			for (int pixelY = aabb.y0; pixelY < aabb.y1; pixelY++) {
				float py = pixelY/scale;

				for (int pixelX = aabb.x0; pixelX < aabb.x1; pixelX++) {
					float px = pixelX/scale;

					point.setTo(pixelX, pixelY);
					if (!Intersection2D_F64.containsConvex(workTri, point))
						continue;

					// See if this is the closest point appearing at this pixel
					float pixelDepth = depthImage.unsafe_get(pixelX, pixelY);

//					// Compute Barycentric
					float alpha = edgeFunction(bx, by, cx, cy, px, py)/area;
					float beta = edgeFunction(cx, cy, ax, ay, px, py)/area;
					float gamma = edgeFunction(ax, ay, bx, by, px, py)/area;

					// depth of the mesh at this point
					float depth = alpha*Z0 + beta*Z1 + gamma*Z2;

					if (!Float.isNaN(pixelDepth) && depth >= pixelDepth) {
						continue;
					}

					// Perspective Correct Interpolation
					// the naive interpolation (without 1/z) is affine and has significant distortion.
					// https://en.wikipedia.org/wiki/Texture_mapping

					float oneOverW = alpha/Z0 + beta/Z1 + gamma/Z2;
					float u = (alpha*t0.x/Z0 + beta*t1.x/Z1 + gamma*t2.x/Z2)/oneOverW;
					float v = (alpha*t0.y/Z0 + beta*t1.y/Z1 + gamma*t2.y/Z2)/oneOverW;

					// back to pixel coordinates in the texture image
					float pixTexX = u*(textureImage.width - 1);
					float pixTexY = (1.0f - v)*(textureImage.height - 1);

					int color = interpolateTextureRgb(pixTexX, pixTexY);

					// Update depth and image
					// Make sure the alpha channel is set to 100% in RGBA format
					depthImage.unsafe_set(pixelX, pixelY, depth);
					rgbImage.set24(pixelX, pixelY, color);
				}
			}
		}
	}

	private static float edgeFunction( float x0, float y0, float x1, float y1, float x2, float y2 ) {
		return (x2 - x0)*(y1 - y0) - (y2 - y0)*(x1 - x0);
	}

	/**
	 * Gets the RGB color using interpolation at the specified pixel coordinate in the texture image
	 */
	int interpolateTextureRgb( float px, float py ) {
		textureInterp.get(px, py, textureValues);
		int r = (int)(textureValues[0] + 0.5f);
		int g = (int)(textureValues[1] + 0.5f);
		int b = (int)(textureValues[2] + 0.5f);
		return (r << 16) | (g << 8) | b;
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
