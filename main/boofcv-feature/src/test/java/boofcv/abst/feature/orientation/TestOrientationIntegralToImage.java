/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.GrayF32;

public class TestOrientationIntegralToImage extends GenericOrientationImageTests {
	static final double angleTol = 0.01;

	public TestOrientationIntegralToImage() {
		super(angleTol, r*2+1, GrayF32.class);

		ConfigAverageIntegral config = new ConfigAverageIntegral();
		config.radius = r;
		config.objectRadiusToScale = 1/3.0;
		OrientationIntegral<GrayF32> orig = FactoryOrientationAlgs.average_ii(config, GrayF32.class);

		OrientationIntegralToImage<GrayF32,GrayF32>
				alg = new OrientationIntegralToImage<>(orig,
				GrayF32.class, GrayF32.class);

		setRegionOrientation(alg);
	}

	@Override
	protected void copy() {
		// skipped because it's known not to be implemented and will take a bit of work to do so
	}
}
