/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.scene.ImageRecognition;
import boofcv.abst.scene.nister2006.ConfigImageRecognitionNister2006;
import boofcv.abst.scene.nister2006.ImageRecognitionNister2006;
import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006;
import boofcv.factory.feature.describe.ConfigConvertTupleDesc;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ImageFileListIterator;
import boofcv.io.image.UtilImageIO;
import boofcv.io.recognition.RecognitionIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 **/
public class ExampleImageRecognition {

	public static void main( String[] args ) {
		String imagePath = "/home/pja/Downloads/inria/jpg1";//UtilIO.pathExample("recognition/vacation");
		List<String> images = UtilIO.listByPrefix(imagePath, null, ".jpg");
		Collections.sort(images);

		int maxResolution = 1024*768;
		var scaled = new GrayU8(1024, 768);

		ImageRecognitionNister2006<GrayU8,?> recognizer;

		File saveDirectory = new File("nister2006");

		var imageIterator = new ImageFileListIterator<>(images, ImageType.SB_U8);
		imageIterator.setFilter(( full ) -> {
			double scale = Math.sqrt(maxResolution)/Math.sqrt(full.width*full.height);
			if (scale < 1.0) {
				scaled.reshape((int)(scale*full.width), (int)(scale*full.height));
				AverageDownSampleOps.down(full, scaled);
			} else {
				scaled.setTo(full);
			}
			return scaled;
		});

		if (saveDirectory.exists()) {
			System.out.println("Loading previously generated model");
			recognizer = RecognitionIO.loadNister2006(saveDirectory, ImageType.SB_U8);
		} else {
			// Learn how to describe images
			var config = new ConfigImageRecognitionNister2006();
//		config.tree.branchFactor = 4;
//		config.tree.maximumLevel = 4;
			config.features.detectFastHessian.extract.radius = 3;
//		config.features.detectFastHessian.extract.threshold = 0.0f;
			config.features.convertDescriptor.outputData = ConfigConvertTupleDesc.DataType.F32;
			config.distanceNorm = RecognitionVocabularyTreeNister2006.DistanceTypes.L2;

			recognizer = new ImageRecognitionNister2006<>(config, ImageType.SB_U8);
			recognizer.setVerbose(System.out, null);

			recognizer.learnModel(imageIterator);

			// Add images to the data base
			System.out.println("Adding images to the database");
			imageIterator.reset();
			while (imageIterator.hasNext()) {
				GrayU8 image = imageIterator.next();
				recognizer.addImage(images.get(imageIterator.getIndex()), image);
			}

			System.out.println("Saving tree");
			BoofMiscOps.profile(()->RecognitionIO.saveNister2006(recognizer, saveDirectory),"");
		}

		ListDisplayPanel gui = new ListDisplayPanel();

		// Add the target which the other images are being matched against
		gui.addImage(UtilImageIO.loadImage(images.get(0)), "Target", ScaleOptions.ALL);

		// Look up images
		DogArray<ImageRecognition.Match> matches = new DogArray<>(ImageRecognition.Match::new);
		imageIterator.reset();
		recognizer.findBestMatch(imageIterator.next(), matches);
		for (int i = 0; i < Math.min(10, matches.size); i++) {
			String file = matches.get(i).id;
			double error = matches.get(i).error;
			BufferedImage image = UtilImageIO.loadImage(file);
			String name = FilenameUtils.getBaseName(new File(file).getName());
			gui.addImage(image, String.format("%20s Error %6.3f",name, error), ScaleOptions.ALL);
		}

		System.out.println("Total images = "+images.size());
		System.out.println(images.get(imageIterator.getIndex()) + " -> " + matches.get(0).id + " matches.size=" + matches.size);

		ShowImages.showWindow(gui, "Similar Images by Features", true);
	}
}
