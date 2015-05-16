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

package boofcv.alg.shapes.square;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.struct.image.ImageSingleBand;
import georegression.fitting.line.FitLine_F64;
import georegression.geometry.UtilLine2D_F64;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LinePolar2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Fits a quadrilateral to an image when given a good initial guess. Each line in the quadrilateral is processed
 * individually and fit to a line using weighted regression. The weight is determined by the difference of points
 * sampled on other side of the line.  This process is repeated until it converges or the maximum number of iterations
 * has been reached.
 *
 * When sampling along the line it avoid the corners since those regions don't have a clear edge.  Points are sampled
 * starting at one end and moving towards the other.  The intensity is sampled at the line then a point 1 pixels to
 * the left, which is outside of the line.  Additional points along the tangent can also be sampled.  The difference
 * in value determines the weight.
 *
 * @author Peter Abeles
 */
public class FitQuadrilateralToImage<T extends ImageSingleBand> {

	// How far away from a corner will it sample the line
	float lineBorder = 2.0f;

	// number of times it will sample along the line
	int numSamples = 20;

	// number of pixels it will sample tangent to the line
	int numTangent = 2;

	// maximum number of iterations
	private int iterations = 10;

	// used to determine if it's snapping to a black (1) or white(-1) quadrilateral
	float sign;

	//---------- storage for local work space
	private LinePolar2D_F64 polar = new LinePolar2D_F64();
	private LineGeneral2D_F64 general[]; // estimated line for each side
	private double weights[];// storage for weights in line fitting
	private Quadrilateral_F64 previous = new Quadrilateral_F64();
	// storage for where the points that are sampled along the line
	private List<Point2D_F64> samplePts = new ArrayList<Point2D_F64>();

	// local coordinate system that line estimation is found in
	private Point2D_F64 center = new Point2D_F64();

	// used to interpolate the pixel's value
	private InterpolatePixelS<T> interpolate;


	public FitQuadrilateralToImage( boolean fitBlack , InterpolatePixelS<T> interpolate) {
		this.interpolate = interpolate;

		general = new LineGeneral2D_F64[4];
		for (int i = 0; i < general.length; i++) {
			general[i] = new LineGeneral2D_F64();
		}

		weights = new double[numSamples];

		for (int i = 0; i < numSamples; i++) {
			samplePts.add( new Point2D_F64());
		}

		sign = fitBlack ? 1 : -1;
	}

	public void setImage( T image ) {
		interpolate.setImage(image);
	}

	/**
	 * Fits a quadrilateral to the contour given an initial set of candidate corners
	 *
	 * @param input (input) Initial estimate for the quadrilateral. Vertexes must be in clockwise order.
	 * @param output (output) the fitted quadrilateral
	 */
	public boolean fit( Quadrilateral_F64 input , Quadrilateral_F64 output )
	{
		// find center to use as local coordinate system.  Improves numerics slightly
		UtilPolygons2D_F64.center(input,center);

		// estimate line equations
		return optimize(input,output);
	}

	/**
	 * Refines the initial line estimates using EM.  The number of iterations is fixed.
	 */
	protected boolean optimize(Quadrilateral_F64 seed , Quadrilateral_F64 current ) {

		previous.set(seed);

		double convergeTol = 0.01*0.01;

		for (int EM = 0; EM < iterations; EM++) {
			// snap each line to the edge independently.  Lines will be in local coordinates
			optimize(previous.a,previous.b,general[0]);
			optimize(previous.b,previous.c,general[1]);
			optimize(previous.c,previous.d,general[2]);
			optimize(previous.d,previous.a,general[3]);

			// Find the corners of the quadrilateral from the lines
			if( !convert(general,current) )
				return false;

			// convert quad from local into image coorindates
			localToImage(current);

			// see if it has converged
			if( current.a.distance2(previous.a) < convergeTol &&
					current.b.distance2(previous.b) < convergeTol &&
					current.c.distance2(previous.c) < convergeTol &&
					current.d.distance2(previous.d) < convergeTol ) {
				break;
			} else {
				previous.set(current);
			}
		}

		return true;
	}

	/**
	 * Converts the quad from local coordinates back into global coordinates
	 */
	private void localToImage(Quadrilateral_F64 current) {
		current.a.x += center.x;
		current.a.y += center.y;
		current.b.x += center.x;
		current.b.y += center.y;
		current.c.x += center.x;
		current.c.y += center.y;
		current.d.x += center.x;
		current.d.y += center.y;
	}

	/**
	 * Fits a line defined by the two points. When fitting the line the weight of the edge is used to determine
	 * how influential the point is
	 * @param a Corner point in image coordinates.
	 * @param b Corner point in image coordinates.
	 * @param foundLocal (output) Line in local coordinates
	 */
	private void optimize( Point2D_F64 a , Point2D_F64 b , LineGeneral2D_F64 foundLocal ) {

		float slopeX = (float)(b.x - a.x);
		float slopeY = (float)(b.y - a.y);

		// define the line segment which points will be sampled along.
		// don't sample too close to the corner since the line because less clear there and it can screw up results
		float x0 = (float)a.x + slopeX*lineBorder;
		float y0 = (float)a.y + slopeY*lineBorder;

		// truncate the slope
		slopeX *= (1.0f-2.0f*lineBorder);
		slopeY *= (1.0f-2.0f*lineBorder);

		// normalized tangent of sample distance length
		float tanX = -slopeY;
		float tanY = slopeX;

		float r = (float)Math.sqrt(tanX*tanX + tanY*tanY);
		tanX = tanX/r;
		tanY = tanY/r;

		// set up inputs into line fitting
		computePointsAndWeights(slopeX, slopeY, x0, y0, tanX, tanY);

		// fit line and convert into generalized format
		FitLine_F64.polar(samplePts, weights, polar);
		UtilLine2D_F64.convert(polar,foundLocal);
	}

	/**
	 * Computes the location of points along the line and their weights
	 */
	private void computePointsAndWeights(float slopeX, float slopeY, float x0, float y0, float tanX, float tanY) {
		float centerX = (float)center.x;
		float centerY = (float)center.y;

		for (int i = 0; i < numSamples; i++) {
			// find point on line
			float frac = i/(float)(numSamples-1);
			float x = x0 + slopeX*frac;
			float y = y0 + slopeY*frac;

			// compute sample point one pixel to the left of the line where the color should be different
			float leftX = x + tanX;
			float leftY = y + tanY;
			float rightX = x;
			float rightY = y;

			for (int j = 0; j < numTangent; j++) {
				// sample the value
				float valueLeft = interpolate.get(leftX,leftY);
				float valueLine = interpolate.get(rightX,rightY);

				// add the point to the list and convert into local coordinates
				samplePts.get(i).set(x-centerX,y-centerY);
				weights[i] = Math.max(0,sign*(valueLeft-valueLine));

				leftX += tanX;
				leftY += tanY;
				rightX -= tanX;
				rightY -= tanY;
			}
		}
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
}
