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

package boofcv.abst.fiducial;

import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author Peter Abeles
 */
public class TestCalibrationFiducialDetector extends GenericFiducialDetectorChecks {

	// selected because it has significant lens distortion
	String directory = UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess/");

	public TestCalibrationFiducialDetector() {
		pixelAndProjectedTol = 10;
		types.add( ImageType.single(GrayU8.class));
		types.add( ImageType.single(GrayF32.class));
	}

	@Override
	public ImageBase loadImage(ImageType imageType) {

		BufferedImage out = UtilImageIO.loadImage(directory , "frame09.jpg");
		return ConvertBufferedImage.convertFrom(out, true, imageType);
	}

	@Override
	public LensDistortionNarrowFOV loadDistortion(boolean distorted) {
		CameraPinholeRadial model = CalibrationIO.load(new File(directory,"intrinsic.yaml"));
		if( !distorted ) {
			model.radial = null;
			model.t1 = model.t2 = 0;
		}
		return new LensDistortionRadialTangential(model);
	}

	@Override
	public FiducialDetector createDetector(ImageType imageType) {
		return FactoryFiducial.calibChessboard(new ConfigChessboard(7, 5, 0.03), imageType.getImageClass());
	}
}