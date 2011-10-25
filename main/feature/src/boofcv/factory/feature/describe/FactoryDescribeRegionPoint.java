/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
import boofcv.alg.feature.describe.DescribePointSteerable2D;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.brief.BriefDefinition_I32;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.feature.orientation.OrientationIntegral;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageBase;

import java.util.Random;


/**
 * Factory for creating implementations of {@link DescribeRegionPoint}.
 *
 * @author Peter Abeles
 */
public class FactoryDescribeRegionPoint {

	/**
	 * <p>
	 * Standard SURF descriptor configured to balance speed and descriptor stability. Invariant
	 * to illumination, orientation, and scale.
	 * </p>
	 *
	 * @param isOriented True for orientation invariant.
	 * @param imageType Type of input image.
	 * @return SURF description extractor
	 */
	public static <T extends ImageBase, II extends ImageBase>
	DescribeRegionPoint<T> surf( boolean isOriented , Class<T> imageType) {
		OrientationIntegral<II> orientation = null;

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		if( isOriented )
			orientation = FactoryOrientationAlgs.average_ii(6, true, integralType);
//			orientation = FactoryOrientationAlgs.sliding_ii(42,Math.PI/3.0,6,true,integralType);

		DescribePointSurf<II> alg = FactoryDescribePointAlgs.<II>surf(integralType);
		return new WrapDescribeSurf<T,II>( alg ,orientation);
	}

	/**
	 * <p>
	 * Modified SURF descriptor configured for optimal descriptor stability.  Runs slower
	 * than {@link #surf(boolean, Class)}, but produces more stable results.
	 * </p>
	 *
	 * @param isOriented True for orientation invariant.
	 * @param imageType Type of input image.
	 * @return SURF description extractor
	 */
	public static <T extends ImageBase, II extends ImageBase>
	DescribeRegionPoint<T> msurf( boolean isOriented , Class<T> imageType) {
		OrientationIntegral<II> orientation = null;

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		if( isOriented )
//			orientation = FactoryOrientationAlgs.average_ii(6, true, integralType);
			orientation = FactoryOrientationAlgs.sliding_ii(42,Math.PI/3.0,6,true,integralType);

		DescribePointSurf<II> alg = FactoryDescribePointAlgs.<II>msurf(integralType);
		return new WrapDescribeSurf<T,II>( alg ,orientation);
	}

	public static <T extends ImageBase, D extends ImageBase>
	DescribeRegionPoint<T> gaussian12( int radius ,Class<T> imageType , Class<D> derivType ) {

		ImageGradient<T,D> gradient = FactoryDerivative.sobel(imageType,derivType);
		DescribePointGaussian12<T, ?> steer = FactoryDescribePointAlgs.steerableGaussian12(radius,imageType);

		return new WrapDescribeGaussian12<T,D>(steer,gradient,imageType,derivType);
	}

	public static <T extends ImageBase, D extends ImageBase>
	DescribeRegionPoint<T> steerableGaussian( int radius , boolean normalized ,
													Class<T> imageType , Class<D> derivType ) {

		ImageGradient<T,D> gradient = FactoryDerivative.sobel(imageType,derivType);
		DescribePointSteerable2D<T, ?> steer = FactoryDescribePointAlgs.steerableGaussian(normalized,-1,radius,imageType);

		return new WrapDescribeSteerable<T,D>(steer,gradient,imageType,derivType);
	}

	/**
	 * <p>
	 * The BRIEF descriptor is HORRIBLY inefficient when used through this interface.  This functionality is only
	 * provided for testing and validation purposes.
	 * </p>
	 *
	 * @param radius Region's radius.  Typical value is 16.
	 * @param numPoints Number of feature/points.  Typical value is 512.
	 * @param blurSigma Typical value is -1.
	 * @param blurRadius Typical value is 4.
	 * @param isFixed Is the orientation and scale fixed? true for original algorithm described in BRIEF paper.
	 *@param imageType  @return
	 */
	public static <T extends ImageBase>
	DescribeRegionPoint<T> brief(int radius, int numPoints,
									   double blurSigma, int blurRadius,
									   boolean isFixed,
									   Class<T> imageType) {

		if( isFixed) {
			BlurFilter<T> filter = FactoryBlurFilter.gaussian(imageType,blurSigma,blurRadius);
			BriefDefinition_I32 definition = FactoryBriefDefinition.gaussian2(new Random(123), radius, numPoints);

			return new WrapDescribeBrief<T>(FactoryDescribePointAlgs.brief(definition,filter));
		} else {
			BlurFilter<T> filter = FactoryBlurFilter.gaussian(imageType,blurSigma,blurRadius);
			BriefDefinition_I32 definition = FactoryBriefDefinition.gaussian2(new Random(123), radius, numPoints);

			return new WrapDescribeBriefSo<T>(FactoryDescribePointAlgs.briefso(definition, filter));
		}
	}
}
