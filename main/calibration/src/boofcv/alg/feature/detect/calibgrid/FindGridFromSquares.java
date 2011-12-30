/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.calibgrid;

import georegression.metric.Intersection2D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.feature.detect.calibgrid.FindQuadCorners.*;

/**
 * Given a set of calibration grids, look for a set of calibration targets
 * 
 * @author Peter Abeles
 */
public class FindGridFromSquares {
	List<SquareBlob> blobs;

	List<Point2D_I32> targetCorners;
	List<Point2D_I32> targetObs = new ArrayList<Point2D_I32>();

	public FindGridFromSquares(List<SquareBlob> blobs) {
		this.blobs = blobs;
	}

	public void process() {
		List<Point2D_I32> points = toPointList(blobs);
		targetCorners = findTargetCorners(points);

		connect(blobs);
		
		SquareBlob seed = findBlobWithPoint(targetCorners.get(0));

		List<SquareBlob> orderedBlob = new ArrayList<SquareBlob>();

		List<SquareBlob> topRow = findLine(seed,targetCorners.get(1));
		List<SquareBlob> leftCol = findLine(seed,targetCorners.get(3));
		List<SquareBlob> rightCol = findLine(topRow.get(topRow.size()-1),targetCorners.get(2));

		// todo order corners using top corners

		int rowSize = topRow.size();
		if( blobs.size() % rowSize != 0 )
			throw new RuntimeException("Total square not divisible by row size");
		while( true ) {
			System.out.println("Row size = "+topRow.size());
			orderedBlob.addAll(topRow);
			removeRow(topRow,blobs);
			topRow.clear();
			leftCol.remove(0);
			rightCol.remove(0);
			// todo update top corners

			if(  blobs.size() > 0 ) {
				seed = leftCol.get(0);
				topRow = findLine(seed,rightCol.get(0).center);
			    // todo top order corners topRow by target
				if( topRow.size() != rowSize )
					throw new RuntimeException("Unexpected row size");
			} else {
				break;
			}
		}

		orderedBlobsIntoPoints(orderedBlob,targetObs,rowSize);
	}

//	private void orderRowByTarget( List<SquareBlob> blobs , Point2D_I32 target ) {
//		Point2D_I32 c = blobs.get(0).center;
//		double angleTarget = Math.atan2(target.y-c.x,target.x-c.x);
//
//		double angles[] = new double[4];
//
//		for( SquareBlob b : blobs ) {
//			for( int i = 0; i < 4; i++ ) {
//				c = b.corners.get(i);
//				double angle = Math.atan2()
//				angles[i] = UtilAngle.minus()
//			}
//		}
//	}

	private void orderedBlobsIntoPoints( List<SquareBlob> blobs , List<Point2D_I32> points , int rowSize ) {
		for( int i = 0; i < blobs.size(); i += rowSize ) {
			for( int j = 0; j < rowSize; j++ ) {
				SquareBlob b = blobs.get(i+j);
				points.add(b.corners.get(0));
				points.add(b.corners.get(1));
			}
			for( int j = 0; j < rowSize; j++ ) {
				SquareBlob b = blobs.get(i+j);
				points.add(b.corners.get(3));
				points.add(b.corners.get(2));
			}
		}
	}

	private void removeRow( List<SquareBlob> row , List<SquareBlob> all ) {
		for( SquareBlob b : row ) {
			for( SquareBlob c : b.conn ) {
				c.conn.remove(b);
			}
			all.remove(b);
		}
	}

	public List<SquareBlob> findLine( SquareBlob blob , Point2D_I32 target ) {
		List<SquareBlob> ret = new ArrayList<SquareBlob>();

		double targetAngle = Math.atan2( target.y - blob.center.y , target.x - blob.center.x );

		while( blob != null ) {
			ret.add(blob);
			SquareBlob found = null;

			// see if any of  the connected point towards the target
			for( SquareBlob c : blob.conn ) {
				double angle = Math.atan2(c.center.y-blob.center.y,c.center.x-blob.center.x);
				double acute = UtilAngle.dist(targetAngle,angle);
				if( acute < Math.PI/3 ) {
					found = c;
					break;
				}
			}

			blob = found;
		}

		return ret;
	}

