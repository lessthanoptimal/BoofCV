/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.shapes.edge;

import boofcv.struct.image.ImageGray;
import georegression.fitting.line.FitLine_F64;
import georegression.geometry.UtilLine2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LinePolar2D_F64;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;

/**
 * <p>
 * Snaps a line to an edge of an object.  The refined line attempts to maximize the absolute value of the
 * difference between the left and right sides of the line.
 * </p>
 *
 * <p>
 * The algorithm works by sampling along the provided line segment.  For each point along the line it also
 * samples points tangential to it in the left and right direction.  When a point is sampled it is actually
 * the line integral between two points which are one pixel apart.  The weight is found as the absolute value of
 * difference the between two adjacent line integrals along the tangent.
 * </p>
 * <p>
 * Internally it will compute the solution in a local coordinate system to reduce numerical errors.
 * </p>
 *
 * <p>
 * DISTORTED INPUT IMAGE:  If the distortion is known it is possible to sample along a straight line in distorted
 * image space.  This can be accomplished through the use of {@link #setTransform} where the provided transform
 * goes from undistorted pixel coordinates into the distorted input image.
 * </p>
 *
 * @author Peter Abeles
 */
// TODO specify weight function.  close to A, close to B, even
public class SnapToLineEdge<T extends ImageGray>extends BaseIntegralEdge<T> {

	// maximum number of times it will sample along the line
	protected int lineSamples;

	// Determines the number of points sampled radially outwards from the line
	// Total intensity values sampled at each point along the line is radius*2+2,
	// and points added to line fitting is radius*2+1.
	protected int radialSamples;

	// storage for computed polar line
	private LinePolar2D_F64 polar = new LinePolar2D_F64();

	protected GrowQueue_F64 weights = new GrowQueue_F64();// storage for weights in line fitting
	// storage for where the points that are sampled along the line
	protected FastQueue<Point2D_F64> samplePts = new FastQueue<>(Point2D_F64.class, true);


	// storage for the line's center.  used to reduce numerical problems.
	protected Point2D_F64 center = new Point2D_F64();
	protected double localScale;

	/**
	 * Configures the algorithm.
	 *
	 * @param lineSamples Number of times it will sample along the line's axis. Try 19
	 * @param tangentialSamples Radius along the tangent of what it will sample.  Must be &ge; 1.  Try 2.
	 * @param imageType Type of image it's going to process
	 */
	public SnapToLineEdge(int lineSamples, int tangentialSamples, Class<T> imageType) {
		super(imageType);

		if( tangentialSamples < 1 )
			throw new IllegalArgumentException("Tangential samples must be >= 1 or else it won't work");

		this.lineSamples = lineSamples;
		this.radialSamples = tangentialSamples;
	}

	/**
	 * Fits a line defined by the two points. When fitting the line the weight of the edge is used to determine.
	 * how influential the point is.  Multiple calls might be required to get a perfect fit.
	 *
	 * @param a Start of line
	 * @param b End of line..
	 * @param found (output) Fitted line to the edge
	 * @return true if successful or false if it failed
	 */
	public boolean refine(Point2D_F64 a, Point2D_F64 b, LineGeneral2D_F64 found) {

		// determine the local coordinate system
		center.x = (a.x + b.x)/2.0;
		center.y = (a.y + b.y)/2.0;
		localScale = a.distance(center);

		// define the line which points are going to be sampled along
		double slopeX = (b.x - a.x);
		double slopeY = (b.y - a.y);
		double r = Math.sqrt(slopeX*slopeX + slopeY*slopeY);

		// tangent of unit length that radial sample samples are going to be along
		// Two choices for tangent here.  Select the one which points to the "right" of the line,
		// which is inside of the edge
		double tanX = slopeY/r;
		double tanY = -slopeX/r;

		// set up inputs into line fitting
		computePointsAndWeights(slopeX, slopeY, a.x, a.y, tanX, tanY);

		if( samplePts.size() >= 4 ) {
			// fit line and convert into generalized format
			if( null == FitLine_F64.polar(samplePts.toList(), weights.data, polar) ) {
				throw new RuntimeException("All weights were zero, bug some place");
			}
			UtilLine2D_F64.convert(polar, found);

			// Convert line from local to global coordinates
			localToGlobal(found);

			return true;
		} else {
			return false;
		}
	}

	/**
	 * Computes the location of points along the line and their weights
	 */
	protected void computePointsAndWeights(double slopeX, double slopeY, double x0, double y0, double tanX, double tanY) {

		samplePts.reset();
		weights.reset();
		int numSamples = radialSamples*2+2;
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

				double w = sample0 - sample1;
				if( w < 0 ) w = -w;

				if( w > 0 ) {
					weights.add(w);
					samplePts.grow().set((x - center.x) / localScale, (y - center.y) / localScale);
				}

				x += tanX; y += tanY;
				sample0 = sample1;
			}
		}
	}

	/**
	 * Converts the line from local to global image coordinates
	 */
	protected void localToGlobal( LineGeneral2D_F64 line ) {
		line.C = localScale*line.C - center.x*line.A - center.y*line.B;
	}

	public int getLineSamples() {
		return lineSamples;
	}

	public void setLineSamples(int lineSamples) {
		this.lineSamples = lineSamples;
	}

	public int getRadialSamples() {
		return radialSamples;
	}

	public void setRadialSamples(int radialSamples) {
		this.radialSamples = radialSamples;
	}

	public Class<T> getImageType() {
		return imageType;
	}

	public void setImageType(Class<T> imageType) {
		this.imageType = imageType;
	}
}
