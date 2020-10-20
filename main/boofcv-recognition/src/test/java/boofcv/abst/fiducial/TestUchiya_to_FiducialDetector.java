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

import boofcv.BoofTesting;
import boofcv.alg.fiducial.dots.RandomDotMarkerGeneratorImage;
import boofcv.factory.fiducial.ConfigUchiyaMarker;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class TestUchiya_to_FiducialDetector extends GenericFiducialTrackerChecks {

	Random rand = BoofTesting.createRandom(0);
	List<List<Point2D_F64>> documents = new ArrayList<>();
	double markerLength = 90;

	public TestUchiya_to_FiducialDetector() {
		stabilityShrink = 0.5;
		tolAccuracyTheta = 0.015;

		types.add( ImageType.single(GrayU8.class));
		types.add( ImageType.single(GrayF32.class));

		for (int i = 0; i < 20; i++) {
			documents.add( RandomDotMarkerGeneratorImage.createRandomMarker(rand,20, markerLength,markerLength,11));
		}
	}

	@Override
	public <T extends ImageBase<T>>
	FiducialDetector<T> createDetector(ImageType<T> imageType)
	{
		var config = new ConfigUchiyaMarker();
		config.ransac.inlierThreshold = 1.0;
		config.markerWidth = markerLength;
		config.markerHeight = markerLength;

		Uchiya_to_FiducialDetector detector = FactoryFiducial.randomDots(config,imageType.getImageClass());
		for( var pts : documents ) {
			detector.addMarker(pts);
		}

		return detector;
	}

	@Override
	public GrayF32 renderFiducial() {
		int target = 3;

		var generator = new RandomDotMarkerGeneratorImage();
		generator.setRadius(20);
		generator.configure(600,600,20);
		generator.render(documents.get(target), markerLength, markerLength);

//		ShowImages.showBlocking(generator.getImage(),"Raw Render",1000);

		return generator.getImageF32();
	}
}
