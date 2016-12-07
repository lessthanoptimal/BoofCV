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

package boofcv.alg.shapes.ellipse;

import boofcv.alg.shapes.edge.BaseIntegralEdge;
import boofcv.struct.image.ImageGray;
import georegression.fitting.ellipse.FitEllipseWeightedAlgebraic;
import georegression.geometry.UtilEllipse_F64;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;

/**
 * Refines an initial estimate of an elipse using a subpixel contour technique.  A local line integral around each
 * point is used to determine how important each point is.  The contour being
 *
 * @author Peter Abeles
 */
public class SnapToEllipseEdge<T extends ImageGray> extends BaseIntegralEdge<T> {

	// maximum number of iterations it will performance
	protected int maxIterations = 10;
	// when the difference between two ellipses is less than this amount stop iterating
	protected double convergenceTol = 1e-6;

	// how many points along the contour it will sample
	protected int numSampleContour;

	// Determines the number of points sampled radially outwards from the line
	// Total intensity values sampled at each point along the line is radius*2+2,
	// and points added to line fitting is radius*2+1.
	protected int radialSamples;

	protected GrowQueue_F64 weights = new GrowQueue_F64();// storage for weights in line fitting
	// storage for where the points that are sampled along the line
	protected FastQueue<Point2D_F64> samplePts = new FastQueue<>(Point2D_F64.class, true);

	protected FitEllipseWeightedAlgebraic fitter = new FitEllipseWeightedAlgebraic();

	protected EllipseRotated_F64 previous = new EllipseRotated_F64();

	/**
	 * Constructor with configuration
	 *
	 * @param numSampleContour Maximum number of iterations it will performance
	 * @param radialSamples When the difference between two ellipses is less than this amount stop iterating
	 * @param imageType Type of gray-scale input image
	 */
	public SnapToEllipseEdge( int numSampleContour, int radialSamples , Class<T> imageType) {
		super(imageType);

		this.numSampleContour = numSampleContour;
		this.radialSamples = radialSamples;
	}

	/**
	 * Refines provided list by snapping it to edges found in the image
	 *
	 * @param input (Output) Close approximation of the ellipse in the image
	 * @param refined (Output) Storage for refined estimate.  Can be same instance as input
	 * @return True if a refined estimate could be found, false if it failed
	 */
	public boolean process(EllipseRotated_F64 input, EllipseRotated_F64 refined) {

		refined.set(input);
		previous.set(input);

		for (int iteration = 0; iteration < maxIterations; iteration++) {
			refined.set(previous);
			computePointsAndWeights(refined);

			if( fitter.process(samplePts.toList(),weights.data) ) {
				// Get the results in local coordinates
				UtilEllipse_F64.convert(fitter.getEllipse(),refined);
				// convert back into image coordinates
				double scale = previous.a;
				refined.center.x = refined.center.x*scale + previous.center.x;
				refined.center.y = refined.center.y*scale + previous.center.y;
				refined.a *= scale;
				refined.b *= scale;
			} else {
				return false;
			}

			// stop once the change between two iterations is insignificant
			if( change(previous,refined) <= convergenceTol) {
				return true;
			} else {
				previous.set(refined);
			}
		}
		return true;
	}

	/**
	 * Computes a numerical value for the difference in parameters between the two ellipses
	 */
	protected static double change( EllipseRotated_F64 a , EllipseRotated_F64 b ) {
		double total = 0;

		total += Math.abs(a.center.x - b.center.x);
		total += Math.abs(a.center.y - b.center.y);
		total += Math.abs(a.a - b.a);
		total += Math.abs(a.b - b.b);

		// only care about the change of angle when it is not a circle
		double weight = Math.min(4,2.0*(a.a/a.b-1.0));
		total += weight*UtilAngle.distHalf(a.phi , b.phi);

		return total;
	}

	/**
	 * Computes the location of points along the line and their weights
	 */
	void computePointsAndWeights(EllipseRotated_F64 ellipse) {

		// use the semi-major axis to scale the input points for numerical stability
		double localScale = ellipse.a;

		samplePts.reset();
		weights.reset();
		int numSamples = radialSamples * 2 + 2;
		int numPts = numSamples - 1;

		Point2D_F64 sample = new Point2D_F64();
		for (int i = 0; i < numSampleContour; i++) {

			// find a point along the ellipse at evenly spaced angles
			double theta = 2.0 * Math.PI * i / numSampleContour;

			UtilEllipse_F64.computePoint(theta, ellipse, sample);

			// compute the unit tangent along the ellipse at this point
			double tanX = sample.x - ellipse.center.x;
			double tanY = sample.y - ellipse.center.y;
			double r = Math.sqrt(tanX * tanX + tanY * tanY);
			tanX /= r;
			tanY /= r;

			// define the line it will sample along
			double x = sample.x - numSamples * tanX / 2.0;
			double y = sample.y - numSamples * tanY / 2.0;

			double lengthX = numSamples * tanX;
			double lengthY = numSamples * tanY;

			// Unless all the sample points are inside the image, ignore this point
			if (!integral.isInside(x, y) || !integral.isInside(x + lengthX, y + lengthY))
				continue;

			double sample0 = integral.compute(x, y, x + tanX, y + tanY);
			x += tanX;
			y += tanY;
			for (int j = 0; j < numPts; j++) {
				double sample1 = integral.compute(x, y, x + tanX, y + tanY);

				double w = sample0 - sample1;
				if (w < 0) w = -w;

				if (w > 0) {
					// convert into a local coordinate so make the linear fitting more numerically stable and
					// independent on position in the image
					samplePts.grow().set((x - ellipse.center.x) / localScale, (y - ellipse.center.y) / localScale);
					weights.add(w);
				}

				x += tanX;
				y += tanY;
				sample0 = sample1;
			}

		}
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public double getConvergenceTol() {
		return convergenceTol;
	}

	public void setConvergenceTol(double convergenceTol) {
		this.convergenceTol = convergenceTol;
	}
}
