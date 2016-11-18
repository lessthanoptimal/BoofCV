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

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoAsymmetricGrid.Grid;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import org.ddogleg.struct.FastQueue;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDetectAsymmetricCircleGrid {
	@Test
	public void process_easy() {

		Affine2D_F64 affine = new Affine2D_F64(1,0,0,1,100,100);

		performDetectionCheck(5, 6, 5, 6, affine);
		performDetectionCheck(5, 4, 5, 4, affine);
		performDetectionCheck(3, 3, 3, 3, affine);
		performDetectionCheck(3, 4, 3, 4, affine);
		performDetectionCheck(4, 3, 4, 3, affine);
	}

	@Test
	public void process_rotated() {
		double c = Math.cos(0.4);
		double s = Math.sin(0.4);
		Affine2D_F64 affine = new Affine2D_F64(c,-s,s,c,100,100);

		performDetectionCheck(5, 6, 5, 6, affine);
	}

	@Test
	public void process_negative() {
		Affine2D_F64 affine = new Affine2D_F64(1,0,0,1,100,100);

		performDetectionCheck(4, 6, 5, 6, affine);
	}

	private void performDetectionCheck(int expectedRows, int expectedCols, int actualRows, int actualCols, Affine2D_F64 affine) {
		int radius = 20;
		int centerDistances = 80;

		DetectAsymmetricCircleGrid<GrayU8> alg = createAlg(expectedRows,expectedCols,radius,centerDistances);
//		alg.setVerbose(true);

		List<Point2D_F64> locations = new ArrayList<>();
		GrayU8 image = new GrayU8(400,450);
		render(actualRows,actualCols, radius, centerDistances, affine, locations,image);

		alg.process(image);

		List<Grid> found = alg.getGrids();

		if( expectedRows != actualRows || expectedCols != actualCols ) {
			assertEquals(0, found.size());
			return;
		} else {
			assertEquals(1, found.size());
		}
		Grid g = found.get(0);
		assertEquals(actualRows , g.rows );
		assertEquals(actualCols , g.columns );
		checkCounterClockWise(g);

		int index = 0;
		for (int row = 0; row < g.rows; row++) {
			for (int col = 0; col < g.columns; col++) {
				boolean check = false;
				if( row%2 == 1 && col%2 == 1)
					check = true;
				else if( row%2 == 0 && col%2 == 0)
					check = true;

				if( check ) {
					EllipseRotated_F64 f = g.get(row,col);
					Point2D_F64 e = locations.get(index++);

					assertEquals( e.x , f.center.x , 1.5 );
					assertEquals( e.y , f.center.y , 1.5 );
					assertEquals( 20 , f.a , 1.0 );
					assertEquals( 20 , f.b , 1.0 );
				}
			}
		}
	}

	private DetectAsymmetricCircleGrid<GrayU8> createAlg( int numRows , int numCols ,
														  double radius , double centerDistance ) {

		double spaceRatio = 1.2*centerDistance/radius;

		InputToBinary<GrayU8> threshold = FactoryThresholdBinary.globalFixed(100,true,GrayU8.class);
		BinaryEllipseDetector<GrayU8> detector = FactoryShapeDetector.ellipse(null, GrayU8.class);
		EllipsesIntoClusters cluster = new EllipsesIntoClusters(spaceRatio,0.8);
		return new DetectAsymmetricCircleGrid<>( numRows, numCols,threshold, detector,  cluster);
	}

	@Test
	public void pruneIncorrectSize() {
		List<List<EllipsesIntoClusters.Node>> clusters = new ArrayList<>();
		clusters.add( createListNodes(4));
		clusters.add( createListNodes(10));
		clusters.add( createListNodes(11));


		DetectAsymmetricCircleGrid.pruneIncorrectSize(clusters, 10);

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

		DetectAsymmetricCircleGrid.pruneIncorrectShape(grids, 4, 5);

		assertEquals( 2 , grids.size );
	}

	/**
	 * Vertical flip is needed to put it into the correct order
	 */
	@Test
	public void putGridIntoCanonical_vertical() {
		putGridIntoCanonical_vertical(5,2);
		putGridIntoCanonical_vertical(5,6);
	}
	private void putGridIntoCanonical_vertical( int numRows , int numCols ) {
		DetectAsymmetricCircleGrid<?> alg = new DetectAsymmetricCircleGrid<>(numRows,numCols,null,null,null);

		Grid g = createGrid(numRows,numCols);
		List<EllipseRotated_F64> original = new ArrayList<>();
		original.addAll(g.ellipses);

		alg.putGridIntoCanonical(g);
		assertEquals(numRows,g.rows);
		assertEquals(numCols,g.columns);
		assertTrue(original.get(0) == g.get(0,0));
		checkCounterClockWise(g);

		alg.putGridIntoCanonical(flipVertical(g));
		assertEquals(numRows,g.rows);
		assertEquals(numCols,g.columns);
		assertTrue(original.get(0) == g.get(0,0));
		checkCounterClockWise(g);
	}

	/**
	 * Horizontal flip is needed to put it into the correct order
	 */
	@Test
	public void putGridIntoCanonical_horizontal() {
		putGridIntoCanonical_horizontal(2,5);
		putGridIntoCanonical_horizontal(6,5);

	}
	private void putGridIntoCanonical_horizontal( int numRows , int numCols) {
		DetectAsymmetricCircleGrid<?> alg = new DetectAsymmetricCircleGrid<>(numRows,numCols,null,null,null);

		Grid g = createGrid(numRows,numCols);
		List<EllipseRotated_F64> original = new ArrayList<>();
		original.addAll(g.ellipses);

		alg.putGridIntoCanonical(g);
		assertEquals(numRows,g.rows);
		assertEquals(numCols,g.columns);
		assertTrue(original.get(0) == g.get(0,0));
		checkCounterClockWise(g);

		g = flipHorizontal(g);
		alg.putGridIntoCanonical(g);
		assertEquals(numRows,g.rows);
		assertEquals(numCols,g.columns);
		assertTrue(original.get(0) == g.get(0,0));
		checkCounterClockWise(g);
	}

	/**
	 * Horizontal flip is needed to put it into the correct order
	 */
	@Test
	public void putGridIntoCanonical_rotate() {
		putGridIntoCanonical_rotate(3,3);
		putGridIntoCanonical_rotate(5,3);
	}
	public void putGridIntoCanonical_rotate(int numRows , int numCols ) {
		DetectAsymmetricCircleGrid<?> alg = new DetectAsymmetricCircleGrid<>(numRows,numCols,null,null,null);

		Grid g = createGrid(numRows,numCols);
		List<EllipseRotated_F64> original = new ArrayList<>();
		original.addAll(g.ellipses);

		alg.putGridIntoCanonical(g);
		assertEquals(numRows,g.rows);
		assertEquals(numCols,g.columns);
		assertTrue(original.get(0) == g.get(0,0));
		checkCounterClockWise(g);

		alg.rotateGridCCW(g);
		alg.putGridIntoCanonical(g);
		assertTrue(original.get(0) == g.get(0,0));
		checkCounterClockWise(g);

		alg.rotateGridCCW(g);
		alg.rotateGridCCW(g);
		alg.putGridIntoCanonical(g);
		assertTrue(original.get(0) == g.get(0,0));
		checkCounterClockWise(g);

		alg.rotateGridCCW(g);
		alg.rotateGridCCW(g);
		alg.rotateGridCCW(g);
		alg.putGridIntoCanonical(g);
		assertTrue(original.get(0) == g.get(0,0));
		checkCounterClockWise(g);

		alg.flipVertical(g);
		alg.putGridIntoCanonical(g);
		assertTrue(original.get(0) == g.get(0,0));
		checkCounterClockWise(g);
	}

	private void checkCounterClockWise(Grid g ) {
		EllipseRotated_F64 a = g.get(0,0);
		EllipseRotated_F64 b = g.columns>=3 ? g.get(0,2) : g.get(1,1);
		EllipseRotated_F64 c = g.rows>=3 ? g.get(2,0) : g.get(1,1);

		double dx0 = b.center.x - a.center.x;
		double dy0 = b.center.y - a.center.y;

		double dx1 = c.center.x - a.center.x;
		double dy1 = c.center.y - a.center.y;

		Vector3D_F64 v = new Vector3D_F64();
		GeometryMath_F64.cross(dx0,dy0,0, dx1,dy1,0, v);
		assertTrue(v.z>0);
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
						g.ellipses.add(new EllipseRotated_F64(j*20,i*20, 0,0,0));
					else
						g.ellipses.add(null);
				} else {
					if( j%2 == 0 )
						g.ellipses.add(null);
					else
						g.ellipses.add(new EllipseRotated_F64(j*20,i*20, 0,0,0));
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

	public static void render(int rows , int cols , double radius , double centerDistance, Affine2D_F64 affine,
							  List<Point2D_F64> locations , GrayU8 image )
	{
		BufferedImage buffered = new BufferedImage(image.width,image.height, BufferedImage.TYPE_INT_BGR);
		Graphics2D g2 = buffered.createGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0,0,buffered.getWidth(),buffered.getHeight());
		g2.setColor(Color.BLACK);

		locations.clear();

		for (int row = 0; row < rows; row++) {
			double y = row*centerDistance/2.0;
			for (int col = 0; col < cols; col++) {
				double x = col*centerDistance/2.0;

				if( row%2 == 1 && col%2 ==0 )
					continue;
				if( row%2 == 0 && col%2 ==1 )
					continue;

				double xx = affine.a11*x + affine.a12*y + affine.tx;
				double yy = affine.a21*x + affine.a22*y + affine.ty;

				g2.fillOval((int)(xx-radius+0.5),(int)(yy-radius+0.5),(int)(radius*2),(int)(radius*2));

				locations.add( new Point2D_F64(xx,yy));
			}
		}

		ConvertBufferedImage.convertFrom(buffered, image);

//		ShowImages.showWindow(buffered,"Rendered",true);
//		try { Thread.sleep(10000); } catch (InterruptedException ignore) {}
	}
}