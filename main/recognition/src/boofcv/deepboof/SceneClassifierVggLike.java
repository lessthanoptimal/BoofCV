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

import boofcv.abst.scene.SceneClassifier;
import boofcv.alg.color.ColorYuv;
import boofcv.alg.filter.stat.ImageLocalNormalization;
import boofcv.core.image.border.BorderType;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import deepboof.Function;
import deepboof.datasets.UtilCifar10;
import deepboof.graph.FunctionSequence;
import deepboof.io.torch7.ParseBinaryTorch7;
import deepboof.io.torch7.SequenceAndParameters;
import deepboof.models.ModelIO;
import deepboof.models.YuvStatistics;
import deepboof.tensors.Tensor_F32;
import org.ddogleg.struct.FastQueue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static deepboof.misc.TensorOps.WI;

/**
 * @author Peter Abeles
 */
public class SceneClassifierVggLike implements SceneClassifier<Planar<GrayF32>> {

	FunctionSequence<Tensor_F32,Function<Tensor_F32>> network;

	Planar<GrayF32> rgb32 = new Planar<>(GrayF32.class,32,32,3);
	Planar<GrayF32> yuv32 = new Planar<>(GrayF32.class,32,32,3);

	Tensor_F32 tensorYuv = new Tensor_F32(1,3,32,32);
	Tensor_F32 output;

	ImageLocalNormalization<GrayF32> localNorm;
	YuvStatistics stats;
	Kernel1D_F32 kernel;

	FastQueue<Score> categoryScores = new FastQueue<>(Score.class,true);
	int categoryBest;

	List<String> categoryNames = UtilCifar10.getClassNames();

	/**
	 * Expects there to be two files in the provided directory:
	 * YuvStatistics.txt
	 * model.net
	 *
	 * @param directory
	 * @throws IOException
	 */
	@Override
	public void loadModel(File directory)  throws IOException {
		stats = ModelIO.load(new File(directory,"YuvStatistics.txt"));

		SequenceAndParameters<Tensor_F32, Function<Tensor_F32>> sequence =
				new ParseBinaryTorch7().parseIntoBoof(new File(directory,"model.net"));

		network = sequence.createForward(3,32,32);
		output = new Tensor_F32(WI(1,network.getOutputShape()));

		BorderType type = BorderType.valueOf(stats.border);
		localNorm = new ImageLocalNormalization<>(GrayF32.class, type);
		kernel = DataManipulationOps.create1D_F32(stats.kernel);
	}

	/**
	 * Classifies the input image.  The input image can be any shape, but will be mangled until
	 * it's 32x32 internally.   It must be an RGB image with pixel values from 0 to 255.
	 *
	 * @param image Image being processed
	 */
	@Override
	public void classify(Planar<GrayF32> image) {
		// shrink then convert into YUV
		Planar<GrayF32> rgb32;
		if( image.width == 32 && image.height == 32 ) {
			rgb32 = image;
		} else {
			rgb32 = this.rgb32;
			massageInto32x32(image, rgb32);
		}
		ColorYuv.rgbToYuv_F32(rgb32,yuv32);

		// Normalize the image
		localNorm.zeroMeanStdOne(kernel,yuv32.getBand(0),255.0,1e-4,yuv32.getBand(0));
		DataManipulationOps.normalize(yuv32.getBand(1), (float)stats.meanU, (float)stats.stdevU);
		DataManipulationOps.normalize(yuv32.getBand(2), (float)stats.meanV, (float)stats.stdevV);

		// Convert it from an image into a tensor
		DataManipulationOps.imageToTensor(yuv32,tensorYuv,0);

		// Feed it through the CNN
		network.process(tensorYuv,output);

		// Examine and save results
		categoryScores.reset();
		double scoreBest = -Double.MAX_VALUE;
		categoryBest = -1;
		for (int category = 0; category < output.length(1); category++) {
			double score = output.get(0,category);
			categoryScores.grow().set(score,category);
			if( score > scoreBest ) {
				scoreBest = score;
				categoryBest = category;
			}
		}
	}

	private void massageInto32x32( Planar<GrayF32> input , Planar<GrayF32> output) {

	}

	@Override
	public int getBestResult() {
		return categoryBest;
	}

	@Override
	public List<Score> getAllResults() {
		return categoryScores.toList();
	}

	@Override
	public List<String> getCategories() {
		return categoryNames;
	}
}
