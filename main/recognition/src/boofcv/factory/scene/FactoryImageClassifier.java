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

package boofcv.factory.scene;

import boofcv.deepboof.ImageClassifierNiNImageNet;
import boofcv.deepboof.ImageClassifierVggCifar10;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating image classifiers.  The classifier and a location for where to download the model
 * data is provided.
 *
 * @author Peter Abeles
 */
public class FactoryImageClassifier {
	/**
	 * VGG trained on CIFAR10 data
	 *
	 * @see ImageClassifierVggCifar10
	 *
	 * @return The classifier and where to download the model
	 */
	public static ClassifierAndSource vgg_cifar10() {
		List<String> sources = new ArrayList<>();
		sources.add( "http://heanet.dl.sourceforge.net/project/deepboof/networks/v1/likevgg_cifar10.zip" );
		sources.add( "http://pilotfiber.dl.sourceforge.net/project/deepboof/networks/v1/likevgg_cifar10.zip" );

		ClassifierAndSource ret = new ClassifierAndSource();

		ret.data0 = new ImageClassifierVggCifar10();
		ret.data1 = sources;

		return ret;
	}

	/**
	 * NIN trained on ImageNet data
	 *
	 * @see ImageClassifierVggCifar10
	 *
	 * @return The classifier and where to download the model
	 */
	public static ClassifierAndSource nin_imagenet() {
		List<String> sources = new ArrayList<>();
		sources.add( "http://heanet.dl.sourceforge.net/project/boofcv/datasets/nin_imagenet.zip" );
		sources.add( "http://pilotfiber.dl.sourceforge.net/project/boofcv/datasets/nin_imagenet.zip" );

		ClassifierAndSource ret = new ClassifierAndSource();

		ret.data0 = new ImageClassifierNiNImageNet();
		ret.data1 = sources;

		return ret;
	}
}
