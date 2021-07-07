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

import boofcv.alg.scene.vocabtree.TestHierarchicalVocabularyTree.Packed2D;
import boofcv.alg.scene.vocabtree.TestHierarchicalVocabularyTree.PointDistance2D;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.clustering.ComputeMeanClusters;
import org.ddogleg.clustering.FactoryClustering;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.LArrayAccessor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static boofcv.alg.scene.vocabtree.TestHierarchicalVocabularyTree.createTree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestLearnHierarchicalTree extends BoofStandardJUnit {
	/**
	 * K-means will give perfect seeds for a single level scenario.
	 */
	@Test void single_level_two_clusters() {
		// Create two well defined clusters
		var points = new Packed2D();
		addCluster(10, -2, 0, points.list);
		addCluster(10, 2, 0, points.list);

		// Very simple graph
		HierarchicalVocabularyTree<Point2D_F64> tree = createTree();
		tree.branchFactor = 2;
		tree.maximumLevel = 1;
		LearnHierarchicalTree<Point2D_F64> alg = createAlg();
		alg.process(points, tree);

		// See if it found the obvious solution
		assertEquals(3, tree.nodes.size);
		findNodeAt(tree, -2, 0, true);
		findNodeAt(tree, 2, 0, true);
	}

	/**
	 * Two levels, with four distinct clusters evenly spaced
	 */
	@Test void two_levels_four_clusters() {
		// Create two well defined clusters
		var points = new Packed2D();
		addCluster(10, -3, 0, points.list);
		addCluster(10, -2, 0, points.list);
		addCluster(10, 2, 0, points.list);
		addCluster(10, 3, 0, points.list);

		// Very simple graph
		HierarchicalVocabularyTree<Point2D_F64> tree = createTree();
		tree.branchFactor = 2;
		tree.maximumLevel = 2;
		LearnHierarchicalTree<Point2D_F64> alg = createAlg();
		alg.process(points, tree);

		// See if it found the obvious solution
		assertEquals(7, tree.nodes.size);
		// level 0 should be the average of the two clusters
		findNodeAt(tree, -2.5, 0, false);
		findNodeAt(tree, 2.5, 0, false);

		// Level 1 should be at the actual location
		findNodeAt(tree, -3, 0, true);
		findNodeAt(tree, -2, 0, true);
		findNodeAt(tree, 2, 0, true);
		findNodeAt(tree, 3, 0, true);
	}

	/**
	 * Crude test. Just see if it blows up and perform basic tests
	 */
	@Test void many_levels_random_data() {
		// Random points
		var points = new Packed2D();
		for (int i = 0; i < 500; i++) {
			points.list.add(new Point2D_F64(rand.nextGaussian()*2, rand.nextGaussian()*2));
		}

		// Process the data
		HierarchicalVocabularyTree<Point2D_F64> tree = createTree();
		tree.branchFactor = 3;
		tree.maximumLevel = 4;
		LearnHierarchicalTree<Point2D_F64> alg = createAlg();
		alg.process(points, tree);

		sanityCheckNodes(tree);
		assertEquals(121, tree.nodes.size);
		assertEquals(120, tree.descriptions.size());

		assertEquals((int)Math.pow(tree.branchFactor, tree.maximumLevel), countLeaves(tree));
	}

	private static int countLeaves( HierarchicalVocabularyTree<?> tree ) {
		int total = 0;
		for (int i = 0; i < tree.nodes.size; i++) {
			HierarchicalVocabularyTree.Node n = tree.nodes.get(i);
			if (n.isLeaf())
				total++;
		}
		return total;
	}

	/**
	 * Makes sure this parameter is obeyed
	 */
	@Test void minimumPointsForChildren() {
		// Random points
		var points = new Packed2D();
		for (int i = 0; i < 100; i++) {
			points.list.add(new Point2D_F64(rand.nextGaussian()*2, rand.nextGaussian()*2));
		}

		// Create a structure that could go much deeper
		HierarchicalVocabularyTree<Point2D_F64> tree = createTree();
		tree.branchFactor = 4;
		tree.maximumLevel = 5;

		LearnHierarchicalTree<Point2D_F64> alg = createAlg();
		// 100/4 = 25. 25/4 = 6.25,  This it should not get past the first level
		alg.minimumPointsForChildren.setFixed(20);
		alg.process(points, tree);

		// level=1 will have children and non after that
		assertTrue(tree.nodes.size <= 1 + 4 + 4*4);
		for (int i = 0; i < tree.nodes.size; i++) {
			assertTrue(2 >= tree.depthOfNode(tree.nodes.get(i)));
		}

		// sanity check
		alg.minimumPointsForChildren.setFixed(0);
		alg.process(points, tree);
		assertTrue(tree.nodes.size > 1 + 4 + 4*4);
	}

	/**
	 * There isn't enough data to fully populate the tree. It should handle this gracefully.
	 */
	@Test void more_leaves_than_data() {
		var points = new Packed2D();
		for (int i = 0; i < 100; i++) {
			points.list.add(new Point2D_F64(rand.nextGaussian()*2, rand.nextGaussian()*2));
		}

		// Process the data
		HierarchicalVocabularyTree<Point2D_F64> tree = createTree();
		tree.branchFactor = 4;
		tree.maximumLevel = 8; // so many levels it has to exhaust all the points
		LearnHierarchicalTree<Point2D_F64> alg = createAlg();
		alg.process(points, tree);

		sanityCheckNodes(tree);

		// Since there are many more leaves potentially than points, these should match
		assertEquals(points.size(), countLeaves(tree));
		// There will be more nodes than points because of the intermediate nodes
		assertTrue(100 < tree.nodes.size);
		// The root node has no description, hence the -1
		assertEquals(tree.descriptions.size(), tree.nodes.size - 1);
	}

	private void sanityCheckNodes( HierarchicalVocabularyTree<Point2D_F64> tree ) {
		for (int i = 0; i < tree.nodes.size; i++) {
			HierarchicalVocabularyTree.Node n = tree.nodes.get(i);
			assertEquals(i, n.index);
			if (i > 0) {
				assertTrue(n.parent != -1);
				assertTrue(n.descIdx != -1);
			}
			assertTrue(n.parent < n.index);

			if (!n.isLeaf()) {
				// if it's not a leaf, there should be more than one child
				assertTrue(n.childrenIndexes.size > 1, "node=" + i + " children.size=" + n.childrenIndexes.size);
			}
		}
	}

	private boolean findNodeAt( HierarchicalVocabularyTree<Point2D_F64> tree,
								double x, double y, boolean leaf ) {
		for (int i = 0; i < tree.descriptions.size(); i++) {
			if (tree.descriptions.getTemp(i).distance(x, y) <= 1e-8) {
				assertEquals(leaf, tree.nodes.get(i + 1).isLeaf());
				return true;
			}
		}
		return false;
	}

	private void addCluster( int count, double x, double y, List<Point2D_F64> list ) {
		for (int i = 0; i < count; i++) {
			list.add(new Point2D_F64(x, y));
		}
	}

	private LearnHierarchicalTree<Point2D_F64> createAlg() {
		return new LearnHierarchicalTree<>(
				Packed2D::new,
				() -> FactoryClustering.kMeans(null, new MeanPoint2D(), new PointDistance2D(), Point2D_F64::new),
				0xDEADBEEF);
	}

	public static class MeanPoint2D implements ComputeMeanClusters<Point2D_F64> {
		@Override public void process( LArrayAccessor<Point2D_F64> points,
									   DogArray_I32 assignments,
									   FastAccess<Point2D_F64> clusters ) {
			DogArray_I32 counts = DogArray_I32.zeros(clusters.size);
			clusters.forEach(p -> p.setTo(0, 0));
			for (int i = 0; i < points.size(); i++) {
				Point2D_F64 p = points.getTemp(i);
				Point2D_F64 mean = clusters.get(assignments.get(i));
				mean.x += p.x;
				mean.y += p.y;
				counts.data[assignments.get(i)]++;
			}

			for (int i = 0; i < clusters.size; i++) {
				int N = counts.get(i);
				Point2D_F64 mean = clusters.get(i);
				mean.x /= N;
				mean.y /= N;
			}
		}

		@Override public ComputeMeanClusters<Point2D_F64> newInstanceThread() {
			return this;
		}
	}
}
