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

package boofcv.alg.feature.detect.chess;

import boofcv.alg.feature.detect.quadblob.QuadBlob;
import georegression.metric.UtilAngle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Orders the blobs for a chessboard into a standard format.  Designed to generate the same order for stereo camera
 * systems even when there is symmetry in the target.
 *
 * @author Peter Abeles
 */
public class OrderChessboardQuadBlobs {

	// dimensions of the chessboard
	int numCols;
	int numRows;

	// storage for the results
	List<QuadBlob> results = new ArrayList<QuadBlob>();

	// storage for the top/bottom
	List<QuadBlob> top = new ArrayList<QuadBlob>();
	List<QuadBlob> bottom = new ArrayList<QuadBlob>();

	// number of expected elements in the first row
	int expectedFirst;
	// number of expected elements in the second row
	int expectedSecond;

	/**
	 * Specifies the size of the chessboard
	 *
	 * @param numCols Number of columns in the chessboard
	 * @param numRows Number of rows in the chessboard
	 */
	public OrderChessboardQuadBlobs(int numCols, int numRows) {
		if( numCols < 2 || numRows < 2 )
			throw new IllegalArgumentException("A valid chessboard needs to have at least 2 rows or columns");
		this.numCols = numCols;
		this.numRows = numRows;

		expectedFirst = numCols/2 + numCols%2;
		expectedSecond = numCols/2;
	}

	/**
	 * Points the points in the QuadBlob into a standard order.  If the target is asymmetric then the order will be
	 * unique
	 * @param blobs Unordered set of blobs with a valid graph
	 * @return true if successful or false if the graph was bad
	 */
	public boolean order( List<QuadBlob> blobs ) {
		results.clear();
		top.clear();
		bottom.clear();

		// search for candidate points which could be the first square
		// if multiple solutions exist pick the one which is the most to the top left
		// this is needed for the stereo case where the order must be the same in both images
		QuadBlob first = null;
		double firstDistance = 0;

		for( QuadBlob b : blobs ) {
			if( b.conn.size() == 1 ) {
				if( findFirstTwoRows(b)){
					if( first != null ) {
						double d = Math.sqrt(b.center.x*b.center.x +  b.center.y*b.center.y)+b.center.y;
						if( d < firstDistance ) {
							first = b;
							firstDistance = d;
						}
					} else {
						firstDistance = Math.sqrt(b.center.x*b.center.x +  b.center.y*b.center.y)+b.center.y;
						first = b;
					}
				}
				top.clear(); bottom.clear();
			}
		}

		// something went seriously wrong
		if( first == null )
			return false;

		// if there are multiple solutions this needs to be found again
		findFirstTwoRows(first);

		results.addAll(top);
		results.addAll(bottom);

		boolean useFirst = true;

		while( true ) {
			QuadBlob pivot = bottom.get(0);
			QuadBlob seed = useFirst ? findBestCW(first,pivot) : findBestCCW(first, pivot);

			if( seed == null )
				break;
			if( useFirst ) {
				if( !findNextRowDown(seed,pivot,expectedFirst) )
					return false;
			} else {
				if( !findNextRowDown2(seed, pivot, expectedSecond) )
					return false;
			}

			results.addAll(bottom);
			useFirst = !useFirst;
			first = pivot;
		}

//		orderConnections();

		return blobs.size() == results.size();
	}

//	/**
//	 * Ensures that each node's connections are ordered by index
//	 */
//	private void orderConnections() {
//		for( int i = 0; i < results.size(); i++ ) {
//			results.get(i).index = i;
//		}
//
//		for( int i = 0; i < results.size(); i++ ) {
//			for( )
//			Collections.sort(results.get(i).conn, comparator);
//		}
//
//	}

