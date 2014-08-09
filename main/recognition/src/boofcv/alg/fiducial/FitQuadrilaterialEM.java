/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial;

import georegression.fitting.line.FitLine_F64;
import georegression.geometry.UtilLine2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LinePolar2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ddogleg.sorting.QuickSortObj_F64;
import org.ddogleg.sorting.SortableParameter_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Uses expectation maximisation to fit a quadrilateral to a contour given an initial set
 * of corner candidates.
 *
 * @author Peter Abeles
 */
public class FitQuadrilaterialEM {

	// maximum distance in pixels away a point can be from the line for it to have a weight
	private double maxDistance = 6.0;

	// maximum number of EM iterations
	private int iterationsEM = 10;

	// used to select the largest segments
	private QuickSortObj_F64 sorterLength = new QuickSortObj_F64();
	private FastQueue<Segment> segments = new FastQueue<Segment>(Segment.class,true);

	private FastQueue<Point> points = new FastQueue<Point>(Point.class,true);
	// lines in general notation with A*A + B*B = 1
	protected LineGeneral2D_F64 lines[];

	// storage for weights provided to line fitter
	private GrowQueue_F64 weights = new GrowQueue_F64();

	// local storage
	private LineSegment2D_F64 work = new LineSegment2D_F64();
	private LinePolar2D_F64 polar = new LinePolar2D_F64();

	public FitQuadrilaterialEM(double maxDistance, int iterationsEM) {
		this();
		this.maxDistance = maxDistance;
		this.iterationsEM = iterationsEM;
	}

	public FitQuadrilaterialEM() {
		lines = new LineGeneral2D_F64[4];
		for (int i = 0; i < lines.length; i++) {
			lines[i] = new LineGeneral2D_F64();
		}
	}

	/**
	 * Fits a quadrilateral to the contour given an initial set of candidate corners
	 *
	 * @param contour Contours around the shape
	 * @param corners Initial set of corners
	 * @param output the fitted quadrilateral
	 */
	public boolean fit( List<Point2D_I32> contour , GrowQueue_I32 corners ,
						Quadrilateral_F64 output )
	{
		// pick the 4 largest segments to act as the initial seeds
		segments.reset();
		for (int i = 0; i < corners.size; i++) {
			int next = (i+1)%corners.size;
			segments.grow().set(corners.get(i),corners.get(next),contour.size());
		}
		sorterLength.sort(segments.data, segments.size);

		// order the lines so that they can be converted into a quad later on easily
		// bubble sort below
		bubbleSortLines(segments);

		// now create the lines
		for (int i = 0; i < 4; i++) {
			createLine(segments.get(i), contour, lines[i]);
		}

		// estimate line equations
		performLineEM(contour);

		// convert from lines to quadrilateral
		return convert(lines,output);
	}

	/**
	 * Performs bubble sort on the first 4 lines to ensure they are in a circular order
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
	 * Refines the initial line estimates using EM.  The number of iterations is fixed.
	 */
	protected void performLineEM(List<Point2D_I32> contour) {
		// convert the original contour into points used for EM
		points.reset();
		for (int i = 0; i < contour.size(); i++) {
			Point2D_I32 p = contour.get(i);
			points.grow().set(p.x,p.y);
		}

//		System.out.println("line0 = "+lines[0]);
		weights.resize(points.size);

		for (int EM = 0; EM < iterationsEM; EM++) {
			// compute the weight for each point to each line based on distance away
			computePointWeights();

			// update line equations using the points and their weights
			computeLineEquations();
//			System.out.println("line0 = "+lines[0]);
		}

		// recompute the lines one last time but without the points which have ambiguous weights
		// which are near the border of two lines and skew the results
		computeLineEquationsNoAmbiguous();
	}

	private void computePointWeights() {
		for (int i = 0; i < points.size; i++) {
			Point p = points.get(i);

			double totalWeight = 0;
			for (int j = 0; j < 4; j++) {
				p.distance[j] = distance(lines[j], p);
				totalWeight += p.weight[j] = Math.max(0,1.0 - p.distance[j]/maxDistance);
			}
			if( totalWeight > 0 ) {
				for (int j = 0; j < 4; j++) {
					p.weight[j] /= totalWeight;
				}
			} else {
				for (int j = 0; j < 4; j++) {
					p.weight[j] = 0;
				}
			}
		}
	}

	private void computeLineEquations() {
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < points.size; j++) {
				weights.data[j] = points.data[j].weight[i];
			}
			FitLine_F64.polar((List) points.toList(), weights.data, polar);
			UtilLine2D_F64.convert(polar, lines[i]);
			// no need to normalize the line since A*A + B*B = 1 already
		}
	}

	private void computeLineEquationsNoAmbiguous() {
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < points.size; j++) {
				double w = points.data[j].weight[i];
				weights.data[j] = w < 0.9 ? 0 : w;
			}
			FitLine_F64.polar((List) points.toList(), weights.data, polar);
			UtilLine2D_F64.convert(polar, lines[i]);
			// no need to normalize the line since A*A + B*B = 1 already
		}
	}

	/**
	 * If A*A + B*B == 1 then a simplified distance formula can be used
	 */
	protected static double distance( LineGeneral2D_F64 line , Point2D_F64 p ) {
		return Math.abs(line.A*p.x + line.B*p.y + line.C);
	}

	/**
	 * Given segment information create a line in general notation which has been normalized
	 */
	private void createLine( Segment segment , List<Point2D_I32> contour , LineGeneral2D_F64 line )
	{
		Point2D_I32 p0 = contour.get(segment.index0);
		Point2D_I32 p1 = contour.get(segment.index1);

		work.a.set(p0.x, p0.y);
		work.b.set(p1.x, p1.y);

		UtilLine2D_F64.convert(work,line);

		// ensure A*A + B*B = 1
		line.normalize();
	}

	/**
	 * Finds the intersections between the four lines and converts it into a quadrilateral
	 *
	 * @param lines Assumes lines are ordered
	 */
	protected static boolean convert( LineGeneral2D_F64[] lines , Quadrilateral_F64 quad ) {

		if( null == Intersection2D_F64.intersection(lines[0],lines[1],quad.a) )
			return false;
		if( null == Intersection2D_F64.intersection(lines[2],lines[1],quad.b) )
			return false;
		if( null == Intersection2D_F64.intersection(lines[2],lines[3],quad.c) )
			return false;
		if( null == Intersection2D_F64.intersection(lines[0],lines[3],quad.d) )
			return false;
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

	public static class Point extends Point2D_F64 {
		// weight assigned to each line
		public double weight[] = new double[4];

		// distance it is from each line
		public double distance[] = new double[4];
	}
}
