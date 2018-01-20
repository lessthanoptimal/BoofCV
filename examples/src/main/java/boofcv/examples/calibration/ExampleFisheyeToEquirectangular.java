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

package boofcv.examples.calibration;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionWideFOV;
import boofcv.alg.distort.spherical.MultiCameraToEquirectangular;
import boofcv.alg.distort.universal.LensDistortionUniversalOmni;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.distort.Point2Transform3_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.geometry.ConvertRotation3D_F32;
import georegression.geometry.UtilVector3D_F64;
import georegression.metric.UtilAngle;
import georegression.misc.GrlConstants;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F32;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_FDRM;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates how to combine multiple images together into a single view.  A 360 camera was used to generate
 * the two input fisheye images.  Each camera has been calibrated independently and the extrinsics between the two
 * cameras is assume to be known.  Because of how the fisheye image is modeled a mask is required to label pixels
 * outside the FOV that should not be considered.
 *
 * @author Peter Abeles
 */
public class ExampleFisheyeToEquirectangular {

	/**
	 * Creates a mask telling the algorithm which pixels are valid and which are not.  The field-of-view (FOV) of the
	 * camera is known so we will use that information to do a better job of filtering out invalid pixels than
	 * it can do alone.
	 */
	public static GrayU8 createMask( CameraUniversalOmni model ,
									 LensDistortionWideFOV distortion , double fov ) {
		GrayU8 mask = new GrayU8(model.width,model.height);

		Point2Transform3_F64 p2s = distortion.undistortPtoS_F64();
		Point3D_F64 ref = new Point3D_F64(0,0,1);
		Point3D_F64 X = new Point3D_F64();

		p2s.compute(model.cx,model.cy,X);

		for (int y = 0; y < model.height; y++) {
			for (int x = 0; x < model.width; x++) {
				p2s.compute(x,y,X);

				if( Double.isNaN(X.x) || Double.isNaN(X.y) || Double.isNaN(X.z)) {
					continue;
				}

				double angle = UtilVector3D_F64.acute(ref,X);
				if( Double.isNaN(angle)) {
					continue;
				}
				if( angle <= fov/2.0 )
					mask.unsafe_set(x,y,1);
			}
		}
		return mask;
	}

	public static void main(String[] args) {
		// Path to image data and calibration data
		String fisheyePath = UtilIO.pathExample("fisheye/theta");

		// load the fisheye camera parameters
		CameraUniversalOmni model0 = CalibrationIO.load(new File(fisheyePath,"front.yaml"));
		CameraUniversalOmni model1 = CalibrationIO.load(new File(fisheyePath,"back.yaml" ));

		LensDistortionWideFOV distort0 = new LensDistortionUniversalOmni(model0);
		LensDistortionWideFOV distort1 = new LensDistortionUniversalOmni(model1);

		ImageType<Planar<GrayF32>> imageType = ImageType.pl(3,GrayF32.class);

		InterpolatePixel<Planar<GrayF32>> interp = FactoryInterpolation.createPixel(0,255, InterpolationType.BILINEAR,
				BorderType.ZERO, imageType);
		ImageDistort<Planar<GrayF32>,Planar<GrayF32>> distort =
				FactoryDistort.distort(false,interp, imageType);

		//This will create an equirectangular image with 800 x 400 pixels
		MultiCameraToEquirectangular<Planar<GrayF32>> alg = new MultiCameraToEquirectangular<>(distort,800,400,imageType);

		// this is an important parameter and is used to filter out falsely mirrored pixels
		alg.setMaskToleranceAngle(UtilAngle.radian(0.1f));

		GrayU8 mask0 =  createMask(model0,distort0,UtilAngle.radian(182)); // camera has a known FOV of 185 degrees
		GrayU8 mask1 =  createMask(model1,distort1,UtilAngle.radian(182)); // the edges are likely to be noisy,
																		   // so crop it a bit..

		// Rotate camera axis so that +x is forward and not +z and make it visually pleasing
		FMatrixRMaj adjR = ConvertRotation3D_F32.eulerToMatrix(EulerType.XYZ, GrlConstants.F_PI/2,0,0,null);
		// Rotation from the front camera to the back facing camera.
		// This is only an approximation.  Should be determined through calibration.
		FMatrixRMaj f2b = ConvertRotation3D_F32.eulerToMatrix(EulerType.ZYX,GrlConstants.F_PI,0,0,null);

		Se3_F32 frontToFront = new Se3_F32();
		frontToFront.setRotation(adjR);
		Se3_F32 frontToBack = new Se3_F32();
		CommonOps_FDRM.mult(f2b,adjR,frontToBack.R);

		// add the camera and specify which pixels are valid.  These functions precompute the entire transform
		// and can be relatively slow, but generating the equirectangular image should be much faster
		alg.addCamera(frontToBack,distort0, mask0 );
		alg.addCamera(frontToFront,distort1, mask1 );

		// Load fisheye RGB image
		BufferedImage buffered0 = UtilImageIO.loadImage(fisheyePath,"front_table.jpg");
		Planar<GrayF32> fisheye0 = ConvertBufferedImage.convertFrom(
				buffered0, true, ImageType.pl(3,GrayF32.class));

		BufferedImage buffered1 = UtilImageIO.loadImage(fisheyePath,"back_table.jpg");
		Planar<GrayF32> fisheye1 = ConvertBufferedImage.convertFrom(
				buffered1, true, ImageType.pl(3,GrayF32.class));

		List<Planar<GrayF32>> images = new ArrayList<>();
		images.add( fisheye0 );
		images.add( fisheye1 );

		alg.render(images);

		BufferedImage equiOut = ConvertBufferedImage.convertTo(alg.getRenderedImage(),null,true);

		ShowImages.showWindow(equiOut,"Dual Fisheye to Equirectangular",true);
	}
}
