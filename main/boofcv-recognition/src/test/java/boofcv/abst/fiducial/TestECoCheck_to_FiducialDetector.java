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

import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckGenerator;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
class TestECoCheck_to_FiducialDetector extends GenericFiducialDetectorChecks {

	ConfigECoCheckMarkers config = new ConfigECoCheckMarkers();
	int squareToPixels = 60;

	public TestECoCheck_to_FiducialDetector() {
		stabilityShrink = 0.5;
		tolAccuracyTheta = 0.015;

		config.markerShapes.add(new ConfigECoCheckMarkers.MarkerShape(4, 5, 0.5));
		config.markerShapes.add(new ConfigECoCheckMarkers.MarkerShape(6, 4, 0.5));

		types.add(ImageType.single(GrayU8.class));
		types.add(ImageType.single(GrayF32.class));
	}

	@Override
	public <T extends ImageBase<T>>
	FiducialDetector<T> createDetector( ImageType<T> imageType ) {
		return FactoryFiducial.ecocheck(null, config, imageType.getImageClass());
	}

	@Override
	public GrayF32 renderFiducial() {
		var utils = new ECoCheckUtils();
		config.convertToGridList(utils.markers);
		utils.fixate();

		var engine = new FiducialImageEngine();
		engine.configure(10, 3*squareToPixels, 5*squareToPixels);
		var generator = new ECoCheckGenerator(utils);
		generator.setRender(engine);
		generator.squareWidth = squareToPixels;
		generator.render(1);

//		ShowImages.showWindow(engine.getGrayF32(), "Foo");
//		BoofMiscOps.sleep(10_000);

		return engine.getGrayF32();
	}
}
