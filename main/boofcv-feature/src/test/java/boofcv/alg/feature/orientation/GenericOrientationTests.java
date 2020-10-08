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

package boofcv.alg.feature.orientation;

import boofcv.abst.feature.orientation.RegionOrientation;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
abstract class GenericOrientationTests<T extends ImageGray<T>> extends BoofStandardJUnit {

	protected Class<T> imageType;
	private RegionOrientation alg;

	protected GenericOrientationTests( Class<T> imageType ) {
		this.imageType = imageType;
	}

	protected void setRegionOrientation( RegionOrientation alg ) {
		this.alg = alg;
	}

	protected abstract void setImage( RegionOrientation alg, T image );

	@Test
	protected void copy() {
		T input = GeneralizedImageOps.createSingleBand(imageType, 50, 60);
		int min = input.getImageType().getDataType().isSigned() ? -50 : 0;
		int max = 50;
		GImageMiscOps.fillUniform(input, rand, min, max);

		RegionOrientation copy = alg.copy();

		setImage(alg, input);
		setImage(copy, input);

		for (int i = 0; i < 4; i++) {
			int x = 15 + i*5;
			for (int j = 0; j < 4; j++) {
				int y = 17 + i*5;

				double expected = alg.compute(x, y);
				double found = copy.compute(x, y);

				assertEquals(expected, found, UtilEjml.TEST_F64);
			}
		}
	}
}
