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

import boofcv.alg.feature.detect.grid.ConnectGridSquares;
import boofcv.alg.feature.detect.quadblob.DetectQuadBlobsBinary;
import boofcv.alg.feature.detect.quadblob.QuadBlob;
import boofcv.struct.FastQueue;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPoint2D_I32;
import georegression.struct.point.Point2D_I32;

import java.util.List;

/**
 * Given a binary image it detects the presence of chess board calibration grids. 1) Detect blobs
 * in binary image and select square like ones. 2) Connect blobs based distance of corners. 3)
 * prune graph. 4) sanity check graph structure. 5) Find bounding quadrilateral.
 *
 * @author Peter Abeles
 */
public class DetectChessSquaresBinary {

	// square blob detector
	private DetectQuadBlobsBinary detectBlobs;

	// how many blobs it expects to find
	private int expectedBlobs;

	// number of rows and columns in blob grid
	private int numRows;
	private int numCols;

	// Find a rectangle which contains the whole target
	private ImageRectangle boundRect = new ImageRectangle();
	// graph of connected bobs
	private List<QuadBlob> graphBlobs;

	// corners on detected squares
	FastQueue<Point2D_I32> corners = new FastQueue<Point2D_I32>(Point2D_I32.class,true);

	/**
	 * Configures chess board detector.
	 *
	 * @param numCols Number of columns in square grid
	 * @param numRows Number of rows in square grid
	 * @param minContourSize Prune blobs which have a contour with few than this number of pixels.
	 */
	public DetectChessSquaresBinary(int numCols, int numRows, int minContourSize)
	{
		this.numRows = numRows;
		this.numCols = numCols;

		// number of black squares in rows/columns
		int blackCols = numCols/2 + numCols%2;
		int blackRows = numRows/2 + numRows%2;

		int innerCols = numCols/2;
		int innerRows = numRows/2;

		expectedBlobs = blackCols*blackRows + innerCols*innerRows;

		setMinimumContourSize(minContourSize);
	}

	/**
	 * Detects chessboard in the binary image.  Square corners must be disconnected.
	 * Returns true if a chessboard was found, false otherwise.
	 *
	 * @param binary Binary image of chessboard
	 * @return True if successful.
	 */
	public boolean process( ImageUInt8 binary ) {
		graphBlobs = null;

		// detect blobs
		if( !detectBlobs.process(binary) )
			return false;

		// connect blobs
		graphBlobs = detectBlobs.getDetected();

		connect(graphBlobs);

		// Remove all but the largest islands in the graph to reduce the number of combinations
		graphBlobs = ConnectGridSquares.pruneSmallIslands(graphBlobs);
		if( graphBlobs.size() != expectedBlobs ) {
//			System.out.println("Unexpected graph size: found = "+graphBlobs.size()+" expected "+expectedBlobs);
			return false;
		}

		// Examine connections
		if( !checkGraphStructure(graphBlobs)) {
//			System.out.println("Bad graph structure");
			return false;
		}

		findBoundingRectangle(graphBlobs);

		return true;
	}


	/**
	 * Connect blobs together based on corner distance. If two corners are uniquely close
	 * together then.
	 */
	public static void connect( List<QuadBlob> blobs  )
	{
		for( int i = 0; i < blobs.size(); i++ ) {
			QuadBlob a = blobs.get(i);

			// A constant threshold for max distance was used before with a requirement that only one point
			// match that criteria.  While a constant threshold is reasonable across
			// images of different resolutions, blurred images caused problems.  They would silently fail
			// confusing users.
			double tol = Math.max( a.largestSide/2.0 , 10 );

			if( a.corners.size() != 4 )
				throw new RuntimeException("WTF is this doing here?");

			for( int indexA = 0; indexA < 4; indexA++ ) {
				Point2D_I32 ac = a.corners.get(indexA);

//				int count = 0;
				QuadBlob match = null;
				double bestScore = Double.MAX_VALUE;

				// find the blobs which has a corner that is the closest match to corner 'indexA'
				for( int j = 0; j < blobs.size(); j++ ) {
					if( j == i )
						continue;

					QuadBlob b = blobs.get(j);

					for( int indexB = 0; indexB < 4; indexB++ ) {
						Point2D_I32 bc = b.corners.get(indexB);

						double d = UtilPoint2D_I32.distance(ac,bc);
						if( d < bestScore ) {
//							System.out.println("  Match distance = "+d+" count = "+count);
							match = b;
							bestScore = d;
						}
					}
				}

				if( match != null && bestScore < tol) {
//					if( a.conn.contains(match) )
//						throw new RuntimeException("MUltiple matches");
					a.conn.add(match);
				}
			}
		}

		// remove connections that are not mutual
		for( int i = 0; i < blobs.size(); i++ ) {
			QuadBlob a = blobs.get(i);

			for( int j = 0; j < a.conn.size(); ) {
				QuadBlob b = a.conn.get(j);
				if( !b.conn.contains(a) ) {
					a.conn.remove(j);
				} else {
					j++;
				}
			}
		}
	}

