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

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class TestSquareBinary_to_FiducialDetector extends GenericFiducialDetectorChecks {

	public TestSquareBinary_to_FiducialDetector() {
		types.add( ImageType.single(GrayU8.class));
		types.add( ImageType.single(GrayF32.class));
		pixelAndProjectedTol = 1.0; // should be very close
	}

	@Override
	public ImageBase loadImage(ImageType imageType) {

		BufferedImage out = UtilImageIO.loadImage(getClass().getResource("test_square_binary.jpg"));
		return ConvertBufferedImage.convertFrom(out,true,imageType);
	}

	@Override
	public LensDistortionNarrowFOV loadDistortion(boolean distorted) {
		CameraPinholeRadial model = CalibrationIO.load(getClass().getResource("intrinsic_binary.yaml"));
		if( !distorted ) {
			model.radial = null;
			model.t1 = model.t2 = 0;
		}
		return new LensDistortionRadialTangential(model);
	}

	@Override
	public FiducialDetector createDetector(ImageType imageType) {
		return FactoryFiducial.squareBinary(new ConfigFiducialBinary(0.1),
				ConfigThreshold.local(ThresholdType.LOCAL_SQUARE,6),
				imageType.getImageClass());
	}
}