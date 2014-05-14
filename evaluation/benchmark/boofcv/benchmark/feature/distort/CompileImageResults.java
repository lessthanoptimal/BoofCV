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

import boofcv.benchmark.feature.AlgorithmResult;
import boofcv.benchmark.feature.BenchmarkAlgorithm;
import boofcv.benchmark.feature.MetricResult;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageSingleBand;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


/**
 * Runs a list of algorithms and images against each other then summarizes the results. 
 *
 * @author Peter Abeles
 */
public class CompileImageResults<T extends ImageSingleBand> {

	private List<String> fileNames = new ArrayList<String>();
	private List<BenchmarkAlgorithm> algs;
	private BenchmarkFeatureDistort<T> benchmark;
	private StabilityEvaluator<T> evaluator;


	private boolean verbose = false;

	public CompileImageResults(BenchmarkFeatureDistort<T> benchmark) {
		this.benchmark = benchmark;
	}

	public void addImage( String fileName ) {
		fileNames.add(fileName);
	}

	public void setAlgorithms( List<BenchmarkAlgorithm> algs , StabilityEvaluator<T> evaluator ) {
		this.algs = algs;
		this.evaluator = evaluator;
		benchmark.setEvaluator(evaluator);
	}

	public void process() {
		for( String imageName : fileNames ) {
			String[] a = imageName.split("/");
			String n = a[a.length-1];
			System.out.println("\nImage Name: "+n);
			BufferedImage original = UtilImageIO.loadImage(imageName);
			if( original == null ) {
				System.out.println("Couldn't load image: "+imageName);
				System.exit(0);
			}
			List<AlgorithmResult<T>> results = processImage(original);
			print(results);
		}
	}

	private void print( List<AlgorithmResult<T>> results ) {
		int N = benchmark.getNumberOfObservations();
		String metrics[] = evaluator.getMetricNames();

		for( int metricNum = 0; metricNum < metrics.length; metricNum++ ) {
			System.out.println("Metric: "+metrics[metricNum]);

			for( AlgorithmResult<T> a : results ) {
				System.out.printf("%20s ",a.algName);
				for( int i = 0; i < N; i++ ) {
					MetricResult r = a.performance.get(metricNum);
					System.out.printf(" %4.1f",r.observed[i]);
				}
				System.out.println();
			}
		}
	}

	protected List<AlgorithmResult<T>> processImage( BufferedImage original ) {
		List<AlgorithmResult<T>> ret = new ArrayList<AlgorithmResult<T>>();

		for( BenchmarkAlgorithm alg : algs ) {
			if( verbose )
				System.out.println("Processing alg "+alg.getName());
			benchmark.setAlg(alg);
			AlgorithmResult<T> a = new AlgorithmResult<T>();
			a.algName = alg.getName();
			a.performance = benchmark.evaluate(original);

			ret.add(a);
		}

		return ret;
	}
}
