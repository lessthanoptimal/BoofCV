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

import boofcv.struct.feature.TupleDesc;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastArray;

/**
 * A hierarchical tree which discretizes an N-Dimensional space. Each region in each node is defined by
 * the set of points which have mean[i] as the closest mean. Once the region is known at a level the child
 * node in the tree can then be looked up and the process repeated until it's at leaf.
 *
 * @author Peter Abeles
 **/
public class HierarchicalVocabularyTree<TD extends TupleDesc<TD>, Leaf> {

	/** Number of children for each node */
	public int branchFactor = -1;

	/** Maximum number of levels in the tree */
	public int maximumLevels = -1;

	/** All the leaves in the tree */
	public final FastArray<Leaf> leaves;

	// list of mean descriptors that define the discretized regions
	protected final DogArray<TD> descriptions;
	// Nodes in the hierarchical tree
	protected final DogArray<Node> nodes = new DogArray<>(Node::new, Node::reset);

	// https://www.cse.unr.edu/~bebis/CS491Y/Papers/Nister06.pdf
	// https://sourceforge.net/projects/vocabularytree/
	// https://sourceforge.net/p/vocabularytree/code-0/HEAD/tree/trunk/py_vt/src/
	// http://webdiis.unizar.es/~dorian/index.php?p=31
	// https://github.com/epignatelli/scalable-recognition-with-a-vocabulary-tree

	public HierarchicalVocabularyTree( DogArray<TD> descriptions, Class<Leaf> leafType ) {
		this.descriptions = descriptions;
		leaves = new FastArray<>(leafType);
	}

	public void findLeaf( TD target, Address result ) {

	}

	public void reset() {

	}

	public void setTo(HierarchicalVocabularyTree<TD,Leaf> src) {

	}

	/** Specifies a specific node and child */
	public static class Address {
		public int indexNode;
		public int child;
	}

	protected static class Node {
		// index of the first mean in the list of descriptions. Means in a set are consecutive.
		public int indexMean;
		// index of the first child in the list of nodes. Children are consecutive.
		// If at the last level then this will point to an index in leaves
		public int indexChildren;

		public void reset() {
			indexMean = -1;
			indexChildren = -1;
		}
	}
}
