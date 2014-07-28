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

	double maxDistance = 6.0; // maximum distance in pixels

	int iterationsEM = 10;

	// used to select the largest segments
	QuickSortObj_F64 sorter = new QuickSortObj_F64();
	FastQueue<Segment> segments = new FastQueue<Segment>(Segment.class,true);

	FastQueue<Point> points = new FastQueue<Point>(Point.class,true);
	// lines in general notation with A*A + B*B = 1
	LineGeneral2D_F64 lines[];

	GrowQueue_F64 weights = new GrowQueue_F64();

	LineSegment2D_F64 work = new LineSegment2D_F64();
	LinePolar2D_F64 polar = new LinePolar2D_F64();

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
	 *
	 * @param contour
	 * @param corners
	 * @param output
	 * @return
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
		sorter.sort(segments.data,segments.size);

		for (int i = 0; i < 4; i++) {
			createLine(segments.get(i), contour, lines[i]);
		}

		// estimate line equations
		performLineEM(contour);

		// compute the quality of the fit and decide if it's valid or not
		// TODO write this part

		// convert from lines to quadrilateral
		convert(lines,output);

		return true;
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

		System.out.println("line0 = "+lines[0]);
		weights.resize(points.size);

		int N = iterationsEM-1;
		for (int EM = 0; EM < iterationsEM; EM++) {
			// compute the weight for each point to each line based on distance away
			computePointWeights();

			// update line equations using the points and their weights
			computeLineEquations();
			System.out.println("line0 = "+lines[0]);
		}
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

	/**
	 * If A*A + B*B == 1 then a simplified distance formula can be used
	 */
	private static double distance( LineGeneral2D_F64 line , Point2D_F64 p ) {
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
	 */
	protected static void convert( LineGeneral2D_F64[] lines , Quadrilateral_F64 quad ) {

		LineGeneral2D_F64 l0 = lines[0];
		LineGeneral2D_F64 l1=null,l2=null,l3=null;

		double angle1 = angle(lines[0],lines[1]);
		double angle2 = angle(lines[0],lines[2]);
		double angle3 = angle(lines[0],lines[3]);

		if( angle1 > angle2 ) {
			l1 = lines[1];
			if( angle2 > angle3 ) {
				l2 = lines[2];
				l3 = lines[3];
			} else {
				l3 = lines[2];
				l2 = lines[3];
			}
		} else {
			l1 = lines[2];

			if( angle1 > angle3 ) {
				l2 = lines[1];
				l3 = lines[3];
			} else {
				l3 = lines[1];
				l2 = lines[3];
			}
		}

		if( null == Intersection2D_F64.intersection(l0,l1,quad.a) )
			throw new RuntimeException("Oh crap");
		if( null == Intersection2D_F64.intersection(l0,l2,quad.b) )
			throw new RuntimeException("Oh crap");
		if( null == Intersection2D_F64.intersection(l3,l2,quad.c) )
			throw new RuntimeException("Oh crap");
		if( null == Intersection2D_F64.intersection(l3,l1,quad.d) )
			throw new RuntimeException("Oh crap");
	}

	protected static double angle( LineGeneral2D_F64 a , LineGeneral2D_F64 b ) {

		double la = Math.sqrt(a.A*a.A + a.B*a.B);
		double lb = Math.sqrt(b.A*b.A + b.B*b.B);

		return Math.acos((a.A*b.A + a.B*b.B)/(la*lb));
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
