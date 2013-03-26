/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.shapes;

import boofcv.struct.GrowQueue_F64;
import boofcv.struct.PointIndex_I32;
import georegression.struct.point.Point2D_I32;
import georegression.struct.trig.Circle2D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

		List<PointIndex_I32> result = ShapeFittingOps.fitPolygon(sequence,true,0.1,0.01,100);

		assertEquals(4,result.size());
		check(0, 0, 0, result.get(0));
		check(0, 9, 9, result.get(1));
		check(5,9,14,result.get(2));
		check(5,0,23,result.get(3));
	}

	/**
	 * Fit a polygon to a simple rectangle, not looped
	 */
	@Test
	public void fitPolygon_regular() {
		List<Point2D_I32> sequence = createRectangle();

		List<PointIndex_I32> result = ShapeFittingOps.fitPolygon(sequence,false,0.1,0.01,100);

		assertEquals(5,result.size());
		check(0,0,0,result.get(0));
		check(0,9,9,result.get(1));
		check(5,9,14,result.get(2));
		check(5,0,23,result.get(3));
		check(1,0,27,result.get(4));
	}

	@Test
	public void fitCircle() {
		List<Point2D_I32> points = new ArrayList<Point2D_I32>();
		points.add( new Point2D_I32(0,0));
		points.add( new Point2D_I32(10,0));
		points.add( new Point2D_I32(5,5));
		points.add( new Point2D_I32(5,-5));

		FitData<Circle2D_F64> found = ShapeFittingOps.fitCircle(points,null,null);
		assertEquals(5,found.shape.center.x,1e-5);
		assertEquals(0,found.shape.center.y,1e-5);
		assertEquals(5,found.shape.radius,1e-5);
		assertEquals(0, found.error, 1e-5);

		// Pass in storage and see if it fails
		found.error = 23; found.shape.center.x = 3;
		GrowQueue_F64 optional = new GrowQueue_F64();
		optional.push(4);

		ShapeFittingOps.fitCircle(points,optional,found);
		assertEquals(5,found.shape.center.x,1e-5);
		assertEquals(0,found.shape.center.y,1e-5);
		assertEquals(5,found.shape.radius,1e-5);
		assertEquals(0, found.error, 1e-5);

		// now make it no longer a perfect fit
		points.get(0).x = -1;
		found = ShapeFittingOps.fitCircle(points,null,null);
		assertTrue( found.error > 0 );
	}

	private void check( int x , int y , int index , PointIndex_I32 found ) {
		assertEquals(x,found.x);
		assertEquals(y,found.y);
		assertEquals(index,found.index);
	}

	/**
	 * Creates a simple rectangle
	 */
	private List<Point2D_I32> createRectangle() {
		List<Point2D_I32> sequence = new ArrayList<Point2D_I32>();
		for( int i = 0; i < 10; i++ ) {
			sequence.add( new Point2D_I32(0,i));
		}
		for( int i = 1; i < 5; i++ ) {
			sequence.add( new Point2D_I32(i,9));
		}
		for( int i = 0; i < 10; i++ ) {
			sequence.add( new Point2D_I32(5,9-i));
		}
		for( int i = 1; i < 5; i++ ) {
			sequence.add( new Point2D_I32(5-i,0));
		}
		return sequence;
	}
}
