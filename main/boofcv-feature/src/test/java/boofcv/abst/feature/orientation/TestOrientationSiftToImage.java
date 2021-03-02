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

package boofcv.abst.feature.orientation;

import boofcv.BoofDefaults;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.orientation.GenericOrientationImageTests;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.image.GrayF32;

/**
 * @author Peter Abeles
 */
public class TestOrientationSiftToImage extends GenericOrientationImageTests<GrayF32> {
	final static double angleTol = 0.1;
	final static ConfigSiftOrientation config = new ConfigSiftOrientation();
	final static double pixelRadiusAtRadius1 = config.sigmaEnlarge*BoofDefaults.SIFT_SCALE_TO_RADIUS;

	TestOrientationSiftToImage() {
		super(angleTol, (int)(pixelRadiusAtRadius1+0.5), GrayF32.class);

		SiftScaleSpace ss = new SiftScaleSpace(-1,5,3,1.6);
		OrientationHistogramSift<GrayF32> orig = FactoryOrientationAlgs.sift(null,GrayF32.class);
		OrientationSiftToImage<GrayF32> alg = new OrientationSiftToImage<>(orig,ss,GrayF32.class);

		setRegionOrientation(alg);
	}

	@Override
	protected void copy() {
		// skipped because it's known not to be implemented and will take a bit of work to do so
	}
}
