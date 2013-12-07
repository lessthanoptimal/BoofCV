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
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPoint2D_I32;
import georegression.geometry.UtilPolygons2D_I32;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_I32;
import org.ddogleg.sorting.QuickSort_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.Arrays;
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
	// bounding quadrilateral
	private Polygon2D_I32 boundPolygon = new Polygon2D_I32();
	// graph of connected bobs
	private List<QuadBlob> graphBlobs;

	// corners on detected squares
	FastQueue<Point2D_I32> corners = new FastQueue<Point2D_I32>(Point2D_I32.class,true);

	QuickSort_F64 sort = new QuickSort_F64();

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
		boundPolygon.vertexes.reset();
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

		findBoundingPolygon(graphBlobs);

		return true;
	}


	/**
	 * Connect blobs together based on corner distance. If two corners are uniquely close
	 * then connect them together.
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

					// they should be about the same size
					double sizeRatio = Math.min(a.contour.size(),b.contour.size())/(double)Math.max(a.contour.size(),b.contour.size());
					if( sizeRatio < 0.25 )
						continue;

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
					// it is possible to try connecting to the same shape more than once
					int index = a.conn.indexOf(match);
					if( index != -1  ) {
						// save the match with the closest distance
						if( a.connDist.get(index) > bestScore ) {
							a.conn.set(index,match);
							a.connDist.data[index] = bestScore;
							a.connIndex.data[index] = indexA;
						}
					} else {
						a.conn.add(match);
						a.connDist.add( bestScore );
						a.connIndex.add(indexA);
					}
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
					a.connDist.remove(j);
					a.connIndex.remove(j);
				} else {
					j++;
				}
			}
		}
	}

	/**
	 * For targets with even sides this isn't the best.  Corners can be chopped too close.  a rectangle
	 * should be returned instead
	 */
	boolean connected[] = new boolean[4];
	List<QuadBlob> outside = new ArrayList<QuadBlob>();
	List<Point2D_I32> points = new ArrayList<Point2D_I32>();
	private void findBoundingPolygon(List<QuadBlob> blobs) {

		outside.clear();
		points.clear();

		int x = 0,y = 0;
		for( int i = 0; i < blobs.size(); i++ ) {
			QuadBlob b = blobs.get(i);
			if( b.conn.size() != 4 ) {
				outside.add( b );
				x += b.center.x;
				y += b.center.y;
			}
		}

		// center
		x /= outside.size();
		y /= outside.size();


		for( int i = 0; i < outside.size(); i++ ) {
			QuadBlob b = outside.get(i);

			Arrays.fill(connected,false);

			for( int j = 0; j < b.conn.size(); j++ ) {
				connected[ b.connIndex.data[j]] = true;
			}

//			if( b.conn.size() == 2 ) {
//				// this strategy fills out the corners in the rectangle
//				int num = 0;
//				Point2D_I32 p0=null,p1=null;
//				for( int j = 0; j < 4; j++ ) {
//					if( !connected[j] )  {
//						if( num == 0 ) {
//							p0 = b.corners.get(j);
//						} else {
//							p1 = b.corners.get(j);
//						}
//						num++;
//					}
//				}
//
//				int dx = p1.x - p0.x;
//				int dy = p1.y - p0.y;
//
//				points.add(new Point2D_I32(p0.x - dx , p0.y - dy));
//				points.add(new Point2D_I32(p1.x + dx , p1.y + dy));
//			} else {
			for( int j = 0; j < 4; j++ ) {
				if( !connected[j] ) {
					points.add(b.corners.get(j));
				}
			}
//			}

		}

		int indexes[] = new int[points.size()];
		double angles[] = new double[points.size()];

		for( int i = 0; i < points.size(); i++ ) {
			Point2D_I32 p = points.get(i);
			angles[i] = Math.atan2( p.y - y , p.x - x );
		}

		sort.sort(angles,points.size(),indexes);

		for( int i = 0; i < points.size(); i++ ) {
			Point2D_I32 b = points.get(indexes[i]);
			boundPolygon.vertexes.grow().set(b.x,b.y);
		}

		UtilPolygons2D_I32.bounding(boundPolygon,boundRect);
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
	 * @param minContourSize The minimum contour size. Try 10
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

	public Polygon2D_I32 getBoundPolygon() {
		return boundPolygon;
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
