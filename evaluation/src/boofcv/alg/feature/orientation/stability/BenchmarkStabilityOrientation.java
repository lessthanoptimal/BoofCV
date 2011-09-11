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

package boofcv.alg.feature.orientation.stability;

import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.benchmark.*;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;


/**
 * @author Peter Abeles
 */
public class BenchmarkStabilityOrientation<T extends ImageBase, D extends ImageBase> {

	int randSeed = 234234;
	Class<T> imageType;
	Class<D> derivType;

	int border = 10;
	int radius = 8;

	public BenchmarkStabilityOrientation(Class<T> imageType, Class<D> derivType) {
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

	// NOTE: Much of the error inside of rotation seems to be caused by interpolation.
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
		CompileImageResults<T> compile = new CompileImageResults<T>(eval);
		compile.addImage("evaluation/data/outdoors01.jpg");
		compile.addImage("evaluation/data/indoors01.jpg");
		compile.addImage("evaluation/data/scale/beach01.jpg");
		compile.addImage("evaluation/data/scale/mountain_7p1mm.jpg");
		compile.addImage("evaluation/data/sunflowers.png");

		ImageGradient<T,D> gradient = FactoryDerivative.sobel(imageType,derivType);
		InterestPointDetector<T> detector = UtilOrientationBenchmark.defaultDetector(imageType,derivType);
		OrientationEvaluator<T,D> evaluator = new OrientationEvaluator<T,D>(border,detector,gradient);

		compile.setAlgorithms(UtilOrientationBenchmark.createAlgorithms(radius,imageType,derivType),evaluator);

		compile.process();
	}

	public static void main( String args[] ) {
//		BenchmarkStabilityOrientation<ImageFloat32,ImageFloat32> benchmark
//				= new BenchmarkStabilityOrientation<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);
		BenchmarkStabilityOrientation<ImageUInt8, ImageSInt16> benchmark
				= new BenchmarkStabilityOrientation<ImageUInt8,ImageSInt16>(ImageUInt8.class,ImageSInt16.class);

//		benchmark.testNoise();
//		benchmark.testIntensity();
		benchmark.testRotation();
//		benchmark.testScale();

	}
}
