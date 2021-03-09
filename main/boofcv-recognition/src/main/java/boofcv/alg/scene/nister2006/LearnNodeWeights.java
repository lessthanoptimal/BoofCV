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

package boofcv.alg.scene.nister2006;

import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree.Node;
import boofcv.errors.BoofCheckFailure;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import lombok.Getter;
import org.ddogleg.struct.DogArray_I32;

import java.util.List;

/**
 * Learns node weights in the {@link HierarchicalVocabularyTree} for use in {@link RecognitionVocabularyTreeNister2006}
 * by counting the number of unique images a specific node/word appears in then computes the weight using an entropy
 * like cost function.
 *
 * Functions must be called in a specific order:
 * <ol>
 *     <li>{@link #reset}</li>
 *     <li>{@link #addImage}</li>
 *     <li>{@link #fixate()}</li>
 * </ol>
 *
 * Each image used to train the tree should have it's descriptors passed in to {@link #addImage}.
 *
 * @author Peter Abeles
 */
public class LearnNodeWeights<Point> {
	/** Tree which has been learned already but with unspecified weights */
	protected @Getter HierarchicalVocabularyTree<Point> tree;

	/**
	 * Sanity check. If true it will make sure every node is observed by an image. This is true when the training
	 * set and the set of images passed into this are the same.
	 */
	public boolean checkEveryNodeSeenOnce = true;

	//---------------- Internal Workspace

	// Look up table from node to counts. Where counts is the number of unique images which see the node/word
	// at least once
	DogArray_I32 numberOfImagesWithNode = new DogArray_I32();
	// Set of unique nodes in the image
	TIntSet nodesInImage = new TIntHashSet();
	// Total number of images used to train the tree/passed in to this class
	int totalImages;

	/**
	 * Initializes and resets with a new tree. Reference to the passed in tree is saved.
	 */
	public void reset( HierarchicalVocabularyTree<Point> tree ) {
		this.tree = tree;
		numberOfImagesWithNode.resize(tree.nodes.size, 0);
		totalImages = 0;
	}

	/**
	 * Adds a new image to the weight computation. It's highly recommended that the same images used to train
	 * the tree be added here. This will ensure that all nodes are filled in with a valid weight.
	 *
	 * @param descriptors Set of all image feature descriptors for a single image
	 */
	public void addImage( List<Point> descriptors ) {
		// Increment image counter
		totalImages++;

		// Reset work data structures
		nodesInImage.clear();

		// Mark nodes that descriptors pass through as being a member of this image
		for (int descIdx = 0; descIdx < descriptors.size(); descIdx++) {
			tree.searchPathToLeaf(descriptors.get(descIdx), ( node ) -> nodesInImage.add(node.index));
		}

		// Number of times each leaf node in the graph is seen at least once in an image
		TIntIterator iterator = nodesInImage.iterator();
		while (iterator.hasNext()) {
			numberOfImagesWithNode.data[iterator.next()]++;
		}
	}

	/**
	 * Call when done. This will compute the weight using an entropy like function: weight[i] = log(N/N[i]) where
	 * N[i] is the number of times a specific node/work appears at least once in the images.
	 */
	public void fixate() {
		// root is always zero since all images are in it
		tree.nodes.get(0).weight = 0;

		for (int i = 1; i < tree.nodes.size; i++) {
			Node n = tree.nodes.get(i);
			int totalImagesFoundInsideOf = numberOfImagesWithNode.get(n.index);

			if (totalImagesFoundInsideOf==0) {
				// this can happen if the set of images used to train the graph is different from the images
				// used to compute the weight. If that case we will set the weight to zero since it's
				// never observed.
				if (checkEveryNodeSeenOnce)
					throw new BoofCheckFailure("Every node should have been seen by at least 1 image if feed the " +
							"same images the tree was trained from.");
				n.weight = 0;
			} else {
				n.weight = Math.log(totalImages/(double)totalImagesFoundInsideOf);
			}
		}
	}
}
