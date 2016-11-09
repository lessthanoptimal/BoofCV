/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.distort.spherical.MultiCameraToRequirectangular;
import boofcv.alg.distort.universal.LensDistortionUniversalOmni;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.image.ShowImages;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ExampleFisheyeToEquirectangular {
	public static Se3_F64 createFrontToBack() {
		double[][] a = new double[][]{
				{-0.99894111, -0.02166867, -0.04058488},
				{0.01742898, -0.99462651,  0.10205069},
				{-0.0425781,   0.10123528,  0.99395097}};

		Se3_F64 ret = new Se3_F64();
		ret.R = new DenseMatrix64F(a);
		ret.T.set(0.10499465, 0.45557971, -0.03715325);

		Rodrigues_F64 rod = new Rodrigues_F64(UtilAngle.radian(-177.5),0.0,0,1);
		ret.R = ConvertRotation3D_F64.rodriguesToMatrix(rod,(DenseMatrix64F)null);

		DenseMatrix64F temp = ret.R.copy();
		DenseMatrix64F R = rotateAxis().R;
		CommonOps.mult(temp,R,ret.R);

		return ret;
	}

	public static Se3_F64 rotateAxis() {
		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,-Math.PI/2,0,0,null);
		Se3_F64 ret = new Se3_F64();
		ret.R = R;
		return ret;
	}

	public static void main(String[] args) {
		// Path to image data and calibration data
		String fisheyePath = "/home/pja/projects/boofcv/a";

		// load the fisheye camera parameters
		CameraUniversalOmni model0 = CalibrationIO.load(new File(fisheyePath,"boof_front.yaml"));
		CameraUniversalOmni model1 = CalibrationIO.load(new File(fisheyePath,"boof_back.yaml"));

		LensDistortionWideFOV distort0 = new LensDistortionUniversalOmni(model0);
		LensDistortionWideFOV distort1 = new LensDistortionUniversalOmni(model1);

		ImageType<Planar<GrayF32>> imageType = ImageType.pl(3,GrayF32.class);

		InterpolatePixel<Planar<GrayF32>> interp = FactoryInterpolation.createPixel(0,255,TypeInterpolate.BILINEAR,
				BorderType.ZERO, imageType);
		ImageDistort<Planar<GrayF32>,Planar<GrayF32>> distort =
				FactoryDistort.distort(false,interp, imageType);
		MultiCameraToRequirectangular<Planar<GrayF32>> alg = new MultiCameraToRequirectangular<>(distort,800,400,imageType);

		alg.setMaskTolerancePixels(0.5f);

		alg.addCamera(createFrontToBack(),distort0, model0.width, model0.height );
		alg.addCamera(rotateAxis(),distort1, model1.width, model1.height );

		// Load fisheye RGB image
		BufferedImage buffered0 = UtilImageIO.loadImage(fisheyePath,"front.png");
		Planar<GrayF32> fisheye0 = ConvertBufferedImage.convertFrom(
				buffered0, true, ImageType.pl(3,GrayF32.class));

		BufferedImage buffered1 = UtilImageIO.loadImage(fisheyePath,"back.png");
		Planar<GrayF32> fisheye1 = ConvertBufferedImage.convertFrom(
				buffered1, true, ImageType.pl(3,GrayF32.class));

		List<Planar<GrayF32>> images = new ArrayList<>();
		images.add( fisheye0 );
		images.add( fisheye1 );

		alg.render(images);

		BufferedImage equiOut = ConvertBufferedImage.convertTo(alg.getRenderedImage(),null,true);

		ShowImages.showWindow(equiOut,"Equirectangular",true);

	}
}
