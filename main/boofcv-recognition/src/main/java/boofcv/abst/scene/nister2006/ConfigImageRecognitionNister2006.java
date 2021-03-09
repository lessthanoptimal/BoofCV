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
import boofcv.struct.Configuration;
import org.ddogleg.clustering.ConfigKMeans;

/**
 * Configuration for {@link ImageRecognitionNister2006}
 *
 * @author Peter Abeles
 */
public class ConfigImageRecognitionNister2006 implements Configuration {

	/**
	 * Images are rescaled so that they have at most this number of pixels. To turn off set to a value &le; 0.
	 * Many of feature related tuning parameters have an implicit assumption about image resolution.
	 * Processing a much higher resolution image could require changing many other parameters for optimal
	 * performance.
	 */
	public int maxImagePixels = 640*480;

	/** Clustering algorithm used when learning the hierarchical tree */
	public ConfigKMeans kmeans = new ConfigKMeans();

	/** Configuration for the tree when it's being learned */
	public ConfigHierarchicalVocabularyTree tree = new ConfigHierarchicalVocabularyTree();

	/** Image feature detector */
	public ConfigDetectDescribe features = new ConfigDetectDescribe();

	/** Specifies which norm to use. L1 should yield better results but is slower than L2 to compute. */
	public DistanceTypes distanceNorm = DistanceTypes.L2;

	/**
	 * Critical tuning parameter for performance. Typically, better retrieval is found when this considers all
	 * nodes (i.e. Integer.MAX_VALUE). However, in that case a simple query might end up needing to consider
	 * almost the entire dataset. For the default tree structure the default value is a reasonable compromise.
	 */
	public int maxDistanceFromLeaf = 2;

	// TODO make entropy weighting configurable

	/** Seed used in random number generators */
	public long randSeed = 0xDEADBEEF;

	{
		// Configure to behave like Method-A in the paper
		tree.branchFactor = 10;
		tree.maximumLevel = 6;

		// In the paper it didn't show a large improvement with lots of training cycles
		// They maxed out at 50 iterations and saw little improvement past 25.
		// This is configured to avoid re-initialization. Experimentation showed little change.
		kmeans.reseedAfterIterations = 30;
		kmeans.maxIterations = 30;
		kmeans.maxReSeed = 0;

		// Let's use SURF-FAST by default
		features.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.SURF_STABLE;
		features.typeDetector = ConfigDetectInterestPoint.DetectorType.FAST_HESSIAN;
		// reduces number of features with less sensitivity than hard limits on the number
		features.detectFastHessian.extract.radius = 6;
		// Performance can be improved slightly by setting this to zero. This removed an implicit assumption about
		// the amount of contrast in the image
		features.detectFastHessian.extract.threshold = 0;

		// Reduce memory usage with very little loss in accuracy
		features.convertDescriptor.outputData = ConfigConvertTupleDesc.DataType.F32;
	}

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(maxDistanceFromLeaf >= 0, "Maximum level must be a non-negative integer");

		kmeans.checkValidity();
		tree.checkValidity();
		features.checkValidity();
	}

	public void setTo( ConfigImageRecognitionNister2006 src ) {
		this.maxImagePixels = src.maxImagePixels;
		this.kmeans.setTo(src.kmeans);
		this.tree.setTo(src.tree);
		this.features.setTo(src.features);
		this.distanceNorm = src.distanceNorm;
		this.maxDistanceFromLeaf = src.maxDistanceFromLeaf;
		this.randSeed = src.randSeed;
	}
}
