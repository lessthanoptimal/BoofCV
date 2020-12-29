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
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.examples.features.ExampleAssociatePoints;
import boofcv.factory.feature.associate.ConfigAssociateGreedy;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.*;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastAccess;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * A Fundamental matrix describes the epipolar relationship between two images. If two points, one from
 * each image, match, then the inner product around the Fundamental matrix will be zero. If a fundamental
 * matrix is known, then information about the scene and its structure can be extracted.
 *
 * Below are two examples of how a Fundamental matrix can be computed using different.
 * The robust technique attempts to find the best fit Fundamental matrix to the data while removing noisy
 * matches, The simple version just assumes that all the matches are correct. Similar techniques can be used
 * to fit various other types of motion or structural models to observations.
 *
 * The input image and associated features are displayed in a window. In another window, inlier features
 * from robust model fitting are shown.
 *
 * @author Peter Abeles
 */
public class ExampleComputeFundamentalMatrix {
	/**
	 * Given a set of noisy observations, compute the Fundamental matrix while removing the noise.
	 *
	 * @param matches List of associated features between the two images
	 * @param inliers List of feature pairs that were determined to not be noise.
	 * @return The found fundamental matrix.
	 */
	public static DMatrixRMaj robustFundamental( List<AssociatedPair> matches,
												 List<AssociatedPair> inliers, double inlierThreshold ) {
		ConfigRansac configRansac = new ConfigRansac();
		configRansac.inlierThreshold = inlierThreshold;
		configRansac.iterations = 1000;
		ConfigFundamental configFundamental = new ConfigFundamental();
		configFundamental.which = EnumFundamental.LINEAR_7;
		configFundamental.numResolve = 2;
		configFundamental.errorModel = ConfigFundamental.ErrorModel.GEOMETRIC;
		// geometric error is the most accurate error metric, but also the slowest to compute. See how the
		// results change if you switch to sampson and how much faster it is. You also should adjust
		// the inlier threshold.

		ModelMatcher<DMatrixRMaj, AssociatedPair> ransac =
				FactoryMultiViewRobust.fundamentalRansac(configFundamental, configRansac);

		// Estimate the fundamental matrix while removing outliers
		if (!ransac.process(matches))
			throw new IllegalArgumentException("Failed");

		// save the set of features that were used to compute the fundamental matrix
		inliers.addAll(ransac.getMatchSet());

		// Improve the estimate of the fundamental matrix using non-linear optimization
		DMatrixRMaj F = new DMatrixRMaj(3, 3);
		ModelFitter<DMatrixRMaj, AssociatedPair> refine =
				FactoryMultiView.fundamentalRefine(1e-8, 400, EpipolarError.SAMPSON);
		if (!refine.fitModel(inliers, ransac.getModelParameters(), F))
			throw new IllegalArgumentException("Failed");

		// Return the solution
		return F;
	}

	/**
	 * If the set of associated features are known to be correct, then the fundamental matrix can
	 * be computed directly with a lot less code. The down side is that this technique is very
	 * sensitive to noise.
	 */
	public static DMatrixRMaj simpleFundamental( List<AssociatedPair> matches ) {
		// Use the 8-point algorithm since it will work with an arbitrary number of points
		Estimate1ofEpipolar estimateF = FactoryMultiView.fundamental_1(EnumFundamental.LINEAR_8, 0);

		DMatrixRMaj F = new DMatrixRMaj(3, 3);
		if (!estimateF.process(matches, F))
			throw new IllegalArgumentException("Failed");

		// while not done here, this initial linear estimate can be refined using non-linear optimization
		// as was done above.
		return F;
	}

	/**
	 * Use the associate point feature example to create a list of {@link AssociatedPair} for use in computing the
	 * fundamental matrix.
	 */
	public static List<AssociatedPair> computeMatches( BufferedImage left, BufferedImage right ) {
		DetectDescribePoint detDesc = FactoryDetectDescribe.surfStable(
				new ConfigFastHessian(0, 2, 400, 1, 9, 4, 4), null, null, GrayF32.class);
//		DetectDescribePoint detDesc = FactoryDetectDescribe.sift(null,new ConfigSiftDetector(2,0,200,5),null,null);

		ScoreAssociation<TupleDesc_F64> scorer = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
		AssociateDescription<TupleDesc_F64> associate = FactoryAssociation.greedy(new ConfigAssociateGreedy(true, 0.1), scorer);

		ExampleAssociatePoints<GrayF32, TupleDesc_F64> findMatches =
				new ExampleAssociatePoints<>(detDesc, associate, GrayF32.class);

		findMatches.associate(left, right);

		List<AssociatedPair> matches = new ArrayList<>();
		FastAccess<AssociatedIndex> matchIndexes = associate.getMatches();

		for (int i = 0; i < matchIndexes.size; i++) {
			AssociatedIndex a = matchIndexes.get(i);
			AssociatedPair p = new AssociatedPair(findMatches.pointsA.get(a.src), findMatches.pointsB.get(a.dst));
			matches.add(p);
		}

		return matches;
	}

	public static void main( String[] args ) {
		String dir = UtilIO.pathExample("structure/");

		BufferedImage imageA = UtilImageIO.loadImage(dir, "undist_cyto_01.jpg");
		BufferedImage imageB = UtilImageIO.loadImage(dir, "undist_cyto_02.jpg");

		List<AssociatedPair> matches = computeMatches(imageA, imageB);

		// Where the fundamental matrix is stored
		DMatrixRMaj F;
		// List of matches that matched the model
		List<AssociatedPair> inliers = new ArrayList<>();

		// estimate and print the results using a robust and simple estimator
		// The results should be difference since there are many false associations in the simple model
		// Also note that the fundamental matrix is only defined up to a scale factor.
		F = robustFundamental(matches, inliers, 0.5);
		System.out.println("Robust");
		CommonOps_DDRM.divide(F, NormOps_DDRM.normF(F)); // scale to make comparision easier
		F.print();

		F = simpleFundamental(matches);
		System.out.println("Simple");
		CommonOps_DDRM.divide(F, NormOps_DDRM.normF(F));
		F.print();

		// display the inlier matches found using the robust estimator
		AssociationPanel panel = new AssociationPanel(20);
		panel.setAssociation(inliers);
		panel.setImages(imageA, imageB);

		ShowImages.showWindow(panel, "Inlier Pairs");
	}
}
