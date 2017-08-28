/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import deepboof.Function;
import deepboof.io.torch7.ConvertTorchToBoofForward;
import deepboof.io.torch7.ParseBinaryTorch7;
import deepboof.io.torch7.SequenceAndParameters;
import deepboof.io.torch7.struct.TorchGeneric;
import deepboof.io.torch7.struct.TorchObject;
import deepboof.tensors.Tensor_F32;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 * 1) Look at Torch source code
 *    a) Determine the shape of the input tensor.  That will be used to set imageSize
 *    b) Figure out how it normalizes the input.  Are normalization parameters stored in the network?
 * 2) Load model and inspect in a debugger
 *    List<TorchObject> list = new ParseBinaryTorch7().parse(new File(path,name));
 * 3) Hopefully no problem loading the target object.
 * 4) Your goal now is to figure out where the network is stored.  Inspect the Lua code and the returned object
 *    There is no standard format.  For
 * 5) For resnet-18.t7 the first element in the list is the start of the network
 *    TorchGeneric torchSequence = (TorchGeneric)list.get(0);
 * 6) Convert it into a DeepBoof network.  Cross your fingers and hope that all the layers are supported.
 * 7) An exception is thrown and it says something isn't support or it just crashes with a weird message
 *    MO**** F***** it's not supported.  Contact Peter and hope there's an easy fix or get ready to make a code contribution to DeepBoof
 * 8) You got lucky and no error messages!!!
 *
 *
 *
 *
 *
 * @author Peter Abeles
 */
public class ImageClassifierResNet extends BaseImageClassifier {

	int resnetID;

	public ImageClassifierResNet(int resnetID) {
		super(50);
		this.resnetID = resnetID;
		this.resnetID = 18;
	}

	@Override
	public void loadModel(File path) throws IOException {
		String name = String.format("resnet/resnet-%d.t7",resnetID);

		List<TorchObject> list = new ParseBinaryTorch7().parse(new File(path,name));
		TorchGeneric torchSequence = (TorchGeneric)list.get(0);


//		mean = torchListToArray( (TorchList)torchNorm.get("mean"));
//		stdev = torchListToArray( (TorchList)torchNorm.get("std"));

		SequenceAndParameters<Tensor_F32, Function<Tensor_F32>> seqparam =
				ConvertTorchToBoofForward.convert(torchSequence);

		System.out.println("Bread here");
	}
}
