/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.benchmark.feature.distort;

import boofcv.benchmark.feature.BenchmarkAlgorithm;
import boofcv.benchmark.feature.MetricResult;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageSingleBand;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Evaluates the stability of an image feature descriptor/detector by distorting an input image
 * under different transforms.  While not as realistic as using real images for each distortion type
 * it provides metrics under known conditions.
 *
 * @author Peter Abeles
 */
public abstract class BenchmarkFeatureDistort<T extends ImageSingleBand> {
		// rand number generator used to add noise
	protected Random rand;
	// type of input image
	protected Class<T> imageType;
	// the distorted image
	private T distortedImage;

	// variable being adjusted during evaluation
	private double variable[];
	// name of the variable
	private String variableName;

	// algorithm being evaluated
	private BenchmarkAlgorithm alg;
	// measures the algorithm's performance
	private StabilityEvaluator<T> evaluator;

	protected BenchmarkFeatureDistort(long randomSeed,
									  double[] variable, String variableName,
									  Class<T> imageType) {
		this.rand = new Random(randomSeed);
		this.imageType = imageType;
		this.variable = variable;
		this.variableName = variableName;

		distortedImage = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
	}

	public void setAlg(BenchmarkAlgorithm alg) {
		this.alg = alg;
	}

	public void setEvaluator(StabilityEvaluator<T> evaluator) {
		this.evaluator = evaluator;
	}

	public List<MetricResult> evaluate( BufferedImage original ) {
		T image = ConvertBufferedImage.convertFromSingle(original, null, imageType);

		evaluator.extractInitial(alg,image);

		List<MetricResult> results = createResultsStorage(evaluator,variable);

		for( int i = 0; i < variable.length; i++ ) {
			distortImage(image,distortedImage,variable[i]);
			DistortParam param = createDistortParam(variable[i]);
			double[]metrics = evaluator.evaluateImage(alg,distortedImage, param);

			for( int j = 0; j < results.size(); j++ ) {
				results.get(j).observed[i] = metrics[j];
			}
		}

		return results;
	}

	protected abstract void distortImage(T image, T distortedImage , double param);

	protected abstract DistortParam createDistortParam( double variable );

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
			r.nameX = variableName;
			r.observed = new double[ variable.length ];
			r.adjustment = variable.clone();
			results.add(r);
		}
		return results;
	}

	public int getNumberOfObservations() {
		return variable.length;
	}
}
