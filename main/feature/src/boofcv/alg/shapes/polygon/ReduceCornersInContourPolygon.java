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

package boofcv.alg.shapes.polygon;

import georegression.geometry.UtilLine2D_F64;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Distance2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.sorting.QuickSortObj_F64;
import org.ddogleg.sorting.SortableParameter_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Given a polygon which has been fit a contour it will attempt to fit a polygon of lower dimention
 * to the contour.  The corners in the polygon will be in clockwise order.  The output polygon is rejected
 * its shape deviates from the contour by too much.
 *
 * @author Peter Abeles
 */
public class ReduceCornersInContourPolygon {

	// Maximum allowed distance of a point on the contour from the polygon
	private double maximumDistance;

	// should the output be clockwise or counter-clockwise
	boolean clockWise;

	private QuickSortObj_F64 sorterLength = new QuickSortObj_F64();
	private FastQueue<Segment> segments = new FastQueue<Segment>(Segment.class,true);

	private LineGeneral2D_F64 lines[];
	LineSegment2D_F64 lineSegment = new LineSegment2D_F64();

	private Polygon2D_F64 output = new Polygon2D_F64();

	// local storage
	Point2D_F64 tmp = new Point2D_F64();
	LineSegment2D_F64 storage = new LineSegment2D_F64();

	/**
	 * Specifies the shape, tolerances, and properties of the shape
	 *
	 * @param numSides Number of sides in the polygon
	 * @param maximumDistance Maximum allowed distance a contour point will have from the polygon
	 * @param clockWise If true the output will be in a clockwise direction, otherwise counter clockwise
	 */
	public ReduceCornersInContourPolygon(int numSides, double maximumDistance, boolean clockWise) {
		if( numSides < 3 )
			throw new IllegalArgumentException("There must be at least 3 sides");

		this.maximumDistance = maximumDistance;
		this.clockWise = clockWise;

		output = new Polygon2D_F64(numSides);

		lines = new LineGeneral2D_F64[numSides];
		for (int i = 0; i < lines.length; i++) {
			lines[i] = new LineGeneral2D_F64();
		}
	}

	/**
	 * Given the contour and a set of vertexes for a polygon as an initial estimate
	 * fit a quadrilateral to the data.
	 *
	 * @param contour List of pixels along the shape's contour
	 * @param corners Indexes of pixels which were selected to be vertexes in a polygon.
	 *                These
	 * @return true if successful or false if not
	 */
	public boolean process(List<Point2D_I32> contour, GrowQueue_I32 corners) {
		// pick the 4 largest segments to act as the initial seeds
		segments.reset();
		for (int i = 0; i < corners.size; i++) {
			int next = (i+1)%corners.size;
			segments.grow().set(corners.get(i),corners.get(next),contour.size());
		}
		sorterLength.sort(segments.data, segments.size);

		// Put the lines in contour order.  this can be cw or ccw
		bubbleSortLines(segments);

		// Find the corners by intersecting the lines
		for (int i = 0; i < 4; i++) {
			createLine(segments.get(i), contour,lines[i]);
		}
		for (int i = 0; i < output.size(); i++) {
			Intersection2D_F64.intersection(lines[i],lines[(i+1)%(output.size())],output.vertexes.data[i]);
		}

		// make sure it's not too different from the original polygon
		if( !checkPolygonCornerDistance(output,contour,corners)) {
			return false;
		}

		// see if it is in the correct order
		if( UtilPolygons2D_F64.isCCW(output.vertexes.toList()) != !clockWise ) {
			UtilPolygons2D_F64.reverseOrder(output);
		}

		return true;
	}

	/**
	 * Returns the found quadrilateral.  vertexes will be in clock-wise order
	 */
	public Polygon2D_F64 getOutput() {
		return output;
	}

	/**
	 * Performs bubble sort on the first 4 lines to ensure they are in a contour order
	 */
	protected static void bubbleSortLines( FastQueue<Segment> segments ) {
		for (int i = 0; i < 4; i++) {
			int bestValue = segments.get(i).index0;
			int bestIndex = i;
			for (int j = i+1; j < 4; j++) {
				Segment b = segments.get(j);
				if( b.index0 < bestValue ) {
					bestIndex = j;
					bestValue = b.index0;
				}
			}
			if( bestIndex != i ) {
				Segment tmp = segments.data[i];
				segments.data[i] = segments.data[bestIndex];
				segments.data[bestIndex] = tmp;
			}
		}
	}

	/**
	 * Given segment information create a line in general notation which has been normalized
	 */
	private void createLine( Segment segment , List<Point2D_I32> contour , LineGeneral2D_F64 line )
	{
		Point2D_I32 p0 = contour.get(segment.index0);
		Point2D_I32 p1 = contour.get(segment.index1);

		lineSegment.a.set(p0.x, p0.y);
		lineSegment.b.set(p1.x, p1.y);

		UtilLine2D_F64.convert(lineSegment, line);

		// ensure A*A + B*B = 1
		line.normalize();
	}

	/**
	 * Sees of the vertexes in the original polygon are too far away from the new polygon
	 *
	 * @return true if within tolerance and false if not
	 */
	protected boolean checkPolygonCornerDistance(Polygon2D_F64 poly,
												 List<Point2D_I32> contour, GrowQueue_I32 corners)
	{
		double thresh2 = maximumDistance*maximumDistance;
		for (int i = 0; i < corners.size(); i++) {
			Point2D_I32 p = contour.get(corners.get(i));
			tmp.set(p.x,p.y);
			double d2 = Distance2D_F64.distanceSq(poly,tmp,storage);

			if( d2 > thresh2 )
				return false;
		}
		return true;
	}

	public static class Segment extends SortableParameter_F64 {

		public int index0;
		public int index1;

		public void set(int index0, int index1 , int contourSize ) {
			this.index0 = index0;
			this.index1 = index1;

			sortValue = index1 > index0 ? index1-index0 : contourSize-index0 + index1;
			sortValue = -sortValue; // so that larger have small indices after sorting
		}
	}

	/**
	 * Returns the number of side it's reducing a polygon to.
	 */
	public int getNumberOfSides() {
		return output.size();
	}

	public double getMaximumDistance() {
		return maximumDistance;
	}

	public void setMaximumDistance(double maximumDistance) {
		this.maximumDistance = maximumDistance;
	}

	public boolean isClockWise() {
		return clockWise;
	}

	public void setClockWise(boolean clockWise) {
		this.clockWise = clockWise;
	}
}
