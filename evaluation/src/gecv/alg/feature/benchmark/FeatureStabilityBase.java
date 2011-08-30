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

package gecv.alg.feature.benchmark;

import gecv.struct.image.ImageBase;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


/**
 * Base class for stability benchmarks
 *
 * @author Peter Abeles
 */
public abstract class FeatureStabilityBase<T extends ImageBase> {

	
	public abstract List<MetricResult> evaluate( BufferedImage original ,
										StabilityAlgorithm alg ,
										StabilityEvaluator<T> evaluator );

	/**
	 * Returns how many different types of distortion are going to be tested.
	 */
	public abstract int getNumberOfObservations() ;

	/**
	 * Creates data structures to store computed results.
	 */
	protected List<MetricResult> createResultsStorage(StabilityEvaluator<T> evaluator ,
													  double variable[] ) {
		String[] metricNames = evaluator.getMetricNames();

		List<MetricResult> results = new ArrayList<MetricResult>();

		for( String n : metricNames ) {
			MetricResult r = new MetricResult();
			r.nameY = n;
			r.nameX = "Noise";
			r.observed = new double[ variable.length ];
			r.adjustment = variable.clone();
			results.add(r);
		}
		return results;
	}

}