	/**
	 * Finds bounds of target using corner points of each blob
	 */
	private void findBoundingRectangle( List<QuadBlob> blobs) {
		boundRect.x0 = Integer.MAX_VALUE;
		boundRect.x1 = -Integer.MAX_VALUE;
		boundRect.y0 = Integer.MAX_VALUE;
		boundRect.y1 = -Integer.MAX_VALUE;

		for( QuadBlob b : blobs ) {
			for( Point2D_I32 c : b.corners ) {
				if( c.x < boundRect.x0 )
					boundRect.x0 = c.x;
				if( c.x > boundRect.x1 )
					boundRect.x1 = c.x;
				if( c.y < boundRect.y0 )
					boundRect.y0 = c.y;
				if( c.y > boundRect.y1 )
					boundRect.y1 = c.y;
			}
		}
	}

	/**
	 * Counts the number of connections each node has.  If it is a legit grid
	 * then it should have a known number of nodes with a specific number
	 * of connections.
	 */
	public boolean checkGraphStructure( List<QuadBlob> blobs )
	{
		// make a histogram of connection counts
		int conn[] = new int[5];

		for( QuadBlob b : blobs ) {
			conn[b.conn.size()]++;
		}


		if( conn[3] != 0 )
			return false;

		if( numCols == 1 && numRows == 1 ) {
			if( conn[0] != 1 )
				return false;
			if( conn[1] != 0 )
				return false;
			if( conn[2] != 0 )
				return false;
		} else {
			if( conn[0] != 0 )
				return false;

			if( numCols%2 == 1 && numRows%2 == 1 ) {
				if( conn[1] != 4 )
					return false;

				if( conn[2] != 2*(numCols/2-1) + 2*(numRows/2-1) )
					return false;
			} else if( numCols%2 == 1 || numRows%2 == 1 ) {
				// can handle both cases here due to symmetry
				if( numRows%2 == 0 ) {
					int tmp = numRows;
					numRows = numCols;
					numCols = tmp;
				}

				if( conn[1] != 2 )
					return false;

				if( conn[2] != (numRows-2) + 2*(numCols/2-1) )
					return false;
			} else if( numRows%2 == 1 ) {
				if( conn[1] != 1 + (numCols%2) + (numRows%2) + ((numCols+numRows+1)%2) )
					return false;

				if( conn[2] != 2*(numCols/2-1) + 2*(numRows/2-1) )
					return false;
			} else {
				if( conn[1] != 2 )
					return false;
				if( numCols == 2 || numRows == 2 ) {
					if( conn[2] != Math.max(numCols,numRows)-2 )
						return false;
				} else {
					if( conn[2] != 2*(numCols/2-1) + 2*(numRows/2-1) )
						return false;
				}
			}
		}

		if( conn[4] != expectedBlobs-conn[0]-conn[1]-conn[2] )
			return false;

		return true;
	}

	/**
	 * Adjusts the minimum contour for a square blob
	 *
	 * @param minContourSize The minimum contour size
	 */
	public void setMinimumContourSize( int minContourSize ) {
		detectBlobs = new DetectQuadBlobsBinary(minContourSize,0.25,expectedBlobs);
	}

	public DetectQuadBlobsBinary getDetectBlobs() {
		return detectBlobs;
	}

	public List<QuadBlob> getGraphBlobs() {
		return graphBlobs;
	}

	public ImageRectangle getBoundRect() {
		return boundRect;
	}

	/**
	 * Returns corners that are near a another square
	 */
	public List<Point2D_I32> getCandidatePoints() {
		corners.reset();
		for( QuadBlob b : graphBlobs ) {
			// find point closest to each connection
			for( QuadBlob c : b.conn ) {
				Point2D_I32 best = null;
				double bestDistance = Double.MAX_VALUE;
				for( Point2D_I32 p : b.corners ) {
					int d = p.distance2(c.center);
					if( d < bestDistance ) {
						bestDistance = d;
						best = p;
					}
				}
				corners.grow().set(best);
			}
		}
		return corners.toList();
	}
}
