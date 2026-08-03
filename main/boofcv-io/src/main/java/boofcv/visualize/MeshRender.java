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

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.distort.NarrowPixelToSphere_F64;
import boofcv.alg.distort.SphereToNarrowPixel_F64;
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.Point2Transform3_F64;
import boofcv.struct.distort.Point3Transform2_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.InterleavedU8;
import boofcv.struct.mesh.VertexMesh;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.VerbosePrint;
import org.ddogleg.util.VerboseUtils;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Set;

/// High level API for rendering a mesh onto a camera view. The mesh can be rendered using a texture map or
/// using [SurfaceColor]. The rendered image is [InterleavedU8] in RGBA format. Specific implementation assume
/// that the mesh is set once but camera views change and are optimized accordingly.
///
///   - [#defaultColorRgb] Specifies what color the background is.
///   - [#surfaceColor] Function which returns the color of a shape. The shape's index is passed.
///   - [#setCamera(Point2Transform3_F64, Point3Transform2_F64, int, int)] This must be set before use.
///   - [#setWorldToView] Transform from work to the current view.
///
public abstract class MeshRender implements VerbosePrint {
	/// What color background pixels are set to by default in RGBA. Default value is white
	public @Getter @Setter int defaultColorRgb = 0xFFFFFF;

	/// Used to change what color a surface is. By default, it's red.
	public @Getter @Setter SurfaceColor surfaceColor = which -> 0xFF0000;

	/// Rendered depth image. Values with no depth information are set to NaN. How depth is defined is implementation
	/// specific. For example, it could be the z-buffer (distance along z-axis) or Euclidean distance from
	/// camera center.
	public @Getter final GrayF32 depthImage = new GrayF32(1, 1);

	/// Rendered color image. Pixels are in RGBA format.
	public @Getter final InterleavedU8 renderedImage = new InterleavedU8(1, 1, 3);

	/// If false (default) faces are visible from both sides. If true then it will compute and add normals to the mesh.
	public @Getter @Setter boolean cullBackFaces = false;

	/// If true it will always use the colorizer, even if there is texture information
	public @Getter @Setter boolean forceColorizer = false;

	@Nullable PrintStream verbose = null;

	/// Used to retrieve a copy of the current world-to-view transform
	public abstract Se3_F64 getWorldToView( @Nullable Se3_F64 output );

	/// Changes the world to view transform
	public abstract void setWorldToView( Se3_F64 worldToView );

	public abstract void setTextureImage( InterleavedU8 textureImage );

	/// General camera model that supports > 180 FOV
	///
	/// @param width Rendered image width
	/// @param height Rendered image height
	public abstract void setCamera( Point2Transform3_F64 pixelToPointing,
	                                Point3Transform2_F64 pointingToPixel,
	                                int width, int height );

	/// Convenience for narrow-FOV models that expose pixel -> normalized image coordinates
	public void setCamera( LensDistortionNarrowFOV model, int width, int height ) {
		var pixelToPointing = new NarrowPixelToSphere_F64(model.undistort_F64(true, false));
		var pointingToPixel = new SphereToNarrowPixel_F64(model.distort_F64(false, true));
		setCamera(pixelToPointing, pointingToPixel, width, height);
	}

	/// Specifies a pinhole camera from its fov and image shape.
	///
	/// @param hfov Horizontal field of view. Degrees.
	public void setCamera( double hfov, int width, int height ) {
		var model = new CameraPinhole();
		PerspectiveOps.createIntrinsic(width, height, hfov, -1, model);
		setCamera(new LensDistortionPinhole(model), model.width, model.height);
	}

	public void setCamera( CameraPinhole pinhole ) {
		setCamera(new LensDistortionPinhole(pinhole), pinhole.width, pinhole.height);
	}

	/// Passes in the mesh which is being viewed. This operation can be expensive or very fast depending on how
	/// much is precomputed. Must be called again whenever the mesh has changed.
	///
	/// An implementation is allowed to keep a reference to the mesh rather than copy it. Do not modify
	/// while rendering.
	public abstract void setMesh( VertexMesh mesh );

	/// Renders the mesh using the provided camera model and view location
	public abstract void render();

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		verbose = VerboseUtils.addPrefix(this, out);
	}

	public interface SurfaceColor {
		/// Called whenever the camera moves
		default void setWorldToView( Se3_F64 worldToCamera ) {}

		/// Returns RGB color of the specified surface
		int surfaceRgb( int which );
	}

	public static MeshRender createType(Type type) {
		return switch (type) {
			case RASTER -> new MeshRasterizer();
			case RAY_TRACE -> new MeshRayTracer();
		};
	}

	public enum Type {
		RASTER,
		RAY_TRACE,
	}
}
