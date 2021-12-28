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

import boofcv.BoofVerbose;
import boofcv.abst.scene.ConfigFeatureToSceneRecognition;
import boofcv.abst.scene.SceneRecognition;
import boofcv.abst.scene.WrapFeatureToSceneRecognition;
import boofcv.factory.scene.FactorySceneRecognition;
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
 * In BoofCV, scene recognition [1] refers to the problem of trying to identify photos of the same scene (not a single
 * object in the image) from different perspectives. This is of interest if you want to organize your photos, find
 * images to create a mosaic from, or cluster photos for 3D reconstruction. Solutions to this problem tend to
 * emphasise fast accurate retrieval from large databases.
 *
 * [1] As far as I can tell there is no universal terminology for this specific sub problem. It is sometimes lumped
 * under Content Based Image Retrieval (CBIR), which is a very generic term.
 *
 * @author Peter Abeles
 */
public class ExampleSceneRecognition {
	public static void main( String[] args ) {
		String imagePath = UtilIO.pathExample("recognition/scene");
		List<String> images = UtilIO.listByPrefix(imagePath, null, ".jpg");
		Collections.sort(images);

		SceneRecognition<GrayU8> recognizer;

		// Except for real-time applications or when there are more than a few hundred images, you might want to
		// just learn the dictionary from scratch
		File saveDirectory = new File("example_recognition");

		// Tell it to process gray U8 images
		ImageType<GrayU8> imageType = ImageType.SB_U8;

		// Used to look up images one at a time from various sources. In this case a list of images.
		var imageIterator = new ImageFileListIterator<>(images, imageType);

		if (false) {
			// Set the line above to true and it will download a pre-built model. Useful when you have a lot of images
			// or simply want to skip the learning step
			System.out.println("Downloading pre-built model");
			recognizer = RecognitionIO.downloadDefaultSceneRecognition(new File("downloaded_models"), imageType);
			recognizer.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));
		} else if (saveDirectory.exists()) {
			System.out.println("Loading previously generated model");
			recognizer = RecognitionIO.loadFeatureToScene(saveDirectory, imageType);
			recognizer.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));
		} else {
			// If many applications, learning a new model is a small fraction of the compute time and since its
			// fit to the images it will be more accurate than a generic pre-built model
			System.out.println("Creating a new model");
			var config = new ConfigFeatureToSceneRecognition();
			// Use a hierarchical vocabulary tree, which is very fast and also one of the more accurate approaches
			config.typeRecognize = ConfigFeatureToSceneRecognition.Type.NISTER_2006;
			config.recognizeNister2006.learningMinimumPointsForChildren.setFixed(20);

			recognizer = FactorySceneRecognition.createFeatureToScene(config, imageType);
			// This will print out a lot of debugging information to stdout
			recognizer.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));

			// Learn the model from the initial set of images
			recognizer.learnModel(imageIterator);
		}

		// See if the recognition algorithm already has images loaded in to it
		if (recognizer.getImageIds(null).isEmpty()) {
			// Add images to the data base
			System.out.println("Adding images to the database");
			imageIterator.reset();
			while (imageIterator.hasNext()) {
				GrayU8 image = imageIterator.next();
				recognizer.addImage(images.get(imageIterator.getIndex()), image);
			}

			// This saves the model with the image database to disk
			System.out.println("Saving model");
			BoofMiscOps.profile(() -> RecognitionIO.saveFeatureToScene(
					(WrapFeatureToSceneRecognition<GrayU8, ?>)recognizer, saveDirectory), "");
		}

		ListDisplayPanel gui = new ListDisplayPanel();

		// Specifies which image it will try to look up. In the example, related images are in sets of 3.
		int queryImage = 9;

		// Add the target which the other images are being matched against
		gui.addImage(UtilImageIO.loadImageNotNull(images.get(queryImage)), "Query", ScaleOptions.ALL);

		// Look up images
		DogArray<SceneRecognition.Match> matches = new DogArray<>(SceneRecognition.Match::new);
		recognizer.query(imageIterator.loadImage(queryImage),/* filter */ ( id ) -> true,/* limit */ 5, matches);
		for (int i = 0; i < matches.size; i++) {
			String file = matches.get(i).id;
			double error = matches.get(i).error;
			BufferedImage image = UtilImageIO.loadImageNotNull(file);
			String name = FilenameUtils.getBaseName(new File(file).getName());
			gui.addImage(image, String.format("%20s Error %6.3f", name, error), ScaleOptions.ALL);
		}

		System.out.println("Total images = " + images.size());
		System.out.println(images.get(queryImage) + " -> " + matches.get(0).id + " matches.size=" + matches.size);

		ShowImages.showWindow(gui, "Similar Images by Features", true);
	}
}
