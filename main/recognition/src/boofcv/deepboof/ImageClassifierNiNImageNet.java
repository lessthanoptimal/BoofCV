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

import boofcv.alg.misc.GPixelMath;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import deepboof.Function;
import deepboof.io.torch7.ConvertTorchToBoofForward;
import deepboof.io.torch7.ParseAsciiTorch7;
import deepboof.io.torch7.ParseBinaryTorch7;
import deepboof.io.torch7.SequenceAndParameters;
import deepboof.io.torch7.struct.*;
import deepboof.tensors.Tensor_F32;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static deepboof.misc.TensorOps.WI;

/**
 * <p>Pretrained Network-in-Network (NiN) image classifier using imagenet data.  Trained by szagoruyko [1,2] and
 * achieves 62.6% top1 center crop accuracy on validation set.</p>
 *
 * <p>
 * [1] https://gist.github.com/szagoruyko/0f5b4c5e2d2b18472854<br>
 * [2] https://github.com/soumith/imagenet-multiGPU.torch/blob/master/models/ninbn.lua
 * </p>
 * @author Peter Abeles
 */
public class ImageClassifierNiNImageNet extends BaseImageClassifier {

	// normalization parameters
	float mean[];
	float stdev[];

//	int imageSize = 256;
	static final int imageCrop = 224;

	// Input image with the bands in the correct order
	Planar<GrayF32> imageBgr = new Planar<>(GrayF32.class,imageCrop,imageCrop,3);

	public ImageClassifierNiNImageNet() {
		super(imageCrop);
	}

	@Override
	public void loadModel(File directory) throws IOException {

		List<TorchObject> list = new ParseBinaryTorch7().parse(new File(directory,"nin_bn_final.t7"));
		TorchGeneric torchSequence = ((TorchGeneric)list.get(0)).get("model");

		TorchGeneric torchNorm = torchSequence.get("transform");

		mean = torchListToArray( (TorchList)torchNorm.get("mean"));
		stdev = torchListToArray( (TorchList)torchNorm.get("std"));

		SequenceAndParameters<Tensor_F32, Function<Tensor_F32>> seqparam =
				ConvertTorchToBoofForward.convert(torchSequence);

		network = seqparam.createForward(3,imageCrop,imageCrop);
		tensorOutput = new Tensor_F32(WI(1,network.getOutputShape()));

		TorchList torchCategories = (TorchList)new ParseAsciiTorch7().parse(new File(directory,"synset.t7")).get(0);

		categories.clear();
		for (int i = 0; i < torchCategories.list.size(); i++) {
			categories.add( ((TorchString)torchCategories.list.get(i)).message );
		}
	}

	private float[] torchListToArray( TorchList torch ) {
		float []ret = new float[ torch.list.size()];

		for (int i = 0; i < ret.length; i++) {
			ret[i] = (float)((TorchNumber)torch.list.get(i)).value;
		}

		return ret;
	}


	/**
	 * Massage the input image into a format recognized by the network
	 */
	protected Planar<GrayF32> preprocess(Planar<GrayF32> image) {
		super.preprocess(image);

		// image net is BGR color order
		imageBgr.bands[0] = imageRgb.bands[2];
		imageBgr.bands[1] = imageRgb.bands[1];
		imageBgr.bands[2] = imageRgb.bands[0];

		// image needs to be between 0 and 1
		GPixelMath.divide(imageBgr,255,imageBgr);

		// Normalize the image's statistics
		for (int band = 0; band < 3; band++) {
			DataManipulationOps.normalize(imageBgr.getBand(band),mean[band],stdev[band]);
		}

		return imageBgr;
	}
}