	public SquareBlob findBlobWithPoint( Point2D_I32 pt )
	{
		for( SquareBlob b : blobs ) {
			if (contains(pt, b)) 
				return b;
		}

		return null;
	}

	private boolean contains(Point2D_I32 pt, SquareBlob b) {
		for( Point2D_I32 c : b.corners ) {
			if( c.x == pt.x && c.y == pt.y ) {
				return true;
			}
		}
		return false;
	}

	public static List<Point2D_I32> toPointList( List<SquareBlob> blobs ) {
		List<Point2D_I32> ret = new ArrayList<Point2D_I32>();
		
		for( SquareBlob s : blobs ) {
			ret.addAll(s.corners);
		}
		
		return ret;
	}

	public static List<Point2D_I32> findTargetCorners( List<Point2D_I32> list ) {

		// find the first corner
		Point2D_I32 corner0 = list.get(findFarthest(list.get(0), list));
		// and the second
		Point2D_I32 corner1 = list.get(findFarthest(corner0, list));

		// third point
		Point2D_I32 corner2 = maximizeArea(corner0,corner1,list);
		Point2D_I32 corner3 = maximizeForth(corner0, corner2, corner1, list);

		List<Point2D_I32> ret = new ArrayList<Point2D_I32>();
		ret.add(corner0);
		ret.add(corner1);
		ret.add(corner2);
		ret.add(corner3);

		// organize the corners
		Point2D_I32 center = findAverage(ret);
		sortByAngleCCW(center,ret);

		return ret;
	}

	public static Point2D_I32 maximizeArea( Point2D_I32 a , Point2D_I32 b , List<Point2D_I32> list )
	{
		double max = 0;
		Point2D_I32 maxPoint = null;
		
		for( Point2D_I32 c : list ) {
			double area = area(a,b,c);
			
			if( area > max ) {
				max = area;
				maxPoint = c;
			}
		}
		
		return maxPoint;
	}

	public static Point2D_I32 maximizeForth( Point2D_I32 a , Point2D_I32 b , Point2D_I32 c , List<Point2D_I32> list )
	{
		double max = 0;
		Point2D_I32 maxPoint = null;

		for( Point2D_I32 d : list ) {
			double l1 = Math.sqrt(a.distance2(d));
			double l2 = Math.sqrt(b.distance2(d));
			double l3 = Math.sqrt(c.distance2(d));

			double score = l1+l2+l3;

			if( score > max ) {
				max = score;
				maxPoint = d;
			}
		}

		return maxPoint;
	}

	
	public List<Point2D_I32> getCorners() {
		return targetCorners;
	}
	
	public static double area( Point2D_I32 a , Point2D_I32 b , Point2D_I32 c )
	{
		double top = a.x*(b.y-c.y) + b.x*(c.y-a.y) + c.x*(a.y-b.y);
		return top/2.0;
	}
	
	public static void connect( List<SquareBlob> blobs ) {

		LineSegment2D_F64 centerLine = new LineSegment2D_F64();
		LineSegment2D_F64 cornerLine = new LineSegment2D_F64();
		
		for( int i = 0; i < blobs.size(); i++ ) {
			SquareBlob b = blobs.get(i);
			// for each pair of corners, find the closest blob
			// center line between blobs must intersect line segement between two corners

			centerLine.a.set(b.center.x,b.center.y);
			
			for( int j = 0; j < 4; j++ ) {
				Point2D_I32 c0 = b.corners.get(j);
				Point2D_I32 c1 = b.corners.get((j+1)%4);

				cornerLine.a.set(c0.x,c0.y);
				cornerLine.b.set(c1.x,c1.y);
			
				double best = Double.MAX_VALUE;
				SquareBlob bestBlob = null;

				for( int k = i+1; k < blobs.size(); k++ ) {
					SquareBlob c = blobs.get(k);
					
					centerLine.b.set(c.center.x,c.center.y);
					
					if(Intersection2D_F64.intersection(cornerLine,centerLine,null) != null ) {
						double d = c.center.distance2(b.center);
						if( d < best ) {
							best = d;
							bestBlob = c;
						}
					}
				}
				
				if( bestBlob != null ) {
					bestBlob.conn.add(b);
					b.conn.add(bestBlob);
				}
			}
			            
		}
	}
}
