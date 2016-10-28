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
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.se.Se3_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ExampleFisheyeToEquirectangular {
	public static void main(String[] args) {
		// Path to image data and calibration data
		String fisheyePath = UtilIO.pathExample("fisheye/theta_front/");

		// load the fisheye camera parameters
		CameraUniversalOmni fisheyeModel = CalibrationIO.load(new File(fisheyePath,"fisheye.yaml"));

		LensDistortionWideFOV fisheyeDistort = new LensDistortionUniversalOmni(fisheyeModel);

		ImageType<Planar<GrayF32>> imageType = ImageType.pl(3,GrayF32.class);

		InterpolatePixel<Planar<GrayF32>> interp = FactoryInterpolation.createPixel(0,255,TypeInterpolate.BILINEAR,
				BorderType.ZERO, imageType);
		ImageDistort<Planar<GrayF32>,Planar<GrayF32>> distort =
				FactoryDistort.distort(false,interp, imageType);
		MultiCameraToRequirectangular<Planar<GrayF32>> alg = new MultiCameraToRequirectangular<>(distort,800,400,imageType);

		alg.addCamera(new Se3_F64(),fisheyeDistort, fisheyeModel.width, fisheyeModel.height );

		// Load fisheye RGB image
		BufferedImage bufferedFisheye = UtilImageIO.loadImage(fisheyePath,"dining_room.jpg");
		Planar<GrayF32> fisheyeImage = ConvertBufferedImage.convertFrom(
				bufferedFisheye, true, ImageType.pl(3,GrayF32.class));

		List<Planar<GrayF32>> images = new ArrayList<>();
		images.add( fisheyeImage );

		alg.render(images);

		BufferedImage equiOut = ConvertBufferedImage.convertTo(alg.getRenderedImage(),null,true);

		ShowImages.showWindow(equiOut,"Equirectangular",true);

	}
}
