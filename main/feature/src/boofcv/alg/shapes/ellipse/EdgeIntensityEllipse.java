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
import georegression.struct.shapes.EllipseRotated_F64;

/**
 * <p>Computes the edge intensity along the an ellipse.</p>
 *
 * Edge Intensity Definition:<br>
 * The average difference in pixel values.  Maximum value is (max pixel val - min pixel val).
 *
 * @author Peter Abeles
 */
public class EdgeIntensityEllipse<T extends ImageGray> extends BaseIntegralEdge<T> {

	// distance away from line in tangent direction it will sample
	private double tangentDistance;

	// number of points along the contour it will sample
	private int numContourPoints;

	// threshold for passing
	double passThreshold;

	// computed edge score
	double score;

	/**
	 * Configures edge intensity calculation
	 *
	 * @param tangentDistance Distance along tangent it will integrate
	 * @param numContourPoints Number of points along the contour it will sample.  If &le; 0
	 *                         the test will always pass
	 * @param passThreshold Threshold for passing. Value: 0 to (max - min) pixel value.
	 * @param imageType Type of input image
	 */
	public EdgeIntensityEllipse(double tangentDistance,
								int numContourPoints,
								double passThreshold,
								Class<T> imageType) {
		super(imageType);

		this.tangentDistance = tangentDistance;
		this.numContourPoints = numContourPoints;
		this.passThreshold = passThreshold;

	}

	/**
	 * Processes the edge along the ellipse and determines if the edge intensity is strong enough
	 * to pass or not
	 * @param ellipse The ellipse in undistorted image coordinates.
	 * @return true if it passes or false if not
	 */
	public boolean process(EllipseRotated_F64 ellipse ) {
		// see if it's disabled
		if( numContourPoints <= 0 ) {
			score = 0;
			return true;
		}

		double cphi = Math.cos(ellipse.phi);
		double sphi = Math.sin(ellipse.phi);

		double aveInside = 0;
		double aveOutside = 0;

		int total = 0;

		for (int contourIndex = 0; contourIndex < numContourPoints; contourIndex++) {
			double theta = contourIndex*Math.PI*2.0/numContourPoints;

			// copied from UtilEllipse_F64.computePoint()  reduced number of cos and sin
			double ct = Math.cos(theta);
			double st = Math.sin(theta);

			// location on ellipse in world frame
			double px = ellipse.center.x + ellipse.a*ct*cphi - ellipse.b*st*sphi;
			double py = ellipse.center.y + ellipse.a*ct*sphi + ellipse.b*st*cphi;

			// find direction of the tangent line
			// ellipse frame
			double edx = ellipse.a*ct*ellipse.b*ellipse.b;
			double edy = ellipse.b*st*ellipse.a*ellipse.a;
			double r = Math.sqrt(edx*edx + edy*edy);
			edx /= r;
			edy /= r;

			// rotate tangent into world frame
			double dx = edx*cphi - edy*sphi;
			double dy = edx*sphi + edy*cphi;

			// define the line integral
			double xin = px-dx*tangentDistance;
			double yin = py-dy*tangentDistance;
			double xout = px+dx*tangentDistance;
			double yout = py+dy*tangentDistance;

			if( integral.isInside(xin,yin) && integral.isInside(xout,yout)) {
				aveInside += integral.compute(px,py, xin, yin);
				aveOutside += integral.compute(px,py, xout, yout);
				total++;
			}
		}

		score = 0;
		if( total > 0 ) {
			score = Math.abs(aveOutside-aveInside)/(total*tangentDistance);
		}

		return score >= passThreshold;
	}

	public double getEdgeIntensity() {
		return score;
	}
}
