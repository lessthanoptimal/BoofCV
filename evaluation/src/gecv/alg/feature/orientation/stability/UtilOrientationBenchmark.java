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

package gecv.alg.feature.orientation.stability;

import gecv.abst.detect.interest.InterestPointDetector;
import gecv.alg.feature.StabilityAlgorithm;
import gecv.factory.feature.detect.interest.FactoryInterestPoint;
import gecv.factory.feature.orientation.FactoryOrientationAlgs;
import gecv.struct.image.ImageBase;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Peter Abeles
 */
public class UtilOrientationBenchmark {

	public static <T extends ImageBase>
	InterestPointDetector<T> defaultDetector() {
		return FactoryInterestPoint.<T>fromFastHessian(200,9,4,1);
	}

	public static <D extends ImageBase>
	List<StabilityAlgorithm> createAlgorithms( int radius , Class<D> derivType )
	{
		List<StabilityAlgorithm> ret = new ArrayList<StabilityAlgorithm>();

		ret.add(new StabilityAlgorithm("Ave Unweighted", FactoryOrientationAlgs.average(radius,false,derivType)));
		ret.add(new StabilityAlgorithm("Ave Weighted", FactoryOrientationAlgs.average(radius,true,derivType)));
		ret.add(new StabilityAlgorithm("Hist5 Unweighted", FactoryOrientationAlgs.histogram(5,radius,false,derivType)));
		ret.add(new StabilityAlgorithm("Hist5 Weighted", FactoryOrientationAlgs.histogram(5,radius,true,derivType)));
		ret.add(new StabilityAlgorithm("Hist10 Unweighted", FactoryOrientationAlgs.histogram(10,radius,false,derivType)));
		ret.add(new StabilityAlgorithm("Hist10 Weighted", FactoryOrientationAlgs.histogram(10,radius,true,derivType)));
		ret.add(new StabilityAlgorithm("Hist20 Unweighted", FactoryOrientationAlgs.histogram(20,radius,false,derivType)));
		ret.add(new StabilityAlgorithm("Slide PI/6 Un-W", FactoryOrientationAlgs.sliding(10,Math.PI/6,radius,false,derivType)));
		ret.add(new StabilityAlgorithm("Slide PI/6 W", FactoryOrientationAlgs.sliding(10,Math.PI/6,radius,false,derivType)));
		ret.add(new StabilityAlgorithm("Slide PI/3 Un-W", FactoryOrientationAlgs.sliding(20,Math.PI/3,radius,false,derivType)));
		ret.add(new StabilityAlgorithm("Slide PI/3 W", FactoryOrientationAlgs.sliding(20,Math.PI/3,radius,true,derivType)));

		return ret;
	}

	public static double[] makeSample( double min , double max , int num ) {
		double []ret = new double[ num ];
		for( int i = 0; i < num; i++ ) {
			ret[i]= min + i*(max-min)/(num-1);
		}
		return ret;
	}

}
