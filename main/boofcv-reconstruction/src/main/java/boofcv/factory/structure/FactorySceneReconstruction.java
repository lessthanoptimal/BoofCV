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

package boofcv.factory.structure;

import boofcv.abst.feature.associate.AssociateDescriptionHashSets;
import boofcv.abst.feature.describe.DescribePoint;
import boofcv.abst.feature.describe.DescribePointRadiusAngle;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.geo.bundle.MetricBundleAdjustmentUtils;
import boofcv.abst.scene.FeatureSceneRecognition;
import boofcv.alg.mvs.MultiViewStereoFromKnownSceneStructure;
import boofcv.alg.similar.*;
import boofcv.alg.structure.EpipolarScore3D;
import boofcv.alg.structure.GeneratePairwiseImageGraph;
import boofcv.alg.structure.GenerateStereoPairGraphFromScene;
import boofcv.alg.structure.SparseSceneToDenseCloud;
import boofcv.alg.structure.score3d.ScoreFundamentalHomographyCompatibility;
import boofcv.alg.structure.score3d.ScoreFundamentalVsRotation;
import boofcv.alg.structure.score3d.ScoreRatioFundamentalHomography;
import boofcv.alg.video.SelectFramesForReconstruction3D;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribePoint;
import boofcv.factory.feature.describe.FactoryDescribePointRadiusAngle;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.factory.scene.FactorySceneRecognition;
import boofcv.factory.sfm.ConfigBundleUtils;
import boofcv.factory.struct.FactoryTupleDesc;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
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

		EpipolarScore3D scorer = epipolarScore3D(config.score);

		return new GeneratePairwiseImageGraph(scorer);
	}

	/**
	 * Creates a new instance of {@link EpipolarScore3D} based on the passed in configuration
	 */
	public static EpipolarScore3D epipolarScore3D( ConfigEpipolarScore3D config) {
		ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D =
				FactoryMultiViewRobust.fundamentalRansac(config.fundamental, config.ransacF);

		return switch (config.type) {
			case MODEL_INLIERS -> {
				ModelMatcher<Homography2D_F64, AssociatedPair> ransacH =
						FactoryMultiViewRobust.homographyRansac(
								config.typeInliers.homography, config.typeInliers.ransacH);

				var alg = new ScoreRatioFundamentalHomography(ransac3D, ransacH);
				alg.minimumInliers.setTo(config.typeInliers.minimumInliers);
				alg.ratio3D = config.typeInliers.ratio3D;
				yield alg;
			}
			case FUNDAMENTAL_COMPATIBLE -> {
				var alg = new ScoreFundamentalHomographyCompatibility(ransac3D);
				alg.inlierErrorTol = config.typeCompatible.inlierErrorTol;
				alg.ratio3D = config.typeCompatible.ratio3D;
				alg.maxRatioScore = config.typeCompatible.maxRatioScore;
				alg.minimumInliers.setTo(config.typeCompatible.minimumInliers);
				yield alg;
			}
			case FUNDAMENTAL_ROTATION -> {
				var alg = new ScoreFundamentalVsRotation(ransac3D);
				alg.inlierErrorTol = config.typeRotation.inlierErrorTol;
				alg.ratio3D = config.typeRotation.ratio3D;
				alg.maxRatioScore = config.typeRotation.maxRatioScore;
				alg.minimumInliers.setTo(config.typeRotation.minimumInliers);
				yield alg;
			}
		};
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

		mvs.minimumQuality3D = config.mvs.minimumQuality3D;
		mvs.maximumCenterOverlap = config.mvs.maximumCenterOverlap;
		mvs.maxCombinePairs = config.mvs.maxCombinePairs;

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

	/**
	 * Creates {@link SimilarImagesSceneRecognition}.
	 */
	public static <Image extends ImageBase<Image>, TD extends TupleDesc<TD>>
	SimilarImagesSceneRecognition<Image, TD> createSimilarImages( @Nullable ConfigSimilarImagesSceneRecognition config,
																  ImageType<Image> imageType ) {
		if (config == null)
			config = new ConfigSimilarImagesSceneRecognition();

		DetectDescribePoint<Image, TD> detector =
				FactoryDetectDescribe.generic(config.features, imageType.getImageClass());

		FeatureSceneRecognition<TD> recognitizer =
				FactorySceneRecognition.createSceneNister2006(config.recognizeNister2006, detector::createDescription);

		AssociateDescriptionHashSets<TD> associator = new AssociateDescriptionHashSets<>(
				FactoryAssociation.generic(config.associate, detector));

		var similar = new SimilarImagesSceneRecognition<>(detector, associator, recognitizer,
				() -> FactoryTupleDesc.createPacked(detector));

		similar.setSimilarityTest(new ImageSimilarityAssociatedRatio(config.minimumSimilar));
		similar.setLimitMatchesConsider(config.limitMatchesConsider);

		return similar;
	}

	/**
	 * Creates {@link SimilarImagesSceneRecognition}.
	 */
	public static <Image extends ImageGray<Image>, TD extends TupleDesc<TD>>
	SimilarImagesTrackThenMatch<Image, TD> createTrackThenMatch( @Nullable ConfigSimilarImagesTrackThenMatch config,
																 ImageType<Image> imageType ) {
		if (config == null)
			config = new ConfigSimilarImagesTrackThenMatch();

		DescribePoint<Image, TD> detector = FactoryDescribePoint.generic(config.descriptions, imageType);

		FeatureSceneRecognition<TD> recognitizer =
				FactorySceneRecognition.createSceneNister2006(config.recognizeNister2006, detector::createDescription);

		AssociateDescriptionHashSets<TD> associator = new AssociateDescriptionHashSets<>(
				FactoryAssociation.generic(config.associate, detector));

		var similar = new SimilarImagesTrackThenMatch<>(detector, associator, recognitizer,
				() -> FactoryTupleDesc.createPacked(detector));

		similar.setSimilarityTest(new ImageSimilarityAssociatedRatio(config.minimumSimilar));
		similar.setLimitQuery(config.limitQuery);
		similar.setMinimumRecognizeDistance(config.minimumRecognizeDistance);
		similar.searchRadius = config.sequentialSearchRadius;
		similar.minimumCommonTracks.setTo(config.sequentialMinimumCommonTracks);

		return similar;
	}

	/**
	 * Creates {@link SelectFramesForReconstruction3D} which is used for down sampling frames in image sequences
	 * to select for ones which have significant 3D information
	 *
	 * @param config (Optional) Configuration
	 * @param imageType Type of input image
	 * @return A new instance.
	 */
	public static <T extends ImageGray<T>> SelectFramesForReconstruction3D<T>
	frameSelector3D( @Nullable ConfigSelectFrames3D config, ImageType<T> imageType ) {
		if (config == null)
			config = new ConfigSelectFrames3D();

		// Image tracker currently only supports gray images
		Class<T> grayType = imageType.getImageClass();

		DescribePointRadiusAngle<T, TupleDesc_F64> describe = FactoryDescribePointRadiusAngle.generic(config.describe, imageType);

		var alg = new SelectFramesForReconstruction3D<T>(describe);
		alg.config.setTo(config);
		alg.setTracker(FactoryPointTracker.tracker(config.tracker, grayType, null));
		alg.setAssociate(FactoryAssociation.generic2(config.associate,describe));
		alg.setScorer(FactorySceneReconstruction.epipolarScore3D(config.scorer3D));

		return alg;
	}
}
