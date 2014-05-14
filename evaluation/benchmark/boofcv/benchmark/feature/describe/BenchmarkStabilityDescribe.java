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

package boofcv.benchmark.feature.describe;

import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.orientation.OrientationImageAverage;
import boofcv.benchmark.feature.BenchmarkAlgorithm;
import boofcv.benchmark.feature.distort.BenchmarkFeatureDistort;
import boofcv.benchmark.feature.distort.CompileImageResults;
import boofcv.benchmark.feature.distort.FactoryBenchmarkFeatureDistort;
import boofcv.benchmark.feature.distort.StabilityEvaluator;
import boofcv.benchmark.feature.orientation.UtilOrientationBenchmark;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

import java.util.List;


/**
 * Evaluates the stability of feature descriptions after applying different image transforms to the image.
 *
 * @author Peter Abeles
 */
public class BenchmarkStabilityDescribe <T extends ImageSingleBand, D extends ImageSingleBand>
{
	Class<T> imageType;
	Class<D> derivType;

	int border = 13;
	int radius = 12;

	// algorithms which are to be evaluated
	List<BenchmarkAlgorithm> algs;

	public BenchmarkStabilityDescribe(Class<T> imageType, Class<D> derivType) {
		this.imageType = imageType;
		this.derivType = derivType;
		algs = UtilStabilityBenchmark.createAlgorithms(radius,imageType,derivType);
	}

	public List<BenchmarkAlgorithm> getEvaluationAlgs() {
		return algs;
	}

	public void testNoise() {
		BenchmarkFeatureDistort<T> benchmark =
				FactoryBenchmarkFeatureDistort.noise(imageType);
		perform(benchmark);
	}

	public void testIntensity() {
		BenchmarkFeatureDistort<T> benchmark =
				FactoryBenchmarkFeatureDistort.intensity(imageType);
		perform(benchmark);
	}

	public void testRotation() {
		BenchmarkFeatureDistort<T> benchmark =
				FactoryBenchmarkFeatureDistort.rotate(imageType);
		perform(benchmark);
	}

	public void testScale() {
		BenchmarkFeatureDistort<T> benchmark =
				FactoryBenchmarkFeatureDistort.scale(imageType);
		perform(benchmark);
	}

	private void perform( BenchmarkFeatureDistort<T> benchmark ) {
		CompileImageResults<T> compile = new CompileImageResults<T>(benchmark);
		compile.addImage("../data/evaluation/outdoors01.jpg");
		compile.addImage("../data/evaluation/indoors01.jpg");
		compile.addImage("../data/evaluation/scale/beach01.jpg");
		compile.addImage("../data/evaluation/scale/mountain_7p1mm.jpg");
		compile.addImage("../data/evaluation/sunflowers.png");

		InterestPointDetector<T> detector = UtilOrientationBenchmark.defaultDetector(imageType,derivType);
		OrientationImageAverage<T> orientation = FactoryOrientationAlgs.nogradient(radius,imageType);
		// comment/uncomment to change the evaluator
		StabilityEvaluator<T> evaluator = new DescribeEvaluator<T,TupleDesc>(border,detector,orientation);

		compile.setAlgorithms(algs,evaluator);

		compile.process();
	}

	public static void main( String args[] ) {
		BenchmarkStabilityDescribe<ImageFloat32,ImageFloat32> benchmark
				= new BenchmarkStabilityDescribe<ImageFloat32,ImageFloat32>(ImageFloat32.class, ImageFloat32.class);

//		benchmark.testNoise();
//		benchmark.testIntensity();
		benchmark.testRotation();
//		benchmark.testScale();
	}
}
