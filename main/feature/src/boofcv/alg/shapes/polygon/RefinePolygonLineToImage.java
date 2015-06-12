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

import boofcv.alg.shapes.edge.SnapToEdge;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

/**
 * <p>
 * Improves the fits of a polygon's which is darker or lighter than the background. Polygon's edges are
 * assumed to be perfectly straight lines.  The edges are processed individually and fit to a line using weighted
 * regression. Both black squares with white backgrounds and white shapes with black backgrounds can be found.
 * The edges are selected to maximize the difference between light and dark regions.
 * </p>
 * <p>
 * For example, assume an image axis aligned rectangle has a lower extent of 1,2 and a upper extent of 12,15, is
 * entirely filled, excluding the upper extent (as is typical).  Then the found lower and upper extends of the
 * found polygon will also be 1,2 and 12,15.
 * </p>
 *
 * <p>For input polygons which are in undistorted coordinates by with a distorted image call {@link #getSnapToEdge()}
 * and invoke {@link SnapToEdge#setTransform(int, int, PixelTransform_F32)}.</p>
 *
 * @author Peter Abeles
 */
public class RefinePolygonLineToImage<T extends ImageSingleBand> {

	// How far away from a corner will it sample the line
	double cornerOffset = 2.0;

	// maximum number of iterations
	private int maxIterations = 10;

	// convergence tolerance in pixels
	private double convergeTolPixels = 0.01;

	// determines if it's fitting to a darker or lighter polygon
	private boolean fitBlack = true;

	private SnapToEdge<T> snapToEdge;

	//---------- storage for local work space
	private LineGeneral2D_F64 general[]; // estimated line for each side
	private Polygon2D_F64 previous;
	// adjusted corner points which have been offset from the true corners
	private Point2D_F64 adjA = new Point2D_F64();
	private Point2D_F64 adjB = new Point2D_F64();

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
		this.maxIterations = maxIterations;
		this.convergeTolPixels = convergeTolPixels;
		this.snapToEdge = new SnapToEdge<T>(lineSamples,sampleRadius,imageType);
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
		this.imageType = imageType;
		this.snapToEdge = new SnapToEdge<T>(20,1,imageType);
		setup(fitBlack);
	}

	/**
	 * Declares data structures
	 */
	private void setup(boolean fitBlack) {
		this.fitBlack = fitBlack;
		general = new LineGeneral2D_F64[4];
		for (int i = 0; i < general.length; i++) {
			general[i] = new LineGeneral2D_F64();
		}
	}

	/**
	 * Sets the image which is going to be processed.  If a transform is to be used
	 * {@link SnapToEdge#setTransform} should be called before this.
	 */
	public void setImage(T image) {
		this.image = image;
		this.snapToEdge.setImage(image);
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
	 * Fits a line defined by the two points. When fitting the line the weight of the edge is used to determine
	 * how influential the point is
	 * @param a Corner point in image coordinates.
	 * @param b Corner point in image coordinates.
	 * @param found (output) Line in image coordinates
	 * @return true if successful or false if it failed
	 */
	protected boolean optimize( Point2D_F64 a , Point2D_F64 b , LineGeneral2D_F64 found ) {

		double slopeX = (b.x - a.x);
		double slopeY = (b.y - a.y);
		double r = Math.sqrt(slopeX*slopeX + slopeY*slopeY);
		// vector of unit length pointing in direction of the slope
		double unitX = slopeX/r;
		double unitY = slopeY/r;

		// offset from corner because the gradient because unstable around there
		adjA.x = a.x + unitX*cornerOffset;
		adjA.y = a.y + unitY*cornerOffset;
		adjB.x = b.x - unitX*cornerOffset;
		adjB.y = b.y - unitY*cornerOffset;

		if( fitBlack )
			return snapToEdge.refine(adjA,adjB,found);
		else
			return snapToEdge.refine(adjB,adjA,found);
	}

	public SnapToEdge<T> getSnapToEdge() {
		return snapToEdge;
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
		return fitBlack ;
	}
}
