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

package boofcv.factory.feature.describe;

import boofcv.abst.feature.describe.*;
import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.DescribePointSurfPlanar;
import boofcv.alg.feature.describe.brief.BinaryCompareDefinition_I32;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.feature.*;
import boofcv.struct.image.*;

import java.util.Random;


/**
 * Factory for creating implementations of {@link DescribeRegionPoint}.
 *
 * @author Peter Abeles
 */
public class FactoryDescribeRegionPoint {

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
	DescribeRegionPoint<T,BrightFeature> surfFast(ConfigSurfDescribe.Speed config , Class<T> imageType) {


		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		DescribePointSurf<II> alg = FactoryDescribePointAlgs.surfSpeed( config, integralType);

		return new WrapDescribeSurf( alg , imageType );
	}

	/**
	 * Color variant of the SURF descriptor which has been designed for speed and sacrifices some stability.
	 *
	 * @see DescribePointSurfPlanar
	 *
	 * @param config SURF configuration. Pass in null for default options.
	 * @param imageType Type of input image.
	 * @return SURF color description extractor
	 */
	public static <T extends ImageMultiBand, II extends ImageGray>
	DescribeRegionPoint<T,BrightFeature> surfColorFast(ConfigSurfDescribe.Speed config , ImageType<T> imageType) {

		Class bandType = imageType.getImageClass();
		Class<II> integralType = GIntegralImageOps.getIntegralType(bandType);

		DescribePointSurf<II> alg = FactoryDescribePointAlgs.surfSpeed( config, integralType);

		if( imageType.getFamily() == ImageType.Family.PLANAR) {
			DescribePointSurfPlanar<II> color = FactoryDescribePointAlgs.surfColor( alg,imageType.getNumBands());

			return new SurfPlanar_to_DescribeRegionPoint(color,bandType,integralType);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
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
	DescribeRegionPoint<T,BrightFeature> surfStable(ConfigSurfDescribe.Stability config, Class<T> imageType) {

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		DescribePointSurf<II> alg = FactoryDescribePointAlgs.surfStability( config, integralType);

		return new WrapDescribeSurf( alg , imageType );
	}

	/**
	 * Color variant of the SURF descriptor which has been designed for stability.
	 *
	 * @see DescribePointSurfPlanar
	 *
	 * @param config SURF configuration. Pass in null for default options.
	 * @param imageType Type of input image.
	 * @return SURF color description extractor
	 */
	public static <T extends ImageBase, II extends ImageGray>
	DescribeRegionPoint<T,BrightFeature> surfColorStable(ConfigSurfDescribe.Stability config, ImageType<T> imageType) {

		Class bandType = imageType.getImageClass();
		Class<II> integralType = GIntegralImageOps.getIntegralType(bandType);

		DescribePointSurf<II> alg = FactoryDescribePointAlgs.surfStability( config, integralType);

		if( imageType.getFamily() == ImageType.Family.PLANAR) {
			DescribePointSurfPlanar<II> color = FactoryDescribePointAlgs.surfColor( alg,imageType.getNumBands());

			return new SurfPlanar_to_DescribeRegionPoint(color,bandType,integralType);
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
	 * @param configDescribe SIFT descriptor configuration.  Pass in null for default options.
	 * @return SIFT descriptor
	 */
	public static <T extends ImageGray>
	DescribeRegionPoint<T,TupleDesc_F64> sift(
			ConfigSiftScaleSpace configSS, ConfigSiftDescribe configDescribe, Class<T> imageType)
	{
		if( configSS == null )
			configSS = new ConfigSiftScaleSpace();
		configSS.checkValidity();

		SiftScaleSpace ss = new SiftScaleSpace(configSS.firstOctave, configSS.lastOctave, configSS.numScales,
				configSS.sigma0);

		DescribePointSift<GrayF32> alg = FactoryDescribePointAlgs.sift(configDescribe,GrayF32.class);

		return new DescribeRegionPoint_SIFT<>(ss, alg, imageType);
	}

	/**
	 * <p>
	 * Creates a BRIEF descriptor.
	 * </p>
	 *
	 * @see boofcv.alg.feature.describe.DescribePointBrief
	 * @see boofcv.alg.feature.describe.DescribePointBriefSO
	 *
	 * @param config Configuration for BRIEF descriptor.  If null then default is used.
	 * @param imageType Type of gray scale image it processes.
	 * @return BRIEF descriptor
	 */
	public static <T extends ImageGray>
	DescribeRegionPoint<T,TupleDesc_B> brief( ConfigBrief config , Class<T> imageType)
	{
		if( config == null )
			config = new ConfigBrief();
		config.checkValidity();

		BlurFilter<T> filter = FactoryBlurFilter.gaussian(imageType,config.blurSigma,config.blurRadius);
		BinaryCompareDefinition_I32 definition =
				FactoryBriefDefinition.gaussian2(new Random(123), config.radius, config.numPoints);

		if( config.fixed) {
			return new WrapDescribeBrief<>(FactoryDescribePointAlgs.brief(definition, filter), imageType);
		} else {
			return new WrapDescribeBriefSo<>(FactoryDescribePointAlgs.briefso(definition, filter), imageType);
		}
	}

	/**
	 * Creates a region descriptor based on pixel intensity values alone.  A classic and fast to compute
	 * descriptor, but much less stable than more modern ones.
	 *
	 * @see boofcv.alg.feature.describe.DescribePointPixelRegion
	 *
	 * @param regionWidth How wide the pixel region is.
	 * @param regionHeight How tall the pixel region is.
	 * @param imageType Type of image it will process.
	 * @return Pixel region descriptor
	 */
	@SuppressWarnings({"unchecked"})
	public static <T extends ImageGray, D extends TupleDesc>
	DescribeRegionPoint<T,D> pixel( int regionWidth , int regionHeight , Class<T> imageType ) {
		return new WrapDescribePixelRegion(
				FactoryDescribePointAlgs.pixelRegion(regionWidth,regionHeight,imageType),imageType);
	}

	/**
	 * Creates a region descriptor based on normalized pixel intensity values alone.  This descriptor
	 * is designed to be light invariance, but is still less stable than more modern ones.
	 *
	 * @see boofcv.alg.feature.describe.DescribePointPixelRegionNCC
	 *
	 * @param regionWidth How wide the pixel region is.
	 * @param regionHeight How tall the pixel region is.
	 * @param imageType Type of image it will process.
	 * @return Pixel region descriptor
	 */
	@SuppressWarnings({"unchecked"})
	public static <T extends ImageGray>
	DescribeRegionPoint<T,NccFeature> pixelNCC( int regionWidth , int regionHeight , Class<T> imageType ) {
		return new WrapDescribePixelRegionNCC(
				FactoryDescribePointAlgs.pixelRegionNCC(regionWidth,regionHeight,imageType),imageType);
	}
}
