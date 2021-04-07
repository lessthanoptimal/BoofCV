/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.describe;

import boofcv.abst.feature.describe.ConfigSiftDescribe;
import boofcv.abst.feature.describe.ConfigSurfDescribe;
import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.feature.describe.*;
import boofcv.alg.feature.describe.brief.BinaryCompareDefinition_I32;
import boofcv.alg.feature.describe.impl.*;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.jetbrains.annotations.Nullable;

/**
 * Creates algorithms for describing point features.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryDescribeAlgs {

	public static <T extends ImageGray<T>>
	DescribePointSurf<T> surfSpeed( @Nullable ConfigSurfDescribe.Fast config, Class<T> imageType ) {
		if (config == null)
			config = new ConfigSurfDescribe.Fast();
		config.checkValidity();


		return new DescribePointSurf<>(config.widthLargeGrid, config.widthSubRegion, config.widthSample,
				config.weightSigma, config.useHaar, imageType);
	}

	public static <T extends ImageGray<T>>
	DescribePointSurfMod<T> surfStability( @Nullable ConfigSurfDescribe.Stability config, Class<T> imageType ) {
		if (config == null)
			config = new ConfigSurfDescribe.Stability();
		config.checkValidity();

		return new DescribePointSurfMod<>(config.widthLargeGrid, config.widthSubRegion, config.widthSample,
				config.overLap, config.sigmaLargeGrid, config.sigmaSubRegion, config.useHaar, imageType);
	}

	public static <T extends ImageGray<T>>
	DescribePointSurfPlanar<T> surfColor( DescribePointSurf<T> describe, int numBands ) {

		return new DescribePointSurfPlanar<>(describe, numBands);
	}

	public static <T extends ImageGray<T>>
	DescribePointBrief<T> brief( BinaryCompareDefinition_I32 definition, BlurFilter<T> filterBlur ) {
		Class<T> imageType = filterBlur.getInputType().getImageClass();

		DescribePointBinaryCompare<T> compare;

		if (imageType == GrayF32.class) {
			compare = (DescribePointBinaryCompare<T>)new ImplDescribeBinaryCompare_F32(definition);
		} else if (imageType == GrayU8.class) {
			compare = (DescribePointBinaryCompare<T>)new ImplDescribeBinaryCompare_U8(definition);
		} else {
			throw new IllegalArgumentException("Unknown image type: " + imageType.getSimpleName());
		}

		return new DescribePointBrief<>(compare, filterBlur);
	}

	// todo remove filterBlur for all BRIEF change to radius,sigma,type
	public static <T extends ImageGray<T>>
	DescribePointBriefSO<T> briefso( BinaryCompareDefinition_I32 definition, BlurFilter<T> filterBlur ) {
		Class<T> imageType = filterBlur.getInputType().getImageClass();

		InterpolatePixelS<T> interp = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);

		return new DescribePointBriefSO<>(definition, filterBlur, interp);
	}

	public static <T extends ImageGray<T>>
	DescribePointSift<T> sift( @Nullable ConfigSiftDescribe config, Class<T> derivType ) {
		if (config == null)
			config = new ConfigSiftDescribe();

		return new DescribePointSift(config.widthSubregion, config.widthGrid, config.numHistogramBins
				, config.sigmaToPixels, config.weightingSigmaFraction, config.maxDescriptorElementValue, derivType);
	}

	public static <T extends ImageGray<T>, TD extends TupleDesc<TD>>
	DescribePointRawPixels<T, TD> pixelRegion( int regionWidth, int regionHeight, Class<T> imageType ) {
		if (imageType == GrayF32.class) {
			return (DescribePointRawPixels<T, TD>)new ImplDescribePointPixelRegion_F32(regionWidth, regionHeight);
		} else if (imageType == GrayU8.class) {
			return (DescribePointRawPixels<T, TD>)new ImplDescribePointPixelRegion_U8(regionWidth, regionHeight);
		} else {
			throw new IllegalArgumentException("Unsupported image type");
		}
	}

	public static <T extends ImageGray<T>>
	DescribePointPixelRegionNCC<T> pixelRegionNCC( int regionWidth, int regionHeight, Class<T> imageType ) {
		if (imageType == GrayF32.class) {
			return (DescribePointPixelRegionNCC<T>)new ImplDescribePointPixelRegionNCC_F32(regionWidth, regionHeight);
		} else if (imageType == GrayU8.class) {
			return (DescribePointPixelRegionNCC<T>)new ImplDescribePointPixelRegionNCC_U8(regionWidth, regionHeight);
		} else {
			throw new IllegalArgumentException("Unsupported image type");
		}
	}
}
