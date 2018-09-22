/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
import boofcv.alg.distort.universal.LensDistortionUniversalOmni;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
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
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.UtilEjml;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates a scene composed of planar objects. The camera is distorted using the provided camera model.
 *
 * @author Peter Abeles
 */
public class SimulatePlanarWorld {

	GrayF32 output = new GrayF32(1,1);
	GrayF32 depthMap = new GrayF32(1,1);

	List<SurfaceRect> scene = new ArrayList<>();

	Point2Transform3_F64 pixelTo3;
	Point3Transform2_F64 sphereToPixel;
	InterpolatePixelS<GrayF32> interp = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.ZERO);

	Se3_F64 worldToCamera = new Se3_F64();

	Point3D_F64 p3 = new Point3D_F64();
	float[] pointing = new float[0];

	float background = 0;

	CameraPinhole tmp;

	public void setCamera( CameraUniversalOmni model ) {
		LensDistortionWideFOV factory = new LensDistortionUniversalOmni(model);

		pixelTo3 = factory.undistortPtoS_F64();
		sphereToPixel = factory.distortStoP_F64();

		computeProjectionTable(model.width, model.height);
	}

	public void setCamera( CameraPinhole model ) {
		LensDistortionNarrowFOV factory = new LensDistortionPinhole(model);
		setCamera( factory, model.width, model.height );
		tmp = model;
	}

	public void setCamera( CameraPinholeRadial model ) {
		LensDistortionNarrowFOV factory = new LensDistortionRadialTangential(model);
		setCamera( factory, model.width, model.height );
	}

	public void setCamera( LensDistortionNarrowFOV model , int width , int height ) {

		pixelTo3 = new NarrowPixelToSphere_F64(model.undistort_F64(true,false));
		sphereToPixel = new SphereToNarrowPixel_F64(model.distort_F64(false,true));

		computeProjectionTable(width,height);
	}

	public void setWorldToCamera( Se3_F64 worldToCamera ) {
		this.worldToCamera.set(worldToCamera);
	}

	/**
	 * Computes 3D pointing vector for every pixel in the simulated camera frame
	 *
	 * @param width width of simulated camera
	 * @param height height of simulated camera
	 */
	void computeProjectionTable( int width , int height ) {
		output.reshape(width,height);
		depthMap.reshape(width,height);

		ImageMiscOps.fill(depthMap,-1);

		pointing = new float[width*height*3];

		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				// Should this add 0.5 so that the ray goes out of the pixel's center? Seems to increase reprojection
				// error in calibration tests if I do...
				pixelTo3.compute(x, y, p3);
				if(UtilEjml.isUncountable(p3.x)) {
					depthMap.unsafe_set(x,y,Float.NaN);
				} else {
					pointing[(y*output.width+x)*3   ] = (float)p3.x;
					pointing[(y*output.width+x)*3+1 ] = (float)p3.y;
					pointing[(y*output.width+x)*3+2 ] = (float)p3.z;
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
	public void addSurface(Se3_F64 rectToWorld , double widthWorld , GrayF32 texture ) {
		SurfaceRect s = new SurfaceRect();
		s.texture = texture.clone();
		s.width3D = widthWorld;
		s.rectToWorld = rectToWorld;

		ImageMiscOps.flipHorizontal(s.texture);

		scene.add(s);
	}

	public void resetScene() {
		scene.clear();
	}

	public void render() {

		for (int i = 0; i < scene.size(); i++) {
			SurfaceRect r = scene.get(i);

			r.rectInCamera();
		}

		LineParametric3D_F64 ray = new LineParametric3D_F64();

		ImageMiscOps.fill(output,background);
		int index = 0;
		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++, index += 3) {
				if( Float.isNaN(depthMap.unsafe_get(x,y)))
					continue;
				ray.slope.x = pointing[index  ];
				ray.slope.y = pointing[index+1];
				ray.slope.z = pointing[index+2];
				renderPixel(ray,x,y);
			}
		}
	}

	Vector3D_F64 _u = new Vector3D_F64();
	Vector3D_F64 _v =new Vector3D_F64();
	Vector3D_F64 _n = new Vector3D_F64();
	Vector3D_F64 _w0 = new Vector3D_F64();

	void renderPixel( LineParametric3D_F64 ray , int x , int y ) {
		double minDepth = Double.MAX_VALUE;

		for (int i = 0; i < scene.size(); i++) {
			SurfaceRect r = scene.get(i);

			// see if the surface is facing the camera
			if( r.normal.z >= 0 )
				continue;

			// See if it intersects at a unique point and is positive in value
			if( 1 == Intersection3D_F64.intersectConvex(r.rect3D,ray,p3,_u,_v,_n,_w0)) {

				// only care about intersections in front of the camera
				double depth = p3.z;
				if( depth >= minDepth) // is this necessary?
					continue;

				minDepth = depth;

				// convert the point into rect coordinates
				SePointOps_F64.transformReverse(r.rectToCamera, p3, p3);

				if (Math.abs(p3.z) > 0.001)
					throw new RuntimeException("BUG!");

				// now into surface pixels.
				// but we have some weirdness due to image coordinate being +y down but normal coordinates +y up
				p3.x += r.width3D / 2;
				p3.y += r.height3D / 2;

				// pixel coordinate on the surface
				double surfaceX = p3.x * r.texture.width / r.width3D;
				double surfaceY = p3.y * r.texture.height / r.height3D;

				if( surfaceX < r.texture.width && surfaceY < r.texture.height ) {
					interp.setImage(r.texture);
					float value = interp.get((float) surfaceX, (float) surfaceY);
					output.unsafe_set(x, y, value);
				}
			}
		}
		depthMap.unsafe_set(x,y,(float)minDepth);
	}

	public SurfaceRect getImageRect(int which ) {
		return scene.get(which);
	}

	public void setBackground(float background) {
		this.background = background;
	}

	/**
	 * Project a point which lies on the 2D planar polygon's surface onto the rendered image
	 */
	public void computePixel(int which, double x, double y, Point2D_F64 output) {
		SurfaceRect r = scene.get(which);

		Point3D_F64 p3 = new Point3D_F64(-x,-y,0);
		SePointOps_F64.transform(r.rectToCamera, p3, p3);

		// unit sphere
		p3.scale(1.0/p3.norm());

		sphereToPixel.compute(p3.x,p3.y,p3.z,output);
	}

	public class SurfaceRect {
		Se3_F64 rectToWorld;
		Se3_F64 rectToCamera = new Se3_F64();
		// surface normal in world frame
		Vector3D_F64 normal = new Vector3D_F64();
		GrayF32 texture;
		double width3D;
		double height3D;

		FastQueue<Point3D_F64> rect3D = new FastQueue<>(Point3D_F64.class,true);
		Polygon2D_F64 rect2D = new Polygon2D_F64();

		/**
		 * Computes the location of the surface's rectangle in the camera reference frame
		 */
		public void rectInCamera() {
			rectToWorld.concat(worldToCamera,rectToCamera);

			// surface normal in world frame
			normal.set(0,0,1);
			GeometryMath_F64.mult(rectToWorld.R,normal,normal);

			double imageRatio = texture.height/(double) texture.width;
			height3D = width3D*imageRatio;

			rect2D.vertexes.resize(4);
			rect2D.set(0,-width3D/2,-height3D/2);
			rect2D.set(1,-width3D/2, height3D/2);
			rect2D.set(2,width3D/2, height3D/2);
			rect2D.set(3,width3D/2,-height3D/2);

			UtilShape3D_F64.polygon2Dto3D(rect2D,rectToCamera,rect3D);
		}
	}

	public GrayF32 getOutput() {
		return output;
	}
}
