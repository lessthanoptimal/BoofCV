/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.describe.DescribePointGaussian12;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.describe.DescribePointSteerable2D;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.brief.BriefDefinition_I32;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.feature.*;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

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
	 * BoofCV provides two variants. BoofCV provides two variants, described below.
	 * </p>
	 *
	 * <p>
	 * The modified variant provides comparable stability to binary provided by the original author.  The
	 * other variant is much faster, but a bit less stable, Both implementations contain several algorithmic
	 * changes from what was described in the original SURF paper.  See tech report [1] for details.
	 * <p>
	 *
	 * @see DescribePointSurf
	 *
	 * @param modified True for more stable but slower and false for a faster but less stable.
	 * @param imageType Type of input image.
	 * @return SURF description extractor
	 */
	public static <T extends ImageSingleBand, II extends ImageSingleBand>
	DescribeRegionPoint<T,SurfFeature> surf( boolean modified , Class<T> imageType) {

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		DescribePointSurf<II> alg;

		if( modified ) {
			alg = FactoryDescribePointAlgs.<II>msurf(integralType);
		} else {
			alg = FactoryDescribePointAlgs.<II>surf(integralType);
		}

		return new WrapDescribeSurf<T,II>( alg );
	}

	/**
	 *
	 * <p>
	 * NOTE: Only a single orientation hypothesis is considered when using this interface.  Consider
	 * using {@link boofcv.factory.feature.detdesc.FactoryDetectDescribe#sift(int, boolean, int)} instead
	 * </p>
	 *
	 * @param scaleSigma
	 * @param numOfScales
	 * @param numOfOctaves
	 * @param doubleInputImage
	 * @return
	 */
	public static DescribeRegionPoint<ImageFloat32,SurfFeature> sift( double scaleSigma ,
																	  int numOfScales ,
																	  int numOfOctaves ,
																	  boolean doubleInputImage ) {
		SiftImageScaleSpace ss = new SiftImageScaleSpace((float)scaleSigma, numOfScales, numOfOctaves,
				doubleInputImage);


		DescribePointSift alg = FactoryDescribePointAlgs.sift(4, 8, 8);

		return new WrapDescribeSift(alg,ss);
	}

	/**
	 * Steerable Gaussian descriptor normalized by 1st order gradient.
	 *
	 * @see DescribePointGaussian12
	 *
	 * @param radius How large the kernel should be. Try 20.
	 * @param imageType Type of input image.
	 * @param derivType Type of image the gradient is.
	 * @param <T> Input image type
	 * @param <D> Derivative type
	 * @return Steerable gaussian descriptor.
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	DescribeRegionPoint<T,TupleDesc_F64> gaussian12( int radius ,Class<T> imageType , Class<D> derivType ) {

		ImageGradient<T,D> gradient = FactoryDerivative.sobel(imageType,derivType);
		DescribePointGaussian12<T, ?> steer = FactoryDescribePointAlgs.steerableGaussian12(radius, imageType);

		return new WrapDescribeGaussian12<T,D>(steer,gradient,imageType,derivType);
	}

	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	DescribeRegionPoint<T,TupleDesc_F64> steerableGaussian( int radius , boolean normalized ,
													Class<T> imageType , Class<D> derivType ) {

		ImageGradient<T,D> gradient = FactoryDerivative.sobel(imageType,derivType);
		DescribePointSteerable2D<T, ?> steer = FactoryDescribePointAlgs.steerableGaussian(normalized, -1, radius, imageType);

		return new WrapDescribeSteerable<T,D>(steer,gradient,imageType,derivType);
	}

	/**
	 * <p>
	 * The BRIEF descriptor is HORRIBLY inefficient when used through this interface.  This functionality is only
	 * provided for testing and validation purposes.
	 * </p>
	 *
	 * @see boofcv.alg.feature.describe.DescribePointBrief
	 * @see boofcv.alg.feature.describe.DescribePointBriefSO
	 *
	 * @param radius Region's radius.  Typical value is 16.
	 * @param numPoints Number of feature/points.  Typical value is 512.
	 * @param blurSigma Typical value is -1.
	 * @param blurRadius Typical value is 4.
	 * @param isFixed Is the orientation and scale fixed? true for original algorithm described in BRIEF paper.
	 * @param imageType Type of gray scale image it processes.
	 * @return BRIEF descriptor
	 */
	public static <T extends ImageSingleBand>
	DescribeRegionPoint<T,TupleDesc_B> brief(int radius, int numPoints,
									   double blurSigma, int blurRadius,
									   boolean isFixed,
									   Class<T> imageType)
	{
		BlurFilter<T> filter = FactoryBlurFilter.gaussian(imageType,blurSigma,blurRadius);
		BriefDefinition_I32 definition = FactoryBriefDefinition.gaussian2(new Random(123), radius, numPoints);

		if( isFixed) {
			return new WrapDescribeBrief<T>(FactoryDescribePointAlgs.brief(definition,filter));
		} else {
			return new WrapDescribeBriefSo<T>(FactoryDescribePointAlgs.briefso(definition, filter));
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
	public static <T extends ImageSingleBand, D extends TupleDesc>
	DescribeRegionPoint<T,D> pixel( int regionWidth , int regionHeight , Class<T> imageType ) {
		return new WrapDescribePixelRegion(FactoryDescribePointAlgs.pixelRegion(regionWidth,regionHeight,imageType));
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
	public static <T extends ImageSingleBand>
	DescribeRegionPoint<T,NccFeature> pixelNCC( int regionWidth , int regionHeight , Class<T> imageType ) {
		return new WrapDescribePixelRegionNCC(FactoryDescribePointAlgs.pixelRegionNCC(regionWidth,regionHeight,imageType));
	}
}
