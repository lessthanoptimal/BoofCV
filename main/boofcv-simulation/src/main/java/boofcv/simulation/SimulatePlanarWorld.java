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

	List<ImageRect> scene = new ArrayList<>();

	Point2Transform3_F64 pixelTo3;
	Point3Transform2_F64 sphereToPixel;
	InterpolatePixelS<GrayF32> interp = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.ZERO);

	Point3D_F64 p3 = new Point3D_F64();
	float[] pointing = new float[0];

	float background = 0;

	public void setCamera( CameraUniversalOmni model ) {
		output.reshape(model.width,model.height);
		depthMap.reshape(model.width,model.height);
		LensDistortionWideFOV factory = new LensDistortionUniversalOmni(model);

		pixelTo3 = factory.undistortPtoS_F64();
		sphereToPixel = factory.distortStoP_F64();

		computeProjectionTable(model);
	}

	public void setCamera( CameraPinholeRadial model ) {
		output.reshape(model.width,model.height);
		depthMap.reshape(model.width,model.height);
		LensDistortionNarrowFOV factory = new LensDistortionRadialTangential(model);

		pixelTo3 = new NarrowPixelToSphere_F64(factory.undistort_F64(true,false));
		sphereToPixel = new SphereToNarrowPixel_F64(factory.distort_F64(false,true));

		computeProjectionTable(model);
	}

	private void computeProjectionTable(CameraPinhole model) {
		ImageMiscOps.fill(depthMap,-1);

		pointing = new float[model.width*model.height*3];

		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				// Should this add 0.5 so that the ray goes out of the pixel's center? Seems to increase reprojection
				// error in calibration tests if I do...
				pixelTo3.compute(x, y, p3);
				if(UtilEjml.isUncountable(p3.x)) {
					depthMap.unsafe_set(x,y,Float.NaN);
				} else {
					pointing[(y*output.width+x)*3 ] = (float)p3.x;
					pointing[(y*output.width+x)*3+1 ] = (float)p3.y;
					pointing[(y*output.width+x)*3+2 ] = (float)p3.z;
				}
			}
		}
	}

	public void addTarget(Se3_F64 rectToWorld , double widthWorld , GrayF32 image ) {
		ImageRect ir = new ImageRect();
		ir.image = image;
		ir.width3D = widthWorld;
		ir.rectToWorld = rectToWorld;

		scene.add(ir);
	}

	public void resetScene() {
		scene.clear();
	}

	public void render() {

		for (int i = 0; i < scene.size(); i++) {
			ImageRect r = scene.get(i);

			r.worldRect();
		}

		LineParametric3D_F64 ray = new LineParametric3D_F64();

		ImageMiscOps.fill(output,background);
		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				if( Float.isNaN(depthMap.unsafe_get(x,y)))
					continue;
				ray.slope.x = pointing[(y*output.width+x)*3];
				ray.slope.y = pointing[(y*output.width+x)*3+1];
				ray.slope.z = pointing[(y*output.width+x)*3+2];
				renderPixel(ray,x,y);
			}
		}
	}

	Vector3D_F64 _u = new Vector3D_F64();
	Vector3D_F64 _v =new Vector3D_F64();
	Vector3D_F64 _n = new Vector3D_F64();
	Vector3D_F64 _w0 = new Vector3D_F64();

	private void renderPixel( LineParametric3D_F64 ray , int x , int y ) {
		float minDepth = Float.MAX_VALUE;

		for (int i = 0; i < scene.size(); i++) {
			ImageRect r = scene.get(i);

			// only care about intersections in front of the camera
			if( 1 == Intersection3D_F64.intersectConvex(r.rect3D,ray,p3,_u,_v,_n,_w0)) {

				double imageRatio = r.image.height/(double)r.image.width;

				float depth = (float)p3.z;
				if( depth < minDepth) {
					minDepth = depth;

					// convert the point into rect coordinates
					SePointOps_F64.transformReverse(r.rectToWorld, p3, p3);

					// now into image pixels
					p3.x += r.width3D / 2;
					p3.y += r.width3D * imageRatio / 2;
//
//					// flip so that it appears the same way it was rendered
//					p3.x = r.width3D - p3.x;
//					p3.y = r.width3D * imageRatio - p3.y;

					if (Math.abs(p3.z) > 0.001)
						throw new RuntimeException("BUG!");

					double pixelX = p3.x * r.image.width / r.width3D;
					double pixelY = p3.y * r.image.height / (r.width3D * imageRatio);

					if( pixelX < r.image.width && pixelY < r.image.height ) {
						interp.setImage(r.image);
						output.unsafe_set(x, y, (int) (interp.get((float) pixelX, (float) pixelY) + 0.5f));
					}
				}
			}
		}
		depthMap.unsafe_set(x,y,minDepth);
	}

	public ImageRect getImageRect( int which ) {
		return scene.get(which);
	}

	public void setBackground(float background) {
		this.background = background;
	}

	/**
	 * Project a point which lies on the 2D planar polygon's surface onto the rendered image
	 */
	public void computePixel(int which, double x, double y, Point2D_F64 output) {
		ImageRect r = scene.get(which);

		Point3D_F64 p3 = new Point3D_F64(x,-y,0);
		SePointOps_F64.transform(r.rectToWorld, p3, p3);

		// unit sphere
		p3.scale(1.0/p3.norm());

		sphereToPixel.compute(p3.x,p3.y,p3.z,output);
	}

	public static class ImageRect {
		Se3_F64 rectToWorld;
		GrayF32 image;
		double width3D;
		FastQueue<Point3D_F64> rect3D = new FastQueue<>(Point3D_F64.class,true);
		Polygon2D_F64 rect2D = new Polygon2D_F64();

		public void worldRect() {
			double imageRatio = image.height/(double)image.width;
			double height3D = width3D*imageRatio;

			rect2D.vertexes.resize(4);
			rect2D.set(0,-width3D/2,-height3D/2);
			rect2D.set(1,-width3D/2, height3D/2);
			rect2D.set(2,width3D/2, height3D/2);
			rect2D.set(3,width3D/2,-height3D/2);

			UtilShape3D_F64.polygon2Dto3D(rect2D,rectToWorld,rect3D);
		}
	}

	public GrayF32 getOutput() {
		return output;
	}
}
