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

package boofcv.alg.shapes.polyline;

import boofcv.misc.CircularIndex;
import georegression.fitting.line.FitLine_F64;
import georegression.geometry.UtilLine2D_F64;
import georegression.geometry.UtilPoint2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LinePolar2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.Arrays;
import java.util.List;

/**
 * Refines a set of corner points along a contour by fitting lines to the points between the corners using a
 * least-squares technique.  It then refines the corners estimates by interesting the lines and finding
 * the closest point on the contour.
 *
 * A surprising number of things can go wrong and there are a lot of adhoc rules in this class and probably valid
 * shapes are rejected.  It's well tested but wouldn't be shocked if it contains bugs that are compensated for else
 * where in the code.
 *
 * @author Peter Abeles
 */
public class FitLinesToContour {

	// maximum number of samples along a line.  After a certain point little is gained by  sampling all of those
	// points and it becomes very computationally expensive
	int maxSamples = 20;

	// number of iterations it will perform before giving up
	int maxIterations = 5;

	// minimum number of pixels a line must have for it to be fit
	int minimumLineLength = 4;

	// reference to the list of contour pixels
	List<Point2D_I32> contour;

	// storage for working space
	FastQueue<LineGeneral2D_F64> lines = new FastQueue<>(LineGeneral2D_F64.class, true);
	FastQueue<Point2D_F64> pointsFit = new FastQueue<>(Point2D_F64.class, true);

	private LinePolar2D_F64 linePolar = new LinePolar2D_F64();

	private Point2D_F64 intersection = new Point2D_F64();

	private GrowQueue_I32 workCorners = new GrowQueue_I32();

	int anchor0;
	int anchor1;

	boolean verbose = false;

	public void setContour(List<Point2D_I32> contour) {
		this.contour = contour;
	}

	/**
	 * Fits line segments along the contour with the first and last corner fixed at the original corners.  The output
	 * will be a new set of corner indexes.  Since the corner list is circular, it is assumed that anchor1 comes after
	 * anchor0.  The same index can be specified for an anchor, it will just go around the entire circle
	 *
	 * @param anchor0 corner index of the first end point
	 * @param anchor1 corner index of the second end point.
	 * @param corners Initial location of the corners
	 * @param output Optimized location of the corners
	 */
	public boolean fitAnchored( int anchor0 , int anchor1 , GrowQueue_I32 corners , GrowQueue_I32 output )
	{
		this.anchor0 = anchor0;
		this.anchor1 = anchor1;

		int numLines = anchor0==anchor1? corners.size() : CircularIndex.distanceP(anchor0,anchor1,corners.size);
		if( numLines < 2 ) {
			throw new RuntimeException("The one line is anchored and can't be optimized");
		}

		lines.resize(numLines);

		if( verbose ) System.out.println("ENTER FitLinesToContour");

		// Check pre-condition
//		checkDuplicateCorner(corners);

		workCorners.setTo(corners);

		for( int iteration = 0; iteration < maxIterations; iteration++ ) {
			// fit the lines to the contour using only lines between each corner for each line
			if( !fitLinesUsingCorners( numLines,workCorners) ) {
				return false;
			}

			// intersect each line and find the closest point on the contour as the new corner
			if( !linesIntoCorners(numLines, workCorners) ) {
				return false;
			}

			// sanity check to see if corner order is still met
			if( !sanityCheckCornerOrder(numLines, workCorners) ) {
				return false; // TODO detect and handle this condition better
			}

			// TODO check for convergence
		}

		if( verbose ) System.out.println("EXIT FitLinesToContour. "+corners.size()+"  "+workCorners.size());
		output.setTo(workCorners);
		return true;
	}

//	/**
//	 * Throws an exception of two corners in a row are duplicates
//	 */
//	private void checkDuplicateCorner(GrowQueue_I32 corners) {
//		for (int i = 0; i < corners.size();) {
//			int j = (i+1)%corners.size();
//			int index0 = corners.get(i);
//			int index1 = corners.get(j);
//
//			Point2D_I32 a = contour.get(index0);
//			Point2D_I32 b = contour.get(index1);
//
//			if( a.x == b.x && a.y == b.y ) {
//				throw new RuntimeException("Duplicate corner!");
//			} else {
//				i++;
//			}
//		}
//	}

	/**
	 * All the corners should be in increasing order from the first anchor.
	 */
	boolean sanityCheckCornerOrder( int numLines, GrowQueue_I32 corners ) {
		int contourAnchor0 = corners.get(anchor0);
		int previous = 0;
		for (int i = 1; i < numLines; i++) {
			int contourIndex = corners.get(CircularIndex.addOffset(anchor0, i, corners.size()));
			int pixelsFromAnchor0 = CircularIndex.distanceP(contourAnchor0, contourIndex, contour.size());

			if (pixelsFromAnchor0 < previous) {
				return false;
			} else {
				previous = pixelsFromAnchor0;
			}
		}
		return true;
	}

