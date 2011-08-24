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

package gecv.alg.feature.describe.stability;

import gecv.abst.detect.describe.WrapDescribeSteerable;
import gecv.abst.detect.describe.WrapDescribeSurf;
import gecv.abst.filter.derivative.ImageGradient;
import gecv.alg.feature.StabilityAlgorithm;
import gecv.alg.feature.describe.DescribePointSteerable2D;
import gecv.factory.feature.describe.FactoryDescribePointAlgs;
import gecv.factory.filter.derivative.FactoryDerivative;
import gecv.struct.image.ImageBase;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Peter Abeles
 */
public class UtilStabilityBenchmark {
	public static <T extends ImageBase, D extends ImageBase>
	List<StabilityAlgorithm> createAlgorithms(int radius, Class<T> imageType , Class<D> derivType ) {
		List<StabilityAlgorithm> ret = new ArrayList<StabilityAlgorithm>();

		ImageGradient<T,D> gradient = FactoryDerivative.sobel(imageType,derivType);
		DescribePointSteerable2D<T, D, ?> steer = FactoryDescribePointAlgs.steerableGaussian(false,-1,9,imageType,derivType);
		DescribePointSteerable2D<T, D, ?> steerN = FactoryDescribePointAlgs.steerableGaussian(true,-1,9, imageType,derivType);

		ret.add( new StabilityAlgorithm("SURF",new WrapDescribeSurf<T>( FactoryDescribePointAlgs.<T>surf())));
		ret.add( new StabilityAlgorithm("Steer", new WrapDescribeSteerable<T,D>(steer,gradient,derivType)));
		ret.add( new StabilityAlgorithm("Steer Norm", new WrapDescribeSteerable<T,D>(steerN,gradient,derivType)));

		return ret;
	}
}
