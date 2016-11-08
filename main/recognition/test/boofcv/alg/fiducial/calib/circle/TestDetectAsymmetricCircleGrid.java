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

package boofcv.alg.fiducial.calib.circle;

import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoAsymmetricGrid.Grid;
import georegression.struct.shapes.EllipseRotated_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestDetectAsymmetricCircleGrid {
	@Test
	public void process_easy() {
		fail("Implement");
	}

	@Test
	public void process_rotated() {
		fail("Implement");
	}

	@Test
	public void process_negative() {
		fail("Implement");
	}

	@Test
	public void pruneIncorrectSize() {
		fail("Implement");
	}

	@Test
	public void pruneIncorrectShape() {
		fail("Implement");
	}

	/**
	 * Vertical flip is needed to put it into the correct order
	 */
	@Test
	public void putGridIntoCanonical_vertical() {
		DetectAsymmetricCircleGrid<?> alg = new DetectAsymmetricCircleGrid<>(5,2,null,null,null);

		Grid g = createGrid(5,2);
		List<EllipseRotated_F64> original = new ArrayList<>();
		original.addAll(g.ellipses);

		alg.putGridIntoCanonical(g);

		assertEquals(5,g.rows);
		assertEquals(2,g.columns);
		assertTrue(original.get(0) == g.get(0,0));

		alg.putGridIntoCanonical(flipVertical(g));

		assertEquals(5,g.rows);
		assertEquals(2,g.columns);
		assertTrue(original.get(0) == g.get(0,0));
	}

	/**
	 * Horizontal flip is needed to put it into the correct order
	 */
	@Test
	public void putGridIntoCanonical_horizontal() {
		DetectAsymmetricCircleGrid<?> alg = new DetectAsymmetricCircleGrid<>(2,5,null,null,null);

		Grid g = createGrid(2,5);
		List<EllipseRotated_F64> original = new ArrayList<>();
		original.addAll(g.ellipses);

		alg.putGridIntoCanonical(g);

		assertEquals(2,g.rows);
		assertEquals(5,g.columns);
		assertTrue(original.get(0) == g.get(0,0));

		alg.putGridIntoCanonical(flipHorizontal(g));

		assertEquals(2,g.rows);
		assertEquals(5,g.columns);
		assertTrue(original.get(0) == g.get(0,0));
	}

	/**
	 * Horizontal flip is needed to put it into the correct order
	 */
	@Test
	public void putGridIntoCanonical_rotate() {
		DetectAsymmetricCircleGrid<?> alg = new DetectAsymmetricCircleGrid<>(3,3,null,null,null);

		Grid g = createGrid(3,3);
		List<EllipseRotated_F64> original = new ArrayList<>();
		original.addAll(g.ellipses);

		alg.putGridIntoCanonical(g);
		assertEquals(3,g.rows);
		assertEquals(3,g.columns);
		assertTrue(original.get(0) == g.get(0,0));

		alg.rotateGridCCW(g);
		alg.putGridIntoCanonical(g);
		assertTrue(original.get(0) == g.get(0,0));

		alg.rotateGridCCW(g);
		alg.rotateGridCCW(g);
		alg.putGridIntoCanonical(g);
		assertTrue(original.get(0) == g.get(0,0));

		alg.rotateGridCCW(g);
		alg.rotateGridCCW(g);
		alg.rotateGridCCW(g);
		alg.putGridIntoCanonical(g);
		assertTrue(original.get(0) == g.get(0,0));
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

		assertEquals(0, DetectAsymmetricCircleGrid.closestCorner4(g));

		g.ellipses.get(0).center.set(20,100);
		g.ellipses.get(2).center.set(100,20);
		g.ellipses.get(6).center.set(100,100);
		g.ellipses.get(8).center.set(20,20);
		assertEquals(2, DetectAsymmetricCircleGrid.closestCorner4(g));

		g.ellipses.get(0).center.set(100,20);
		g.ellipses.get(2).center.set(100,100);
		g.ellipses.get(6).center.set(20,20);
		g.ellipses.get(8).center.set(20,100);
		assertEquals(1, DetectAsymmetricCircleGrid.closestCorner4(g));

		g.ellipses.get(0).center.set(100,100);
		g.ellipses.get(2).center.set(20,20);
		g.ellipses.get(6).center.set(20,100);
		g.ellipses.get(8).center.set(100,20);
		assertEquals(3, DetectAsymmetricCircleGrid.closestCorner4(g));
	}

	private Grid createGrid(int numRows, int numCols) {
		Grid g = new Grid();

		g.rows = numRows;
		g.columns = numCols;

		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numCols; j++) {
				if( i%2 == 0 ) {
					if( j%2 == 0 )
						g.ellipses.add(new EllipseRotated_F64(i*20,j*20, 0,0,0));
					else
						g.ellipses.add(null);
				} else {
					if( j%2 == 0 )
						g.ellipses.add(null);
					else
						g.ellipses.add(new EllipseRotated_F64(i*20,j*20, 0,0,0));
				}
			}
		}

		return g;
	}

	@Test
	public void rotateGridCCW() {
		Grid g = createGrid(3,3);
		List<EllipseRotated_F64> original = new ArrayList<>();
		original.addAll(g.ellipses);

		DetectAsymmetricCircleGrid<?> alg = new DetectAsymmetricCircleGrid<>(3,3,null,null,null);

		alg.rotateGridCCW(g);
		assertEquals(9,g.ellipses.size());
		assertTrue( original.get(6) == g.get(0,0));
		assertTrue( original.get(0) == g.get(0,2));
		assertTrue( original.get(2) == g.get(2,2));
		assertTrue( original.get(8) == g.get(2,0));

	}

	private Grid flipHorizontal( Grid g ) {
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

	private Grid flipVertical( Grid g ) {
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
}