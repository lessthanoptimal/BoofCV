/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib;

import boofcv.alg.distort.LensDistortionWideFOV;
import boofcv.alg.distort.universal.LensDistortionUniversalOmni;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.distort.Point2Transform3_F64;
import boofcv.struct.image.GrayU8;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.Intersection3D_F64;
import georegression.struct.EulerType;
import georegression.struct.line.LineParametric3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Triangle3D_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.UtilEjml;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author Peter Abeles
 */
public class RenderSimulatedFisheye {

	GrayU8 output = new GrayU8(1,1);

	Se3_F64 rectToWorld;
	GrayU8 image;
	double width3D;

	Point2Transform3_F64 pixelTo3;
	InterpolatePixelS<GrayU8> interp = FactoryInterpolation.bilinearPixelS(GrayU8.class, BorderType.ZERO);

	public void setCamera( CameraUniversalOmni model ) {
		output.reshape(model.width,model.height);
		LensDistortionWideFOV factory = new LensDistortionUniversalOmni(model);

		pixelTo3 = factory.undistortPtoS_F64();
	}

	public void addTarget(Se3_F64 rectToWorld , double widthWorld , GrayU8 image ) {
		this.rectToWorld = rectToWorld;
		this.image = image;
		this.width3D = widthWorld;
	}

	public void render() {
		Point3D_F64 p3 = new Point3D_F64();

		double imageRatio = image.height/(double)image.width;

		Triangle3D_F64 A = new Triangle3D_F64(
				-width3D/2,-width3D*imageRatio/2,0,
				-width3D/2, width3D*imageRatio/2,0,
				 width3D/2, -width3D*imageRatio/2,0);
		Triangle3D_F64 B = new Triangle3D_F64(
				-width3D/2, width3D*imageRatio/2,0,
				 width3D/2, width3D*imageRatio/2,0,
				 width3D/2, -width3D*imageRatio/2,0);

		interp.setImage(image);

		SePointOps_F64.transform(rectToWorld,A.v0,A.v0);
		SePointOps_F64.transform(rectToWorld,A.v1,A.v1);
		SePointOps_F64.transform(rectToWorld,A.v2,A.v2);
		SePointOps_F64.transform(rectToWorld,B.v0,B.v0);
		SePointOps_F64.transform(rectToWorld,B.v1,B.v1);
		SePointOps_F64.transform(rectToWorld,B.v2,B.v2);

		LineParametric3D_F64 ray = new LineParametric3D_F64();

		ImageMiscOps.fill(output,0);
		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				pixelTo3.compute(x,y,p3);
				if(UtilEjml.isUncountable(p3.x))
					continue;
				ray.slope.set(p3.x,p3.y,p3.z);

//				if( p3.z > 0.95 && Math.abs(p3.x) > 0.2 )
//					System.out.println("Egads");

				if( 1==Intersection3D_F64.intersect(A,ray,p3) ||
						1 == Intersection3D_F64.intersect(B,ray,p3) ) {

					// convert the point into rect coordinates
					SePointOps_F64.transformReverse(rectToWorld,p3,p3);

					// now into image pixels
					p3.x += width3D/2;
					p3.y += width3D*imageRatio/2;
					p3.y = width3D*imageRatio-p3.y;

					if( Math.abs(p3.z) > 0.001 )
						throw new RuntimeException("BUG!");

					double pixelX = p3.x*image.width/width3D;
					double pixelY = p3.y*image.height/(width3D*imageRatio);

					output.unsafe_set(x,y,(int)(interp.get((float)pixelX,(float)pixelY)+0.5f));
				}
			}
		}
	}

	public GrayU8 getOutput() {
		return output;
	}

	public static void main(String[] args) {
		GrayU8 image = new GrayU8(400,300);
		GImageMiscOps.fill(image,255);
		GImageMiscOps.fillRectangle(image,90,20,20,40,40);
		GImageMiscOps.fillRectangle(image,90,60,60,40,40);
		GImageMiscOps.fillRectangle(image,90,100,20,40,40);

		GImageMiscOps.fillRectangle(image,90,300,200,60,60);

		Se3_F64 rectToWorld = new Se3_F64();
		rectToWorld.T.set(0,0,-0.2);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0,1.0,0,rectToWorld.R);
		rectToWorld = rectToWorld.invert(null);

		String fisheyePath = UtilIO.pathExample("fisheye/theta/");
		CameraUniversalOmni model = CalibrationIO.load(new File(fisheyePath,"front.yaml"));

		RenderSimulatedFisheye alg = new RenderSimulatedFisheye();

		alg.setCamera(model);
		alg.addTarget(rectToWorld,0.3,image);
		alg.render();

		BufferedImage output = new BufferedImage(model.width,model.height,BufferedImage.TYPE_INT_RGB);

		ConvertBufferedImage.convertTo(alg.getOutput(),output);

		ShowImages.showWindow(output,"Rendered Fisheye",true);
	}
}
