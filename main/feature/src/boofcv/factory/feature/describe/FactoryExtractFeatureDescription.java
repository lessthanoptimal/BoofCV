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

import boofcv.abst.feature.describe.ExtractFeatureDescription;
import boofcv.abst.feature.describe.WrapDescribeGaussian12;
import boofcv.abst.feature.describe.WrapDescribeSteerable;
import boofcv.abst.feature.describe.WrapDescribeSurf;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.describe.DescribePointGaussian12;
import boofcv.alg.feature.describe.DescribePointSteerable2D;
import boofcv.alg.feature.orientation.OrientationIntegral;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageBase;


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
}
