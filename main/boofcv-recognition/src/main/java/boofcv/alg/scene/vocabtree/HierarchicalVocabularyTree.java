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

import boofcv.misc.BoofLambdas;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.kmeans.PackedArray;
import org.ddogleg.clustering.PointDistance;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastArray;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * A hierarchical tree which discretizes an N-Dimensional space. Each region in each node is defined by
 * the set of points which have mean[i] as the closest mean. Once the region is known at a level the child
 * node in the tree can then be looked up and the process repeated until it's at leaf.
 *
 * @author Peter Abeles
 **/
public class HierarchicalVocabularyTree<Point, Data> {

	/** Number of children for each node */
	public int branchFactor = -1;

	/** Maximum number of levels in the tree */
	public int maximumLevel = -1;

	/** Optional data associated with any of the nodes */
	public final FastArray<Data> listData;

	/** Computes distance between two points. Together with the 'mean' points, this defines the sub-regions */
	public final PointDistance<Point> distanceFunction;

	// list of mean descriptors that define the discretized regions
	public final PackedArray<Point> descriptions;
	// Nodes in the hierarchical tree
	// Node[0] is the root node
	public final DogArray<Node> nodes = new DogArray<>(Node::new, Node::reset);

	public HierarchicalVocabularyTree( PointDistance<Point> distanceFunction,
									   PackedArray<Point> descriptions,
									   Class<Data> leafType ) {
		this.distanceFunction = distanceFunction;
		this.descriptions = descriptions;
		listData = new FastArray<>(leafType);
		reset();
	}

	/**
	 * Adds a new node to the graph and returns its index
	 *
	 * @param parentIndex Index of parent node.
	 * @param branch Which branch off the parent does it map to.
	 * @param desc The mean/description for this region
	 * @return Index of the newly added node
	 */
	public int addNode( int parentIndex, int branch, Point desc ) {
		int index = nodes.size;
		Node n = nodes.grow();
		// assign to ID to the index. An alternative would be to use level + branch.
		n.id = index;
		n.branch = branch;
		n.descIdx = descriptions.size();
		descriptions.addCopy(desc);
		Node parent = nodes.get(parentIndex);
		checkTrue(branch == parent.childrenIndexes.size, "Branch index must map to child index");
		n.parent = parentIndex;
		parent.childrenIndexes.add(index);
		return index;
	}

	/**
	 * Traverses the tree to find the leaf node for the provided point. As it traverses the tree each node it
	 * passes through is passed to 'op'. Index of the leaf node is returned. The root node is not passed in
	 * since all points belong to it.
	 *
	 * @param point (Input) Point
	 * @param op Traversed nodes are passed to this function from level 0 to the leaf
	 * @return index of the leaf node
	 */
	public int searchPathToLeaf( Point point, BoofLambdas.ProcessObject<Node> op ) {
		int level = 0;
		Node parent = nodes.get(0);

		if (parent.isLeaf())
			return 0;

		while (level < maximumLevel) {
			int bestNodeIdx = -1;
			double bestDistance = Double.MAX_VALUE;

			// Find the child/node/branch that the 'point' belongs to
			for (int childIdx = 0; childIdx < parent.childrenIndexes.size; childIdx++) {
				int nodeIdx = parent.childrenIndexes.get(childIdx);

				Point desc = descriptions.getTemp(nodes.get(nodeIdx).descIdx);
				double distance = distanceFunction.distance(point, desc);
				if (distance > bestDistance)
					continue;

				bestNodeIdx = nodeIdx;
				bestDistance = distance;
			}

			parent = nodes.get(bestNodeIdx);

			// Pass in the node being explored
			op.process(parent);

			// See if it has reached a leaf and the search is finished
			if (parent.isLeaf()) {
				return bestNodeIdx;
			}

			level++;
		}

		throw new RuntimeException("Invalid tree. Max depth exceeded searching for leaf");
	}

	/**
	 * Traverses every node in the graph (excluding the root) in a depth first manor.
	 *
	 * @param op Every node is feed into this function
	 */
	public void traverseGraphDepthFirst( BoofLambdas.ProcessObject<Node> op ) {
		FastArray<Node> queue = new FastArray<>(Node.class, maximumLevel);
		queue.add(nodes.get(0));
		DogArray_I32 branches = new DogArray_I32(maximumLevel);
		branches.add(0);

		if (nodes.get(0).isLeaf())
			return;

		// NOTE: Root node is intentionally skipped since it will contain all the features and has no information

		while (!queue.isEmpty()) {
			Node n = queue.getTail();
			int branch = branches.getTail();

			// If there are no more children to traverse in this node go back to the parent
			if (branch >= n.childrenIndexes.size) {
				queue.removeTail();
				branches.removeTail();
				continue;
			}

			// next iteration will explore the next branch
			branches.setTail(0, branch + 1);

			// Pass in the child
			n = nodes.get(n.childrenIndexes.get(branch));
			op.process(n);

			// Can't dive into any children/branches if it's a leaf
			if (n.isLeaf())
				continue;

			queue.add(n);
			branches.add(0);
		}
	}

	/**
	 * Ensures it has a valid configuration
	 */
	public void checkConfig() {
		BoofMiscOps.checkTrue(branchFactor > 0, "branchFactor needs to be set");
		BoofMiscOps.checkTrue(maximumLevel > 0, "maximumLevels needs to be set");
	}

	/**
	 * Clears references to initial state but keeps allocated memory
	 */
	public void reset() {
		listData.reset();
		descriptions.reset();
		nodes.reset();
		// create root node, which will contain the set of all points
		Node root = nodes.grow();
		root.id = 0;
	}

	/**
	 * Adds data to the node
	 *
	 * @param node Node that data is added to
	 * @param data The data which is not associated with it
	 */
	public void addData( Node node, Data data ) {
		BoofMiscOps.checkTrue(node.dataIdx < 0);
		node.dataIdx = listData.size;
		listData.add(data);
	}

	/** Node in the Vocabulary tree */
	public static class Node {
		// How useful a match to this node is. Higher the weight, more unique it is typically.
		public double weight;
		// The unique ID assigned to this node
		public int id;
		// Which branch/child in the parent it is
		public int branch;
		// Index of the parent. The root node will have -1 here
		public int parent;
		// Index of data associated with this node
		public int dataIdx;
		// index of the first mean in the list of descriptions. Means in a set are consecutive.
		public int descIdx;
		// index of the first child in the list of nodes. Children are consecutive.
		// If at the last level then this will point to an index in leaves
		public final DogArray_I32 childrenIndexes = new DogArray_I32();

		public boolean isLeaf() {
			return childrenIndexes.isEmpty();
		}

		public void reset() {
			weight = -1;
			id = -1;
			branch = -1;
			parent = -1;
			dataIdx = -1;
			descIdx = -1;
			childrenIndexes.reset();
		}

		public void setTo( Node src ) {
			id = src.id;
			branch = src.branch;
			parent = src.parent;
			dataIdx = src.dataIdx;
			descIdx = src.descIdx;
			childrenIndexes.setTo(src.childrenIndexes);
		}
	}
}
