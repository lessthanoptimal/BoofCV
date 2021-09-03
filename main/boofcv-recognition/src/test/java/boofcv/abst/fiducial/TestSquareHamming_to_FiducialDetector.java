/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.fiducial.square.FiducialSquareHammingGenerator;
import boofcv.factory.fiducial.ConfigHammingMarker;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.fiducial.HammingDictionary;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class TestSquareHamming_to_FiducialDetector extends GenericFiducialDetectorChecks {

	ConfigHammingMarker config = ConfigHammingMarker.loadDictionary(HammingDictionary.ARUCO_MIP_36h12);

	public TestSquareHamming_to_FiducialDetector() {
		types.add(ImageType.single(GrayU8.class));
		types.add(ImageType.single(GrayF32.class));
		pixelAndProjectedTol = 1.0; // should be very close
	}

	@Override
	public GrayF32 renderFiducial() {
		var engine = new FiducialImageEngine();
		engine.configure(0, 200, 200);

		var generator = new FiducialSquareHammingGenerator(config);
		generator.setRenderer(engine);
		generator.setMarkerWidth(200);
		generator.generate(5);

		return engine.getGrayF32();
	}

	@Override
	public FiducialDetector createDetector( ImageType imageType ) {
		return FactoryFiducial.squareHamming(config, null,
				(Class<ImageGray>)imageType.getImageClass());
	}
}
