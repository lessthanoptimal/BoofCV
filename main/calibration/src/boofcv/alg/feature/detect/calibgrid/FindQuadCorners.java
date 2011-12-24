/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

import georegression.metric.Distance2D_F64;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import pja.sorting.QuickSort_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Given a set of unordered pixels that form the outside edge of a convex blob, it finds the 4 extreme points that
 * are the corners of a quadrilateral.  First  the two points
 *
 * @author Peter Abeles
 */
// TODO create unit test
public class FindQuadCorners {

	// how close a pixel can be to be considered part of the same line
	double lineTolerance=2;

	/**
	 * Finds corners from list of contour points and orders contour points into clockwise order.
	 *
	 * @param contour An unordered list of contour points.  Is modified to be on clockwise order.
	 * @return List of 4 best corner points in clockwise order.
	 */
	public List<Point2D_I32> process( List<Point2D_I32> contour ) {
		// order points in clockwise order
		Point2D_I32 center = findCenter(contour);
		sortByAngle(center,contour);

		// find the first corner
		int corner0 = findFarthest( contour.get(0) , contour );
		// and the second
		int corner1 = findFarthest( contour.get(corner0) , contour );

		// now the other corners are harder
		List<Point2D_I32> corners = new ArrayList<Point2D_I32>();

		corners.add( contour.get(corner0));
		corners.add( contour.get(corner1));

		// find points which maximize the inlier to a line model and are not close to existing points
		findCorner(corner0,corner1,contour,corners);
		findCorner(corner1,corner0,contour,corners);

		// sort the corners to make future calculations easier
		sortByAngle(center,corners);

		return corners;
	}

	/**
	 * Returns the index of the point farthest away from the sample point
	 */
	protected static int findFarthest( Point2D_I32 a , List<Point2D_I32> contour ) {
		int best = -1;
		int index = -1;
		
		for( int i = 0; i < contour.size(); i++ ) {
			Point2D_I32 b = contour.get(i);

			int d = a.distance2(b);
			
			if( d > best ) {
				best = d;
				index = i;
			}
		}
		
		return index;
	}

	protected Point2D_I32 findCenter( List<Point2D_I32> contour ) {

		int x = 0;
		int y = 0;
		
		for( Point2D_I32 p : contour ) {
			x += p.x;
			y += p.y;
		}
		
		x /= contour.size();
		y /= contour.size();

		return new Point2D_I32(x,y);
	}
	
	
	protected void sortByAngle( Point2D_I32 center , List<Point2D_I32> contour ) {
		double angles[] = new double[ contour.size() ];
		int indexes[] = new int[ angles.length ];
		
		for( int i = 0; i < contour.size(); i++ ) {
			Point2D_I32 c = contour.get(i);
			int dx = c.x-center.x;
			int dy = c.y-center.y;

			angles[i] = Math.atan2(dy,dx);
		}

		QuickSort_F64 sort = new QuickSort_F64();
		sort.sort(angles,angles.length,indexes);
		
		List<Point2D_I32> sorted = new ArrayList<Point2D_I32>(contour.size());
		for( int i = 0; i < indexes.length; i++ ) {
			
			sorted.add( contour.get( indexes[i]));
		}
		
		contour.clear();
		contour.addAll(sorted);
	}
	
	private void findCorner( int start , int stop , List<Point2D_I32> contour , List<Point2D_I32> corners )
	{
		int candidate0 = findMaxInlier(start,stop,1,contour);
		int candidate1 = findMaxInlier(start,stop,-1,contour);
		
		Point2D_I32 c0 = contour.get(candidate0);
		Point2D_I32 c1 = contour.get(candidate1);
		
		int dist0 = 0, dist1 = 0;
		
		for( Point2D_I32 p : corners ) {
			dist0 += p.distance2(c0);
			dist1 += p.distance2(c1);
		}

		if( dist0 > dist1 ) {
			corners.add(c0);
		} else {
			corners.add(c1);
		}
	}
	
	private int findMaxInlier(int start, int stop, int dir, List<Point2D_I32> contour) {
		
		int bestCount = -1;
		int bestIndex = 0;
		
		for( int i = start; i != stop; i = incrementCircle(i,dir,contour.size()) ) {

			int count = countInliers(start,i,dir,contour,lineTolerance);
//			System.out.println(" i = "+i+" count = "+count);
			
			if( count > bestCount ) {
				bestCount = count;
				bestIndex = i;
			}
		}
		
		return bestIndex;
	}
	
	protected static int countInliers( int start , int stop , int dir ,
									   List<Point2D_I32> contour ,
									   double lineTolerance ) {
		int count = 0;

		Point2D_I32 a = contour.get(start);
		Point2D_I32 b = contour.get(stop);

		LineParametric2D_F64 line = new LineParametric2D_F64(a.x,a.y,b.x-a.x,b.y-a.y);

		Point2D_F64 p = new Point2D_F64();
		for( int i = start; i != stop;  i = incrementCircle(i,dir,contour.size())) {
			Point2D_I32 c = contour.get(i);
			p.x = c.x;
			p.y = c.y;
			
			double d = Distance2D_F64.distanceSq(line,p);
			if( d <= lineTolerance ) {
				count++;
			}
		}
		
		return count;
	}
	
	protected static int incrementCircle( int i , int dir , int size ) {
		i += dir;
		if( i < 0 ) i = size+i;
		else if( i >= size ) i = i - size;
		return i;
	}
}
