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

package boofcv.alg.scene.vocabtree;

import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree.Node;
import boofcv.misc.BoofLambdas;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.PackedArray;
import org.ddogleg.clustering.kmeans.StandardKMeans;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;

/**
 * The graph is constructed using a depth first search. Each level has its own k-means algorithm. Labeling results
 * are used to segment points for each branch before going to the next level.
 *
 * @author Peter Abeles
 **/
public class LearnHierarchicalTree<Point> implements VerbosePrint {
	/**
	 * If a node has less than this number of points it will not spawn children. This is intended to avoid over
	 * fitting. If relative then it will be relative to the total number of points.
	 */
	public ConfigLength minimumPointsForChildren = ConfigLength.fixed(0);

	// Stores points for a branch at each level in DFS
	protected final DogArray<PackedArray<Point>> listPoints;
	// k-means instance for each level in tree
	protected final DogArray<StandardKMeans<Point>> listKMeans;
	// Storage for weights
	protected final DogArray<DogArray_F64> listWeights = new DogArray<>(DogArray_F64::new);

	//---------- Workspace variables

	// Dynamically computed. The actual threshold for adding children nodes based on the number of points
	protected int pointsRequiredForChildren;

	// Total points in the input list/dataset
	protected int totalPoints;

	// If not null then verbose debug information is printed
	protected @Nullable PrintStream verbose;

	/**
	 * Constructor which specifies factories for internal data structures which are dynamic bsaed on the
	 * maximum number of levels
	 *
	 * @param factoryStorage Factory for point storage
	 * @param factoryKMeans Factory for new K-Means instances
	 * @param randomSeed Seed used in random number generators
	 */
	public LearnHierarchicalTree( BoofLambdas.Factory<PackedArray<Point>> factoryStorage,
								  BoofLambdas.Factory<StandardKMeans<Point>> factoryKMeans,
								  long randomSeed ) {
		this.listPoints = new DogArray<>(factoryStorage::newInstance, PackedArray::reset);

		// Start with an internal array size of zero so that the passed in initializer will take affect
		this.listKMeans = new DogArray<>(0, factoryKMeans::newInstance);
		this.listKMeans.setInitialize(( kmeans ) -> kmeans.initialize(randomSeed));
	}

	/**
	 * Performs clustering
	 *
	 * @param points (Input) points which are to be segmented into the hierarchical tree
	 * @param tree (Output) generated tree
	 */
	public void process( PackedArray<Point> points, HierarchicalVocabularyTree<Point> tree ) {
		// Initialize data structures
		tree.checkConfig();
		tree.reset();
		this.totalPoints = points.size();

		// Abort if it can't do anything
		if (points.size() == 0) {
			if (verbose != null) verbose.println("No points to process!");
			return;
		}

		// first level is provided by points
		listPoints.resize(tree.maximumLevel);
		// each level has it's own k-means instance
		listKMeans.resize(tree.maximumLevel);
		listWeights.resize(tree.maximumLevel);

		// Computes how many points a node needs to create children. It clearly needs at least 1.
		// The user can configure it to require more
		// Minus one below because the check below is <= (inclusive) while the parameter is exclusive
		pointsRequiredForChildren = Math.max(1, minimumPointsForChildren.computeI(points.size())-1);

		if (verbose!=null)
			verbose.println("pointsRequiredForChildren="+pointsRequiredForChildren+" points.size="+points.size());


		// Construct the tree
		processLevel(points, tree, 0, 0);
	}

	/**
	 * Cluster each branch in the tree for the set of points in the node
	 *
	 * @param pointsInParent Points that are members of the parent
	 * @param tree The tree that's being learned
	 * @param level Level in the HierarchicalVocabularyTree
	 * @param parentNodeIdx Array index for the parent node
	 */
	private void processLevel( PackedArray<Point> pointsInParent,
							   HierarchicalVocabularyTree<Point> tree,
							   int level, int parentNodeIdx ) {
		// Stop here if we are at the maximum number of levels or there are too few points
		if (level >= tree.maximumLevel || pointsInParent.size() <= pointsRequiredForChildren)
			return;

		// Get k-means for this level
		StandardKMeans<Point> kmeans = listKMeans.get(level);

		// Cluster the input points
		kmeans.process(pointsInParent, tree.branchFactor);
		DogArray_I32 assignments = kmeans.getAssignments();
		List<Point> clusterMeans = kmeans.getBestClusters().toList();

		// Create the children nodes all at once. As a result the region descriptions will be close in memory
		// and this "might" reduce cache misses in searching
		for (int label = 0; label < clusterMeans.size(); label++) {
			tree.addNode(parentNodeIdx, label, clusterMeans.get(label));
		}

		if (verbose != null) verbose.println("level=" + level + " kmeans.score=" + kmeans.getBestClusterScore());

		// Load the points that are in the child sub region
		Node parent = tree.nodes.get(parentNodeIdx);

		// Create pyramid nodes from the children
		PackedArray<Point> pointsInBranch = listPoints.get(level);
		pointsInBranch.reserve(pointsInParent.size()/(tree.branchFactor - 1));
		processChildren(tree, level, parent, pointsInParent, clusterMeans, assignments, pointsInBranch);
	}

	/**
	 * Goes through each child/branch one at a time splits the points into a subset for each child's region.
	 * Then processes the next level in the pyramid for each branch.
	 */
	private void processChildren( HierarchicalVocabularyTree<Point> tree,
								  int level,
								  Node parent, PackedArray<Point> pointsInParent,
								  List<Point> clusterMeans, DogArray_I32 assignments,
								  PackedArray<Point> pointsInBranch ) {
		// Sanity check to see if the sum of segmented points equals the original list size
		int sumLabeledPoints = 0;

		// Go through all the (just created) children in the parent
		for (int label = 0; label < clusterMeans.size(); label++) {
			// Get the index of the child node
			int nodeIdx = parent.childrenIndexes.get(label);

			// Find all the points in this branch and put them into an array
			pointsInBranch.reset();
			for (int pointIdx = 0; pointIdx < pointsInParent.size(); pointIdx++) {
				if (assignments.get(pointIdx) != label)
					continue;
				pointsInBranch.append(pointsInParent.getTemp(pointIdx));
			}
			sumLabeledPoints += pointsInBranch.size();

			if (verbose != null)
				verbose.println("level=" + level + " branch=" + label + " points.size=" + pointsInBranch.size());

			// Next level in depth first search
			processLevel(pointsInBranch, tree, level + 1, nodeIdx);
		}

		// This better match or else something is wrong with the labels!
		BoofMiscOps.checkEq(sumLabeledPoints, pointsInParent.size());
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> set ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
