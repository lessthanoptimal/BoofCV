/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.dense.DescribeImageDense;
import boofcv.abst.feature.dense.DescribeImageDenseSift;
import boofcv.abst.feature.dense.GenericDenseDescribeImageDense;
import boofcv.abst.feature.describe.ConfigSiftDescribe;
import boofcv.abst.feature.describe.ConfigSurfDescribe;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.alg.feature.dense.DescribeDenseSiftAlg;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.struct.BoofDefaults;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.image.ImageSingleBand;

/**
 * Factory for creating {@link DescribeImageDense}.
 *
 * @author Peter Abeles
 */
public class FactoryDescribeImageDense {
	/**
	 * <p>
	 * Creates a SURF descriptor.  SURF descriptors are invariant to illumination, orientation, and scale.
	 * BoofCV provides two variants. This SURF variant created here is designed for speed and sacrifices some stability.
	 * Different descriptors are produced for gray-scale and color images.
	 * </p>
	 *
	 * @see DescribePointSurf
	 *
	 * @param configSurf SURF configuration. Pass in null for default options.
	 * @param imageType Type of input image.
	 * @return SURF description extractor
	 */
	public static <T extends ImageSingleBand, II extends ImageSingleBand>
	DescribeImageDense<T,BrightFeature> surfFast(ConfigSurfDescribe.Speed configSurf , Class<T> imageType)
	{
		DescribeRegionPoint<T,BrightFeature> surf = FactoryDescribeRegionPoint.surfFast(configSurf, imageType);

		return new GenericDenseDescribeImageDense<T,BrightFeature>( surf , BoofDefaults.SURF_SCALE_TO_RADIUS );
	}

	/**
	 * <p>
	 * Creates a SURF descriptor.  SURF descriptors are invariant to illumination, orientation, and scale.
	 * BoofCV provides two variants. The SURF variant created here is designed for stability.  Different
	 * descriptors are produced for gray-scale and color images.
	 * </p>
	 *
	 * @see DescribePointSurf
	 *
	 * @param configSurf SURF configuration. Pass in null for default options.
	 * @param imageType Type of input image.
	 * @return SURF description extractor
	 */
	public static <T extends ImageSingleBand, II extends ImageSingleBand>
	DescribeImageDense<T,BrightFeature> surfStable(ConfigSurfDescribe.Stability configSurf,
												   Class<T> imageType) {

		DescribeRegionPoint<T,BrightFeature> surf = FactoryDescribeRegionPoint.surfStable(configSurf, imageType);

		return new GenericDenseDescribeImageDense<T,BrightFeature>( surf , BoofDefaults.SURF_SCALE_TO_RADIUS);
	}

	/**
	 * Creates a dense SIFT descriptor.
	 *
	 * @param config Configuration for SIFT descriptor.  All parameters are used but sigmaToPixels.  null for defaults.
	 * @param imageType Type of input image
	 * @return Dense SIFT
	 */
	public static <T extends ImageSingleBand>
	DescribeImageDense<T,BrightFeature> sift( ConfigSiftDescribe config , Class<T> imageType ) {
		if( config == null )
			config = new ConfigSiftDescribe();

		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		DescribeDenseSiftAlg alg = new DescribeDenseSiftAlg(config.widthSubregion,config.widthGrid,
				config.numHistogramBins,config.weightingSigmaFraction,config.maxDescriptorElementValue,1,1,derivType);

		return new DescribeImageDenseSift(alg,imageType);
	}
}
