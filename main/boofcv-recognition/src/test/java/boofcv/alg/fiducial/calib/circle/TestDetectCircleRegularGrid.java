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

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.Grid;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.curve.EllipseRotated_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
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
public class TestDetectCircleRegularGrid {

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
		Affine2D_F64 affine = new Affine2D_F64(c,-s,s,c,140,100);

		performDetectionCheck(5, 6, 5, 6, affine);
	}

	@Test
	public void process_negative() {
		Affine2D_F64 affine = new Affine2D_F64(1,0,0,1,100,100);

		performDetectionCheck(4, 6, 5, 6, affine);
	}

	private DetectCircleRegularGrid<GrayU8> createAlg(int numRows , int numCols ,
													  double diameter , double centerDistance ) {

		double spaceRatio = 2.0*1.2*centerDistance/diameter;

		InputToBinary<GrayU8> threshold = FactoryThresholdBinary.globalFixed(100,true,GrayU8.class);
		BinaryEllipseDetector<GrayU8> detector = FactoryShapeDetector.ellipse(null, GrayU8.class);
		EllipsesIntoClusters cluster = new EllipsesIntoClusters(spaceRatio,0.8,0.5);
		return new DetectCircleRegularGrid<>( numRows, numCols,threshold, detector,  cluster);
	}

	private void performDetectionCheck(int expectedRows, int expectedCols, int actualRows, int actualCols, Affine2D_F64 affine) {
		int diameter = 40;
		int centerDistances = 120;

		DetectCircleRegularGrid<GrayU8> alg = createAlg(expectedRows,expectedCols,diameter,centerDistances);
//		alg.setVerbose(true);

		List<Point2D_F64> keypoints = new ArrayList<>();
		List<Point2D_F64> centers = new ArrayList<>();
		GrayU8 image = new GrayU8(500,550);
		render(actualRows,actualCols, diameter/2.0, centerDistances, affine, keypoints,centers,image);

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
		TestDetectCircleRegularGrid.checkCounterClockWise(g);

		for (int row = 0; row < g.rows; row++) {
			for (int col = 0; col < g.columns; col++) {
				EllipseRotated_F64 f = g.get(row,col);
				Point2D_F64 e = centers.get(row*g.columns+col);

				assertEquals( e.x , f.center.x , 1.5 );
				assertEquals( e.y , f.center.y , 1.5 );
				assertEquals( 20 , f.a , 1.0 );
				assertEquals( 20 , f.b , 1.0 );
			}
		}
	}

	/**
	 * A flip is needed to get it into canonical
	 */
	@Test
	public void putGridIntoCanonical_flip() {
		putGridIntoCanonical_flip(2,5);
		putGridIntoCanonical_flip(6,5);
		putGridIntoCanonical_flip(2,2);
		putGridIntoCanonical_flip(3,3);
	}

	private void putGridIntoCanonical_flip( int numRows , int numCols) {
		DetectCircleRegularGrid<?> alg = new DetectCircleRegularGrid(numRows,numCols,null,null,null);

		Grid g = createGrid(numRows,numCols);
		List<EllipseRotated_F64> original = new ArrayList<>();
		original.addAll(g.ellipses);

		alg.putGridIntoCanonical(g);
		assertEquals(numRows,g.rows);
		assertEquals(numCols,g.columns);
		assertTrue(original.get(0) == g.get(0,0));
		checkCounterClockWise(g);

		g = TestDetectCircleGrid.flipHorizontal(g);
		alg.putGridIntoCanonical(g);
		assertEquals(numRows,g.rows);
		assertEquals(numCols,g.columns);
		assertTrue(original.get(0) == g.get(0,0));
		checkCounterClockWise(g);

		g = TestDetectCircleGrid.flipVertical(g);
		alg.putGridIntoCanonical(g);
		assertEquals(numRows,g.rows);
		assertEquals(numCols,g.columns);
		assertTrue(original.get(0) == g.get(0,0));
		checkCounterClockWise(g);
	}

	static Grid createGrid(int numRows, int numCols) {
		Grid g = new Grid();

		g.rows = numRows;
		g.columns = numCols;

		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numCols; j++) {
				g.ellipses.add(new EllipseRotated_F64(j*20,i*20, 0,0,0));
			}
		}

		return g;
	}

	static void checkCounterClockWise(Grid g ) {
		EllipseRotated_F64 a = g.get(0,0);
		EllipseRotated_F64 b = g.get(0,1);
		EllipseRotated_F64 c = g.get(1,0);

		double dx0 = b.center.x - a.center.x;
		double dy0 = b.center.y - a.center.y;

		double dx1 = c.center.x - a.center.x;
		double dy1 = c.center.y - a.center.y;

		Vector3D_F64 v = new Vector3D_F64();
		GeometryMath_F64.cross(dx0,dy0,0, dx1,dy1,0, v);
		assertTrue(v.z>0);
	}

	public static void render(int rows , int cols , double radius , double centerDistance, Affine2D_F64 affine,
							  List<Point2D_F64> keypoints ,  List<Point2D_F64> centers, GrayU8 image )
	{
		BufferedImage buffered = new BufferedImage(image.width,image.height, BufferedImage.TYPE_INT_BGR);
		Graphics2D g2 = buffered.createGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0,0,buffered.getWidth(),buffered.getHeight());
		g2.setColor(Color.BLACK);

		keypoints.clear();

		for (int row = 0; row < rows; row++) {
			double y = row*centerDistance/2.0;
			for (int col = 0; col < cols; col++) {
				double x = col*centerDistance/2.0;

				double xx = affine.a11*x + affine.a12*y + affine.tx;
				double yy = affine.a21*x + affine.a22*y + affine.ty;

				g2.fillOval((int)(xx-radius+0.5),(int)(yy-radius+0.5),(int)(radius*2),(int)(radius*2));

				centers.add( new Point2D_F64(xx,yy));

				// take in account y-axis being flipped
				keypoints.add( new Point2D_F64(xx,yy+radius));
				keypoints.add( new Point2D_F64(xx+radius,yy));
				keypoints.add( new Point2D_F64(xx,yy-radius));
				keypoints.add( new Point2D_F64(xx-radius,yy));
			}
		}

		ConvertBufferedImage.convertFrom(buffered, image);

//		ShowImages.showWindow(buffered,"Rendered",true);
//		try { Thread.sleep(10000); } catch (InterruptedException ignore) {}
	}
}