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

package gecv.alg.detect.interest.benchmark;

import gecv.alg.feature.CompileImageResults;
import gecv.alg.feature.FeatureStabilityNoise;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;

/**
 * Compares different image interest point detection algorithms stability under different amounts of
 * i.i.d. gaussian pixel noise.
 *
 * @author Peter Abeles
 */
public class BenchmarkDetectStability_Noise<T extends ImageBase, D extends ImageBase>
	extends FeatureStabilityNoise<T>
{

	/**
	 * Evaluating input images of the specified type.
	 *
	 * @param imageType Original input image type.
	 * @param derivType Type of the image derivative.
	 */
	public BenchmarkDetectStability_Noise(Class<T> imageType, Class<D> derivType) {
		super(imageType, 234234, 1,2,4,8,12,16,20);
	}

	public static <T extends ImageBase, D extends ImageBase>
			void evaluate( Class<T> imageType , Class<D> derivType ) {

		BenchmarkInterestParameters<T,D> param = new BenchmarkInterestParameters<T,D>();
		param.imageType = imageType;
		param.derivType = derivType;

		BenchmarkDetectStability_Noise<T,D> eval = new BenchmarkDetectStability_Noise<T,D>(imageType,derivType);

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

	public static void main( String args[] )
	{
		evaluate(ImageFloat32.class,ImageFloat32.class);
	}
}
