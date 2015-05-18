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

package boofcv.alg.shapes.quad;

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
 * <p>
 * Fits a quadrilateral to an image when given a good initial guess. The edges of the quadrilateral are assumed
 * to be perfectly straight lines.  The edges are processed individually and fit to a line using weighted regression.
 * Both black squares with white backgrounds and white squares with black backgrounds can be found.
 * </p>
 * <p>
 * The weight of each sample point is determined by the intensity difference between it and a point 1 pixel to its
 * left.  Points are sampled along the line and tangentially from each of those points along the line.
 * This allows for the line's estimate to be improved.  This entire process is iterated until it converges or
 * the maximum number of iterations has been reached.
 * </p>
 * <p>
 * When sampling along the line corners are avoided since those regions don't have a clear edge.  Points are sampled
 * starting at one end and moving towards the other.  The intensity is sampled using interpolation from (radius+1) pixels
 * to the left up to radius pixels to the right.  The weight for points of distance radius to the left and right
 * is found by computing the difference for that point and one to its left.  So for a radius of 1, the intensity
 * is sampled 4 times but 3 points are added to the line fitting, for each point sampled along the line.
 * </p>
 *
 * @author Peter Abeles
 */
public class RefineQuadrilateralLineToImage<T extends ImageSingleBand> {

	// How far away from a corner will it sample the line
	float lineBorder = 2.0f;

	// number of times it will sample along the line
	int lineSamples = 20;

	// Determines the number of tangental points sampled at each point along the line.
	// Total intensity values sampled is radius*2+2, and points added to line fitting is radius*2+1.
	int sampleRadius = 1;

	// maximum number of iterations
	private int iterations = 10;

	// convergence tolerance in pixels
	private double convergeTolPixels = 0.01;

	// used to determine if it's snapping to a black (1) or white(-1) quadrilateral
	float sign;

	//---------- storage for local work space
	private LinePolar2D_F64 polar = new LinePolar2D_F64();
	private LineGeneral2D_F64 general[]; // estimated line for each side
	protected double weights[];// storage for weights in line fitting
	private Quadrilateral_F64 previous = new Quadrilateral_F64();
	// storage for where the points that are sampled along the line
	protected List<Point2D_F64> samplePts = new ArrayList<Point2D_F64>();
	// temporary storage for sample image intensity values
	float values[];

	// local coordinate system that line estimation is found in
	protected Point2D_F64 center = new Point2D_F64();

	// used to interpolate the pixel's value
	InterpolatePixelS<T> interpolate;

	/**
	 * Constructor which provides full access to all parameters.  See code documents
	 * value a description of these variables.
	 */
	public RefineQuadrilateralLineToImage(float lineBorder, int lineSamples, int sampleRadius,
										  int iterations, double convergeTolPixels, boolean fitBlack,
										  InterpolatePixelS<T> interpolate) {
		this.lineBorder = lineBorder;
		this.lineSamples = lineSamples;
		this.sampleRadius = sampleRadius;
		this.iterations = iterations;
		this.convergeTolPixels = convergeTolPixels;
		this.interpolate = interpolate;

		initialize(fitBlack);
	}

	/**
	 * Simplified constructor which uses reasonable default values for most variables
	 * @param fitBlack If true it's fitting a black square with a white background.  false is the inverse.
	 * @param interpolate Interpolation class
	 */
	public RefineQuadrilateralLineToImage(boolean fitBlack, InterpolatePixelS<T> interpolate) {
		this.interpolate = interpolate;
		initialize(fitBlack);
	}

	/**
	 * Declares data structures
	 */
	private void initialize( boolean fitBlack ) {
		if( sampleRadius < 1 )
			throw new IllegalArgumentException("If sampleRadius < 1 it won't do anything");

		general = new LineGeneral2D_F64[4];
		for (int i = 0; i < general.length; i++) {
			general[i] = new LineGeneral2D_F64();
		}

		values = new float[sampleRadius*2+2];

		int totalPts = lineSamples *(2*sampleRadius+1);
		weights = new double[totalPts];

		for (int i = 0; i < totalPts; i++) {
			samplePts.add( new Point2D_F64());
		}

		sign = fitBlack ? 1 : -1;
	}

	/**
	 * Sets the image which is going to be processed
	 */
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

		// pixels squares is faster to compute
		double convergeTol = convergeTolPixels*convergeTolPixels;

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
	protected void optimize( Point2D_F64 a , Point2D_F64 b , LineGeneral2D_F64 foundLocal ) {

		float slopeX = (float)(b.x - a.x);
		float slopeY = (float)(b.y - a.y);
		float r = (float)Math.sqrt(slopeX*slopeX + slopeY*slopeY);
		// vector of unit length pointing in direction of the slope
		float unitX = slopeX/r;
		float unitY = slopeY/r;

		// define the line segment which points will be sampled along.
		// don't sample too close to the corner since the line because less clear there and it can screw up results
		float x0 = (float)a.x + unitX*lineBorder;
		float y0 = (float)a.y + unitY*lineBorder;

		// truncate the slope
		slopeX -= 2.0f*unitX*lineBorder;
		slopeY -= 2.0f*unitY*lineBorder;

		// normalized tangent of sample distance length
		float tanX = -unitY;
		float tanY = unitX;

		// set up inputs into line fitting
		computePointsAndWeights(slopeX, slopeY, x0, y0, tanX, tanY);

		// fit line and convert into generalized format
		FitLine_F64.polar(samplePts, weights, polar);
		UtilLine2D_F64.convert(polar,foundLocal);
	}

	/**
	 * Computes the location of points along the line and their weights
	 */
	protected void computePointsAndWeights(float slopeX, float slopeY, float x0, float y0, float tanX, float tanY) {
		float centerX = (float)center.x;
		float centerY = (float)center.y;

		for (int i = 0; i < lineSamples; i++ ) {
			// find point on line and shift it over to the first sample point
			float frac = i/(float)(lineSamples -1);
			float x = x0 + slopeX*frac + (sampleRadius+1)*tanX;
			float y = y0 + slopeY*frac + (sampleRadius+1)*tanY;

			int indexPts = i*(values.length-1);

			values[0] = interpolate.get(x,y);
			for (int j = 1; j < values.length; j++) {
				x -= tanX;
				y -= tanY;

				// sample the value
				values[j] = interpolate.get(x,y);

				// add the point to the list and convert into local coordinates
				samplePts.get(indexPts+j-1).set(x-centerX,y-centerY);
			}

			// compute the weights using the difference between adjacent sample points
			// the weight should be maximized if the right sample point is inside the square
			for (int j = 0; j < values.length-1; j++) {
				weights[indexPts+j] = Math.max(0,sign*(values[j]-values[j+1]));
			}
		}
	}

	/**
	 * Finds the intersections between the four lines and converts it into a quadrilateral
	 *
	 * @param lines Assumes lines are ordered
	 */
	protected static boolean convert( LineGeneral2D_F64[] lines , Quadrilateral_F64 quad ) {

		if( null == Intersection2D_F64.intersection(lines[0],lines[1],quad.b) )
			return false;
		if( null == Intersection2D_F64.intersection(lines[2],lines[1],quad.c) )
			return false;
		if( null == Intersection2D_F64.intersection(lines[2],lines[3],quad.d) )
			return false;
		if( null == Intersection2D_F64.intersection(lines[0],lines[3],quad.a) )
			return false;
		return true;
	}
}
