/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.chess;

import boofcv.alg.feature.detect.quadblob.QuadBlob;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestApproximateChessPoints {

	int numRows = 2;
	int numCols = 4;


	QuadBlob a11 = create(10,10);
	QuadBlob a13 = create(30,10);
	QuadBlob a15 = create(50,10);
	QuadBlob a22 = create(20,20);
	QuadBlob a24 = create(40,20);
	QuadBlob a31 = create(10,30);
	QuadBlob a33 = create(30,30);
	QuadBlob a35 = create(50,30);

	public TestApproximateChessPoints() {
		connect(a11,a22);
		connect(a13,a22);
		connect(a13,a24);
		connect(a15,a24);
		connect(a31,a22);
		connect(a33,a22);
		connect(a33,a24);
		connect(a35,a24);
	}

	/**
	 * Extract the whole grid using each of the corners as a seed
	 */
	@Test
	public void predictPoints() {
		List<Point2D_I32> all = ApproximateChessPoints.predictPoints(a11, numCols, numRows);
		
		assertEquals(8,all.size());

		all = ApproximateChessPoints.predictPoints(a15, numCols, numRows);
		assertEquals(8,all.size());

		all = ApproximateChessPoints.predictPoints(a31, numCols, numRows);
		assertEquals(8,all.size());

		all = ApproximateChessPoints.predictPoints(a35, numCols, numRows);
		assertEquals(8,all.size());
	}

	@Test
	public void addRow() {
		List<Point2D_I32> rowBottom = new ArrayList<Point2D_I32>();
		List<Point2D_I32> rowTop = new ArrayList<Point2D_I32>();

		ApproximateChessPoints.addRow(a11, a22, rowBottom, true);
		ApproximateChessPoints.addRow(a31, a22, rowTop, false);

		assertEquals(4,rowBottom.size());
		assertEquals(4,rowTop.size());
		
		for( int i = 1; i < rowBottom.size(); i++ ) {
			Point2D_I32 a = rowBottom.get(i-1);
			Point2D_I32 b = rowBottom.get(i);

			assertEquals(a.y,b.y);
			assertEquals(10,b.x-a.x);
		}

		for( int i = 1; i < rowTop.size(); i++ ) {
			Point2D_I32 a = rowTop.get(i-1);
			Point2D_I32 b = rowTop.get(i);

			assertEquals(a.y,b.y);
			assertEquals(10,b.x-a.x);
		}

	}

	@Test
	public void next() {
		// add top left
		QuadBlob found = ApproximateChessPoints.next(a11, a22, false);
		assertTrue(found==a13);
		found = ApproximateChessPoints.next(a11, a22, true);
		assertTrue(found==a31);

		// add top right
		found = ApproximateChessPoints.next(a15, a24, false);
		assertTrue(found==a35);
		found = ApproximateChessPoints.next(a15, a24, true);
		assertTrue(found==a13);

	}

	private void connect( QuadBlob a , QuadBlob b ) {
		a.conn.add(b);
		b.conn.add(a);
	}
	
	private QuadBlob create( int x , int y ) {
		QuadBlob a = new QuadBlob();
		
		a.center = new Point2D_I32(x,y);
		
		return a;
	}
}
