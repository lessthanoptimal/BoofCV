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

package boofcv.abst.feature.orientation;

import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.orientation.GenericOrientationImageTests;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.BoofDefaults;
import boofcv.struct.image.GrayF32;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestOrientationSiftToImage {
	double angleTol = 0.1;

	/**
	 * Tests using generic tests for image orientation
	 */
	@Test
	public void generic() {

		ConfigSiftOrientation config = new ConfigSiftOrientation();
		double pixelRadiusAtRadius1 = config.sigmaEnlarge/BoofDefaults.SIFT_SCALE_TO_RADIUS;

		SiftScaleSpace ss = new SiftScaleSpace(-1,5,3,1.6);
		OrientationHistogramSift<GrayF32> orig = FactoryOrientationAlgs.sift(null,GrayF32.class);

		OrientationSiftToImage<GrayF32> alg =
				new OrientationSiftToImage<>(orig,ss,GrayF32.class);

		GenericOrientationImageTests tests = new GenericOrientationImageTests();
		tests.setup(angleTol, (int)(pixelRadiusAtRadius1+0.5), alg, GrayF32.class);
		tests.performAll();
	}
}
