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

import boofcv.alg.color.ColorYuv;
import boofcv.alg.filter.stat.ImageLocalNormalization;
import boofcv.core.image.border.BorderType;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import deepboof.Function;
import deepboof.datasets.UtilCifar10;
import deepboof.io.torch7.ParseBinaryTorch7;
import deepboof.io.torch7.SequenceAndParameters;
import deepboof.models.DeepModelIO;
import deepboof.models.YuvStatistics;
import deepboof.tensors.Tensor_F32;

import java.io.File;
import java.io.IOException;

import static deepboof.misc.TensorOps.WI;

/**
 * TODO comment
 *
 * @author Peter Abeles
 */
public class ImageClassifierVggCifar10 extends BaseImageClassifier {

	static final int inputSize = 32;

	Planar<GrayF32> imageYuv = new Planar<>(GrayF32.class,inputSize,inputSize,3);

	ImageLocalNormalization<GrayF32> localNorm;
	YuvStatistics stats;
	Kernel1D_F32 kernel;

	public ImageClassifierVggCifar10() {
		super(inputSize);
		categories.addAll(UtilCifar10.getClassNames());

	}

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
		stats = DeepModelIO.load(new File(directory,"YuvStatistics.txt"));

		SequenceAndParameters<Tensor_F32, Function<Tensor_F32>> sequence =
				new ParseBinaryTorch7().parseIntoBoof(new File(directory,"model.net"));

		network = sequence.createForward(3,inputSize,inputSize);
		tensorOutput = new Tensor_F32(WI(1,network.getOutputShape()));

		BorderType type = BorderType.valueOf(stats.border);
		localNorm = new ImageLocalNormalization<>(GrayF32.class, type);
		kernel = DataManipulationOps.create1D_F32(stats.kernel);
	}

	@Override
	protected Planar<GrayF32> preprocess(Planar<GrayF32> image) {
		super.preprocess(image);

		ColorYuv.rgbToYuv_F32(imageRgb, imageYuv);

		// Normalize the image
		localNorm.zeroMeanStdOne(kernel, imageYuv.getBand(0),255.0,1e-4, imageYuv.getBand(0));
		DataManipulationOps.normalize(imageYuv.getBand(1), (float)stats.meanU, (float)stats.stdevU);
		DataManipulationOps.normalize(imageYuv.getBand(2), (float)stats.meanV, (float)stats.stdevV);

		return imageYuv;
	}
}
