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

import boofcv.alg.scene.vocabtree.ConfigHierarchicalVocabularyTree;
import boofcv.factory.feature.describe.ConfigDescribeRegionPoint;
import boofcv.factory.feature.detdesc.ConfigDetectDescribe;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.struct.Configuration;
import org.ddogleg.clustering.ConfigKMeans;

/**
 * Configuration for {@link ImageRecognitionNister2006}
 *
 * @author Peter Abeles
 */
public class ConfigImageRecognitionNister2006 implements Configuration {

	/** Clustering algorithm used when learning the hierarchical tree */
	public ConfigKMeans kmeans = new ConfigKMeans();

	/** Configuration for the tree when it's being learned */
	public ConfigHierarchicalVocabularyTree tree = new ConfigHierarchicalVocabularyTree();

	/** Image feature detector */
	public ConfigDetectDescribe features = new ConfigDetectDescribe();

	// TODO make entropy weighting configurable

	/** Maximum number of matches. If &le; 0, then all matches are returned */
	public int maxMatches = 4; // TODO use this

	/** Seed used in random number generators */
	public long randSeed = 0xDEADBEEF;

	{
		// Configure to behave like Method-A in the paper
		tree.branchFactor = 10;
		tree.maximumLevel = 6;

		// TODO default to L1-Norm

		// In the paper it didn't show a large improvement with lots of training cycles
		// They maxed out at 50 iterations and saw little improvement past 25.
		// This is configured to avoid re-initialization. Experimentation showed little change.
		kmeans.reseedAfterIterations = 30;
		kmeans.maxIterations = 30;
		kmeans.maxReSeed = 0;

		// Let's use SURF-FAST by default
		features.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.SURF_STABLE;
		features.typeDetector = ConfigDetectInterestPoint.DetectorType.FAST_HESSIAN;
	}

	@Override public void checkValidity() {
		kmeans.checkValidity();
		tree.checkValidity();
		features.checkValidity();
	}

	public void setTo( ConfigImageRecognitionNister2006 src ) {
		this.kmeans.setTo(src.kmeans);
		this.tree.setTo(src.tree);
		this.features.setTo(src.features);
		this.maxMatches = src.maxMatches;
		this.randSeed = src.randSeed;
	}
}
