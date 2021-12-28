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

import boofcv.abst.feature.convert.ConvertTupleDesc;
import boofcv.abst.feature.describe.*;
import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.feature.describe.DescribePointRawPixels;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.DescribePointSurfPlanar;
import boofcv.alg.feature.describe.brief.BinaryCompareDefinition_I32;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.feature.NccFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.*;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Factory for creating implementations of {@link DescribePointRadiusAngle}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("rawtypes")
public class FactoryDescribePointRadiusAngle {

	/**
	 * Factory function for creating many different types of region descriptors
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ImageBase<T>, TD extends TupleDesc<TD>>
	DescribePointRadiusAngle<T, TD> generic( ConfigDescribeRegion config, ImageType<T> imageType ) {
		Class imageClass = imageType.getImageClass();

		DescribePointRadiusAngle<T, TD> ret = switch (config.type) {
			case SURF_FAST -> (DescribePointRadiusAngle)surfFast(config.surfFast, imageClass);
			case SURF_STABLE -> (DescribePointRadiusAngle)surfStable(config.surfStability, imageClass);
			case SURF_COLOR_FAST -> (DescribePointRadiusAngle)surfColorFast(config.surfFast, (ImageType)imageType);
			case SURF_COLOR_STABLE -> (DescribePointRadiusAngle)surfColorStable(config.surfStability, imageType);
			case SIFT -> (DescribePointRadiusAngle)sift(config.scaleSpaceSift, config.sift, imageClass);
			case BRIEF -> (DescribePointRadiusAngle)brief(config.brief, imageClass);
			case TEMPLATE -> (DescribePointRadiusAngle)template(config.template, imageClass);
		};

		// See if it's in the native format and no need to modify the descriptor
		if (config.convert.outputData == ConfigConvertTupleDesc.DataType.NATIVE)
			return ret;

		// Descriptor is going to be modified, create the converter then wrap the algorithm
		int dof = ret.createDescription().size();
		ConvertTupleDesc converter = FactoryConvertTupleDesc.generic(config.convert, dof, ret.getDescriptionType());
		return new DescribePointRadiusAngleConvertTuple(ret, converter);
	}

	/**
	 * <p>
	 * Creates a SURF descriptor. SURF descriptors are invariant to illumination, orientation, and scale.
	 * BoofCV provides two variants. This SURF variant created here is designed for speed and sacrifices some stability.
	 * Different descriptors are produced for gray-scale and color images.
	 * </p>
	 *
	 * @param config SURF configuration. Pass in null for default options.
	 * @param imageType Type of input image.
	 * @return SURF description extractor
	 * @see DescribePointSurf
	 */
	public static <T extends ImageGray<T>, II extends ImageGray<II>>
	DescribePointRadiusAngle<T, TupleDesc_F64> surfFast( @Nullable ConfigSurfDescribe.Fast config, Class<T> imageType ) {
		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		DescribePointSurf<II> alg = FactoryDescribeAlgs.surfSpeed(config, integralType);

		return new DescribeSurf_RadiusAngle(alg, imageType);
	}

