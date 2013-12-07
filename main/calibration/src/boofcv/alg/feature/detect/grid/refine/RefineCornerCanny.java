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

package boofcv.alg.feature.detect.grid.refine;

import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.feature.detect.edge.EdgeSegment;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.struct.image.ImageFloat32;
import georegression.geometry.UtilPoint2D_F64;
import georegression.metric.Distance2D_F64;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedMinimization;
import org.ddogleg.optimization.UtilOptimize;
import org.ddogleg.optimization.functions.FunctionNtoS;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Refines the corner estimate by detecting the square's contour using a canny edge detector.
 * The corner point is then approximately detected and refined using non-linear optimization.
 *
 * @author Peter Abeles
 */
public class RefineCornerCanny {

	// structures used to refine pixel estimate to sub-pixel accuracy
	private CostFunction func = new CostFunction();
	private UnconstrainedMinimization alg = FactoryOptimization.unconstrained();

	CannyEdge<ImageFloat32,ImageFloat32> detectEdge;

	Point2D_F64 corner;

	// stores points in the boundary region
	private FastQueue<Point2D_F64> points = new FastQueue<Point2D_F64>(100,Point2D_F64.class,true);

	InitialEstimate initial = new InitialEstimate();

	public RefineCornerCanny() {
		detectEdge = FactoryEdgeDetectors.canny(2,true, true, ImageFloat32.class, ImageFloat32.class);
	}

	public void process( ImageFloat32 image ) {
		detectEdge.process(image,0.1f, 0.3f,null);

		List<EdgeContour> edges = detectEdge.getContours();


		List<Point2D_I32> all = new ArrayList<Point2D_I32>();
		
		points.reset();
		for( EdgeContour e : edges )
			for( EdgeSegment l : e.segments )
				for( Point2D_I32 p : l.points ) {
//				binary.set(p.x,p.y,1);
					all.add(p);
					points.grow().set(p.x,p.y);
				}

//		UtilImageIO.print(image);
//		binary.print();
//		System.out.println("---------------");

		// select optimization parameters
		selectEdgeParam(all,image.width,image.height);
		selectCornerParam(all);

		// optimize
		corner = optimizeFit();
	}

	/**
	 * Selects initial corner parameter as two points on image edge
	 */
	private void selectEdgeParam( List<Point2D_I32> all , int w , int h ) {

		// find points on edge
		List<Point2D_I32> onEdge = new ArrayList<Point2D_I32>();

		for( Point2D_I32 p : all  ) {
			if( p.x == 0 || p.x == w-1 || p.y == 0 || p.y == h-1) {
				onEdge.add(p);
			}
		}

		// find the two points which are farthest part
		int bestDistance = -1;
		Point2D_I32 bestA = null;
		Point2D_I32 bestB = null;
		for( int i = 0; i < onEdge.size(); i++ ) {
			Point2D_I32 first = onEdge.get(i);
			for( int j = i+1; j < onEdge.size(); j++ ) {
				Point2D_I32 second = onEdge.get(j);
				int distance = first.distance2(second);
				if( distance > bestDistance ) {
					bestDistance = distance;
					bestA = first;
					bestB = second;
				}
			}
		}

		if( bestDistance == -1 )
			throw new RuntimeException("Something went very wrong!");

		initial.sideA = bestA;
		initial.sideB = bestB;
	}

	/**
	 * Initial estimate of the corner location is selected to be the point farthest away from the
	 * two points select on the image's edge
	 */
	@SuppressWarnings("ConstantConditions")
	private void selectCornerParam( List<Point2D_I32> all ) {
		Point2D_I32 a = initial.sideA;
		Point2D_I32 b = initial.sideB;

		Point2D_I32 bestPoint = null;
		double bestDist = -1;

		for( int i = 0; i < all.size(); i++ ) {
			Point2D_I32 p = all.get(i);

			double distA = UtilPoint2D_F64.distance(a.x, a.y, p.x, p.y);
			double distB = UtilPoint2D_F64.distance(b.x,b.y, p.x,p.y);

			double sum = distA+distB;

			if( sum >= bestDist ) {
				bestDist = sum;
				bestPoint = p;
			}
		}

		initial.corner = new Point2D_F64(bestPoint.x,bestPoint.y);
	}

	/**
	 * Given the initial estimate of the corner parameters, perform non-linear estimation to
	 * find the best fit corner
	 *
	 * @return best corner
	 */
	private Point2D_F64 optimizeFit() {

		double param[] = new double[4];
		param[0] = initial.corner.x;
		param[1] = initial.corner.y;
		param[2] = Math.atan2(initial.sideA.y-initial.corner.y,initial.sideA.x-initial.corner.x);
		param[3] = Math.atan2(initial.sideB.y-initial.corner.y,initial.sideB.x-initial.corner.x);

		alg.setFunction(func,null,0);
		alg.initialize(param,0,1e-8);

		if( !UtilOptimize.process(alg, 500) ) {
//			throw new InvalidCalibrationTarget("Minimization failed?!? "+alg.getWarning());
		}

//		System.out.println("Error "+alg.getFunctionValue());
//		if( alg.getFunctionValue() > 8 )
//			System.out.println("EGH");
		
		double found[] = alg.getParameters();

		return new Point2D_F64(found[0],found[1]);
	}

	/**
	 * Structure containing initial corner estimate
	 */
	private static class InitialEstimate
	{
		Point2D_I32 sideA;
		Point2D_I32 sideB;
		Point2D_F64 corner;
	}

	/**
	 * Cost function which computes the cost as the sum of distances between the set of points
	 * and the corner.  Distance from a point to the corner is defined as the minimum distance of
	 * a point from the two lines.
	 */
	private class CostFunction implements FunctionNtoS
	{
		LineParametric2D_F64 lineA = new LineParametric2D_F64();
		LineParametric2D_F64 lineB = new LineParametric2D_F64();

		@Override
		public int getNumOfInputsN() {
			return 4;
		}

		@Override
		public double process(double[] input) {
			double x = input[0];
			double y = input[1];
			double thetaA = input[2];
			double thetaB = input[3];

			lineA.p.set(x,y);
			lineB.p.set(x, y);

			lineA.slope.set(Math.cos(thetaA),Math.sin(thetaA));
			lineB.slope.set(Math.cos(thetaB),Math.sin(thetaB));

			double cost = 0;

			for( int i = 0; i < points.size; i++ ) {

				Point2D_F64 p = points.get(i);

				double distA = Distance2D_F64.distanceSq(lineA, p);
				double distB = Distance2D_F64.distanceSq(lineB, p);

				double distMin = Math.min(distA,distB);

				// there can be stray islands which are hard to filter out, but should be ignored
				// They tend to not be too close to the desired edges, but setting the cost of
				// far away pixels to zero causes instability
				if( distMin < 4 )
					cost += distMin;
				else
					cost += 4;
			}

			return cost;
		}
	}

	public Point2D_F64 getCorner() {
		return corner;
	}
}
