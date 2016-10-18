/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.squares;

import georegression.struct.shapes.Polygon2D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestSquareRegularClustersIntoGrids {

	public static double DEFAULT_WIDTH = 1.2;
	Random rand = new Random(23423);

	@Test
	public void highLevelCheck() {
		List<List<SquareNode>> clusters = new ArrayList<>();

		for (int length = 1; length <= 4; length++) {
			clusters.add(createGrid(length, length));
		}

		SquareRegularClustersIntoGrids alg = new SquareRegularClustersIntoGrids(1);

		alg.process(clusters);
		assertEquals(4,alg.getGrids().size());

		// see if running it twice messes things up
		alg.process(clusters);
		assertEquals(4,alg.getGrids().size());
	}

	@Test
	public void checkNumberOfConnections_positive() {
		SquareRegularClustersIntoGrids alg = new SquareRegularClustersIntoGrids(1);

		for (int numRows = 1; numRows <= 4; numRows++) {
			for (int numCols = 1; numCols <= 4; numCols++) {
				List<SquareNode> nodes = createGrid(numRows, numCols);
				Collections.shuffle(nodes, rand);

				if( numRows == 1 || numCols == 1 )
					assertEquals(1,alg.checkNumberOfConnections(nodes));
				else
					assertEquals(2,alg.checkNumberOfConnections(nodes));
			}
		}
	}

	@Test
	public void checkNumberOfConnections_negative() {
		List<SquareNode> nodes = createGrid(1, 2);
		nodes.addAll( createGrid(2,3));

		SquareRegularClustersIntoGrids alg = new SquareRegularClustersIntoGrids(1);
		assertEquals(0, alg.checkNumberOfConnections(nodes));
	}

	@Test
	public void orderInfoLine() {
		for( int length = 1; length < 4; length++ ) {
			for (int i = 0; i < 2; i++) {
				int numRows,numCols;

				if (i == 0) {
					numRows = 1;
					numCols = length;
				} else {
					numRows = length;
					numCols = 1;
				}

				List<SquareNode> nodes = createGrid(numRows, numCols);

				Collections.shuffle(nodes, rand);

				SquareRegularClustersIntoGrids alg = new SquareRegularClustersIntoGrids(1);
				alg.orderIntoLine(nodes);
				SquareGrid found = alg.valid.getTail();

				assertEquals(length, found.nodes.size());
				if (found.columns == numCols) {
					assertEquals(numRows, found.rows);
				} else {
					assertEquals(numRows, found.columns);
					assertEquals(numCols, found.rows);
				}
			}
		}
	}

	@Test
	public void orderIntoGrid() {

		for (int numRows = 2; numRows <= 4; numRows++) {
			for (int numCols = 2; numCols <= 4; numCols++) {
				List<SquareNode> nodes = createGrid(numRows, numCols);

				Collections.shuffle(nodes,rand);

				SquareRegularClustersIntoGrids alg = new SquareRegularClustersIntoGrids(1);
				alg.orderIntoGrid(nodes);
				SquareGrid found = alg.valid.getTail();

				assertEquals(numRows*numCols,found.nodes.size());
				if( found.columns == numCols ) {
					assertEquals(numRows,found.rows);
				} else {
					assertEquals(numRows,found.columns);
					assertEquals(numCols,found.rows);
				}
			}
		}
	}

	@Test
	public void addRows() {
		int numRows = 3;
		int numCols = 4;
		List<SquareNode> nodes = createGrid(numRows, numCols);

		List<SquareNode> column = new ArrayList<>();
		for (int i = 0; i < numRows; i++) {
			column.add( nodes.get(i*numCols));
		}

		List<SquareNode> found = new ArrayList<>();
		SquareRegularClustersIntoGrids alg = new SquareRegularClustersIntoGrids(1);

		assertFalse(alg.addRowsToGrid(column, found));

		assertEquals(nodes.size(), found.size());
		for (int i = 0; i < found.size(); i++) {
			assertTrue(nodes.get(i) == found.get(i));
		}
	}

	@Test
	public void addLineToGrid() {
		int numRows = 3;
		int numCols = 4;
		List<SquareNode> nodes = createGrid(numRows, numCols);

		SquareRegularClustersIntoGrids alg = new SquareRegularClustersIntoGrids(1);

		List<SquareNode> row = new ArrayList<>();
		List<SquareNode> col = new ArrayList<>();

		checkAddLineToGrid(alg, nodes.get(0), nodes.get(1), row);
		checkAddLineToGrid(alg, nodes.get(0), nodes.get(numCols), col);

		assertEquals(numCols - 2, row.size());
		assertEquals(numRows - 2, col.size());
		assertTrue(row.get(1) == nodes.get(3));
		assertTrue(col.get(0) == nodes.get(2 * numCols));

		// try it the other direction
		nodes = createGrid(numRows, numCols);
		checkAddLineToGrid(alg,nodes.get(numCols - 1), nodes.get(numCols - 2), row);
		nodes = createGrid(numRows, numCols);
		checkAddLineToGrid(alg, nodes.get(2 * numCols), nodes.get(1 * numCols), col);
		assertEquals(numCols - 2, row.size());
		assertEquals(numRows - 2, col.size());

	}

	void checkAddLineToGrid(SquareRegularClustersIntoGrids alg, SquareNode a, SquareNode b, List<SquareNode> list) {
		list.clear();
		a.graph = SquareRegularClustersIntoGrids.SEARCHED;
		b.graph = SquareRegularClustersIntoGrids.SEARCHED;
		alg.addLineToGrid(a,b,list);
	}

	@Test
	public void pickNot_1() {
		SquareNode a = new SquareNode();
		SquareNode b = new SquareNode();
		SquareNode c = new SquareNode();

		connect(a, 0, b, 0);
		connect(a, 1, c, 0);

		assertTrue(c == SquareRegularClustersIntoGrids.pickNot(a, b));
		assertTrue(b == SquareRegularClustersIntoGrids.pickNot(a, c));
	}

	@Test
	public void pickNot_2() {
		SquareNode a = new SquareNode();
		SquareNode b = new SquareNode();
		SquareNode c = new SquareNode();
		SquareNode d = new SquareNode();

		connect(a,0,b,0);
		connect(a,1,c,0);
		connect(a,2,d,0);

		assertTrue(d == SquareRegularClustersIntoGrids.pickNot(a, b, c));
		assertTrue(b == SquareRegularClustersIntoGrids.pickNot(a, c, d));
		assertTrue(c == SquareRegularClustersIntoGrids.pickNot(a, d, b));
	}

	public static List<SquareNode> createGrid(int numRows, int numCols) {
		List<SquareNode> nodes = new ArrayList<>();
		double w = DEFAULT_WIDTH;
		for (int y = 0; y < numRows; y++) {
			for (int x = 0; x < numCols; x++) {
				nodes.add( createSquare(x*w*2,y*w*2,w) );
			}
		}
		int index = 0;
		for (int y = 0; y < numRows; y++) {
			for (int x = 0; x < numCols; x++, index++ ) {
				SquareNode a = nodes.get(index);
				if( x < numCols-1) {
					SquareNode b = nodes.get(index + 1);
					connect(a,0,b,2);
				}
				if( y < numRows-1) {
					SquareNode c = nodes.get(index + numCols);
					connect(a, 1, c, 3);
				}
			}
		}
		return nodes;
	}


	public static void connect( SquareNode a , int sideA , SquareNode b , int sideB ) {
		SquareEdge e = new SquareEdge();
		e.a = a;
		e.sideA = sideA;
		e.b = b;
		e.sideB = sideB;
		a.edges[sideA] = e;
		b.edges[sideB] = e;
	}

	public static SquareNode createSquare( double x , double y , double width ) {

		double r = width/2;
		Polygon2D_F64 poly = new Polygon2D_F64(4);
		poly.get(0).set(-r, r);
		poly.get(1).set( r, r);
		poly.get(2).set( r,-r);
		poly.get(3).set(-r,-r);

		SquareNode square = new SquareNode();
		for (int i = 0; i < 4; i++) {
			poly.get(i).x += x;
			poly.get(i).y += y;
			square.sideLengths[i] = width;
		}

		square.corners = poly;
		square.center.set(x,y);
		square.largestSide = width;

		return square;
	}

}
