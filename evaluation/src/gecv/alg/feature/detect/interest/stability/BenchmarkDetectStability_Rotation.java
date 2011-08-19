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

import gecv.alg.feature.CompileImageResults;
import gecv.alg.feature.FeatureStabilityRotation;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;

/**
 * Evaluate feature detection algorithms for stability against image rotation
 *
 * @author Peter Abeles
 */
public class BenchmarkDetectStability_Rotation <T extends ImageBase>
	extends FeatureStabilityRotation<T>
{

	/**
	 * Evaluating input images of the specified type.
	 *
	 * @param imageType Original input image type.
	 */
	public BenchmarkDetectStability_Rotation(Class<T> imageType ) {
		super(imageType, 0,0.125*Math.PI,0.25*Math.PI,0.5*Math.PI,0.75*Math.PI,Math.PI);
	}

	public static <T extends ImageBase, D extends ImageBase>
			void evaluate( Class<T> imageType , Class<D> derivType ) {

		BenchmarkInterestParameters<T,D> param = new BenchmarkInterestParameters<T,D>();
		param.imageType = imageType;
		param.derivType = derivType;

		BenchmarkDetectStability_Rotation<T> eval = new BenchmarkDetectStability_Rotation<T>(imageType);

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
