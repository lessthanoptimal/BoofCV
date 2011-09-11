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

package boofcv.alg.feature.describe.stability;

import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.benchmark.*;
import boofcv.alg.feature.orientation.stability.UtilOrientationBenchmark;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

import java.util.List;


/**
 * @author Peter Abeles
 */
public class BenchmarkStabilityDescribe <T extends ImageBase, D extends ImageBase>
{
	int randSeed = 234234;
	Class<T> imageType;
	Class<D> derivType;

	int border = 10;
	int radius = 8;

	// algorithms which are to be evaluated
	List<StabilityAlgorithm> algs;

	public BenchmarkStabilityDescribe(Class<T> imageType, Class<D> derivType) {
		this.imageType = imageType;
		this.derivType = derivType;
		algs = UtilStabilityBenchmark.createAlgorithms(radius,imageType,derivType);
	}

	public List<StabilityAlgorithm> getEvaluationAlgs() {
		return algs;
	}

	public void testNoise() {
		FeatureStabilityNoise<T> stability =
				new FeatureStabilityNoise<T>(imageType,randSeed, 1,2,4,8,12,16,20,40);
		perform(stability);
	}

	public void testIntensity() {
		FeatureStabilityIntensity<T> stability =
				new FeatureStabilityIntensity<T>(imageType,0.2,0.5,0.8,1,1.1,1.5);
		perform(stability);
	}

	// NOTE: Much of the error inside of rotation seems to be caused by interpolation.
	public void testRotation() {
		FeatureStabilityRotation<T> stability =
				new FeatureStabilityRotation<T>(imageType, UtilOrientationBenchmark.makeSample(0,Math.PI,20));

		perform(stability);
	}

	public void testScale() {
		FeatureStabilityScale<T> stability =
				new FeatureStabilityScale<T>(imageType,0.5,0.75,1,1.5,2,3,4);
		perform(stability);
	}

	private void perform( FeatureStabilityBase<T> eval ) {
		CompileImageResults<T> compile = new CompileImageResults<T>(eval);
		compile.addImage("evaluation/data/outdoors01.jpg");
		compile.addImage("evaluation/data/indoors01.jpg");
		compile.addImage("evaluation/data/scale/beach01.jpg");
		compile.addImage("evaluation/data/scale/mountain_7p1mm.jpg");
		compile.addImage("evaluation/data/sunflowers.png");

		InterestPointDetector<T> detector = UtilOrientationBenchmark.defaultDetector(imageType,derivType);
		DescribeEvaluator<T> evaluator = new DescribeEvaluator<T>(border,detector);

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
