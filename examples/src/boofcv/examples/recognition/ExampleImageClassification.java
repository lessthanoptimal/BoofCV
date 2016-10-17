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

package boofcv.examples.recognition;

import boofcv.abst.scene.ImageClassifier;
import boofcv.factory.scene.ClassifierAndSource;
import boofcv.factory.scene.FactoryImageClassifier;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import deepboof.io.DeepBoofDataBaseOps;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This example shows how to create an image classifier using the high level factory, download the model, load it,
 * process images, and then look at the results.
 *
 * @author Peter Abeles
 */
// TODO visualize results.  Show image and the class labels
public class ExampleImageClassification {

	public static void main(String[] args) throws IOException {
		ClassifierAndSource cs = FactoryImageClassifier.vgg_cifar10();
//		ClassifierAndSource cs = FactoryImageClassifier.nin_imagenet();

		File path = DeepBoofDataBaseOps.downloadModel(cs.getSource(),new File("download_data"));

		ImageClassifier<Planar<GrayF32>> classifier = cs.getClassifier();
		classifier.loadModel(path);

		// TODO get test images
		String images[] = new String[]{"horse4.jpg","airplane.jpg"};

		BufferedImage buffered = UtilImageIO.loadImage(images[0]);
		if( buffered == null)
			throw new RuntimeException("Couldn't find input image");

		Planar<GrayF32> image = new Planar<>(GrayF32.class,buffered.getWidth(), buffered.getHeight(), 3);
		ConvertBufferedImage.convertFromMulti(buffered,image,true,GrayF32.class);

		classifier.classify(image);

		List<String> categories = classifier.getCategories();
		System.out.println();
		System.out.println("Selected "+categories.get( classifier.getBestResult()));
		System.out.println();

		List<ImageClassifier.Score> scores = new ArrayList<>(classifier.getAllResults());
		int N = Math.min(5,scores.size());
		for( ImageClassifier.Score score : scores.subList(0,N) ) {
			System.out.printf(" %7.3f  %s\n",score.score,categories.get(score.category));
		}
	}
}
