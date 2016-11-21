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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import deepboof.Function;
import deepboof.graph.FunctionSequence;
import deepboof.graph.Node;
import deepboof.impl.forward.standard.FunctionLinear_F32;
import deepboof.misc.TensorFactory_F32;
import deepboof.tensors.Tensor_F32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static deepboof.misc.TensorOps.WI;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class CheckBaseImageClassifier {

	protected Random rand = new Random(234);
	protected int numCategories = 8;

	/**
	 * Basic test which sees if it blows up.  Does not validate quality of results since a fake network
	 * is provided.  Regression test is required to validate correctness.
	 *
	 * The real network is not used because it requires downloading external data and can be slow.
	 */
	@Test
	public void checkForBlowUp() {
		Planar<GrayF32> input = createImage();
		GImageMiscOps.fillUniform(input,rand,0,255);
		BaseImageClassifier classifier = createClassifier();

		createDummyNetwork(classifier, input.width, input.height);

		classifier.classify(input);

		int best = classifier.getBestResult();
		assertTrue(best>=0 && best < numCategories);
	}

	public abstract Planar<GrayF32> createImage();

	public abstract BaseImageClassifier createClassifier();

	private void createDummyNetwork(BaseImageClassifier alg, int width , int height ) {

		for (int i = 0; i < numCategories; i++) {
			alg.getCategories().add("Category "+i);
		}

		FunctionLinear_F32 function = new FunctionLinear_F32(numCategories);
		function.initialize(3,height,width);

		List<Tensor_F32> parameters = new ArrayList<>();
		parameters.add(TensorFactory_F32.random(rand,false,function.getParameterShapes().get(0)));
		parameters.add(TensorFactory_F32.random(rand,false,function.getParameterShapes().get(1)));
		function.setParameters(parameters);

		Node<Tensor_F32, Function<Tensor_F32>> node = new Node<>();
		node.function = function;

		List<Node<Tensor_F32, Function<Tensor_F32>>> sequence = new ArrayList<>();
		sequence.add(node);

		alg.network = new FunctionSequence<>(sequence, Tensor_F32.class);
		alg.tensorOutput = new Tensor_F32(WI(1,alg.network.getOutputShape()));
	}
}
