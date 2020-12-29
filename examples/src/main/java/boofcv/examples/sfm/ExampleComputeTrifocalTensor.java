/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.sfm;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.feature.associate.AssociateThreeByPairs;
import boofcv.factory.feature.associate.ConfigAssociateGreedy;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.ConfigTrifocal;
import boofcv.factory.geo.ConfigTrifocalError;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.gui.feature.AssociatedTriplePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.AssociatedTripleIndex;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import java.util.List;

/**
 * This example shows how to robustly compute a trifocal tensor to features across three views. The trifocal tensor
 * is used extensively in reconstruction scenarios and is more robust than applying two-view tensors (e.g. Fundamental
 * and Essential matrices) which have issues along the epipolar lines.
 */
public class ExampleComputeTrifocalTensor {

	/**
	 * Computes the Trifocal Tensor using RANSAC given associated features across 3-views. The found tensor
	 * is copied into model and the inliers are returned.
	 */
	public static List<AssociatedTriple> computeTrifocal( DogArray<AssociatedTriple> associated, TrifocalTensor model ) {
		var configRansac = new ConfigRansac();
		configRansac.iterations = 500;
		configRansac.inlierThreshold = 1;

		var configTri = new ConfigTrifocal();
		ConfigTrifocalError configError = new ConfigTrifocalError();
		configError.model = ConfigTrifocalError.Model.REPROJECTION_REFINE;

		Ransac<TrifocalTensor, AssociatedTriple> ransac =
				FactoryMultiViewRobust.trifocalRansac(configTri, configError, configRansac);

		ransac.process(associated.toList());
		model.setTo(ransac.getModelParameters());

		return ransac.getMatchSet();
	}

	/**
	 * Matches features across three views by performing pairwise association first then finding features which can
	 * be tracked in a loop across all three views
	 */
	public static DogArray<AssociatedTripleIndex>
	threeViewPairwiseAssociate( int numSets,
								DogArray<TupleDesc_F64> features01, DogArray<TupleDesc_F64> features02,
								DogArray<TupleDesc_F64> features03,
								DogArray_I32 featureSet01, DogArray_I32 featureSet02, DogArray_I32 featureSet03 ) {
		ScoreAssociation<TupleDesc_F64> scorer =
				FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
		AssociateDescription<TupleDesc_F64> associate =
				FactoryAssociation.greedy(new ConfigAssociateGreedy(true, 0.1), scorer);

		var associateThree = new AssociateThreeByPairs<>(associate, TupleDesc_F64.class);

		associateThree.initialize(numSets);
		associateThree.setFeaturesA(features01, featureSet01);
		associateThree.setFeaturesB(features02, featureSet02);
		associateThree.setFeaturesC(features03, featureSet03);

		associateThree.associate();

		return associateThree.getMatches();
	}

	public static void detectFeatures( DetectDescribePoint<GrayU8, TupleDesc_F64> detDesc,
									   GrayU8 gray, DogArray<Point2D_F64> locations, DogArray<TupleDesc_F64> features,
									   DogArray_I32 featureSet ) {
		detDesc.detect(gray);
		for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
			Point2D_F64 pixel = detDesc.getLocation(i);
			locations.grow().setTo(pixel.x, pixel.y);
			features.grow().setTo(detDesc.getDescription(i));
			featureSet.add(detDesc.getSet(i));
		}
	}

	public static void main( String[] args ) {
		String name = "rock_leaves_";
		GrayU8 gray01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/" + name + "01.jpg"), GrayU8.class);
		GrayU8 gray02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/" + name + "02.jpg"), GrayU8.class);
		GrayU8 gray03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/" + name + "03.jpg"), GrayU8.class);

		// Using SURF features. Robust and fairly fast to compute
		DetectDescribePoint<GrayU8, TupleDesc_F64> detDesc = FactoryDetectDescribe.surfStable(
				new ConfigFastHessian(0, 4, 1000, 1, 9, 4, 2), null, null, GrayU8.class);

		// Stores image coordinate
		var locations01 = new DogArray<>(Point2D_F64::new);
		var locations02 = new DogArray<>(Point2D_F64::new);
		var locations03 = new DogArray<>(Point2D_F64::new);

		// Stores the descriptor for each feature
		DogArray<TupleDesc_F64> features01 = UtilFeature.createArray(detDesc, 100);
		DogArray<TupleDesc_F64> features02 = UtilFeature.createArray(detDesc, 100);
		DogArray<TupleDesc_F64> features03 = UtilFeature.createArray(detDesc, 100);

		// Indicates which "set" a feature belongs in. SURF can be white or black
		var featureSet01 = new DogArray_I32();
		var featureSet02 = new DogArray_I32();
		var featureSet03 = new DogArray_I32();

		// Compute and describe features inside the image
		detectFeatures(detDesc, gray01, locations01, features01, featureSet01);
		detectFeatures(detDesc, gray02, locations02, features02, featureSet02);
		detectFeatures(detDesc, gray03, locations03, features03, featureSet03);

		System.out.println("features01.size = " + features01.size);
		System.out.println("features02.size = " + features02.size);
		System.out.println("features03.size = " + features03.size);

		// Perform pair-wise matching across all three views
		DogArray<AssociatedTripleIndex> associatedIdx =
				threeViewPairwiseAssociate(detDesc.getNumberOfSets(),
						features01, features02, features03,
						featureSet01, featureSet02, featureSet03);

		// Convert the matched indexes into AssociatedTriple which contain the actual pixel coordinates
		var associated = new DogArray<>(AssociatedTriple::new);
		associatedIdx.forEach(p->associated.grow().setTo(
				locations01.get(p.a), locations02.get(p.b), locations03.get(p.c)));

		System.out.println("Total Matched Triples = " + associated.size);

		// Storage for the found model. In this example we don't actually use the tensor.
		var model = new TrifocalTensor();
		List<AssociatedTriple> inliers = computeTrifocal(associated, model);

		System.out.println("Remaining after RANSAC " + inliers.size());

		// Show remaining associations from RANSAC
		var triplePanel = new AssociatedTriplePanel();
		triplePanel.setImages(
				UtilImageIO.loadImage(UtilIO.pathExample("triple/" + name + "01.jpg")),
				UtilImageIO.loadImage(UtilIO.pathExample("triple/" + name + "02.jpg")),
				UtilImageIO.loadImage(UtilIO.pathExample("triple/" + name + "03.jpg")));
		triplePanel.setAssociation(inliers);
		ShowImages.showWindow(triplePanel, "Associations", true);
	}
}
