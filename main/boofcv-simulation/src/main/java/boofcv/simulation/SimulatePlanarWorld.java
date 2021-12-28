/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.simulation;

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.distort.LensDistortionWideFOV;
import boofcv.alg.distort.NarrowPixelToSphere_F64;
import boofcv.alg.distort.SphereToNarrowPixel_F64;
import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.distort.universal.LensDistortionUniversalOmni;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.distort.Point2Transform3_F64;
import boofcv.struct.distort.Point3Transform2_F64;
import boofcv.struct.image.GrayF32;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.UtilShape3D_F64;
import georegression.metric.Intersection3D_F64;
import georegression.struct.line.LineParametric3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates a scene composed of planar objects. The camera is distorted using the provided camera model.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class SimulatePlanarWorld {

	GrayF32 output = new GrayF32(1, 1);
	GrayF32 depthMap = new GrayF32(1, 1);

	List<SurfaceRect> scene = new ArrayList<>();

	Point2Transform3_F64 pixelTo3;
	Point3Transform2_F64 sphereToPixel;

	Se3_F64 worldToCamera = new Se3_F64();

	Point3D_F64 p3 = new Point3D_F64();
	float[] pointing = new float[0];

	float background = 0;

	// work space
	Point2D_F64 pixel = new Point2D_F64();
	LineParametric3D_F64 ray = new LineParametric3D_F64();
	RenderPixel renderPixel = new RenderPixel();

	/** Increase this value to increase the simulator's accuracy. More points are sampled per pixel. */
	public int renderSampling = 2;

	/**
	 * More accurate rendering but will run slower
	 */
	public void enableHighAccuracy() {
		renderSampling = 4;
	}

	public void setCamera( CameraUniversalOmni model ) {
		LensDistortionWideFOV factory = new LensDistortionUniversalOmni(model);

		pixelTo3 = factory.undistortPtoS_F64();
		sphereToPixel = factory.distortStoP_F64();

		computeProjectionTable(model.width, model.height);
	}

	public void setCamera( CameraPinhole model ) {
		LensDistortionNarrowFOV factory = new LensDistortionPinhole(model);
		setCamera(factory, model.width, model.height);
	}

	public void setCamera( CameraPinholeBrown model ) {
		LensDistortionNarrowFOV factory = new LensDistortionBrown(model);
		setCamera(factory, model.width, model.height);
	}

	public void setCamera( LensDistortionNarrowFOV model, int width, int height ) {

		pixelTo3 = new NarrowPixelToSphere_F64(model.undistort_F64(true, false));
		sphereToPixel = new SphereToNarrowPixel_F64(model.distort_F64(false, true));

		computeProjectionTable(width, height);
	}

	public void setWorldToCamera( Se3_F64 worldToCamera ) {
		this.worldToCamera.setTo(worldToCamera);
	}

	/**
	 * Computes 3D pointing vector for every pixel in the simulated camera frame
	 *
	 * @param width width of simulated camera
	 * @param height height of simulated camera
	 */
	void computeProjectionTable( int width, int height ) {
		output.reshape(width, height);
		depthMap.reshape(width, height);

		ImageMiscOps.fill(depthMap, -1);

		int samplesPerPixel = renderSampling*renderSampling;
		int pointingPixelStride = samplesPerPixel*3;
		int pointingStride = output.width*pointingPixelStride;

		pointing = new float[height*pointingStride];

		double offsetSample = 0.5/renderSampling;

		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				int pointingIndex = y*pointingStride + x*pointingPixelStride;
				for (int sampleIdx = 0; sampleIdx < samplesPerPixel; sampleIdx++) {
					int sampleY = sampleIdx/renderSampling;
					int sampleX = sampleIdx%renderSampling;

					double subY = sampleY/(double)renderSampling;
					double subX = sampleX/(double)renderSampling;

					pixelTo3.compute(offsetSample + x + subX, offsetSample + y + subY, p3);

					if (UtilEjml.isUncountable(p3.x)) {
						depthMap.unsafe_set(x, y, Float.NaN);
						break;
					}

					pointing[pointingIndex++] = (float)p3.x;
					pointing[pointingIndex++] = (float)p3.y;
					pointing[pointingIndex++] = (float)p3.z;
				}
			}
		}
	}

	/**
	 * <p>Adds a surface to the simulation. The center of the surface's coordinate system will be the image
	 * center. Width is the length along the image's width. the world length along the image height is
	 * width*texture.height/texture.width.</p>
	 *
	 * <p>NOTE: The image is flipped horizontally internally so that when it is rendered it appears the same way
	 * it is displayed on the screen as usual.</p>
	 *
	 * @param rectToWorld Transform from surface to world coordinate systems
	 * @param widthWorld Size of surface as measured along its width
	 * @param texture Image describing the surface's appearance and shape.
	 */
	public void addSurface( Se3_F64 rectToWorld, double widthWorld, GrayF32 texture ) {
		SurfaceRect s = new SurfaceRect();
		s.texture.setTo(texture);
		s.width3D = widthWorld;
		s.rectToWorld = rectToWorld;

		scene.add(s);
	}

	public void resetScene() {
		scene.clear();
	}

	/**
	 * Render the scene and returns the rendered image.
	 *
	 * @return rendered image
	 * @see #getOutput()
	 */
	public GrayF32 render() {
		ImageMiscOps.fill(output, background);
		ImageMiscOps.fill(depthMap, Float.MAX_VALUE);

		for (int i = 0; i < scene.size(); i++) {
			scene.get(i).rectInCamera();
		}

		if (BoofConcurrency.USE_CONCURRENT && output.width*output.height > 100*100) {
			renderMultiThread();
		} else {
			renderSingleThread(0, output.height, renderPixel, ray);
		}

		return getOutput();
	}

	private void renderSingleThread( int y0, int y1, RenderPixel renderPixel, LineParametric3D_F64 ray ) {
		int samplesPerPixel = renderSampling*renderSampling;
		int pointingPixelStride = samplesPerPixel*3;
		int pointingStride = output.width*pointingPixelStride;

		for (int pixelY = y0; pixelY < y1; pixelY++) {
			int depthIdx = pixelY*depthMap.stride;
			for (int pixelX = 0; pixelX < output.width; pixelX++) {
				if (Float.isNaN(depthMap.data[depthIdx++]))
					continue;

				for (int i = 0; i < scene.size(); i++) {
					SurfaceRect r = scene.get(i);
					if (!r.visible)
						continue;

					float sumValue = 0.0f;
					float sumDepth = 0.0f;
					int pointingIndex = pixelY*pointingStride + pixelX*pointingPixelStride;
					for (int sampleIdx = 0; sampleIdx < samplesPerPixel; sampleIdx++) {
						ray.slope.x = pointing[pointingIndex++];
						ray.slope.y = pointing[pointingIndex++];
						ray.slope.z = pointing[pointingIndex++];

						if (pixelX >= r.pixelRect.x0 && pixelX < r.pixelRect.x1 && pixelY >= r.pixelRect.y0 && pixelY < r.pixelRect.y1) {
							if (renderPixel.render(ray, r, depthMap.unsafe_get(pixelX, pixelY))) {
								sumValue += renderPixel.value;
								sumDepth += renderPixel.depth;
							} else {
								sumDepth = 0.0f;
								break;
							}
						}
					}

					if (sumDepth > 0.0f) {
						output.unsafe_set(pixelX, pixelY, sumValue/samplesPerPixel);
						depthMap.unsafe_set(pixelX, pixelY, sumDepth/samplesPerPixel);
					}
				}
			}
		}
	}

	private void renderMultiThread() {
		BoofConcurrency.loopBlocks(0, output.height, ( y0, y1 ) -> {
			var renderPixel = new RenderPixel();
			var ray = new LineParametric3D_F64();
			renderSingleThread(y0, y1, renderPixel, ray);
		});
	}

	static class RenderPixel {
		// Pixel intensity on the surface where the ray hits
		public float value;
		// View depth at the location the ray hits
		public float depth;

		Vector3D_F64 _u = new Vector3D_F64();
		Vector3D_F64 _v = new Vector3D_F64();
		Vector3D_F64 _n = new Vector3D_F64();
		Vector3D_F64 _w0 = new Vector3D_F64();
		Point3D_F64 p3 = new Point3D_F64();

		public boolean render( LineParametric3D_F64 ray, SurfaceRect r, float currentDepth ) {
			// See if it intersects at a unique point and is positive in value
			if (1 != Intersection3D_F64.intersectConvex(r.rect3D, ray, p3, _u, _v, _n, _w0)) {
				return false;
			}

			// only care about intersections in front of the camera and closer that what was previously seen
			this.depth = (float)p3.z;
			if (depth <= 0 || depth >= currentDepth)
				return false;

			// convert the point into rect coordinates
			SePointOps_F64.transformReverse(r.rectToCamera, p3, p3);

			// pixel coordinate on the surface.
			double surfaceX = (-p3.x/r.width3D + 0.5)*r.texture.width;
			double surfaceY = (p3.y/r.height3D + 0.5)*r.texture.height;

			// We want to round towards the nearest pixel to remove rendering bias
			surfaceX += 0.5;
			surfaceY += 0.5;

			// make sure it's in bounds of the texture
			if (surfaceX >= 0.0 && surfaceX < r.texture.width && surfaceY >= 0.0 && surfaceY < r.texture.height) {
				this.value = r.texture.unsafe_get((int)surfaceX, (int)surfaceY);
				return true;
			}
			return false;
		}
	}

	public SurfaceRect getImageRect( int which ) {
		return scene.get(which);
	}

	public void setBackground( float background ) {
		this.background = background;
	}

	/**
	 * Project a point which lies on the 2D planar polygon's surface onto the rendered image
	 * The rectangle's coordinate system's origin will be at its center> +x right, +y up
	 */
	public void computePixel( int which, double x, double y, Point2D_F64 output ) {
		SurfaceRect r = scene.get(which);

		Point3D_F64 p3 = new Point3D_F64(-x, -y, 0);
		SePointOps_F64.transform(r.rectToCamera, p3, p3);

		// unit sphere
		p3.scale(1.0/p3.norm());

		sphereToPixel.compute(p3.x, p3.y, p3.z, output);
	}

	@SuppressWarnings({"NullAway.Init"})
	public class SurfaceRect {
		Se3_F64 rectToWorld;
		Se3_F64 rectToCamera = new Se3_F64();
		// surface normal in world frame
		Vector3D_F64 normal = new Vector3D_F64();
		public final GrayF32 texture = new GrayF32(1, 1);
		public double width3D;
		public double height3D;

		// 3D point of corners in camera frame
		DogArray<Point3D_F64> rect3D = new DogArray<>(Point3D_F64::new);
		// 2D point of corners in surface frame
		Polygon2D_F64 rect2D = new Polygon2D_F64();
		// bounding box of visible region in pixels
		Rectangle2D_I32 pixelRect = new Rectangle2D_I32();
		// true if its visible
		public boolean visible;

		/**
		 * Computes the location of the surface's rectangle in the camera reference frame
		 */
		public void rectInCamera() {
			rectToWorld.concat(worldToCamera, rectToCamera);

			// surface normal in world frame
			normal.setTo(0, 0, 1);
			GeometryMath_F64.mult(rectToCamera.R, normal, normal);

			visible = normal.z < 0;

			if (!visible) {
				pixelRect.x0 = pixelRect.y0 = pixelRect.x1 = pixelRect.y1 = 0;
				return;
			}

			double imageRatio = texture.height/(double)texture.width;
			height3D = width3D*imageRatio;

			rect2D.vertexes.resize(4);
			rect2D.set(0, -width3D/2, -height3D/2);
			rect2D.set(1, -width3D/2, height3D/2);
			rect2D.set(2, width3D/2, height3D/2);
			rect2D.set(3, width3D/2, -height3D/2);

			UtilShape3D_F64.polygon2Dto3D(rect2D, rectToCamera, rect3D);

			pixelRect.setTo(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

			for (int i = 0, j = 3; i < 4; j = i, i++) {
				Point3D_F64 a = rect3D.get(j);
				Point3D_F64 b = rect3D.get(i);

				for (int k = 0; k < 50; k++) {
					double w = k/50.0;
					p3.x = a.x*(1 - w) + b.x*w;
					p3.y = a.y*(1 - w) + b.y*w;
					p3.z = a.z*(1 - w) + b.z*w;

					p3.divideIP(p3.norm());

					sphereToPixel.compute(p3.x, p3.y, p3.z, pixel);
					int x = (int)Math.round(pixel.x);
					int y = (int)Math.round(pixel.y);

					pixelRect.x0 = Math.min(pixelRect.x0, x);
					pixelRect.y0 = Math.min(pixelRect.y0, y);
					pixelRect.x1 = Math.max(pixelRect.x1, x + 1);
					pixelRect.y1 = Math.max(pixelRect.y1, y + 1);
				}
			}
			// it's an approximation so add in some fudge room
			pixelRect.x0 -= 2;
			pixelRect.x1 += 2;
			pixelRect.y0 -= 2;
			pixelRect.y1 += 2;

//			System.out.println("PixelRect "+pixelRect);
		}
	}

	public GrayF32 getOutput() {
		return output;
	}
}
