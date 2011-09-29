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
import boofcv.alg.feature.describe.brief.BriefDefinition;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.feature.orientation.OrientationIntegral;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageBase;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class FactoryExtractFeatureDescription {

	public static <T extends ImageBase, II extends ImageBase>
	ExtractFeatureDescription<T> surf( boolean isOriented , Class<T> imageType) {
		OrientationIntegral<II> orientation = null;

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		if( isOriented )
			orientation = FactoryOrientationAlgs.average_ii(6,false,integralType);

		return new WrapDescribeSurf<T,II>( FactoryDescribePointAlgs.<II>surf(integralType),orientation);
	}

	public static <T extends ImageBase, D extends ImageBase>
	ExtractFeatureDescription<T> gaussian12( int radius ,Class<T> imageType , Class<D> derivType ) {

		ImageGradient<T,D> gradient = FactoryDerivative.sobel(imageType,derivType);
		DescribePointGaussian12<T, ?> steer = FactoryDescribePointAlgs.steerableGaussian12(radius,imageType);

		return new WrapDescribeGaussian12<T,D>(steer,gradient,imageType,derivType);
	}

	public static <T extends ImageBase, D extends ImageBase>
	ExtractFeatureDescription<T> steerableGaussian( int radius , boolean normalized ,
													Class<T> imageType , Class<D> derivType ) {

		ImageGradient<T,D> gradient = FactoryDerivative.sobel(imageType,derivType);
		DescribePointSteerable2D<T, ?> steer = FactoryDescribePointAlgs.steerableGaussian(normalized,-1,radius,imageType);

		return new WrapDescribeSteerable<T,D>(steer,gradient,imageType,derivType);
	}

	/**
	 * The brief descriptor is HORRIBLY inefficient when used through this interface.  This functionality is only
	 * provided for testing and validation purposes.
	 *
	 * TODO Describe what to do for efficiency
	 *
	 * @param radius Region's radius.  Typical value is 16.
	 * @param numPoints Number of feature/points.  Typical value is 512.
	 * @param blurSigma Typical value is -1.
	 * @param blurRadius Typical value is 4.
	 * @param isScale
	 * @param isOriented
	 * @param imageType
	 * @param <T>
	 * @return
	 */
	public static <T extends ImageBase>
	ExtractFeatureDescription<T> brief( int radius , int numPoints ,
										double blurSigma , int blurRadius ,
										 boolean isScale , boolean isOriented ,
										 Class<T> imageType) {

		if( !isOriented ) {
			BlurFilter<T> filter = FactoryBlurFilter.gaussian(imageType,blurSigma,blurRadius);
			BriefDefinition definition = FactoryBriefDefinition.gaussian2(new Random(123), radius, numPoints);

			return new WrapDescribeBrief<T>(FactoryDescribePointAlgs.brief(definition,filter));
		} else {
			BlurFilter<T> filter = FactoryBlurFilter.gaussian(imageType,blurSigma,blurRadius);
			BriefDefinition definition = FactoryBriefDefinition.gaussian2(new Random(123), radius, numPoints);

			return new WrapDescribeBriefO<T>(FactoryDescribePointAlgs.briefo(definition,filter));
		}
	}
}
