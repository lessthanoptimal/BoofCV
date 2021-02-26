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

package boofcv.factory.sfm;

import boofcv.abst.geo.bundle.MetricBundleAdjustmentUtils;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.mvs.MultiViewStereoFromKnownSceneStructure;
import boofcv.alg.sfm.structure.*;
import boofcv.alg.sfm.structure.score3d.ScoreFundamentalReprojectionError;
import boofcv.alg.sfm.structure.score3d.ScoreRatioFundamentalHomography;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.homography.Homography2D_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for operations related to scene reconstruction
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class FactorySceneReconstruction {
	/**
	 * Creates {@link MetricBundleAdjustmentUtils}
	 */
	public static MetricBundleAdjustmentUtils bundleUtils( @Nullable ConfigBundleUtils config ) {
		if (config == null)
			config = new ConfigBundleUtils();

		var alg = new MetricBundleAdjustmentUtils(config.triangulation, config.homogenous);
		alg.configConverge.setTo(config.converge);
		alg.configScale = config.scale;
		alg.keepFraction = config.keepFraction;

		return alg;
	}

	/**
	 * Creates {@link GeneratePairwiseImageGraph}
	 */
	public static GeneratePairwiseImageGraph generatePairwise( @Nullable ConfigGeneratePairwiseImageGraph config ) {
		if (config == null)
			config = new ConfigGeneratePairwiseImageGraph();

		ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D =
				FactoryMultiViewRobust.fundamentalRansac(config.score.fundamental, config.score.ransacF);

		EpipolarScore3D scorer =
				switch (config.score.type) {
					case MODEL_INLIERS -> {
						ModelMatcher<Homography2D_F64, AssociatedPair> ransacH =
								FactoryMultiViewRobust.homographyRansac(
										config.score.typeInliers.homography, config.score.typeInliers.ransacH);

						var alg = new ScoreRatioFundamentalHomography(ransac3D, ransacH);
						alg.minimumInliers = config.score.typeInliers.minimumInliers;
						alg.ratio3D = config.score.typeInliers.ratio3D;
						yield alg;
					}
					case FUNDAMENTAL_ERROR -> {
						var alg = new ScoreFundamentalReprojectionError(ransac3D);
						alg.eps = config.score.typeErrors.eps;
						alg.ratio3D = config.score.typeErrors.ratio3D;
						yield alg;
					}
				};

		return new GeneratePairwiseImageGraph(scorer);
	}

	/**
	 * Creates {@link ImageSequenceToSparseScene}
	 *
	 * @param config (Input) Optional configuration. Null will use default values.
	 * @param imageType (Input) Image type it uses internally.
	 * @return New instance
	 */
	public static <T extends ImageGray<T>> ImageSequenceToSparseScene<T>
	sequenceToSparseScene( @Nullable ConfigSequenceToSparseScene config, ImageType<T> imageType ) {
		if (config == null)
			config = new ConfigSequenceToSparseScene();

		PointTracker<T> tracker = FactoryPointTracker.tracker(config.tracker, imageType.getImageClass(), null);

		GeneratePairwiseImageGraph pairwise = generatePairwise(config.pairwise);
		var similar = new PointTrackerToSimilarImages();
		var metric = new MetricFromUncalibratedPairwiseGraph(config.projective);
		var refine = new RefineMetricWorkingGraph(bundleUtils(config.bundleAdjustment));

		var alg = new ImageSequenceToSparseScene<>(tracker, similar, pairwise, metric, refine, imageType);
		alg.maxImagePixels = config.maxImagePixels;

		return alg;
	}

	/**
	 * Creates {@link SparseSceneToDenseCloud} for creating dense clouds of a scene.
	 *
	 * @param config (Input) Optional configuration. Null will use default values.
	 * @param imageType (Input) Image type it uses internally.
	 * @return New instance
	 */
	public static <T extends ImageGray<T>>
	SparseSceneToDenseCloud<T> sparseSceneToDenseCloud( @Nullable ConfigSparseToDenseCloud config,
														ImageType<T> imageType ) {
		if (config == null)
			config = new ConfigSparseToDenseCloud();

		Class<T> grayType = imageType.getImageClass();

		SparseSceneToDenseCloud<T> s2c = new SparseSceneToDenseCloud<>(grayType);
		MultiViewStereoFromKnownSceneStructure<T> mvs = s2c.getMultiViewStereo();

		mvs.setStereoDisparity(FactoryStereoDisparity.generic(
				config.disparity, grayType, GrayF32.class));
		mvs.getComputeFused().setDisparitySmoother(
				FactoryStereoDisparity.removeSpeckle(config.smoother, GrayF32.class));

		GenerateStereoPairGraphFromScene generateGraph = s2c.getGenerateGraph();

		generateGraph.targetDisparity = config.graph.targetDisparity;
		generateGraph.countSmootherParam = config.graph.countSmootherParam;
		generateGraph.minimumCommonFeaturesFrac = config.graph.minimumCommonFeaturesFrac;

		return s2c;
	}
}