	GrowQueue_I32 skippedCorners = new GrowQueue_I32();
	/**
	 * finds the intersection of a line and update the corner index
	 */
	boolean linesIntoCorners( int numLines, GrowQueue_I32 contourCorners ) {

		skippedCorners.reset();

		// this is the index in the contour of the previous corner.  When a new corner is found this is used
		// to see if the newly fit lines point to the same corner.  If that happens a corner is "skipped"
		int contourIndexPrevious = contourCorners.get(anchor0);
		for (int i = 1; i < numLines; i++) {
			LineGeneral2D_F64 line0 = lines.get(i - 1);
			LineGeneral2D_F64 line1 = lines.get(i);

			int cornerIndex = CircularIndex.addOffset(anchor0, i, contourCorners.size);
			boolean skipped = false;

//			System.out.println("  corner index "+cornerIndex);

			if (null == Intersection2D_F64.intersection(line0, line1, intersection)) {
				if( verbose ) System.out.println("  SKIPPING no intersection");
				// the two lines are parallel (or a bug earlier inserted NaN), so skip and remove one of them
				skipped = true;
			} else {

				int contourIndex = closestPoint(intersection);
				if( contourIndex != contourIndexPrevious ) {

					Point2D_I32 a = contour.get(contourIndexPrevious);
					Point2D_I32 b = contour.get(contourIndex);

					if( a.x == b.x && a.y == b.y ) {
						if( verbose ) System.out.println("  SKIPPING duplicate coordinate");
//						System.out.println("  duplicate "+a+" "+b);
						skipped = true;
					} else {
//						System.out.println("contourCorners[ "+cornerIndex+" ] = "+contourIndex);
						contourCorners.set(cornerIndex, contourIndex);
						contourIndexPrevious = contourIndex;
					}
				} else {
					if( verbose ) System.out.println("  SKIPPING duplicate corner index");
					skipped = true;
				}
			}

			if( skipped ) {
				skippedCorners.add( cornerIndex );
			}

		}
		// check the last anchor to see if there's a duplicate
		int cornerIndex = CircularIndex.addOffset(anchor0, numLines, contourCorners.size);
		Point2D_I32 a = contour.get(contourIndexPrevious);
		Point2D_I32 b = contour.get(contourCorners.get(cornerIndex));
		if( a.x == b.x && a.y == b.y ) {
			skippedCorners.add( cornerIndex );
		}

		// now handle all the skipped corners
		Arrays.sort(skippedCorners.data,0,skippedCorners.size);

		for (int i = skippedCorners.size-1; i >= 0; i--) {
			int index = skippedCorners.get(i);
			contourCorners.remove(index);

			if( anchor0 >= index ) {
				anchor0--;
			}
			if( anchor1 >= index ) {
				anchor1--;
			}
		}
//		cornerIndexes.size -= skippedCorners.size();

		numLines -= skippedCorners.size;
		for (int i = 0; i < numLines; i++) {
			int c0 = CircularIndex.addOffset(anchor0, i, contourCorners.size);
			int c1 = CircularIndex.addOffset(anchor0, i+1, contourCorners.size);
			a = contour.get(contourCorners.get(c0));
			b = contour.get(contourCorners.get(c1));

			if( a.x == b.x && a.y == b.y ) {
				throw new RuntimeException("Well I screwed up");
			}

		}

		return contourCorners.size()>=3;
	}

	/**
	 * Fits lines across the sequence of corners
	 *
	 * @param numLines number of lines it will fit
	 */
	boolean fitLinesUsingCorners( int numLines , GrowQueue_I32 cornerIndexes) {
		for (int i = 1; i <= numLines; i++) {
			int index0 = cornerIndexes.get(CircularIndex.addOffset(anchor0, i - 1, cornerIndexes.size));
			int index1 = cornerIndexes.get(CircularIndex.addOffset(anchor0, i, cornerIndexes.size));

			if( index0 == index1 )
				return false;

			if (!fitLine(index0, index1, lines.get(i - 1))) {
				// TODO do something more intelligent here.  Just leave the corners as is?
				return false;
			}
			LineGeneral2D_F64 l = lines.get(i-1);
			if( Double.isNaN(l.A) || Double.isNaN(l.B) || Double.isNaN(l.C)) {
				throw new RuntimeException("This should be impossible");
			}
		}
		return true;
	}

	/**
	 * Given a sequence of points on the contour find the best fit line.
	 *
	 * @param contourIndex0 contour index of first point in the sequence
	 * @param contourIndex1 contour index of last point (exclusive) in the sequence
	 * @param line storage for the found line
	 * @return true if successful or false if it failed
	 */
	boolean fitLine( int contourIndex0 , int contourIndex1 , LineGeneral2D_F64 line ) {
		int numPixels = CircularIndex.distanceP(contourIndex0,contourIndex1,contour.size());

		// if its too small
		if( numPixels < minimumLineLength )
			return false;

		Point2D_I32 c0 = contour.get(contourIndex0);
		Point2D_I32 c1 = contour.get(contourIndex1);

		double scale = c0.distance(c1);
		double centerX = (c1.x+c0.x)/2.0;
		double centerY = (c1.y+c0.y)/2.0;

		int numSamples = Math.min(maxSamples,numPixels);

		pointsFit.reset();
		for (int i = 0; i < numSamples; i++) {

			int index = i*(numPixels-1)/(numSamples-1);

			Point2D_I32 c = contour.get( CircularIndex.addOffset(contourIndex0,index,contour.size()));

			Point2D_F64 p = pointsFit.grow();
			p.x = (c.x-centerX)/scale;
			p.y = (c.y-centerY)/scale;
		}

		if( null == FitLine_F64.polar(pointsFit.toList(),linePolar) ) {
			return false;
		}
		UtilLine2D_F64.convert(linePolar,line);

		// go from local coordinates into global
		line.C = scale*line.C - centerX*line.A - centerY*line.B;

		return true;
	}

	/**
	 * Returns the closest point on the contour to the provided point in space
	 * @return index of closest point
	 */
	int closestPoint( Point2D_F64 target ) {
		double bestDistance = Double.MAX_VALUE;
		int bestIndex = -1;
		for (int i = 0; i < contour.size(); i++) {
			Point2D_I32 c = contour.get(i);

			double d = UtilPoint2D_F64.distanceSq(target.x,target.y,c.x,c.y);
			if( d < bestDistance ) {
				bestDistance = d;
				bestIndex = i;
			}
		}
		return bestIndex;
	}
}
