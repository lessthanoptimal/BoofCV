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

package boofcv.abst.scene.nister2006;

import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006.DistanceTypes;
import boofcv.alg.scene.vocabtree.ConfigHierarchicalVocabularyTree;
import boofcv.factory.feature.describe.ConfigConvertTupleDesc;
import boofcv.factory.feature.describe.ConfigDescribeRegionPoint;
import boofcv.factory.feature.detdesc.ConfigDetectDescribe;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;
import org.ddogleg.clustering.ConfigKMeans;

/**
 * Configuration for {@link SceneRecognitionNister2006}
 *
 * @author Peter Abeles
 */
public class ConfigSceneRecognitionNister2006 implements Configuration {

	/**
	 * Images are rescaled so that they have at most this number of pixels. To turn off set to a value &le; 0.
	 * Many of feature related tuning parameters have an implicit assumption about image resolution.
	 * Processing a much higher resolution image could require changing many other parameters for optimal
	 * performance.
	 */
	public int maxImagePixels = 640*480;

	/** Clustering algorithm used when learning the hierarchical tree */
	public final ConfigKMeans kmeans = new ConfigKMeans();

	/** Configuration for the tree when it's being learned */
	public final ConfigHierarchicalVocabularyTree tree = new ConfigHierarchicalVocabularyTree();

	/** Image feature detector */
	public final ConfigDetectDescribe features = new ConfigDetectDescribe();

	/** Specifies which norm to use. L1 should yield better results but is slower than L2 to compute. */
	public DistanceTypes distanceNorm = DistanceTypes.L1;

	/**
	 * Critical tuning parameter for performance. A node can't be a "word" in the descriptor if it's this close
	 * to the root node. Small values will prune less or no images in the database. As a result more images are
	 * considered slowing everything down. However, if this is set too high then valid images are pruned and
	 * recognition goes down.
	 *
	 * If set larger than tree.maximumLevel, then there are no valid nodes.
	 */
	public int minimumDepthFromRoot = 0;

	/**
	 * If a node has an inverted file list greater than this amount then it will be skipped when scoring. This
	 * should be viewed as a last ditch effort when the query is too slow as it will degrade the quality.
	 *
	 * For example, with 1,000,000 images, setting this to be 5000 images reduced query time from 7,000 (ms) to
	 * 85 (ms).
	 */
	public final ConfigLength maximumQueryImagesInNode = ConfigLength.relative(1.0, 1);

	/**
	 * If a node, during training, is viewed by more than this number of images then its weight is set to zero.
	 * This is useful because it provides a more strategic way to eliminate less informative words from the
	 * image descriptor than by setting {@link #minimumDepthFromRoot}. Disabled by default.
	 */
	public final ConfigLength maximumTrainingImagesInNode = ConfigLength.relative(1.0, 1);

	/** Seed used in random number generators */
	public long randSeed = 0xDEADBEEF;

	{
		// Deviation from paper. This was determined through empirical tuning. See tech report
		tree.branchFactor = 23;
		tree.maximumLevel = 4;
		minimumDepthFromRoot = 2;

		// In the paper it didn't show a large improvement with lots of training cycles
		// They maxed out at 50 iterations and saw little improvement past 25.
		// This is configured to avoid re-initialization. Experimentation showed little change.
		kmeans.reseedAfterIterations = 30;
		kmeans.maxIterations = 30;
		kmeans.maxReSeed = 0;

		// Let's use SURF-FAST by default
		features.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.SURF_STABLE;
		features.typeDetector = ConfigDetectInterestPoint.DetectorType.FAST_HESSIAN;
		// Settings a threshold degrades overall results, even if in some specific situations makes it better
		features.detectFastHessian.extract.threshold = 0;
		features.detectFastHessian.extract.radius = 2;
		// 500 features is a good trade off for memory and performance. Accuracy can be improved
		// with more features but becomes prohibitively expensive in larger datasets
		features.detectFastHessian.maxFeaturesAll = 500;
		features.detectFastHessian.maxFeaturesPerScale = 0;

		// Reduce memory usage with very little loss in accuracy
		features.convertDescriptor.outputData = ConfigConvertTupleDesc.DataType.F32;
	}

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(minimumDepthFromRoot >= 0, "Maximum level must be a non-negative integer");

		kmeans.checkValidity();
		tree.checkValidity();
		features.checkValidity();
		maximumQueryImagesInNode.checkValidity();
		maximumTrainingImagesInNode.checkValidity();
	}

	public void setTo( ConfigSceneRecognitionNister2006 src ) {
		this.maxImagePixels = src.maxImagePixels;
		this.kmeans.setTo(src.kmeans);
		this.tree.setTo(src.tree);
		this.features.setTo(src.features);
		this.distanceNorm = src.distanceNorm;
		this.minimumDepthFromRoot = src.minimumDepthFromRoot;
		this.randSeed = src.randSeed;
		this.maximumQueryImagesInNode.setTo(src.maximumQueryImagesInNode);
		this.maximumTrainingImagesInNode.setTo(src.maximumTrainingImagesInNode);
	}
}
