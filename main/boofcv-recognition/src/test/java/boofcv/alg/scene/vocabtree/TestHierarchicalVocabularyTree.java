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
import boofcv.struct.kmeans.PackedArray;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.clustering.PointDistance;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestHierarchicalVocabularyTree extends BoofStandardJUnit {
	@Test void searchPathToLeaf() {
		HierarchicalVocabularyTree<Point2D_F64, Object> tree = createTree(Object.class);
		tree.branchFactor = 2;
		tree.maximumLevel = 2;

		// check edge case where it's empty
		List<Node> found = new ArrayList<>();
		assertEquals(0, tree.searchPathToLeaf(new Point2D_F64(5, -0.9), found::add));
		assertEquals(0, found.size());

		// Create a graph
		assertEquals(1, tree.addNode(0, 0, new Point2D_F64(-5,  0)));
		assertEquals(2, tree.addNode(0, 1, new Point2D_F64( 5,  0)));
		assertEquals(3, tree.addNode(1, 0, new Point2D_F64(-5, -1)));
		assertEquals(4, tree.addNode(1, 1, new Point2D_F64(-5,  1)));
		assertEquals(5, tree.addNode(2, 0, new Point2D_F64( 5, -1)));
		assertEquals(6, tree.addNode(2, 1, new Point2D_F64( 5,  1)));

		// Compare results to manual
		assertEquals(5, tree.searchPathToLeaf(new Point2D_F64(5, -0.9), found::add));

		assertEquals(2, found.size());
		assertEquals(0.0, tree.descriptions.getTemp(found.get(0).descIdx).distance(5,  0), UtilEjml.TEST_F64);
		assertEquals(0.0, tree.descriptions.getTemp(found.get(1).descIdx).distance(5, -1), UtilEjml.TEST_F64);
	}

	@Test void traverseGraphDepthFirst() {
		HierarchicalVocabularyTree<Point2D_F64, Object> tree = createTree(Object.class);
		tree.branchFactor = 2;
		tree.maximumLevel = 2;

		// check edge case where it's empty
		List<Node> found = new ArrayList<>();
		tree.traverseGraphDepthFirst(found::add);
		assertEquals(0, found.size());

		// Create a graph
		assertEquals(1, tree.addNode(0, 0, new Point2D_F64(-5,  0)));
		assertEquals(2, tree.addNode(0, 1, new Point2D_F64( 5,  0)));
		assertEquals(3, tree.addNode(1, 0, new Point2D_F64(-5, -1)));
		assertEquals(4, tree.addNode(1, 1, new Point2D_F64(-5,  1)));
		assertEquals(5, tree.addNode(2, 0, new Point2D_F64( 5, -1)));
		assertEquals(6, tree.addNode(2, 1, new Point2D_F64( 5,  1)));

		// Compare results to manual
		tree.traverseGraphDepthFirst(found::add);

		assertEquals(6, found.size());
		assertEquals(0.0, tree.descriptions.getTemp(found.get(0).descIdx).distance(-5,  0), UtilEjml.TEST_F64);
		assertEquals(0.0, tree.descriptions.getTemp(found.get(1).descIdx).distance(-5, -1), UtilEjml.TEST_F64);
		assertEquals(0.0, tree.descriptions.getTemp(found.get(2).descIdx).distance(-5,  1), UtilEjml.TEST_F64);
		assertEquals(0.0, tree.descriptions.getTemp(found.get(3).descIdx).distance( 5,  0), UtilEjml.TEST_F64);
		assertEquals(0.0, tree.descriptions.getTemp(found.get(4).descIdx).distance( 5, -1), UtilEjml.TEST_F64);
		assertEquals(0.0, tree.descriptions.getTemp(found.get(5).descIdx).distance( 5,  1), UtilEjml.TEST_F64);
	}

	@Test void addData() {
		// create a simple graph
		HierarchicalVocabularyTree<Point2D_F64, Object> tree = createTree(Object.class);
		tree.branchFactor = 2;
		tree.maximumLevel = 2;

		tree.addNode(0, 0, new Point2D_F64(-5,  0));
		tree.addNode(0, 1, new Point2D_F64( 5,  0));
		tree.addNode(1, 0, new Point2D_F64(-5, -1));

		// Verify there is no data at this node
		assertEquals(-1, tree.nodes.get(2).dataIdx);

		// Assign data to the node
		tree.addData(tree.nodes.get(2), 1);

		// Check data structures
		assertEquals(1, tree.listData.size);
		assertEquals(1, (int)tree.listData.get(0));
		assertEquals(0, tree.nodes.get(2).dataIdx);
	}

	@Test void reset() {
		HierarchicalVocabularyTree<Point2D_F64, Object> tree = createTree(Object.class);
		tree.branchFactor = 2;
		tree.maximumLevel = 2;

		tree.addNode(0, 0, new Point2D_F64(-5,  0));
		tree.addNode(0, 1, new Point2D_F64( 5,  0));
		tree.addNode(1, 0, new Point2D_F64(-5, -1));
		tree.addData(tree.nodes.get(2), 1);

		tree.reset();

		// Verify everything is back to initial state and that settings have not been modified
		assertEquals(1, tree.nodes.size);
		assertEquals(0, tree.descriptions.size());
		assertEquals(0, tree.listData.size());
		assertEquals(2, tree.branchFactor);
		assertEquals(2, tree.maximumLevel);
	}

	public static <T> HierarchicalVocabularyTree<Point2D_F64, T> createTree(Class<T> type) {
		return new HierarchicalVocabularyTree<>(new PointDistance2D(), new Packed2D(), type);
	}

	// @formatter:off
	public static class PointDistance2D implements PointDistance<Point2D_F64> {
		@Override public double distance( Point2D_F64 a, Point2D_F64 b ) {return a.distance2(b);}
		@Override public PointDistance<Point2D_F64> newInstanceThread() {return this;}
	}

	public static class Packed2D implements PackedArray<Point2D_F64> {
		public final List<Point2D_F64> list = new ArrayList<>();
		@Override public void reset() {list.clear();}
		@Override public void reserve( int numElements ) {}
		@Override public void addCopy( Point2D_F64 element ) {list.add(element.copy());}
		@Override public Point2D_F64 getTemp( int index ) {return list.get(index);}
		@Override public void getCopy( int index, Point2D_F64 dst ) {dst.setTo(list.get(index));}
		@Override public void copy( Point2D_F64 src, Point2D_F64 dst ) {dst.setTo(src);}
		@Override public int size() {return list.size();}
	}
	// @formatter:on
}