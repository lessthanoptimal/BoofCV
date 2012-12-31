/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.alg.feature.orientation.GenericOrientationImageTests;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.image.ImageFloat32;
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
		SiftImageScaleSpace ss = new SiftImageScaleSpace(1.6f,5,4,false);
		OrientationHistogramSift orig = FactoryOrientationAlgs.sift(null);

		OrientationSiftToImage alg = new OrientationSiftToImage(orig,ss);

		GenericOrientationImageTests tests = new GenericOrientationImageTests();
		tests.setup(angleTol, (int)(2*2.5*1.5), alg, ImageFloat32.class);
		tests.performAll();
	}
}
