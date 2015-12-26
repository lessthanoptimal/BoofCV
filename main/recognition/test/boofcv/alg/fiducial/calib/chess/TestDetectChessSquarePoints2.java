/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.chess;

import boofcv.alg.fiducial.calib.squares.*;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.PixelMath;
import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ejml.simple.SimpleMatrix;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestDetectChessSquarePoints2 {

	int offsetX = 15;
	int offsetY = 10;
	int squareLength = 30;

	Random rand = new Random(234);
	int w = 400;
	int h = 500;

	@Before
	public void setup() {
		offsetX = 15;
		offsetY = 10;
		squareLength = 30;
	}

	/**
	 * Give it a simple target and see if it finds the expected number of squares
	 */
	@Test
	public void basicTest() {

		basicTest(3, 3);
		basicTest(5, 5);
		basicTest(5, 7);
		basicTest(7, 5);

		// handle non-symmetric cases here
		basicTest(2, 2);
		basicTest(4, 4);
		basicTest(6, 6);
		basicTest(4, 2);
		basicTest(6, 2);
		basicTest(2, 4);
		basicTest(2, 6);

		basicTest(2, 3);
		basicTest(2, 5);
		basicTest(2, 7);
		basicTest(4, 5);
		basicTest(6, 5);

		basicTest(3, 2);
		basicTest(5, 2);
		basicTest(7, 2);
		basicTest(5, 4);
		basicTest(5, 6);
	}

	public void basicTest(int rows, int cols) {
//		System.out.println("grid shape rows = "+ rows +" cols = "+ cols);

		ImageUInt8 binary = createTarget(rows, cols);

		ImageUInt8 gray = binary.clone();
		PixelMath.multiply(gray, 200, gray);
		PixelMath.minus(255,gray,gray);

//		ShowImages.showWindow(gray,"Input");
//		try {
//			Thread.sleep(2000);
//		} catch (InterruptedException ignore) {}

		BinaryPolygonDetector<ImageUInt8> detectorSquare = FactoryShapeDetector.
				polygon(new ConfigPolygonDetector(4,4),ImageUInt8.class);
		DetectChessSquarePoints2<ImageUInt8> alg =
				new DetectChessSquarePoints2<ImageUInt8>(rows, cols,2, detectorSquare);

//		System.out.println("test grid "+ gridWidth + " " + gridHeight);
		assertTrue(alg.process(gray, binary));

		List<Point2D_F64> calib = alg.getCalibrationPoints().toList();

		double x0 = offsetX+squareLength;
		double y0 = offsetY+squareLength;

		int pointRows = 2*(rows /2)-1+ rows %2;
		int pointCols = 2*(cols /2)-1+ cols %2;

		assertEquals(pointCols*pointRows, calib.size());

		int index = 0;
		for (int row = 0; row < pointRows; row++) {
			for (int col = 0; col < pointCols; col++) {
				assertTrue(calib.get(index++).distance(x0+col*squareLength,y0+row*squareLength) < 3  );
			}
		}
	}

	private ImageUInt8 createTarget(int rows, int cols) {
		int squareLength2 = squareLength-2;
		ImageUInt8 binary = new ImageUInt8(w,h);

		SimpleMatrix a = new SimpleMatrix(1,2);
		a.set(5);

		// create the grid
		for(int y = 0; y < rows; y += 2) {
			for(int x = 0; x < cols; x += 2 ) {
				int pixelX = x*squareLength+offsetX;
				int pixelY = y*squareLength+offsetY;

				ImageMiscOps.fillRectangle(binary, 1, pixelX, pixelY, squareLength, squareLength);
			}
		}
		// don't want the square touching each other
		for(int y = 1; y < rows; y += 2) {
			for(int x = 1; x < cols; x += 2 ) {
				int pixelX = x*squareLength+offsetX+1;
				int pixelY = y*squareLength+offsetY+1;

				ImageMiscOps.fillRectangle(binary, 1, pixelX, pixelY, squareLength2, squareLength2);
			}
		}

		return binary;
	}

	/**
	 * Crash case.  The outer grid touches the image edge but not the inner.
	 */
	@Test
	public void touchImageEdge() {
		offsetX = -10;
		offsetY = -15;

		int gridWidth=4;
		int gridHeight=5;

		ImageUInt8 binary = createTarget(gridHeight, gridWidth);

		ImageUInt8 gray = binary.clone();
		PixelMath.multiply(gray, 200, gray);
		PixelMath.minus(255,gray,gray);

//		ShowImages.showWindow(gray, "Input");
//		try {
//			Thread.sleep(2000);
//		} catch (InterruptedException ignore) {}

		BinaryPolygonDetector<ImageUInt8> detectorSquare = FactoryShapeDetector.
				polygon(new ConfigPolygonDetector(4,4),ImageUInt8.class);
		DetectChessSquarePoints<ImageUInt8> alg =
				new DetectChessSquarePoints<ImageUInt8>(gridWidth,gridHeight,2, detectorSquare);

		assertFalse(alg.process(gray, binary));
	}

	@Test
	public void forceToZero() {

		DetectChessSquarePoints alg = new DetectChessSquarePoints(2,2,10,null);

		for (int rows = 2; rows <= 5; rows++) {
			for (int cols = 2; cols <= 5; cols++) {
				SquareGrid grid = TestSquareGridTools.createGrid(rows,cols);

				SquareNode a = grid.getCornerByIndex(0);
				SquareNode b = grid.getCornerByIndex(1);
				SquareNode c = grid.getCornerByIndex(2);
				SquareNode d = grid.getCornerByIndex(3);

				alg.forceToZero(a, grid);
				assertTrue(a == grid.get(0, 0));
				alg.forceToZero(b, grid);
				assertTrue(b == grid.get(0, 0));
				alg.forceToZero(d, grid);
				assertTrue(d == grid.get(0, 0));
				alg.forceToZero(c, grid);
				assertTrue(c == grid.get(0, 0));
			}
		}
	}

	@Test
	public void createUber() {

		SquareGrid uber = new SquareGrid();

		for (int rows = 2; rows <= 5; rows++) {
			for (int cols = 2; cols <= 5; cols++) {
				SquareGrid outer = TestSquareGridTools.createGrid(rows/2 + rows%2, cols/2 + cols%2);
				SquareGrid inner = TestSquareGridTools.createGrid(rows / 2 , cols / 2 );

				DetectChessSquarePoints.createUber(inner, outer, uber);

				assertEquals(rows, uber.rows);
				assertEquals(cols, uber.columns);
				assertEquals(rows*cols, uber.nodes.size());

				for (int i = 0; i < rows; i++) {
					for (int j = 0; j < cols; j++) {
						if( i%2 == 0 )
							if( j%2 == 0 )
								assertTrue(outer.get(i / 2, j / 2) == uber.get(i, j));
							else
								assertTrue(null == uber.get(i, j));
						else {
							if( j%2 == 0 )
								assertTrue(null == uber.get(i, j));
							else
								assertTrue(inner.get(i / 2, j / 2)  == uber.get(i, j));
						}
					}
				}
			}
		}

	}


	@Test
	public void putIntoCanonical() {
		SquareGridTools tools = new SquareGridTools();

		DetectChessSquarePoints alg = new DetectChessSquarePoints(2,2,10,null);
		for (int rows = 2; rows <= 5; rows++) {
			for (int cols = 2; cols <= 5; cols++) {
				SquareGrid uber = createUber(rows, cols);

				alg.putIntoCanonical(uber);
				checkCanonical(uber);

				// make it do some work
				boolean oddRow = rows%2 == 1;
				boolean oddCol = cols%2 == 1;

				if( oddRow == oddCol ) {
					if( oddRow && rows==cols ) {
						tools.rotateCCW(uber);
					} else{
						tools.reverse(uber);
					}
				}

				alg.putIntoCanonical(uber);
				checkCanonical(uber);
			}
		}
	}

	private void checkCanonical( SquareGrid uber ) {
		double best = uber.nodes.get(0).center.norm();

		for( SquareNode n : uber.nodes ) {
			if( n == null ) continue;
			double d = n.center.norm();
			if( d < best )
				fail("0 should be best");
		}
	}

	@Test
	public void orderUberCorners() {
		for (int rows = 2; rows <= 5; rows++) {
			for (int cols = 2; cols <= 5; cols++) {
				SquareGrid uber = createUber(rows, cols);

				// randomize it some
				for( SquareNode n : uber.nodes ) {
					if( n == null ) continue;

					int N = rand.nextInt(4);
					for (int i = 0; i < N; i++) {
						UtilPolygons2D_F64.shiftDown(n.corners);
					}
				}

				// let's fix it
				DetectChessSquarePoints.orderUberCorners(uber);

				checkCornerOrder(uber);
			}
		}
	}

	private void checkCornerOrder( SquareGrid grid ) {
		double w = TestRegularClustersIntoGrids.DEFAULT_WIDTH;

		for (int row = 0; row < grid.rows; row++) {
			for (int col = 0; col < grid.columns; col++) {
				SquareNode n = grid.get(row,col);

				if( n == null ) continue;

				double x = col*w;
				double y = row*w;

				assertTrue(n.corners.get(0).distance(x-w/2,y-w/2)<1e-8);
				assertTrue(n.corners.get(1).distance(x+w/2,y-w/2)<1e-8);
				assertTrue(n.corners.get(2).distance(x+w/2,y+w/2)<1e-8);
				assertTrue(n.corners.get(3).distance(x-w/2,y+w/2)<1e-8);

			}
		}
	}

	@Test
	public void orderCorner() {
		double w = TestRegularClustersIntoGrids.DEFAULT_WIDTH;

		SquareGrid uber = createUber(5, 5);

		SquareNode n = uber.get(1,1);

		for (int diag = 0; diag < 4; diag++) {
			UtilPolygons2D_F64.shiftUp(n.corners);

			SquareNode target = DetectChessSquarePoints.getDiag(uber, 1, 1, diag);
			DetectChessSquarePoints.orderCorner(n, target.center, diag);

			assertEquals(n.corners.get(0).distance(w - w/2, w - w/2), 0, 1e-8);
			assertEquals(n.corners.get(1).distance(w + w/2, w - w/2) ,0, 1e-8);
			assertEquals(n.corners.get(2).distance(w + w/2, w + w/2) ,0, 1e-8);
			assertEquals(n.corners.get(3).distance(w - w/2, w + w/2) ,0, 1e-8);
		}
	}

	@Test
	public void getDiag() {
		SquareGrid uber = createUber(5, 5);

		assertTrue(null == DetectChessSquarePoints.getDiag(uber, 0, 0, 0));
		assertTrue(null == DetectChessSquarePoints.getDiag(uber, 0, 0, 1));
		assertTrue(uber.get(1,1) == DetectChessSquarePoints.getDiag(uber, 0, 0, 2));
		assertTrue(null == DetectChessSquarePoints.getDiag(uber, 0, 0, 3));

		assertTrue(uber.get(3,3) == DetectChessSquarePoints.getDiag(uber, 4, 4, 0));
		assertTrue(null == DetectChessSquarePoints.getDiag(uber, 4, 4, 1));
		assertTrue(null == DetectChessSquarePoints.getDiag(uber, 4, 4, 2));
		assertTrue(null == DetectChessSquarePoints.getDiag(uber, 4, 4, 3));

		assertTrue(uber.get(0,0) == DetectChessSquarePoints.getDiag(uber, 1, 1, 0));
		assertTrue(uber.get(0,2) == DetectChessSquarePoints.getDiag(uber, 1, 1, 1));
		assertTrue(uber.get(2,2) == DetectChessSquarePoints.getDiag(uber, 1, 1, 2));
		assertTrue(uber.get(2,0) == DetectChessSquarePoints.getDiag(uber, 1, 1, 3));
	}

	@Test
	public void computeCalibrationPoints() {

		DetectChessSquarePoints<ImageUInt8> alg = new DetectChessSquarePoints<ImageUInt8>(2,2,0.01,null);

		double w = TestRegularClustersIntoGrids.DEFAULT_WIDTH;

		for (int rows = 2; rows <= 5; rows++) {
			for (int cols = 2; cols <= 5; cols++) {
				SquareGrid uber = createUber(rows, cols);

				assertTrue(alg.computeCalibrationPoints(uber));

				assertEquals((rows - 1) * (cols - 1), alg.calibrationPoints.size());

				double x0 =  w/2;
				double y0 =  w/2;

				int index = 0;
				for (int i = 0; i < rows - 1; i++) {
					for (int j = 0; j < cols - 1; j++) {
						double x = x0 + j*w;
						double y = y0 + i*w;

						Point2D_F64 p = alg.calibrationPoints.get(index++);

						assertTrue(p.distance(x, y) < 1e-8);
					}
				}
			}
		}
	}

	public static SquareGrid createUber( int rows , int cols ) {
		SquareGrid uber = new SquareGrid();
		uber.columns = cols;
		uber.rows = rows;

		double w = TestRegularClustersIntoGrids.DEFAULT_WIDTH;
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				if( row%2 == 0 ) {
					if( col%2 == 0 ) {
						uber.nodes.add( createSquare(col*w,row*w,w));
					} else {
						uber.nodes.add(null);
					}
				} else {
					if( col%2 == 0 ) {
						uber.nodes.add(null);
					} else {
						uber.nodes.add( createSquare(col*w,row*w,w));
					}
				}
			}
		}

		return uber;
	}

	public static SquareNode createSquare( double x , double y , double width ) {

		double r = width/2;
		Polygon2D_F64 poly = new Polygon2D_F64(4);
		poly.get(0).set(-r, -r);
		poly.get(1).set( r, -r);
		poly.get(2).set( r,  r);
		poly.get(3).set(-r,  r);

		SquareNode square = new SquareNode();
		for (int i = 0; i < 4; i++) {
			poly.get(i).x += x;
			poly.get(i).y += y;
		}

		square.corners = poly;
		square.center.set(x, y);
		square.largestSide = width;

		return square;
	}
 }
