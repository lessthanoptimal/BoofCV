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

package boofcv.factory.feature.dense;

import boofcv.alg.feature.dense.DescribeDenseHogAlg;
import boofcv.alg.feature.dense.DescribeDenseHogFastAlg;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Returns low level implementations of dense image descriptor algorithms.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class FactoryDescribeImageDenseAlg {

	public static <T extends ImageBase>
	DescribeDenseHogAlg<T> hog(ConfigDenseHoG config , ImageType<T> imageType ) {
		config.checkValidity();

		return new DescribeDenseHogAlg<>(config.orientationBins, config.pixelsPerCell,
				config.cellsPerBlockX, config.cellsPerBlockY,
				config.stepBlock, imageType);

	}

	public static <T extends ImageBase>
	DescribeDenseHogFastAlg<T> hogFast(ConfigDenseHoG config , ImageType<T> imageType ) {
		config.checkValidity();

		return new DescribeDenseHogFastAlg(config.orientationBins,config.pixelsPerCell
							,config.cellsPerBlockX,config.cellsPerBlockY,config.stepBlock, imageType);
	}
}
