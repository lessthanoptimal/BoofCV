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

package boofcv.examples.sfm;

import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.examples.features.ExampleAssociateThreeView;
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
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.DogArray;

import java.util.List;

/**
 * This example shows how to robustly compute a trifocal tensor to features across three views. The trifocal tensor
 * is used extensively in reconstruction scenarios and is more robust than applying two-view tensors (e.g. Fundamental
 * and Essential matrices) which have issues along the epipolar lines.
 */
public class ExampleComputeTrifocalTensor {
	/**
	 * Computes the trifocal tensor given three images. Returns the set of inlier features found when computing
	 * the tensor
	 */
	public static List<AssociatedTriple> imagesToTrifocal(
			GrayU8 gray01, GrayU8 gray02, GrayU8 gray03, TrifocalTensor model ) {
		// Using SURF features. Robust and fairly fast to compute
		var configDetector = new ConfigFastHessian();
		configDetector.maxFeaturesAll = 2500; // limit the feature count
		configDetector.extract.radius = 4;
		DetectDescribePoint<GrayU8, TupleDesc_F64> detDesc = FactoryDetectDescribe.surfStable(configDetector, null, null, GrayU8.class);

		// Associate features across all three views using previous example code
		var associateThree = new ExampleAssociateThreeView();
		associateThree.initialize(detDesc);
		associateThree.detectFeatures(gray01, 0);
		associateThree.detectFeatures(gray02, 1);
		associateThree.detectFeatures(gray03, 2);
		DogArray<AssociatedTripleIndex> associatedIdx = associateThree.threeViewPairwiseAssociate();

		System.out.println("features01.size = " + associateThree.features01.size);
		System.out.println("features02.size = " + associateThree.features02.size);
		System.out.println("features03.size = " + associateThree.features03.size);

		// Convert the matched indexes into AssociatedTriple which contain the actual pixel coordinates
		var associated = new DogArray<>(AssociatedTriple::new);
		associatedIdx.forEach(p -> associated.grow().setTo(
				associateThree.locations01.get(p.a),
				associateThree.locations02.get(p.b),
				associateThree.locations03.get(p.c)));

		System.out.println("Total Matched Triples = " + associated.size);

		// Storage for the found model. In this example we don't actually use the tensor.
		List<AssociatedTriple> inliers = computeTrifocal(associated, model);
		System.out.println("Remaining after RANSAC " + inliers.size());

		return inliers;
	}

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

	public static void main( String[] args ) {
		String name = "rock_leaves_";
		GrayU8 gray01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/" + name + "01.jpg"), GrayU8.class);
		GrayU8 gray02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/" + name + "02.jpg"), GrayU8.class);
		GrayU8 gray03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/" + name + "03.jpg"), GrayU8.class);

		// Compute the trifocal tensor which matches features inside these images
		var model = new TrifocalTensor();
		List<AssociatedTriple> inliers = imagesToTrifocal(gray01, gray02, gray03, model);

		// Show inlier associations from RANSAC
		var triplePanel = new AssociatedTriplePanel();
		triplePanel.setImages(
				UtilImageIO.loadImageNotNull(UtilIO.pathExample("triple/" + name + "01.jpg")),
				UtilImageIO.loadImageNotNull(UtilIO.pathExample("triple/" + name + "02.jpg")),
				UtilImageIO.loadImageNotNull(UtilIO.pathExample("triple/" + name + "03.jpg")));
		triplePanel.setAssociation(inliers);
		ShowImages.showWindow(triplePanel, "Inliers", true);
	}
}
