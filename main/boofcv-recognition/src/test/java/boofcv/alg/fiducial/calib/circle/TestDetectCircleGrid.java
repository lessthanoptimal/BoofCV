/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.circle;

import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.Grid;
import boofcv.struct.image.ImageGray;
import georegression.struct.curve.EllipseRotated_F64;
import org.ddogleg.struct.FastQueue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.fiducial.calib.circle.TestDetectCircleHexagonalGrid.createGrid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDetectCircleGrid {

	static Grid flipHorizontal( Grid g ) {
		Grid out = new Grid();

		for (int i = 0; i < g.rows; i++) {
			for (int j = 0; j < g.columns; j++) {
				out.ellipses.add( g.get(i,g.columns-j-1) );
			}
		}

		out.columns = g.columns;
		out.rows = g.rows;

		return out;
	}

	static  Grid flipVertical(Grid g ) {
		Grid out = new Grid();

		for (int i = 0; i < g.rows; i++) {
			for (int j = 0; j < g.columns; j++) {
				out.ellipses.add( g.get(g.rows-i-1,j) );
			}
		}

		out.columns = g.columns;
		out.rows = g.rows;

		return out;
	}

	@Test
	public void closestCorner4() {
		Grid g = new Grid();

		g.rows = 3;
		g.columns = 3;

		g.ellipses.add(new EllipseRotated_F64(20,20, 0,0,0));
		g.ellipses.add(null);
		g.ellipses.add(new EllipseRotated_F64(20,100, 0,0,0));

		g.ellipses.add(null);
		g.ellipses.add(new EllipseRotated_F64());
		g.ellipses.add(null);

		g.ellipses.add(new EllipseRotated_F64(100,20, 0,0,0));
		g.ellipses.add(null);
		g.ellipses.add(new EllipseRotated_F64(100,100, 0,0,0));

		assertEquals(0, DetectCircleGrid.closestCorner4(g));

		g.ellipses.get(0).center.set(20,100);
		g.ellipses.get(2).center.set(100,20);
		g.ellipses.get(6).center.set(100,100);
		g.ellipses.get(8).center.set(20,20);
		assertEquals(2, DetectCircleGrid.closestCorner4(g));

		g.ellipses.get(0).center.set(100,20);
		g.ellipses.get(2).center.set(100,100);
		g.ellipses.get(6).center.set(20,20);
		g.ellipses.get(8).center.set(20,100);
		assertEquals(1, DetectCircleGrid.closestCorner4(g));

		g.ellipses.get(0).center.set(100,100);
		g.ellipses.get(2).center.set(20,20);
		g.ellipses.get(6).center.set(20,100);
		g.ellipses.get(8).center.set(100,20);
		assertEquals(3, DetectCircleGrid.closestCorner4(g));
	}

	@Test
	public void pruneIncorrectSize() {
		List<List<EllipsesIntoClusters.Node>> clusters = new ArrayList<>();
		clusters.add( createListNodes(4));
		clusters.add( createListNodes(10));
		clusters.add( createListNodes(11));

		DetectCircleGrid.pruneIncorrectSize(clusters, 10);

		assertEquals(1,clusters.size());
		assertEquals(10,clusters.get(0).size());
	}

	private static List<EllipsesIntoClusters.Node> createListNodes( int N ) {
		List<EllipsesIntoClusters.Node> list = new ArrayList<>();

		for (int i = 0; i < N; i++) {
			list.add( new EllipsesIntoClusters.Node() );
		}

		return list;
	}

	@Test
	public void pruneIncorrectShape() {
		FastQueue<Grid> grids = new FastQueue<>(Grid.class,true);
		grids.grow().setShape(4,5);
		grids.grow().setShape(5,4);
		grids.grow().setShape(4,3);
		grids.grow().setShape(5,5);

		DetectCircleGrid.pruneIncorrectShape(grids, 4, 5);

		assertEquals( 2 , grids.size );
	}

	@Test
	public void rotateGridCCW() {
		Grid g = createGrid(3,3);
		List<EllipseRotated_F64> original = new ArrayList<>();
		original.addAll(g.ellipses);

		DetectCircleGrid<?> alg = new HelperAlg(3,3);

		alg.rotateGridCCW(g);
		assertEquals(9,g.ellipses.size());
		assertTrue( original.get(6) == g.get(0,0));
		assertTrue( original.get(0) == g.get(0,2));
		assertTrue( original.get(2) == g.get(2,2));
		assertTrue( original.get(8) == g.get(2,0));

	}

	private static class HelperAlg extends DetectCircleGrid {
		public HelperAlg(int numRows, int numCols) {
			super(numRows, numCols, null, null, null, null);
		}

		@Override
		protected void configureContourDetector(ImageGray gray) {

		}

		@Override
		protected int totalEllipses(int numRows, int numCols) {
			return 0;
		}

		@Override
		protected void putGridIntoCanonical(Grid g) {

		}
	}
}