/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.factory.feature.describe;

import gecv.abst.feature.describe.ExtractFeatureDescription;
import gecv.abst.feature.describe.WrapDescribeSteerable;
import gecv.abst.feature.describe.WrapDescribeSurf;
import gecv.abst.filter.derivative.ImageGradient;
import gecv.alg.feature.describe.DescribePointSteerable2D;
import gecv.alg.feature.orientation.OrientationGradient;
import gecv.alg.feature.orientation.OrientationIntegral;
import gecv.factory.feature.orientation.FactoryOrientationAlgs;
import gecv.factory.filter.derivative.FactoryDerivative;
import gecv.struct.image.ImageBase;


/**
 * @author Peter Abeles
 */
public class FactoryExtractFeatureDescription {

	public static <T extends ImageBase>
	ExtractFeatureDescription<T> surf( boolean isOriented , Class<T> imageType) {
		OrientationIntegral<T> orientation = null;
		if( isOriented )
			orientation = FactoryOrientationAlgs.average_ii(6,false,imageType);

		return new WrapDescribeSurf<T>( FactoryDescribePointAlgs.<T>surf(imageType),orientation);
	}

	public static <T extends ImageBase, D extends ImageBase>
	ExtractFeatureDescription<T> steerableGaussian( int radius , boolean normalized ,
													Class<T> imageType , Class<D> derivType ) {

		ImageGradient<T,D> gradient = FactoryDerivative.sobel(imageType,derivType);
		OrientationGradient<D> orientation = FactoryOrientationAlgs.average(radius,false,derivType);
		DescribePointSteerable2D<T, ?> steer = FactoryDescribePointAlgs.steerableGaussian(normalized,-1,radius,imageType);

		return new WrapDescribeSteerable<T,D>(steer,orientation,gradient,imageType,derivType);
	}
}
