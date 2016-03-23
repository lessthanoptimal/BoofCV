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

package boofcv.alg.background.stationary;

import boofcv.alg.background.BackgroundModelStationary;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class TestBackgroundStationaryGaussian_PL extends GenericBackgroundStationaryGaussianChecks {

	public TestBackgroundStationaryGaussian_PL() {
		imageTypes.add(ImageType.pl(2, GrayU8.class));
		imageTypes.add(ImageType.pl(3, GrayU8.class));
		imageTypes.add(ImageType.pl(3, GrayF32.class));
	}

	@Override
	public <T extends ImageBase> BackgroundModelStationary<T>
	create(ImageType<T> imageType) {
		BackgroundStationaryGaussian alg = new BackgroundStationaryGaussian_PL(0.05f,10f,imageType);
		if( !Float.isNaN(initialVariance))
			alg.setInitialVariance(initialVariance);
		return alg;
	}
}
