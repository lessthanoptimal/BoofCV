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

package boofcv.alg.feature.detect.quadblob;

import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestFindBoundingQuadrilateral {
	
	Random rand = new Random(234);

	/**
	 * Test it against an easy case where the corners are aligned along the image's axis and easily
	 * identifiable.
	 */
	@Test
	public void findCorners_simple() {
		List<Point2D_F64> list = new ArrayList<Point2D_F64>();
		
		for( int i = 0; i <= 5; i++ ) {
			list.add( new Point2D_F64(i,2));
			list.add( new Point2D_F64(i,8));
		}
		list.add( new Point2D_F64(0,3));
		list.add( new Point2D_F64(5,3));
		
		Collections.shuffle(list);
		
		List<Point2D_F64> corners = FindBoundingQuadrilateral.findCorners(list);
		
		assertEquals(4,corners.size());

		assertEquals(1,count(0,2,list));
		assertEquals(1,count(0,8,list));
		assertEquals(1,count(5,2,list));
		assertEquals(1,count(5,8,list));
	}
	
	private int count( int x , int y , List<Point2D_F64> list ) {
		int ret = 0;
		
		for( Point2D_F64 p : list ) {
			if( p.x == x && p.y == y )
				ret++;
		}
		return ret;
	}

	@Test
	public void area() {
		Point2D_F64 a = new Point2D_F64(1,5);
		Point2D_F64 b = new Point2D_F64(6,1);
		Point2D_F64 c = new Point2D_F64(1,1);
	
		double expected = 0.5*(4*5);
		double found = FindBoundingQuadrilateral.area(a,b,c);
		
		assertEquals(expected,found,1e-8);
	}

	@Test
	public void maximizeArea() {
		Point2D_F64 a = new Point2D_F64(1,5);
		Point2D_F64 b = new Point2D_F64(6,1);
		Point2D_F64 c = new Point2D_F64(-10,-10);

		List<Point2D_F64> list = new ArrayList<Point2D_F64>();
		list.add(c);
		for( int i = 0; i < 5; i++ ) {
			int x = rand.nextInt(10)-5;
			int y = rand.nextInt(10)-5;
			list.add(new Point2D_F64(x,y));
		}
		Collections.shuffle(list,rand);

		Point2D_F64 found = FindBoundingQuadrilateral.maximizeArea(a,b,list);

		assertEquals(c.x, found.x,1e-8);
		assertEquals(c.y, found.y,1e-8);
	}

	@Test
	public void maximizeForth() {
		Point2D_F64 a = new Point2D_F64(1,5);
		Point2D_F64 b = new Point2D_F64(6,1);
		Point2D_F64 c = new Point2D_F64(7,8);
		Point2D_F64 d = new Point2D_F64(-10,-12);

		List<Point2D_F64> list = new ArrayList<Point2D_F64>();
		list.add(d);
		for( int i = 0; i < 5; i++ ) {
			int x = rand.nextInt(10)-5;
			int y = rand.nextInt(10)-5;
			list.add(new Point2D_F64(x,y));
		}
		Collections.shuffle(list,rand);

		Point2D_F64 found = FindBoundingQuadrilateral.maximizeForth(a,b,c,list);

		assertEquals(d.x, found.x,1e-8);
		assertEquals(d.y, found.y,1e-8);
	}
}
