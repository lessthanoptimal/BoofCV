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

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.ImageLineIntegral;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.GImageSingleBandDistorted;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageSingleBand;
import georegression.fitting.line.FitLine_F64;
import georegression.geometry.UtilLine2D_F64;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LinePolar2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.RectangleLength2D_F32;
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
	double cornerOffset = 2.0;

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
	protected ImageLineIntegral integral;
	protected GImageSingleBand integralImage;
	// the input image
	protected T image;
	Class<T> imageType;

	/**
	 * Constructor which provides full access to all parameters.  See code documents
	 * value a description of these variables.
	 *
	 * @param numSides Number of sides on the polygon
	 */
	public RefinePolygonLineToImage(int numSides,
									double cornerOffset, int lineSamples, int sampleRadius,
									int maxIterations, double convergeTolPixels, boolean fitBlack,
									Class<T> imageType ) {
		if( sampleRadius < 1 )
			throw new IllegalArgumentException("Sample radius must be >= 1 to work");

		this.cornerOffset = cornerOffset;
		this.lineSamples = lineSamples;
		this.sampleRadius = sampleRadius;
		this.maxIterations = maxIterations;
		this.convergeTolPixels = convergeTolPixels;
		this.integral = new ImageLineIntegral();
		this.integralImage = FactoryGImageSingleBand.create(imageType);
		this.imageType = imageType;

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
		this.integral = new ImageLineIntegral();
		this.integralImage = FactoryGImageSingleBand.create(imageType);
		this.imageType = imageType;
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
	 * Used to specify a transform that's applied pixel coordinates.  The bounds of the
	 * transformed coordinates MUST be the same as the input image.  Call before {@link #initialize(ImageSingleBand)}.
	 *
	 * @param width Input image width.  Used in sanity check only.
	 * @param height Input image height.  Used in sanity check only.
	 * @param transform Pixel transformation.
	 */
	public void setTransform( int width , int height , PixelTransform_F32 transform ) {
		// sanity check since I think many people will screw this up.
		RectangleLength2D_F32 rect = DistortImageOps.boundBox_F32(width, height, transform);
		float x1 = rect.x0 + rect.width;
		float y1 = rect.y0 + rect.height;

		if( rect.getX() < 0 || rect.getY() < 0 || x1 > width || y1 > height ) {
			throw new IllegalArgumentException("You failed the idiot test! RTFM! The undistorted image "+
					"must be contained by the same bounds as the input distorted image");
		}

		InterpolatePixelS<T> interpolate = FactoryInterpolation.bilinearPixelS(imageType);
		integralImage = new GImageSingleBandDistorted<T>(transform,interpolate);
	}

	/**
	 * Sets the image which is going to be processed.  If a transform is to be used
	 * {@link #setTransform} should be called before this.
	 */
	public void initialize(T image) {
		this.image = image;
		integralImage.wrap(image);
		integral.setImage(integralImage);
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
			throw new IllegalArgumentException("Unexpected number of sides in the output polygon");

		if( input.isCCW() )
			throw new IllegalArgumentException("Polygon must be in clockwise order");

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
		double minLength = cornerOffset*2 + 2;
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
				if( !optimize(a,b,general[i]) ) {
					// if it fails then the line is likely along the wrong edge (not leading into the square)
					// or in an invalid region
					return false;
				}
			}

			// Find the corners of the quadrilateral from the lines
			if( !UtilShapePolygon.convert(general,current) )
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
	 * @return true if successful or false if it failed
	 */
	protected boolean optimize( Point2D_F64 a , Point2D_F64 b , LineGeneral2D_F64 foundLocal ) {

		double slopeX = (b.x - a.x);
		double slopeY = (b.y - a.y);
		double r = Math.sqrt(slopeX*slopeX + slopeY*slopeY);
		// vector of unit length pointing in direction of the slope
		double unitX = slopeX/r;
		double unitY = slopeY/r;

		// define the line segment which points will be sampled along.
		// don't sample too close to the corner since the line because less clear there and it can screw up results
		double x0 = a.x + unitX*cornerOffset;
		double y0 = a.y + unitY*cornerOffset;

		// truncate the slope
		slopeX -= 2.0*unitX*cornerOffset;
		slopeY -= 2.0*unitY*cornerOffset;

		// normalized tangent of sample distance length
		// Two choices for tangent here.  Select the one which points to the "right" of the line,
		// which is inside of the polygon
		double tanX = unitY;
		double tanY = -unitX;

		// set up inputs into line fitting
		computePointsAndWeights(slopeX, slopeY, x0, y0, tanX, tanY);

		if( samplePts.size() >= 4 ) {
			// fit line and convert into generalized format
			if( null == FitLine_F64.polar(samplePts.toList(), weights, polar) ) {
				throw new RuntimeException("All weights were zero, bug some place");
			}
			UtilLine2D_F64.convert(polar, foundLocal);

			return true;
		} else {
			return false;
		}
	}

	/**
	 * Computes the location of points along the line and their weights
	 */
	protected void computePointsAndWeights(double slopeX, double slopeY, double x0, double y0, double tanX, double tanY) {

		int index = 0;
		samplePts.reset();
		int numSamples = sampleRadius*2+2;
		int numPts = numSamples-1;
		double widthX = numSamples*tanX;
		double widthY = numSamples*tanY;

		for (int i = 0; i < lineSamples; i++ ) {
			// find point on line and shift it over to the first sample point
			double frac = i/(double)(lineSamples -1);
			double x = x0 + slopeX*frac-widthX/2.0;
			double y = y0 + slopeY*frac-widthY/2.0;

			// Unless all the sample points are inside the image, ignore this point
			if(!integral.isInside(x, y) || !integral.isInside(x + widthX,y + widthY))
				continue;

			double sample0 = integral.compute(x, y, x + tanX, y + tanY);
			x += tanX; y += tanY;
			for (int j = 0; j < numPts; j++) {
				double sample1 = integral.compute(x, y, x + tanX, y + tanY);

				double w = Math.max(0, sign * (sample0 - sample1));
				if( w > 0 ) {
					weights[index] = w;
					samplePts.grow().set(x - center.x, y - center.y);
					index++;
				}
				x += tanX; y += tanY;
				sample0 = sample1;
			}
		}
	}

	/**
	 * Returns the expected number of sides of the input polygon
	 */
	public int getNumberOfSides() {
		return previous.size();
	}

	/**
	 * True if it is fitting black to the image
	 */
	public boolean isFitBlack() {
		return sign < 0 ;
	}
}
