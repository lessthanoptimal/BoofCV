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

package boofcv.alg.shapes;

import boofcv.struct.PointIndex_I32;
import georegression.geometry.UtilEllipse_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.EllipseRotated_F64;
import georegression.struct.trig.Circle2D_F64;
import org.ddogleg.struct.GrowQueue_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestShapeFittingOps {

	/**
	 * Fit a polygon to a simple rectangle, loop assumed
	 */
	@Test
	public void fitPolygon_loop() {
		List<Point2D_I32> sequence = createRectangle();

		List<PointIndex_I32> result = ShapeFittingOps.fitPolygon(sequence,true,0.05,0,100);

		assertEquals(4, result.size());
		checkPolygon(new int[]{5, 0, 5, 9, 0, 9, 0, 0}, new int[]{5, 14, 19, 0}, result);
	}

	/**
	 * Fit a polygon to a simple rectangle, not looped
	 */
	@Test
	public void fitPolygon_regular() {
		List<Point2D_I32> sequence = createRectangle();

		List<PointIndex_I32> result = ShapeFittingOps.fitPolygon(sequence,false,0.05,0,100);

		assertEquals(5, result.size());
		checkPolygon(new int[]{0, 0, 5, 0, 5, 9, 0, 9, 0, 1}, new int[]{0, 5, 14, 19, 27}, result);
	}

	/**
	 * Checks found polygon in a "shift" independent manor
	 */
	public static void checkPolygon( int[] coordinate , int indexes[], List<PointIndex_I32> found  ) {
		assertEquals(indexes.length, found.size());

		for (int i = 0; i < found.size(); i++) {
			boolean matched = true;
			for (int j = 0; j < found.size(); j++) {
				int x = coordinate[j*2];
				int y = coordinate[j*2+1];
				int index = indexes[j];

				if( !check(x,y,index,found.get((i+j)%found.size()))) {
					matched = false;
					break;
				}
			}
			if( matched )
				return;
		}
		fail("No match");
	}

	/**
	 * Check the found solution
	 */
	@Test
	public void fitEllipse_F64() {
		EllipseRotated_F64 rotated = new EllipseRotated_F64(1,2,3,2,-0.05);

		List<Point2D_F64> points = new ArrayList<>();
		for( int i = 0; i < 20; i++ ) {
			double theta = 2.0*(double)Math.PI*i/20;
			points.add(UtilEllipse_F64.computePoint(theta, rotated, null));
		}

		EllipseRotated_F64 found = ShapeFittingOps.fitEllipse_F64(points,0,false,null).shape;

		assertEquals(rotated.center.x,found.center.x,1e-8);
		assertEquals(rotated.center.y,found.center.y,1e-8);
		assertEquals(rotated.a,found.a,1e-8);
		assertEquals(rotated.b,found.b,1e-8);
		assertEquals(rotated.phi,found.phi,1e-8);

		// make sure refinement doesn't skew it up
		found = ShapeFittingOps.fitEllipse_F64(points,20,false,null).shape;

		assertEquals(rotated.center.x,found.center.x,1e-8);
		assertEquals(rotated.center.y,found.center.y,1e-8);
		assertEquals(rotated.a,found.a,1e-8);
		assertEquals(rotated.b,found.b,1e-8);
		assertEquals(rotated.phi,found.phi,1e-8);
	}

	/**
	 * Request that error be computed
	 */
	@Test
	public void fitEllipse_F64_error() {
		EllipseRotated_F64 rotated = new EllipseRotated_F64(1,2,3,2,-0.05);

		List<Point2D_F64> points = new ArrayList<>();
		for( int i = 0; i < 20; i++ ) {
			double theta = 2.0*(double)Math.PI*i/20;
			points.add(UtilEllipse_F64.computePoint(theta, rotated, null));
		}
		points.get(5).x += 2;

		// Algebraic solution
		FitData<EllipseRotated_F64> found = ShapeFittingOps.fitEllipse_F64(points,0,false,null);

		assertTrue(found.error == 0);

		// make sure refinement doesn't skew it up
		found = ShapeFittingOps.fitEllipse_F64(points,0,true,null);

		assertTrue(found.error > 0);

		// Refined solution
		found = ShapeFittingOps.fitEllipse_F64(points,10,false,null);

		assertTrue(found.error == 0);

		// make sure refinement doesn't skew it up
		found = ShapeFittingOps.fitEllipse_F64(points,10,true,null);

		assertTrue(found.error > 0);
	}

	/**
	 * Checks to see if they produce the same solution
	 */
	@Test
	public void fitEllipse_I32() {
		EllipseRotated_F64 rotated = new EllipseRotated_F64(1,2,3,2,-0.05);

		List<Point2D_F64> pointsF = new ArrayList<>();
		List<Point2D_I32> pointsI = new ArrayList<>();
		for( int i = 0; i < 20; i++ ) {
			double theta = 2.0*(double)Math.PI*i/20;
			Point2D_F64 p = UtilEllipse_F64.computePoint(theta, rotated, null);
			Point2D_I32 pi = new Point2D_I32((int)p.x,(int)p.y) ;
			p.set(pi.x,pi.y);
			pointsF.add(p);
			pointsI.add(pi);
		}

		EllipseRotated_F64 expected = ShapeFittingOps.fitEllipse_F64(pointsF,0,false,null).shape;
		EllipseRotated_F64 found = ShapeFittingOps.fitEllipse_I32(pointsI, 0, false, null).shape;

		assertEquals(expected.center.x, found.center.x,1e-8);
		assertEquals(expected.center.y, found.center.y,1e-8);
		assertEquals(expected.a, found.a,1e-8);
		assertEquals(expected.b, found.b,1e-8);
		assertEquals(expected.phi, found.phi,1e-8);
	}

	@Test
	public void averageCircle_I32() {
		List<Point2D_I32> points = new ArrayList<>();
		points.add( new Point2D_I32(0,0));
		points.add( new Point2D_I32(10,0));
		points.add( new Point2D_I32(5,5));
		points.add( new Point2D_I32(5,-5));

		FitData<Circle2D_F64> found = ShapeFittingOps.averageCircle_I32(points, null, null);
		assertEquals(5,found.shape.center.x,1e-5);
		assertEquals(0,found.shape.center.y,1e-5);
		assertEquals(5,found.shape.radius,1e-5);
		assertEquals(0, found.error, 1e-5);

		// Pass in storage and see if it fails
		found.error = 23; found.shape.center.x = 3;
		GrowQueue_F64 optional = new GrowQueue_F64();
		optional.push(4);

		ShapeFittingOps.averageCircle_I32(points, optional, found);
		assertEquals(5,found.shape.center.x,1e-5);
		assertEquals(0,found.shape.center.y,1e-5);
		assertEquals(5,found.shape.radius,1e-5);
		assertEquals(0, found.error, 1e-5);

		// now make it no longer a perfect fit
		points.get(0).x = -1;
		found = ShapeFittingOps.averageCircle_I32(points, null, null);
		assertTrue( found.error > 0 );
	}

	private static boolean check( int x , int y , int index , PointIndex_I32 found ) {
		if( x != found.x ) return false;
		if( y != found.y ) return false;
		return index == found.index;
	}

	@Test
	public void averageCircle_F64() {
		List<Point2D_F64> points = new ArrayList<>();
		points.add( new Point2D_F64(0,0));
		points.add( new Point2D_F64(10,0));
		points.add( new Point2D_F64(5,5));
		points.add( new Point2D_F64(5,-5));

		FitData<Circle2D_F64> found = ShapeFittingOps.averageCircle_F64(points, null, null);
		assertEquals(5, found.shape.center.x, 1e-5);
		assertEquals(0,found.shape.center.y,1e-5);
		assertEquals(5,found.shape.radius,1e-5);
		assertEquals(0, found.error, 1e-5);

		// Pass in storage and see if it fails
		found.error = 23; found.shape.center.x = 3;
		GrowQueue_F64 optional = new GrowQueue_F64();
		optional.push(4);

		ShapeFittingOps.averageCircle_F64(points, optional, found);
		assertEquals(5,found.shape.center.x,1e-5);
		assertEquals(0,found.shape.center.y,1e-5);
		assertEquals(5,found.shape.radius,1e-5);
		assertEquals(0, found.error, 1e-5);

		// now make it no longer a perfect fit
		points.get(0).x = -1;
		found = ShapeFittingOps.averageCircle_F64(points, null, null);
		assertTrue(found.error > 0);
	}

	/**
	 * Creates a simple rectangle
	 */
	public static List<Point2D_I32> createRectangle() {
		return createRectangle_I32(6,10,(6+10)*2 - 4);
	}

	public static List<Point2D_I32> createRectangle_I32( int width , int height , int numPoints ) {
		List<Point2D_I32> points = new ArrayList<>();

		int length = width*2 + height*2 - 4;

		for (int i = 0; i < numPoints; i++) {
			int x = i*length/numPoints;

			if( x < width ) {
				points.add( new Point2D_I32(x,0));
			} else if( x < width+height-2) {
				int y = x - width+1;
				points.add( new Point2D_I32(width-1,y));
			} else if( x < width*2+height-2) {
				int xx = x - width - height+3;
				points.add( new Point2D_I32(width-xx,height-1));
			} else {
				int y = x - width*2 - height+4;
				points.add( new Point2D_I32(0,height-y));
			}
		}

//		for( Point2D_I32 p : points ) {
//			System.out.println(p);
//		}
//		System.out.println("Length = "+points.size());

		return points;
	}

	public static List<Point2D_F64> createRectangle_F64( int width , int height , int numPoints ) {
		return ShapeFittingOps.convert_I32_F64(createRectangle_I32(width,height,numPoints));
	}

	public static List<Point2D_F64> createEllipse_F64( EllipseRotated_F64 ellipse , int numPoints ) {

		List<Point2D_F64> sequence = new ArrayList<>();

		for (int i = 0; i < numPoints; i++) {
			double theta = 2.0*Math.PI*i/numPoints;
			Point2D_F64 p = new Point2D_F64();

			UtilEllipse_F64.computePoint(theta,ellipse,p);
			sequence.add( p );
		}

		return sequence;
	}
}
