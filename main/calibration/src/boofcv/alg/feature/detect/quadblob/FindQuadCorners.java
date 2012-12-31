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

import boofcv.alg.feature.detect.grid.UtilCalibrationGrid;
import georegression.geometry.UtilPoint2D_I32;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.sorting.QuickSort_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Find a list of corner pixels in a blob using its contour. Four corners are assumed.  A list of candidate
 * corners is done looking for local minimums in acute angle across the contour.  From these candidates the
 * candidate with the smallest acute angle is selected and used as the initial seed.  Next the candidate which
 * is the farthest distance is selected.  This process is repeated two more times, each time maximizing the distance
 * to all the previously selected corners.
 *
 * @author Peter Abeles
 */
public class FindQuadCorners {

	// number of pixels in the contour
	int N;
	// storage for acute angles along the contour
	double acuteAngles[] = new double[1];

	/**
	 * Finds corners from list of contour points and orders contour points into clockwise order.
	 *
	 * @param contour An unordered list of contour points.  Is modified to be on clockwise order.
	 * @return List of 4 best corner points in clockwise order.
	 */
	public List<Point2D_I32> process( List<Point2D_I32> contour ) {
		// order points in clockwise order
		Point2D_I32 center = findAverage(contour);
		sortByAngleCCW(center, contour);

		N = contour.size();
		int radiusLarge = N / 10;
		if( radiusLarge < 2 )
			radiusLarge = 2;

		if( acuteAngles.length < N ) {
			acuteAngles = new double[N];
		}

		computeResponse(contour,radiusLarge, acuteAngles);
		
		// find local minimums
		int minIndex=-1;
		double minValue = Double.MAX_VALUE;
		List<Point2D_I32> candidates = new ArrayList<Point2D_I32>();
		for( int i = 0; i < N; i++ ) {
			int indexBefore = UtilCalibrationGrid.incrementCircle(i, -1, N);
			int indexAfter = UtilCalibrationGrid.incrementCircle(i, 1, N);
			
			double r = acuteAngles[i];
			if( r <= acuteAngles[indexBefore] && r <= acuteAngles[indexAfter ]) {
				candidates.add(contour.get(i));
				
				if( r < minValue ) {
					minValue = r;
					minIndex = candidates.size()-1;
				}
			}
		}
		
		if( candidates.size() < 4 )
			return candidates;

		// use the more initial corner as a seed
		List<Point2D_I32> corners = new ArrayList<Point2D_I32>();
		corners.add(candidates.remove(minIndex));

		// select corners which maximize the distance
		selectCorner(corners,candidates);
		selectCorner(corners,candidates);
		selectCorner(corners,candidates);
			
		// sort the corners to make future calculations easier
		sortByAngleCCW(center, corners);
		
		return corners;
	}

	/**
	 * Selects the corner which has the largest sum of cartesian distance from the already selected
	 * corners.
	 */
	private void selectCorner(List<Point2D_I32> corners, List<Point2D_I32> candidates) {
		double maxDistance = -1;
		int maxIndex = -1;
		
		for( int i = 0; i < candidates.size(); i++ ) {
			Point2D_I32 c = candidates.get(i);
			double d = 0;
			for( Point2D_I32 p : corners ) {
				d += UtilPoint2D_I32.distance(c,p);
			}
			if( d > maxDistance ) {
				maxDistance = d;
				maxIndex = i;
			}
		}                    
		
		corners.add(candidates.remove(maxIndex));
	}

	/**
	 * Find the average of all the points in the list.
	 *
	 * @param contour
	 * @return
	 */
	protected static Point2D_I32 findAverage(List<Point2D_I32> contour) {

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


	/**
	 * Sorts the points in counter clockwise direction around the provided point
	 *
	 * @param center Point that the angle is computed relative to
	 * @param contour List of all the points which are to be sorted by angle
	 */
	protected static void sortByAngleCCW(Point2D_I32 center, List<Point2D_I32> contour) {
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

	/**
	 * Computes the acute angle for each pixel in the contour.  The acute angle is computed by selecting
	 * two points which are 'radius' indexes away and finding the relative angle between those two vectors.
	 */
	protected void computeResponse( List<Point2D_I32> contour , int radius , double response[] )
	{
		for( int i = 0; i < N; i++ ) {
			int i0 = UtilCalibrationGrid.incrementCircle(i, -radius, N);
			int i1 = UtilCalibrationGrid.incrementCircle(i, radius, N);
			
			Point2D_I32 p0 = contour.get(i0);
			Point2D_I32 p1 = contour.get(i1);
			Point2D_I32 p = contour.get(i);

			double angle0 = Math.atan2(p0.y-p.y,p0.x-p.x);
			double angle1 = Math.atan2(p1.y-p.y,p1.x-p.x);

			response[i] = UtilAngle.dist(angle0, angle1);
		}
	}
}
