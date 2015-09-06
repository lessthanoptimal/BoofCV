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
import boofcv.abst.feature.dense.GenericDenseDescribeImageDense;
import boofcv.abst.feature.describe.ConfigSurfDescribe;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.struct.feature.SurfFeature;
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
	 * @param configSample Describes how it should be sampled across the image
	 * @param imageType Type of input image.
	 * @return SURF description extractor
	 */
	public static <T extends ImageSingleBand, II extends ImageSingleBand>
	DescribeImageDense<T,SurfFeature> surfFast( ConfigSurfDescribe.Speed configSurf ,
												ConfigDenseSample configSample ,
												Class<T> imageType)
	{
		configSample.checkValidity();
		DescribeRegionPoint<T,SurfFeature> surf = FactoryDescribeRegionPoint.surfFast(configSurf, imageType);

		int width = (int)(surf.getCanonicalWidth()*configSample.scale+0.5);

		return new GenericDenseDescribeImageDense<T,SurfFeature>( surf , configSample.scale , width ,
				configSample.periodX, configSample.periodY );
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
	 * @param configSample Describes how it should be sampled across the image
	 * @param imageType Type of input image.
	 * @return SURF description extractor
	 */
	public static <T extends ImageSingleBand, II extends ImageSingleBand>
	DescribeImageDense<T,SurfFeature> surfStable(ConfigSurfDescribe.Stability configSurf,
												 ConfigDenseSample configSample ,
												 Class<T> imageType) {

		configSample.checkValidity();
		DescribeRegionPoint<T,SurfFeature> surf = FactoryDescribeRegionPoint.surfStable(configSurf, imageType);

		int width = (int)(surf.getCanonicalWidth()*configSample.scale+0.5);

		return new GenericDenseDescribeImageDense<T,SurfFeature>( surf , configSample.scale , width ,
				configSample.periodX, configSample.periodY );
	}
}