	/**
	 * Color variant of the SURF descriptor which has been designed for speed and sacrifices some stability.
	 *
	 * @param config SURF configuration. Pass in null for default options.
	 * @param imageType Type of input image.
	 * @return SURF color description extractor
	 * @see DescribePointSurfPlanar
	 */
	public static <T extends ImageMultiBand<T>, II extends ImageGray<II>>
	DescribePointRadiusAngle<T, TupleDesc_F64> surfColorFast( @Nullable ConfigSurfDescribe.Fast config, ImageType<T> imageType ) {

		Class bandType = imageType.getImageClass();
		Class<II> integralType = GIntegralImageOps.getIntegralType(bandType);

		DescribePointSurf<II> alg = FactoryDescribeAlgs.surfSpeed(config, integralType);

		if (imageType.getFamily() == ImageType.Family.PLANAR) {
			DescribePointSurfPlanar<II> color = FactoryDescribeAlgs.surfColor(alg, imageType.getNumBands());

			return new DescribeSurfPlanar_RadiusAngle(color, bandType, integralType);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
	}

	/**
	 * <p>
	 * Creates a SURF descriptor. SURF descriptors are invariant to illumination, orientation, and scale.
	 * BoofCV provides two variants. The SURF variant created here is designed for stability. Different
	 * descriptors are produced for gray-scale and color images.
	 * </p>
	 *
	 * @param config SURF configuration. Pass in null for default options.
	 * @param imageType Type of input image.
	 * @return SURF description extractor
	 * @see DescribePointSurf
	 */
	public static <T extends ImageGray<T>, II extends ImageGray<II>>
	DescribePointRadiusAngle<T, TupleDesc_F64> surfStable( @Nullable ConfigSurfDescribe.Stability config, Class<T> imageType ) {

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		DescribePointSurf<II> alg = FactoryDescribeAlgs.surfStability(config, integralType);

		return new DescribeSurf_RadiusAngle(alg, imageType);
	}

	/**
	 * Color variant of the SURF descriptor which has been designed for stability.
	 *
	 * @param config SURF configuration. Pass in null for default options.
	 * @param imageType Type of input image.
	 * @return SURF color description extractor
	 * @see DescribePointSurfPlanar
	 */
	public static <T extends ImageBase<T>, II extends ImageGray<II>>
	DescribePointRadiusAngle<T, TupleDesc_F64> surfColorStable( @Nullable ConfigSurfDescribe.Stability config, ImageType<T> imageType ) {

		Class bandType = imageType.getImageClass();
		Class<II> integralType = GIntegralImageOps.getIntegralType(bandType);

		DescribePointSurf<II> alg = FactoryDescribeAlgs.surfStability(config, integralType);

		if (imageType.getFamily() == ImageType.Family.PLANAR) {
			DescribePointSurfPlanar<II> color = FactoryDescribeAlgs.surfColor(alg, imageType.getNumBands());

			return new DescribeSurfPlanar_RadiusAngle(color, bandType, integralType);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
	}

	/**
	 * <p>
	 * Creates a SIFT region descriptor.
	 * </p>
	 *
	 * <p>
	 * NOTE: If detecting and describing SIFT features then it is more efficient to use
	 * {@link boofcv.factory.feature.detdesc.FactoryDetectDescribe#sift} instead
	 * </p>
	 *
	 * @param configSS SIFT scale-space configuration. Pass in null for default options.
	 * @param configDescribe SIFT descriptor configuration. Pass in null for default options.
	 * @return SIFT descriptor
	 */
	public static <T extends ImageGray<T>>
	DescribePointRadiusAngle<T, TupleDesc_F64> sift( @Nullable ConfigSiftScaleSpace configSS,
													 @Nullable ConfigSiftDescribe configDescribe, Class<T> imageType ) {
		if (configSS == null)
			configSS = new ConfigSiftScaleSpace();
		configSS.checkValidity();

		SiftScaleSpace ss = new SiftScaleSpace(configSS.firstOctave, configSS.lastOctave, configSS.numScales,
				configSS.sigma0);

		DescribePointSift<GrayF32> alg = FactoryDescribeAlgs.sift(configDescribe, GrayF32.class);

		return new DescribeSift_RadiusAngle<>(ss, alg, imageType);
	}

	/**
	 * <p>
	 * Creates a BRIEF descriptor.
	 * </p>
	 *
	 * @param config Configuration for BRIEF descriptor. If null then default is used.
	 * @param imageType Type of gray scale image it processes.
	 * @return BRIEF descriptor
	 * @see boofcv.alg.feature.describe.DescribePointBrief
	 * @see boofcv.alg.feature.describe.DescribePointBriefSO
	 */
	public static <T extends ImageGray<T>>
	DescribePointRadiusAngle<T, TupleDesc_B> brief( @Nullable ConfigBrief config, Class<T> imageType ) {
		if (config == null)
			config = new ConfigBrief();
		config.checkValidity();

		BlurFilter<T> filter = FactoryBlurFilter.gaussian(ImageType.single(imageType), config.blurSigma, config.blurRadius);
		BinaryCompareDefinition_I32 definition =
				FactoryBriefDefinition.gaussian2(new Random(123), config.radius, config.numPoints);

		if (config.fixed) {
			return new DescribeBrief_RadiusAngle<>(FactoryDescribeAlgs.brief(definition, filter), imageType);
		} else {
			return new DescribeBriefSO_RadiusAngle<>(FactoryDescribeAlgs.briefso(definition, filter), imageType);
		}
	}

	/**
	 * Creates a template based descriptor.
	 *
	 * @param config The configuration.
	 * @param imageType Type of input image
	 * @return Pixel region descriptor
	 * @see ConfigTemplateDescribe
	 */
	public static <T extends ImageGray<T>, TD extends TupleDesc<TD>>
	DescribePointRadiusAngle<T, TD> template( @Nullable ConfigTemplateDescribe config, Class<T> imageType ) {
		if (config == null)
			config = new ConfigTemplateDescribe();

		return switch (config.type) {
			case PIXEL -> new DescribePointRawPixels_RadiusAngle<T, TD>(
					FactoryDescribeAlgs.pixelRegion(config.width, config.height, imageType), imageType);
			case NCC -> new DescribeNCC_RadiusAngle(
					FactoryDescribeAlgs.pixelRegionNCC(config.width, config.height, imageType), imageType);
		};
	}

	/**
	 * Creates a region descriptor based on pixel intensity values alone. A classic and fast to compute
	 * descriptor, but much less stable than more modern ones.
	 *
	 * @param regionWidth How wide the pixel region is.
	 * @param regionHeight How tall the pixel region is.
	 * @param imageType Type of image it will process.
	 * @return Pixel region descriptor
	 * @see DescribePointRawPixels
	 */
	@SuppressWarnings({"unchecked"})
	public static <T extends ImageGray<T>, TD extends TupleDesc<TD>>
	DescribePointRadiusAngle<T, TD> pixel( int regionWidth, int regionHeight, Class<T> imageType ) {
		return new DescribePointRawPixels_RadiusAngle(
				FactoryDescribeAlgs.pixelRegion(regionWidth, regionHeight, imageType), imageType);
	}

	/**
	 * Creates a region descriptor based on normalized pixel intensity values alone. This descriptor
	 * is designed to be light invariance, but is still less stable than more modern ones.
	 *
	 * @param regionWidth How wide the pixel region is.
	 * @param regionHeight How tall the pixel region is.
	 * @param imageType Type of image it will process.
	 * @return Pixel region descriptor
	 * @see boofcv.alg.feature.describe.DescribePointPixelRegionNCC
	 */
	@SuppressWarnings({"unchecked"})
	public static <T extends ImageGray<T>>
	DescribePointRadiusAngle<T, NccFeature> pixelNCC( int regionWidth, int regionHeight, Class<T> imageType ) {
		return new DescribeNCC_RadiusAngle(
				FactoryDescribeAlgs.pixelRegionNCC(regionWidth, regionHeight, imageType), imageType);
	}
}
