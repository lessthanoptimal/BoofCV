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

import boofcv.alg.distort.*;
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.distort.universal.LensDistortionUniversalOmni;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.geometry.ConvertRotation3D_F32;
import georegression.struct.EulerType;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Demonstration for how to project a fisheye camera onto a synthetic pinhole camera view.  Internally NarrowToWide
 * has the following steps.  1) compute normalized image coordinates for each pixel. 2) convert into unit circle
 * coordinates, and 3) compute pixe location in fisheye camera.
 *
 * @author Peter Abeles
 */
public class ExampleFisheyeToPinhole {
	public static void main(String[] args) {
		// Path to image data and calibration data
		String fisheyePath = UtilIO.pathExample("fisheye/theta/");

		// load the fisheye camera parameters
		CameraUniversalOmni fisheyeModel = CalibrationIO.load(new File(fisheyePath,"front.yaml"));

		// Specify what the pinhole camera should look like
		CameraPinhole pinholeModel = new CameraPinhole(400,400,0,300,300,600,600);

		// Create the transform from pinhole to fisheye views
		LensDistortionNarrowFOV pinholeDistort = new LensDistortionPinhole(pinholeModel);
		LensDistortionWideFOV fisheyeDistort = new LensDistortionUniversalOmni(fisheyeModel);
		NarrowToWidePtoP_F32 transform = new NarrowToWidePtoP_F32(pinholeDistort,fisheyeDistort);

		// Load fisheye RGB image
		BufferedImage bufferedFisheye = UtilImageIO.loadImage(fisheyePath,"front_table.jpg");
		Planar<GrayU8> fisheyeImage = ConvertBufferedImage.convertFrom(
				bufferedFisheye, true, ImageType.pl(3,GrayU8.class));

		// Create the image distorter which will render the image
		InterpolatePixel<Planar<GrayU8>> interp = FactoryInterpolation.
				createPixel(0, 255, InterpolationType.BILINEAR, BorderType.ZERO, fisheyeImage.getImageType());
		ImageDistort<Planar<GrayU8>,Planar<GrayU8>> distorter =
				FactoryDistort.distort(false,interp,fisheyeImage.getImageType());

		// Pass in the transform created above
		distorter.setModel(new PointToPixelTransform_F32(transform));

		// Render the image.  The camera will have a rotation of 0 and will thus be looking straight forward
		Planar<GrayU8> pinholeImage = fisheyeImage.createNew(pinholeModel.width, pinholeModel.height);

		distorter.apply(fisheyeImage,pinholeImage);
		BufferedImage bufferedPinhole0 = ConvertBufferedImage.convertTo(pinholeImage,null,true);

		// rotate the virtual pinhole camera to the right
		transform.setRotationWideToNarrow(ConvertRotation3D_F32.eulerToMatrix(EulerType.YXZ,0.8f,0,0,null));

		distorter.apply(fisheyeImage,pinholeImage);
		BufferedImage bufferedPinhole1 = ConvertBufferedImage.convertTo(pinholeImage,null,true);

		// Display the results
		ListDisplayPanel panel = new ListDisplayPanel();
		panel.addImage(bufferedPinhole0,"Pinehole Forward");
		panel.addImage(bufferedPinhole1,"Pinehole Right");
		panel.addImage(bufferedFisheye,"Fisheye");
		panel.setPreferredSize(new Dimension(600,450));

		ShowImages.showWindow(panel, "Fisheye to Pinhole", true);
	}
}
