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

import boofcv.alg.interpolate.ImageLineIntegral;
import boofcv.struct.image.ImageSingleBand;
import georegression.fitting.line.FitLine_F64;
import georegression.geometry.UtilLine2D_F64;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LinePolar2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

/**
 * <p>
 * Fits a polygon the edge of a shape inside an image when given a good initial guess. The edges of the polygon are
 * assumed to be perfectly straight lines.  The edges are processed individually and fit to a line using weighted
 * regression. Both black squares with white backgrounds and white squares with black backgrounds can be found.
 * The edges are selected such that they will contain the entire black/white shape.
 * </p>
 * <p>
 * For example, assume an image axis aligned rectangle has a lower extent of 1,2 and a upper extent of 12,15, is
 * entirely filled, excluding the upper extent (as is typical).  Then the found lower and upper extends of the
 * found polygon will also be 1,2 and 12,15.
 * <p>
 * <p>
 * The weight of each sample point is determined by computing a the line integral across the image in the tangential
 * left and right directions by a lenght of one pixel.  Then the weight is max(0,sgn*(right-left)).  This will more
 * heavily weigh pixels which lie along an edge.
 * </p>
 * <p>
 * When sampling along the line, corners are avoided since those regions don't always have a clear edge.  Points are
 * sampled starting at one end and moving towards the other.  In addition to points along the line points are also
 * sampled tangentially to the left and right.  This allows the line's angle to be corrected.
 * </p>
 *
 * @author Peter Abeles
 */
public class RefinePolygonLineToImage<T extends ImageSingleBand> {

	// How far away from a corner will it sample the line
	double lineBorder = 2.0;

	// number of times it will sample along the line
	int lineSamples = 20;

	// Determines the number of tangential points sampled at each point along the line.
	// Total intensity values sampled is radius*2+2, and points added to line fitting is radius*2+1.
	int sampleRadius = 1;

	// maximum number of iterations
	private int maxIterations = 10;

	// convergence tolerance in pixels
	private double convergeTolPixels = 0.01;

	// used to determine if it's snapping to a black (1) or white(-1) shape
	double sign;

	//---------- storage for local work space
	private LinePolar2D_F64 polar = new LinePolar2D_F64();
	private LineGeneral2D_F64 general[]; // estimated line for each side
	protected double weights[];// storage for weights in line fitting
	private Polygon2D_F64 previous;
	// storage for where the points that are sampled along the line
	protected FastQueue<Point2D_F64> samplePts;

	// local coordinate system that line estimation is found in
	protected Point2D_F64 center = new Point2D_F64();

	// used when computing the fit for a line at specific points
	protected ImageLineIntegral<T> integral;
	// the input image
	protected T image;

	/**
	 * Constructor which provides full access to all parameters.  See code documents
	 * value a description of these variables.
	 *
	 * @param numSides Number of sides on the polygon
	 */
	public RefinePolygonLineToImage(int numSides,
									double lineBorder, int lineSamples, int sampleRadius,
									int maxIterations, double convergeTolPixels, boolean fitBlack,
									Class<T> imageType ) {
		if( sampleRadius < 1 )
			throw new IllegalArgumentException("Sample radius must be >= 1 to work");

		this.lineBorder = lineBorder;
		this.lineSamples = lineSamples;
		this.sampleRadius = sampleRadius;
		this.maxIterations = maxIterations;
		this.convergeTolPixels = convergeTolPixels;
		this.integral = new ImageLineIntegral<T>(imageType);

		previous = new Polygon2D_F64(numSides);

		setup(fitBlack);
	}

	/**
	 * Simplified constructor which uses reasonable default values for most variables
	 * @param numSides Number of sides on the polygon
	 * @param fitBlack If true it's fitting a black square with a white background.  false is the inverse.
	 * @param imageType Type of input image it processes
	 */
	public RefinePolygonLineToImage(int numSides , boolean fitBlack, Class<T> imageType) {
		previous = new Polygon2D_F64(numSides);
		this.integral = new ImageLineIntegral<T>(imageType);
		setup(fitBlack);
	}

	/**
	 * Declares data structures
	 */
	private void setup(boolean fitBlack) {
		if( sampleRadius < 1 )
			throw new IllegalArgumentException("If sampleRadius < 1 it won't do anything");

		general = new LineGeneral2D_F64[4];
		for (int i = 0; i < general.length; i++) {
			general[i] = new LineGeneral2D_F64();
		}

		int totalPts = lineSamples *(2*sampleRadius+1);
		weights = new double[totalPts];

		samplePts = new FastQueue<Point2D_F64>(totalPts,Point2D_F64.class,true);

		sign = fitBlack ? 1 : -1;
	}

	/**
	 * Sets the image which is going to be processed
	 */
	public void initialize(T image) {
		this.image = image;
		integral.setImage(image);
	}

	/**
	 * Refines the fit a polygon by snapping it to the edges.
	 *
	 * @param input (input) Initial estimate for the polygon. Vertexes must be in clockwise order.
	 * @param output (output) the fitted polygon
	 */
	public boolean refine(Polygon2D_F64 input, Polygon2D_F64 output)
	{
		if( input.size() != previous.size() )
			throw new IllegalArgumentException("Unexpected number of sides in the input polygon");
		if( output.size() != previous.size() )
			throw new IllegalArgumentException("Unexpected number of sides in the input polygon");


		// sanity check input.  If it's too small this algorithm won't work
		if( checkShapeTooSmall(input) )
			return false;

		// find center to use as local coordinate system.  Improves numerics slightly
		UtilPolygons2D_F64.vertexAverage(input, center);

		// estimate line equations
		return optimize(input,output);
	}

