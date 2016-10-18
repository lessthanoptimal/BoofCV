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

import boofcv.abst.feature.dense.*;
import boofcv.abst.feature.describe.ConfigSiftDescribe;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.alg.feature.dense.BaseDenseHog;
import boofcv.alg.feature.dense.DescribeDenseHogAlg;
import boofcv.alg.feature.dense.DescribeDenseHogFastAlg;
import boofcv.alg.feature.dense.DescribeDenseSiftAlg;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.struct.BoofDefaults;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Factory for creating {@link DescribeImageDense}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
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
	 * @param config SURF configuration. Pass in null for default options.
	 * @param imageType Type of input image.
	 * @return SURF description extractor
	 */
	public static <T extends ImageGray, II extends ImageGray>
	DescribeImageDense<T,TupleDesc_F64> surfFast(ConfigDenseSurfFast config , Class<T> imageType)
	{
		if( config == null )
			config = new ConfigDenseSurfFast();

		DescribeRegionPoint<T,TupleDesc_F64> surf =
				(DescribeRegionPoint)FactoryDescribeRegionPoint.surfFast(config.surf, imageType);

		return new GenericDenseDescribeImageDense<>(surf, BoofDefaults.SURF_SCALE_TO_RADIUS,
				config.descriptorScale, config.sampling.periodX, config.sampling.periodY);
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
	 * @param config SURF configuration. Pass in null for default options.
	 * @param imageType Type of input image.
	 * @return SURF description extractor
	 */
	public static <T extends ImageGray, II extends ImageGray>
	DescribeImageDense<T,TupleDesc_F64> surfStable( ConfigDenseSurfStable config,
												   Class<T> imageType) {

		if( config == null )
			config = new ConfigDenseSurfStable();

		config.checkValidity();

		DescribeRegionPoint<T,TupleDesc_F64> surf =
				(DescribeRegionPoint)FactoryDescribeRegionPoint.surfStable(config.surf, imageType);

		return new GenericDenseDescribeImageDense<>(surf, BoofDefaults.SURF_SCALE_TO_RADIUS,
				config.descriptorScale, config.sampling.periodX, config.sampling.periodY);
	}

	/**
	 * Creates a dense SIFT descriptor.
	 *
	 * @see DescribeDenseSiftAlg
	 *
	 * @param config Configuration for SIFT descriptor. null for defaults.
	 * @param imageType Type of input image
	 * @return Dense SIFT
	 */
	public static <T extends ImageGray>
	DescribeImageDense<T,TupleDesc_F64> sift(ConfigDenseSift config , Class<T> imageType ) {
		if( config == null )
			config = new ConfigDenseSift();

		config.checkValidity();

		ConfigSiftDescribe c = config.sift;

		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		DescribeDenseSiftAlg alg = new DescribeDenseSiftAlg(c.widthSubregion,c.widthGrid,
				c.numHistogramBins,c.weightingSigmaFraction,c.maxDescriptorElementValue,1,1,derivType);

		return new DescribeImageDenseSift(alg,config.sampling.periodX,config.sampling.periodY,imageType);
	}


	/**
	 * Creates a dense HOG descriptor.
	 *
	 * @see DescribeDenseHogFastAlg
	 * @see DescribeDenseHogAlg
	 *
	 * @param config Configuration for HOG descriptor.  Can't be null.
	 * @param imageType Type of input image.  Can be single band or planar
	 * @return Dense HOG extractor
	 */
	public static <T extends ImageBase>
	DescribeImageDense<T,TupleDesc_F64> hog(ConfigDenseHoG config , ImageType<T> imageType ) {
		if( config == null )
			config = new ConfigDenseHoG();

		config.checkValidity();

		ImageType actualType;
		if( imageType.getDataType() != ImageDataType.F32 ) {
			actualType = new ImageType(imageType.getFamily(),ImageDataType.F32,imageType.getNumBands());
		} else {
			actualType = imageType;
		}

		BaseDenseHog hog;
		if( config.fastVariant ) {
			hog = FactoryDescribeImageDenseAlg.hogFast(config, actualType);
		} else {
			hog = FactoryDescribeImageDenseAlg.hog(config, actualType);
		}

		DescribeImageDenseHoG output = new DescribeImageDenseHoG(hog);

		// If the data type isn't F32 convert it into that data type first
		if( actualType != imageType ) {
			return new DescribeImageDense_Convert<>(output, imageType);
		} else {
			return output;
		}
	}
}
