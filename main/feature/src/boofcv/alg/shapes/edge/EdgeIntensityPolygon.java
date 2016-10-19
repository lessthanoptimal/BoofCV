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

import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

/**
 * Looks at the difference in pixel values along the edge of a polygon and decides if its a false positive or not.
 * The average difference along the polygons edge is the score.  Note that the abs is only taken after the sum
 * is finished, so objects which are entirely dark/light along the edge will have an advantage.
 *
 * @author Peter Abeles
 */
public class EdgeIntensityPolygon<T extends ImageGray>  {

	// distance away from corner that sampling will start and end
	private double cornerOffset;
	// distance away from line in tangent direction it will sample
	private double tangentDistance;

	// storage for points offset from corner
	private Point2D_F64 offsetA = new Point2D_F64();
	private Point2D_F64 offsetB = new Point2D_F64();

	// average pixel intensity inside and outside the polygon's edge
	private double averageInside;
	private double averageOutside;

	ScoreLineSegmentEdge<T> scorer;

	/**
	 * Constructor which configures scoring.
	 *
	 * @param cornerOffset Number of pixels away from corner it will start sampling
	 * @param tangentDistance How far from the line it will sample tangentially
	 * @param numSamples Number of points it will sample along an edge
	 * @param imageType Type of image it will process
	 */
	public EdgeIntensityPolygon(double cornerOffset ,
								double tangentDistance,
								int numSamples,
								Class<T> imageType ) {
		this.cornerOffset = cornerOffset;
		this.tangentDistance = tangentDistance;

		scorer = new ScoreLineSegmentEdge<>(numSamples, imageType);
	}

	/**
	 * Used to specify a transform that is applied to pixel coordinates to bring them back into original input
	 * image coordinates.  For example if the input image has lens distortion but the edge were found
	 * in undistorted coordinates this code needs to know how to go from undistorted back into distorted
	 * image coordinates in order to read the pixel's value.
	 *
	 * @param undistToDist Pixel transformation from undistorted pixels into the actual distorted input image..
	 */
	public void setTransform( PixelTransform2_F32 undistToDist ) {
		scorer.setTransform(undistToDist);
	}

	/**
	 * Sets the image which is going to be processed.
	 */
	public void setImage(T image) {
		scorer.setImage(image);
	}

	/**
	 * Checks to see if its a valid polygon or a false positive by looking at edge intensity
	 *
	 * @param polygon The polygon being tested
	 * @param ccw True if the polygon is counter clockwise
	 * @return true if it could compute the edge intensity, otherwise false
	 */
	public boolean computeEdge(Polygon2D_F64 polygon , boolean ccw ) {
		averageInside = 0;
		averageOutside = 0;

		double tangentSign = ccw ? 1 : -1;

		int totalSides = 0;
		for (int i = polygon.size()-1,j=0; j < polygon.size(); i=j,j++) {

			Point2D_F64 a = polygon.get(i);
			Point2D_F64 b = polygon.get(j);

			double dx = b.x-a.x;
			double dy = b.y-a.y;
			double t = Math.sqrt(dx*dx + dy*dy);
			dx /= t;
			dy /= t;

			// see if the side is too small
			if( t <= 3*cornerOffset )
				return false;

			offsetA.x = a.x + cornerOffset*dx;
			offsetA.y = a.y + cornerOffset*dy;

			offsetB.x = b.x - cornerOffset*dx;
			offsetB.y = b.y - cornerOffset*dy;

			double tanX = -dy*tangentDistance*tangentSign;
			double tanY =  dx*tangentDistance*tangentSign;

			scorer.computeAverageDerivative(offsetA, offsetB, tanX,tanY);

			if( scorer.getSamplesInside() > 0 ) {
				totalSides++;
				averageInside += scorer.getAverageUp() / tangentDistance;
				averageOutside += scorer.getAverageDown() / tangentDistance;
			}
		}

		if( totalSides > 0 ) {
			averageInside /= totalSides;
			averageOutside /= totalSides;
		} else {
			averageInside = averageOutside = 0;
			return false;
		}

		return true;
	}

	/**
	 * Checks the edge intensity against a threshold.
	 *
	 * dark: outside-inside &ge; threshold
	 * light: inside-outside &ge; threshold
	 *
	 * @param insideDark is the inside of the polygon supposed to be dark or light?
	 * @param threshold threshold for average difference
	 * @return true if the edge intensity is significant enough
	 */
	public boolean checkIntensity( boolean insideDark , double threshold ) {
		if( insideDark )
			return averageOutside-averageInside >= threshold;
		else
			return averageInside-averageOutside >= threshold;
	}

	public double getCornerOffset() {
		return cornerOffset;
	}

	public void setCornerOffset(double cornerOffset) {
		this.cornerOffset = cornerOffset;
	}

	public double getTangentDistance() {
		return tangentDistance;
	}

	public void setTangentDistance(double tangentDistance) {
		this.tangentDistance = tangentDistance;
	}

	public double getAverageInside() {
		return averageInside;
	}

	public double getAverageOutside() {
		return averageOutside;
	}
}