	/**
	 * Looks at the distance between each vertex.  If that distance is so small the edge can't be measured the
	 * return true.
	 * @param input polygon
	 * @return true if too small or false if not
	 */
	private boolean checkShapeTooSmall(Polygon2D_F64 input) {
		// must be longer than the border plus some small fudge factor
		double minLength = lineBorder*2 + 2;
		for (int i = 0; i < input.size(); i++) {
			int j = (i+1)%input.size();
			Point2D_F64 a = input.get(i);
			Point2D_F64 b = input.get(j);
			if( a.distance2(b) < minLength*minLength )
				return true;
		}

		return false;
	}

	/**
	 * Refines the initial line estimates using EM.  The number of iterations is fixed.
	 */
	protected boolean optimize(Polygon2D_F64 seed , Polygon2D_F64 current ) {

		previous.set(seed);

		// pixels squares is faster to compute
		double convergeTol = convergeTolPixels*convergeTolPixels;

		for (int iteration = 0; iteration < maxIterations; iteration++) {
			// snap each line to the edge independently.  Lines will be in local coordinates
			for (int i = 0; i < previous.size(); i++) {
				int j = (i + 1) % previous.size();
				Point2D_F64 a = previous.get(i);
				Point2D_F64 b = previous.get(j);
				optimize(a,b,general[i]);
			}

			// Find the corners of the quadrilateral from the lines
			if( !convert(general,current) )
				return false;

			// convert quad from local into image coorindates
			localToImage(current);

			// see if it has converged
			boolean converged = true;
			for (int i = 0; i < current.size(); i++) {
				if( current.get(i).distance2(previous.get(i)) > convergeTol ) {
					converged = false;
					break;
				}
 			}
			if( converged ) {
//				System.out.println("Converged early at "+iteration);
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
	private void localToImage(Polygon2D_F64 current) {
		for (int i = 0; i < current.size(); i++) {
			Point2D_F64 v = current.get(i);
			v.x += center.x;
			v.y += center.y;
		}
	}

	/**
	 * Fits a line defined by the two points. When fitting the line the weight of the edge is used to determine
	 * how influential the point is
	 * @param a Corner point in image coordinates.
	 * @param b Corner point in image coordinates.
	 * @param foundLocal (output) Line in local coordinates
	 */
	protected void optimize( Point2D_F64 a , Point2D_F64 b , LineGeneral2D_F64 foundLocal ) {

		double slopeX = (b.x - a.x);
		double slopeY = (b.y - a.y);
		double r = Math.sqrt(slopeX*slopeX + slopeY*slopeY);
		// vector of unit length pointing in direction of the slope
		double unitX = slopeX/r;
		double unitY = slopeY/r;

		// define the line segment which points will be sampled along.
		// don't sample too close to the corner since the line because less clear there and it can screw up results
		double x0 = a.x + unitX*lineBorder;
		double y0 = a.y + unitY*lineBorder;

		// truncate the slope
		slopeX -= 2.0*unitX*lineBorder;
		slopeY -= 2.0*unitY*lineBorder;

		// normalized tangent of sample distance length
		double tanX = -unitY;
		double tanY = unitX;

		// set up inputs into line fitting
		computePointsAndWeights(slopeX, slopeY, x0, y0, tanX, tanY);

		if( samplePts.size() >= 4 ) {
			// fit line and convert into generalized format
			FitLine_F64.polar(samplePts.toList(), weights, polar);
			UtilLine2D_F64.convert(polar, foundLocal);
		}
	}

	/**
	 * Computes the location of points along the line and their weights
	 */
	protected void computePointsAndWeights(double slopeX, double slopeY, double x0, double y0, double tanX, double tanY) {

		int index = 0;
		samplePts.reset();
		for (int i = 0; i < lineSamples; i++ ) {
			// find point on line and shift it over to the first sample point
			double frac = i/(double)(lineSamples -1);
			double x = x0 + slopeX*frac-sampleRadius*tanX;
			double y = y0 + slopeY*frac-sampleRadius*tanY;

			// Unless all the sample points are inside the image, ignore this point
			double leftX = x - tanX;
			double leftY = y - tanY;
			double rightX = x + (sampleRadius*2+1)*tanX;
			double rightY = y + (sampleRadius*2+1)*tanY;

			if(integral.isInside(leftX, leftY) && integral.isInside(rightX,rightY)) {
				double sample0 = integral.computeInside(x, y, x - tanX, y - tanY);
				for (int j = 0; j < sampleRadius * 2 + 1; j++) {
					double sample1 = integral.computeInside(x, y, x + tanX, y + tanY);

					weights[index] = Math.max(0, sign * (sample1 - sample0));
					samplePts.grow().set(x - center.x, y - center.y);
					x += tanX;
					y += tanY;
					sample0 = sample1;
					index++;
				}
			}
		}
	}

	/**
	 * Finds the intersections between the four lines and converts it into a quadrilateral
	 *
	 * @param lines Assumes lines are ordered
	 */
	protected static boolean convert( LineGeneral2D_F64[] lines , Polygon2D_F64 poly ) {

		for (int i = 0; i < poly.size(); i++) {
			int j = (i + 1) % poly.size();
			if( null == Intersection2D_F64.intersection(lines[i],lines[j],poly.get(j)) )
				return false;
		}

		return true;
	}

	/**
	 * Returns the expected number of sides of the input polygon
	 */
	public int getNumberOfSides() {
		return previous.size();
	}

	/**
	 * True if it is fitting black to the image
	 * @return
	 */
	public boolean isFitBlack() {
		return sign < 0 ;
	}
}
