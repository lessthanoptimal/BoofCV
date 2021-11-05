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

import boofcv.alg.scene.bow.BowDistanceTypes;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006;
import boofcv.alg.scene.vocabtree.ConfigHierarchicalVocabularyTree;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.clustering.ConfigKMeans;

/**
 * Configuration for recognition algorithms based on {@link RecognitionVocabularyTreeNister2006}
 *
 * @author Peter Abeles
 */
public class ConfigRecognitionNister2006 implements Configuration {
	/** Clustering algorithm used when learning the hierarchical tree */
	public final ConfigKMeans kmeans = new ConfigKMeans();

	/** Configuration for the tree when it's being learned */
	public final ConfigHierarchicalVocabularyTree tree = new ConfigHierarchicalVocabularyTree();

	/** Specifies which norm to use. L1 should yield better results but is slower than L2 to compute. */
	public BowDistanceTypes distanceNorm = BowDistanceTypes.L1;

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
	 * When making a query, If a node has an inverted file list greater than this amount then it will be skipped when scoring. This
	 * should be viewed as a last ditch effort when the query is too slow as it will degrade the quality.
	 *
	 * For example, with 1,000,000 images, setting this to be 5000 images reduced query time from 7,000 (ms) to
	 * 85 (ms).
	 */
	public final ConfigLength queryMaximumImagesInNode = ConfigLength.relative(0.002, 5000);

	/**
	 * When learning, if a node is viewed by more than this number of images then its weight is set to zero.
	 * This is useful because it provides a more strategic way to eliminate less informative words from the
	 * image descriptor than by setting {@link #minimumDepthFromRoot}. Disabled by default.
	 */
	public final ConfigLength learningMaximumImagesInNode = ConfigLength.relative(1.0, 1);

	/**
	 * When learning, if a node has less than this number of points it will not spawn children. If
	 * relative then its relative to the total number of points. This is intended to avoid over fitting.
	 */
	public final ConfigLength learningMinimumPointsForChildren = ConfigLength.fixed(0);

	/**
	 * If true then it will learn node weights. If false the all nodes but the root node will have a weight of 1.0
	 */
	public boolean learnNodeWeights = true;

	/**
	 * When converting a descriptor into a word it will return the word which is this many hops from the leaf.
	 * The leaf can be too specific and by "hoping" away from the leaf it gets more generic and will have more
	 * matches.
	 *
	 * If words are being used for frame-to-frame matching then this is a critical parameter. Default value will
	 * force it to the root's children. This won't fail but might not be the most effective choice.
	 */
	@Getter @Setter public int featureSingleWordHops = Integer.MAX_VALUE;

	/** Seed used in random number generators */
	public long randSeed = 0xDEADBEEF;

	{
		// Deviation from paper. This was determined through empirical tuning. See tech report
		tree.branchFactor = 20;
		tree.maximumLevel = 4;
		minimumDepthFromRoot = 2;

		// In the paper it didn't show a large improvement with lots of training cycles
		// They maxed out at 50 iterations and saw little improvement past 25.
		// This is configured to avoid re-initialization. Experimentation showed little change.
		kmeans.reseedAfterIterations = 30;
		kmeans.maxIterations = 30;
		kmeans.maxReSeed = 0;
	}

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(minimumDepthFromRoot >= 0, "Maximum level must be a non-negative integer");
		BoofMiscOps.checkTrue(featureSingleWordHops >= 0, "Can't hop backwards in the tree");

		kmeans.checkValidity();
		tree.checkValidity();
		queryMaximumImagesInNode.checkValidity();
		learningMaximumImagesInNode.checkValidity();
		learningMinimumPointsForChildren.checkValidity();
	}

	public ConfigRecognitionNister2006 setTo( ConfigRecognitionNister2006 src ) {
		this.kmeans.setTo(src.kmeans);
		this.tree.setTo(src.tree);
		this.distanceNorm = src.distanceNorm;
		this.minimumDepthFromRoot = src.minimumDepthFromRoot;
		this.randSeed = src.randSeed;
		this.learnNodeWeights = src.learnNodeWeights;
		this.featureSingleWordHops = src.featureSingleWordHops;
		this.queryMaximumImagesInNode.setTo(src.queryMaximumImagesInNode);
		this.learningMaximumImagesInNode.setTo(src.learningMaximumImagesInNode);
		this.learningMinimumPointsForChildren.setTo(src.learningMinimumPointsForChildren);
		return this;
	}
}
