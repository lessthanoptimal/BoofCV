/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.deepboof;

import boofcv.abst.scene.ImageClassifier;
import deepboof.io.torch7.ParseBinaryTorch7;
import deepboof.tensors.Tensor_F32;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
// TODO convert into a regression test
public class TestImageClassifierNiNImageNet {
	@Test
	public void compareToTorch() throws IOException {
		File path = new File("test/boofcv/deepboof");

		Tensor_F32 input = new ParseBinaryTorch7().parseIntoBoof(new File(path,"nin_input"));
		Tensor_F32 output = new ParseBinaryTorch7().parseIntoBoof(new File(path,"nin_output"));

		ImageClassifierNiNImageNet alg = new ImageClassifierNiNImageNet();
		alg.loadModel(new File("../../"));

		// skip all the proccessing
		alg.innerProcess(input);

		List<ImageClassifier.Score> scores = alg.getAllResults();

		for (int i = 0; i < scores.size(); i++) {
			ImageClassifier.Score score = scores.get(i);

			float expected = output.get(0,score.category);

//			System.out.printf("%6.2f    %6.2f\n",expected,score.score);
			assertEquals(expected,score.score,1e-4f);
		}
	}
}
