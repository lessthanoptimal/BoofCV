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
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import deepboof.Function;
import deepboof.io.torch7.ParseBinaryTorch7;
import deepboof.io.torch7.SequenceAndParameters;
import deepboof.tensors.Tensor_F32;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * TODO comment
 *
 * @author Peter Abeles
 */
public class ImageClassifierNiNImageNet implements ImageClassifier<Planar<GrayF32>> {

	@Override
	public void loadModel(File directory) throws IOException {
		SequenceAndParameters<Tensor_F32, Function<Tensor_F32>> sequence =
				new ParseBinaryTorch7().parseIntoBoof(new File(directory,"nin_nobn_final.t7"));

		Object o = new ParseBinaryTorch7().parseIntoBoof(new File(directory,"synset.t7.t7"));

		System.out.println("Loaded");

//		network = sequence.createForward(3,32,32);
//		output = new Tensor_F32(WI(1,network.getOutputShape()));
//
//		BorderType type = BorderType.valueOf(stats.border);
//		localNorm = new ImageLocalNormalization<>(GrayF32.class, type);
//		kernel = DataManipulationOps.create1D_F32(stats.kernel);
	}

	@Override
	public ImageType<Planar<GrayF32>> getInputType() {
		return null;
	}

	@Override
	public void classify(Planar<GrayF32> image) {

	}

	@Override
	public int getBestResult() {
		return 0;
	}

	@Override
	public List<Score> getAllResults() {
		return null;
	}

	@Override
	public List<String> getCategories() {
		return null;
	}
}
