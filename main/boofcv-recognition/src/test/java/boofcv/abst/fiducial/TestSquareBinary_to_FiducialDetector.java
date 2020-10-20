/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.fiducial.square.FiducialSquareGenerator;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class TestSquareBinary_to_FiducialDetector extends GenericFiducialDetectorChecks
{
	public TestSquareBinary_to_FiducialDetector() {
		types.add( ImageType.single(GrayU8.class));
		types.add( ImageType.single(GrayF32.class));
		pixelAndProjectedTol = 1.0; // should be very close
	}

	@Override
	public GrayF32 renderFiducial() {
		FiducialImageEngine render = new FiducialImageEngine();
		render.configure(0,200);

		FiducialSquareGenerator generator = new FiducialSquareGenerator(render);
		generator.setMarkerWidth(200);
		generator.generate(345,4);

		return render.getGrayF32();
	}

	@Override
	public FiducialDetector createDetector(ImageType imageType) {
		return FactoryFiducial.squareBinary(new ConfigFiducialBinary(1.5),
				ConfigThreshold.local(ThresholdType.LOCAL_MEAN,13),
				(Class<ImageGray>)imageType.getImageClass());
	}
}
