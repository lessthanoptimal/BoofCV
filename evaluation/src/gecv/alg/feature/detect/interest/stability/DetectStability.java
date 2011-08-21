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

package gecv.alg.feature.detect.interest.stability;

import gecv.alg.feature.*;
import gecv.alg.feature.describe.stability.UtilOrientationBenchmark;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
public class DetectStability<T extends ImageBase, D extends ImageBase> {

	int randSeed = 234234;
	Class<T> imageType;
	Class<D> derivType;

	public DetectStability(Class<T> imageType, Class<D> derivType) {
		this.imageType = imageType;
		this.derivType = derivType;
	}

	public void testNoise() {
		FeatureStabilityNoise<T> stability =
				new FeatureStabilityNoise<T>(imageType,randSeed, 1,2,4,8,12,16,20,40);
		perform(stability);
	}

	public void testIntensity() {
		FeatureStabilityIntensity<T> stability =
				new FeatureStabilityIntensity<T>(imageType,0.2,0.5,0.8,1.1,1.5);
		perform(stability);
	}

	public void testRotation() {
		FeatureStabilityRotation<T> stability =
				new FeatureStabilityRotation<T>(imageType,UtilOrientationBenchmark.makeSample(0,Math.PI,20));

		perform(stability);
	}

	public void testScale() {
		FeatureStabilityScale<T> stability =
				new FeatureStabilityScale<T>(imageType,0.5,0.75,1,1.5,2,3,4);
		perform(stability);
	}

	private void perform( FeatureStabilityBase<T> eval ) {
		BenchmarkInterestParameters<T,D> param = new BenchmarkInterestParameters<T,D>();
		param.imageType = imageType;
		param.derivType = derivType;

		CompileImageResults<T> compile = new CompileImageResults<T>(eval);
		compile.addImage("evaluation/data/outdoors01.jpg");
		compile.addImage("evaluation/data/indoors01.jpg");
		compile.addImage("evaluation/data/scale/beach01.jpg");
		compile.addImage("evaluation/data/scale/mountain_7p1mm.jpg");
		compile.addImage("evaluation/data/sunflowers.png");

		DetectEvaluator<T> evaluator = new DetectEvaluator<T>();

		compile.setAlgorithms(BenchmarkDetectHelper.createAlgs(param),evaluator);

		compile.process();

	}

	public static void main( String args[] ) {
		DetectStability<ImageFloat32,ImageFloat32> benchmark
				= new DetectStability<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);

//		benchmark.testNoise();
//		benchmark.testIntensity();
		benchmark.testRotation();
//		benchmark.testScale();

	}
}
