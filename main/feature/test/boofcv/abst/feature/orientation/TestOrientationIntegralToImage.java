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

import boofcv.alg.feature.orientation.GenericOrientationImageTests;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestOrientationIntegralToImage {
	double angleTol = 0.01;
	int r = 3;

	/**
	 * Tests using generic tests for image orientation
	 */
	@Test
	public void generic() {
		ConfigAverageIntegral config = new ConfigAverageIntegral();
		config.radius = r;
		OrientationIntegral<ImageFloat32> orig = FactoryOrientationAlgs.average_ii(config, ImageFloat32.class);

		OrientationIntegralToImage<ImageFloat32,ImageFloat32>
				alg = new OrientationIntegralToImage<ImageFloat32, ImageFloat32>(orig,
				ImageFloat32.class,ImageFloat32.class);

		GenericOrientationImageTests tests = new GenericOrientationImageTests();
		tests.setup(angleTol, r * 2 + 1, alg, ImageFloat32.class);
		tests.performAll();
	}
}
