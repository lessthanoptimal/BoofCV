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
import boofcv.alg.feature.dense.impl.DescribeDenseHogAlg_F32;
import boofcv.alg.feature.dense.impl.DescribeDenseHogAlg_MSF32;
import boofcv.alg.feature.dense.impl.DescribeDenseHogAlg_MSU8;
import boofcv.alg.feature.dense.impl.DescribeDenseHogAlg_U8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Returns low level implementations of dense image descriptor algorithms.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class FactoryDescribeImageDenseAlg {
	public static <T extends ImageBase, D extends ImageBase>
	DescribeDenseHogAlg<T,D> hog(ConfigDenseHoG config , ImageType<T> imageType ) {
		config.checkValidity();

		DescribeDenseHogAlg hog;
		if( imageType.getFamily() == ImageType.Family.SINGLE_BAND ) {
			switch( imageType.getDataType() ) {
				case U8:
					hog = new DescribeDenseHogAlg_U8(config.orientationBins,config.widthCell
							,config.widthBlock,config.stepBlock);
					break;

				case F32:
					hog = new DescribeDenseHogAlg_F32(config.orientationBins,config.widthCell
							,config.widthBlock,config.stepBlock);
					break;

				default:
					throw new IllegalArgumentException("Unsupported image type");
			}
		} else if( imageType.getFamily() == ImageType.Family.MULTI_SPECTRAL ) {
			switch( imageType.getDataType() ) {
				case U8:
					hog = new DescribeDenseHogAlg_MSU8(config.orientationBins,config.widthCell
							,config.widthBlock,config.stepBlock,imageType.getNumBands());
					break;

				case F32:
					hog = new DescribeDenseHogAlg_MSF32(config.orientationBins,config.widthCell
							,config.widthBlock,config.stepBlock,imageType.getNumBands());
					break;

				default:
					throw new IllegalArgumentException("Unsupported image type");
			}
		} else {
			throw new IllegalArgumentException("Unsupported image type");
		}

		return hog;
	}
}