	/**
	 * Adds elements in the first two rows.  It is assumed that the seed has one child
	 */
	private boolean findFirstTwoRows(QuadBlob seed) {

		top.clear();
		bottom.clear();

		QuadBlob c = seed.conn.get(0);
		top.add(seed);
		bottom.add(c);

		while( true ) {
			QuadBlob t = findBestCCW(seed, c);
			if( t == null ) break;
			top.add(t);

			QuadBlob b = findBestCW(c, t);
			if( b == null ) break;
			bottom.add(b);

			seed = t;
			c = b;

			if( top.size() > expectedFirst )
				return false;
		}
		return top.size() == expectedFirst && bottom.size() == expectedSecond;
	}

	/**
	 * Adds elements in the next row down.
	 */
	private boolean findNextRowDown(QuadBlob seed , QuadBlob parent , int expected ) {

		bottom.clear();
		bottom.add(seed);

		QuadBlob d = seed;
		QuadBlob c = parent;

		while( true ) {
			QuadBlob b = findBestCW(d, c);
			if( b == null ) break;
			bottom.add(b);

			QuadBlob t = findBestCCW(c, b);
			if( t == null ) break;

			c = t;
			d = b;

			if( bottom.size() > expected )
				return false;
		}

		return bottom.size() == expected;
	}

	/**
	 * Adds elements in the next row down.
	 */
	private boolean findNextRowDown2(QuadBlob seed , QuadBlob parent , int expected ) {

		bottom.clear();
		bottom.add(seed);

		QuadBlob d = parent;
		QuadBlob c = seed;

		while( true ) {
			QuadBlob t = findBestCCW(d, c);
			if( t == null ) break;


			QuadBlob b = findBestCW(c, t);
			if( b == null ) break;
			bottom.add(b);

			c = b;
			d = t;

			if( bottom.size() > expected )
				return false;
		}

		return bottom.size() == expected;
	}

	protected static QuadBlob findBestCW( QuadBlob parent , QuadBlob pivot ) {
		int bestIndex = -1;
		double bestAngle = Double.MAX_VALUE;
		double angle0 = Math.atan2(parent.center.y-pivot.center.y, parent.center.x-pivot.center.x);
		for( int i = 0; i < pivot.conn.size(); i++ ) {
			QuadBlob d = pivot.conn.get(i);
			if( d == parent)
				continue;
			double angle1 = Math.atan2(d.center.y-pivot.center.y, d.center.x-pivot.center.x);

			double cw = UtilAngle.distanceCW(angle0,angle1);

			if( cw < bestAngle ) {
				bestAngle = cw;
				bestIndex = i;
			}
		}


		if( bestAngle > Math.PI*0.75 || bestIndex < 0 ) {
			return null;
		}
		return pivot.conn.get(bestIndex);
	}

	protected static QuadBlob findBestCCW( QuadBlob parent , QuadBlob pivot ) {
		int bestIndex = -1;
		double bestAngle = Double.MAX_VALUE;
		double angle0 = Math.atan2(parent.center.y-pivot.center.y, parent.center.x-pivot.center.x);
		for( int i = 0; i < pivot.conn.size(); i++ ) {
			QuadBlob d = pivot.conn.get(i);
			if( d == parent)
				continue;
			double angle1 = Math.atan2(d.center.y-pivot.center.y, d.center.x-pivot.center.x);

			double cw = UtilAngle.distanceCCW(angle0, angle1);

			if( cw < bestAngle ) {
				bestAngle = cw;
				bestIndex = i;
			}
		}


		if( bestAngle > Math.PI*0.75 || bestIndex < 0 ) {
			return null;
		}
		return pivot.conn.get(bestIndex);
	}

	public List<QuadBlob> getResults() {
		return results;
	}

	public static class ComparatorQuads implements Comparator<QuadBlob> {
		@Override
		public int compare(QuadBlob o1, QuadBlob o2) {
			if( o1.index < o2.index )
				return -1;
			else if( o1.index == o2.index )
				return 0;
			else
				return 1;
		}
	}
}
