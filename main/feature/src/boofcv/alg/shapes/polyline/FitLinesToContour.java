/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import java.util.List;

/**
 * @author Peter Abeles
 */
public class FitLinesToContour {


	int maxIterations = 5;

	List<Point2D_I32> contour;

	FastQueue<LineGeneral2D_F64> lines = new FastQueue<LineGeneral2D_F64>(LineGeneral2D_F64.class,true);
	FastQueue<Point2D_F64> pointsFit = new FastQueue<Point2D_F64>(Point2D_F64.class,true);

	LinePolar2D_F64 linePolar = new LinePolar2D_F64();

	Point2D_F64 intersection = new Point2D_F64();

	GrowQueue_I32 workCorners = new GrowQueue_I32();

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
	public void fitAnchored( int anchor0 , int anchor1 , GrowQueue_I32 corners , GrowQueue_I32 output )
	{
		int numLines = anchor0==anchor1? corners.size() : CircularIndex.distanceP(anchor0,anchor1,corners.size);
		if( numLines < 2 ) {
			throw new RuntimeException("The one line is anchored");
		}

		int contourAnchor0 = corners.get(anchor0);

		workCorners.setTo(corners);

		for( int iteration = 0; iteration < maxIterations; iteration++ ) {
			// fit the lines to the contour using only lines between each corner for each line
			fitLinesUsingCorners(anchor0, numLines);

			// intersect each line and find the closest point on the contour as the new corner
			linesIntoCorners(anchor0, numLines);

			// sanity check to see if corner order is still met
			sanityCheckCornerOrder(anchor0, numLines, contourAnchor0);

			// TODO check for convergence
		}

		output.setTo(workCorners);
	}

	void sanityCheckCornerOrder(int anchor0, int numLines, int contourAnchor0) {
		int previous = 0;
		for (int i = 1; i <= numLines; i++) {
			int contourIndex = workCorners.get(CircularIndex.addOffset(anchor0, i, workCorners.size()));
			int pixelsFromAnchor0 = CircularIndex.distanceP(contourAnchor0, contourIndex, contour.size());

			if (pixelsFromAnchor0 < previous) {
				throw new RuntimeException("Do something here");
			} else {
				previous = pixelsFromAnchor0;
			}
		}
	}

	void linesIntoCorners(int anchor0, int numLines) {
		for (int i = 1; i < numLines; i++) {
			LineGeneral2D_F64 line0 = lines.get(i - 1);
			LineGeneral2D_F64 line1 = lines.get(i);

			if (null == Intersection2D_F64.intersection(line0, line1, intersection)) {
				throw new RuntimeException("Do something here");
			}

			int contourIndex = closestPoint(intersection);

			int cornerIndex = CircularIndex.addOffset(anchor0, i, workCorners.size);
			workCorners.set(contourIndex, cornerIndex);
		}
	}

	void fitLinesUsingCorners(int anchor0, int numLines) {
		for (int i = 1; i <= numLines; i++) {
			int index0 = workCorners.get(CircularIndex.addOffset(anchor0, i - 1, workCorners.size));
			int index1 = workCorners.get(CircularIndex.addOffset(anchor0, i, workCorners.size));

			if (!fitLine(workCorners.get(index0), workCorners.get(index1), lines.get(i - 1))) {
				throw new RuntimeException("Do something here");
			}
		}
	}

	boolean fitLine( int contourIndex0 , int contourIndex1 , LineGeneral2D_F64 line ) {
		int numPixels = CircularIndex.distanceP(contourIndex0,contourIndex1,contour.size());

		Point2D_I32 c0 = contour.get(contourIndex0);
		Point2D_I32 c1 = contour.get(contourIndex1);

		double scale = c0.distance(c1);
		double centerX = (c1.x+c0.x)/2.0;
		double centerY = (c1.y+c0.y)/2.0;

		pointsFit.reset();
		for (int i = 0; i < numPixels; i++) {
			Point2D_I32 c = contour.get( CircularIndex.addOffset(contourIndex0,i,contour.size()));

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
