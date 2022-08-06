/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.reconstruction;

import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.scene.FeatureSceneRecognition;
import boofcv.abst.scene.SceneRecognition;
import boofcv.abst.scene.nister2006.ConfigRecognitionNister2006;
import boofcv.factory.feature.associate.ConfigAssociateGreedy;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.scene.FactorySceneRecognition;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedU8;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Shows how you can detect if two images are of the same scene. This is known as loop closure and is done in
 * robotic mapping, e.g. SLAM. Here will use a fast recognition approach that takes only a few milliseconds to find
 * the most likely candidate images using image features alone. After that we perform feature matching to reduce
 * false positives. A complete solution would involve a geometric check, i.e. Fundamental matrix.
 *
 * Using scene recognition drastically reduces computational time as it eliminates most bad matches. As a result
 * this can run in a real-time or near real-time environment.
 *
 * @author Peter Abeles
 */
public class ExampleLoopClosure {
	public static void main( String[] args ) {
		System.out.println("Finding Images");
		String pathImages = "loop_closure";
		videoToImages(UtilIO.pathExample("mvs/stone_sign.mp4"), pathImages);
		List<String> imagePaths = UtilIO.listSmart(String.format("glob:%s/*.png", pathImages), true, ( f ) -> true);

		// Create the feature detector. Default settings are often not the best configuration for recognition.
		// Finding the best settings is left as an exercise for the reader.
		DetectDescribePoint<GrayU8, TupleDesc_F64> detector =
				FactoryDetectDescribe.surfFast(null, null, null, GrayU8.class);

		// Detect features in all the images
		var descriptions = new ArrayList<FastAccess<TupleDesc_F64>>();
		var locations = new ArrayList<FastAccess<Point2D_F64>>();

		System.out.println("Feature Detection");
		for (int pathIdx = 0; pathIdx < imagePaths.size(); pathIdx++) {
			// Print out the progress
			System.out.print("*");
			if (pathIdx%80 == 79)
				System.out.println();

			// Load the image and detect features
			String path = imagePaths.get(pathIdx);
			GrayU8 gray = UtilImageIO.loadImage(path, GrayU8.class);

			detector.detect(gray);

			// Copy all the features into lists for this image
			var imageDescriptions = new DogArray<>(detector::createDescription);
			var imageLocations = new DogArray<>(Point2D_F64::new);

			for (int i = 0; i < detector.getNumberOfFeatures(); i++) {
				imageDescriptions.grow().setTo(detector.getDescription(i));
				imageLocations.grow().setTo(detector.getLocation(i));
			}
			descriptions.add(imageDescriptions);
			locations.add(imageLocations);
		}
		System.out.println();

		// Put feature information into a format scene recognition understands
		var listRecFeat = new ArrayList<FeatureSceneRecognition.Features<TupleDesc_F64>>();
		for (int i = 0; i < descriptions.size(); i++) {
			FastAccess<Point2D_F64> pixels = locations.get(i);
			FastAccess<TupleDesc_F64> descs = descriptions.get(i);
			listRecFeat.add(new FeatureSceneRecognition.Features<>() {
				@Override public Point2D_F64 getPixel( int index ) {return pixels.get(index);}

				@Override public TupleDesc_F64 getDescription( int index ) {return descs.get(index);}

				@Override public int size() {return pixels.size();}
			});
		}

		System.out.println("Learning model. Can take a minute. You can save and reload this model.");

		var config = new ConfigRecognitionNister2006();
		config.learningMinimumPointsForChildren.setFixed(20);
		FeatureSceneRecognition<TupleDesc_F64> recognizer =
				FactorySceneRecognition.createSceneNister2006(config, detector::createDescription);

		// Pass image information in as an iterator that it understands.
		recognizer.learnModel(new Iterator<>() {
			int imageIndex = 0;

			@Override public boolean hasNext() {return imageIndex < descriptions.size();}

			@Override public FeatureSceneRecognition.Features<TupleDesc_F64> next() {
				return listRecFeat.get(imageIndex++);
			}
		});

		// To find functions for saving and loading these models look at RecognitionIO

		System.out.println("Creating database");
		for (int imageIdx = 0; imageIdx < descriptions.size(); imageIdx++) {
			// Note that image are assigned a name equal to their index
			recognizer.addImage(imageIdx + "", listRecFeat.get(imageIdx));
		}

		System.out.println("Scoring likely loop closures");

		// Have a strict requirement for matching to reduce false positives
		var configAssociate = new ConfigAssociateGreedy();
		configAssociate.forwardsBackwards = true;
		configAssociate.scoreRatioThreshold = 0.9;

		var scorer = FactoryAssociation.scoreEuclidean(detector.getDescriptionType(), true);
		var associate = FactoryAssociation.greedy(configAssociate, scorer);

		// Go through all the images and use scene recongition to greatly reduce the number of images that need
		// to be considered. Scene recognition is very fast, while feature matching is slow, and geometric
		// checks are even slower.
		var matches = new DogArray<>(SceneRecognition.Match::new);
		for (int imageIdx = 0; imageIdx < descriptions.size(); imageIdx++) {
			// Query results to find the best matches.
			// We are going to pass in a filter that will remove all the most recent frames since we don't care
			// about those. This way we know all the returned results are potential loop closures.
			int _imageIdx = imageIdx;
			recognizer.query(
					/*query*/ listRecFeat.get(imageIdx),
					/*filter*/ ( id ) -> Math.abs(_imageIdx - Integer.parseInt(id)) > 20,
					/*limit*/ 5, /*found matches*/ matches);

			// Set up association
			associate.setSource(descriptions.get(imageIdx));
			int numFeatures = descriptions.get(imageIdx).size;

			System.out.printf("Image[%3d]\n", imageIdx);
			for (var m : matches.toList()) {
				// Note how earlier it assigned the image name to be the index value as a string
				int imageDstIdx = Integer.parseInt(m.id);

				// Perform association
				associate.setDestination(descriptions.get(imageDstIdx));
				associate.associate();

				// Compute and print quality of fit metrics
				double matchFraction = associate.getMatches().size/(double)numFeatures;
				System.out.printf("  %4s error=%.2f matches=%.2f\n", m.id, m.error, matchFraction);

				// A loop closure will have a large number of matching features. When the fraction goes
				// over 30% in this example, you probably have a good match.

				// Typically a geometric check is done next, such as estimating a fundamental matrix or PNP.
				// With a geometric check the odds of a false positive are low.
			}
		}
		System.out.println("Done!");
	}

	// Convenience functions to convert a video into images
	public static void videoToImages( String pathVideo, String pathOutput ) {
		// Make sure the output path exists
		UtilIO.mkdirs(new File(pathOutput));

		// Load te video
		SimpleImageSequence<InterleavedU8> sequence = DefaultMediaManager.INSTANCE.openVideo(pathVideo, ImageType.IL_U8);
		if (sequence == null)
			throw new RuntimeException("Failed to load video sequence '" + pathVideo + "'");

		// Extract the frames
		int frame = 0;
		while (sequence.hasNext()) {
			InterleavedU8 image = sequence.next();
			File imageFile = new File(pathOutput, String.format("frame%04d.png", frame++));
			UtilImageIO.saveImage(image, imageFile.getPath());
		}
	}
}
