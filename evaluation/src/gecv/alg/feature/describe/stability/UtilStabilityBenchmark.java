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

import gecv.alg.feature.benchmark.StabilityAlgorithm;
import gecv.factory.feature.describe.FactoryExtractFeatureDescription;
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

		ret.add( new StabilityAlgorithm("SURF", FactoryExtractFeatureDescription.surf(true,imageType)));
		ret.add( new StabilityAlgorithm("Gaussian 12", FactoryExtractFeatureDescription.gaussian12(20,imageType,derivType)));
		ret.add( new StabilityAlgorithm("Steer", FactoryExtractFeatureDescription.steerableGaussian(20,false,imageType,derivType)));
		ret.add( new StabilityAlgorithm("Steer Norm", FactoryExtractFeatureDescription.steerableGaussian(20,true,imageType,derivType)));

		return ret;
	}
}
