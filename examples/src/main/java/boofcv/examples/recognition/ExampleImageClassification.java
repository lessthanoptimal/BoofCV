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

package boofcv.examples.recognition;

import boofcv.abst.scene.ImageClassifier;
import boofcv.factory.scene.ClassifierAndSource;
import boofcv.factory.scene.FactoryImageClassifier;
import boofcv.gui.ImageClassificationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import deepboof.io.DeepBoofDataBaseOps;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This example shows how to create an image classifier using the high level factory, download the model, load it,
 * process images, and then look at the results.
 *
 * @author Peter Abeles
 */
public class ExampleImageClassification {

	public static void main(String[] args) throws IOException {
		ClassifierAndSource cs = FactoryImageClassifier.vgg_cifar10();  // Test set 89.9% for 10 categories
//		ClassifierAndSource cs = FactoryImageClassifier.nin_imagenet(); // Test set 62.6% for 1000 categories

		File path = DeepBoofDataBaseOps.downloadModel(cs.getSource(),new File("download_data"));

		ImageClassifier<Planar<GrayF32>> classifier = cs.getClassifier();
		classifier.loadModel(path);
		List<String> categories = classifier.getCategories();

		String imagePath = UtilIO.pathExample("recognition/pixabay");
		List<File> images = Arrays.asList(UtilIO.findMatches(new File(imagePath),"\\w*.jpg"));
		Collections.sort(images);

		ImageClassificationPanel gui = new ImageClassificationPanel();
		ShowImages.showWindow(gui, "Image Classification", true);

		for( File f : images ) {
			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null)
				throw new RuntimeException("Couldn't find input image");

			Planar<GrayF32> image = new Planar<>(GrayF32.class,buffered.getWidth(), buffered.getHeight(), 3);
			ConvertBufferedImage.convertFromPlanar(buffered,image,true,GrayF32.class);

			classifier.classify(image);

			// add image and results to the GUI for display
			gui.addImage(buffered,f.getName(),classifier.getAllResults(),categories);
		}
	}
}
