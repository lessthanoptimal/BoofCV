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

package boofcv.examples.features;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.feature.associate.AssociateThreeByPairs;
import boofcv.factory.feature.associate.ConfigAssociateGreedy;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.gui.feature.AssociatedTriplePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.AssociatedTripleIndex;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import java.util.List;

/**
 * Common matches between sets of three views are important in SFM as they filter out even more false positives
 * and three view geometry, unlike two view-geometry, has a unique projection in each image. This makes it even
 * easier to remove false matches using geometric constraints. In BoofCV's reconstruction pipeline three views
 * are always used over two views whenever possible due to the added robustness.
 *
 * In this example, association is first done pairwise between each image pair. Then the matches are traversed
 * to find features which form a "ring", that is that when traversed from image 1 -> 2 - > 3 -> 1 you wind up
 * back at the same location.
 */
public class ExampleAssociateThreeView {

	// Stores image pixel coordinate
	public final DogArray<Point2D_F64> locations01 = new DogArray<>(Point2D_F64::new);
	public final DogArray<Point2D_F64> locations02 = new DogArray<>(Point2D_F64::new);
	public final DogArray<Point2D_F64> locations03 = new DogArray<>(Point2D_F64::new);

	// Stores the descriptor for each feature
	public DogArray<TupleDesc_F64> features01, features02, features03;

	// Indicates which "set" a feature belongs in. SURF can be white or black. Using sets simplifies
	// feature association since only features in the same set can be matched
	public final DogArray_I32 featureSet01 = new DogArray_I32();
	public final DogArray_I32 featureSet02 = new DogArray_I32();
	public final DogArray_I32 featureSet03 = new DogArray_I32();

	// Reference to the feature detector/descriptor
	DetectDescribePoint<GrayU8, TupleDesc_F64> detDesc;

	// Create lists when accessing by index makes more sense
	List<DogArray<Point2D_F64>> listLocations = BoofMiscOps.asList(locations01, locations02, locations03);
	List<DogArray_I32> listFeatureSets = BoofMiscOps.asList(featureSet01, featureSet02, featureSet03);
	List<DogArray<TupleDesc_F64>> listFeatures;

	/**
	 * Initializes data structures to use the feature descriptor
	 */
	public <T extends ImageBase<T>> void initialize( DetectDescribePoint<GrayU8, TupleDesc_F64> detDesc ) {
		this.detDesc = detDesc;
		features01 = UtilFeature.createArray(detDesc, 100);
		features02 = UtilFeature.createArray(detDesc, 100);
		features03 = UtilFeature.createArray(detDesc, 100);
		listFeatures = BoofMiscOps.asList(features01, features02, features03);
	}

	/**
	 * Detects and saves features in the specified image
	 */
	public void detectFeatures( GrayU8 gray, int which ) {
		DogArray<Point2D_F64> locations = listLocations.get(which);
		DogArray<TupleDesc_F64> features = listFeatures.get(which);
		DogArray_I32 featureSet = listFeatureSets.get(which);

		detDesc.detect(gray);
		for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
			Point2D_F64 pixel = detDesc.getLocation(i);
			locations.grow().setTo(pixel.x, pixel.y);
			features.grow().setTo(detDesc.getDescription(i));
			featureSet.add(detDesc.getSet(i));
		}
	}

	/**
	 * BoofCV comes with a class which does all the three view matching for you. Which association and scoring
	 * function are used is all configurable.
	 */
	public DogArray<AssociatedTripleIndex> threeViewPairwiseAssociate() {
		ScoreAssociation<TupleDesc_F64> scorer =
				FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
		AssociateDescription<TupleDesc_F64> associate =
				FactoryAssociation.greedy(new ConfigAssociateGreedy(true, 0.1), scorer);

		var associateThree = new AssociateThreeByPairs<>(associate);

		associateThree.initialize(detDesc.getNumberOfSets());
		associateThree.setFeaturesA(features01, featureSet01);
		associateThree.setFeaturesB(features02, featureSet02);
		associateThree.setFeaturesC(features03, featureSet03);

		associateThree.associate();

		return associateThree.getMatches();
	}

	public static void main( String[] args ) {
		String name = "rock_leaves_";
		GrayU8 gray01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/" + name + "01.jpg"), GrayU8.class);
		GrayU8 gray02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/" + name + "02.jpg"), GrayU8.class);
		GrayU8 gray03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/" + name + "03.jpg"), GrayU8.class);

		// Using SURF features. Robust and fairly fast to compute
		DetectDescribePoint<GrayU8, TupleDesc_F64> detDesc = FactoryDetectDescribe.surfStable(
				new ConfigFastHessian(0, 4, 1000, 1, 9, 4, 2), null, null, GrayU8.class);

		ExampleAssociateThreeView example = new ExampleAssociateThreeView();
		example.initialize(detDesc);

		// Compute and describe features inside the image
		example.detectFeatures(gray01, 0);
		example.detectFeatures(gray02, 1);
		example.detectFeatures(gray03, 2);

		System.out.println("features01.size = " + example.features01.size);
		System.out.println("features02.size = " + example.features02.size);
		System.out.println("features03.size = " + example.features03.size);

		// Find features for an association ring across all the views. This removes most false positives.
		DogArray<AssociatedTripleIndex> associatedIdx = example.threeViewPairwiseAssociate();

		// Convert the matched indexes into AssociatedTriple which contain the actual pixel coordinates
		var associated = new DogArray<>(AssociatedTriple::new);
		associatedIdx.forEach(p -> associated.grow().setTo(
				example.locations01.get(p.a), example.locations02.get(p.b), example.locations03.get(p.c)));

		System.out.println("Total Matched Triples = " + associated.size);

		// Show remaining associations from RANSAC
		var triplePanel = new AssociatedTriplePanel();
		triplePanel.setImages(
				UtilImageIO.loadImageNotNull(UtilIO.pathExample("triple/" + name + "01.jpg")),
				UtilImageIO.loadImageNotNull(UtilIO.pathExample("triple/" + name + "02.jpg")),
				UtilImageIO.loadImageNotNull(UtilIO.pathExample("triple/" + name + "03.jpg")));
		triplePanel.setAssociation(associated.toList());
		ShowImages.showWindow(triplePanel, "Associations", true);
	}
}
