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

package boofcv.alg.shapes.edge;

import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

/**
 * Looks at the difference in pixel values along the edge of a polygon and decides if its a false positive or not.
 * The average difference along the polygons edge is the score.  Note that the abs is only taken after the sum
 * is finished, so objects which are entirely dark/light along the edge will have an advantage.
 *
 * @author Peter Abeles
 */
public class PolygonEdgeScore<T extends ImageSingleBand>  {

	// distance away from corner that sampling will start and end
	private double cornerOffset;
	// distnace away from line in tangent direction it will sample
	private double tangentDistance;

	// the minimum acceptable score/average pixel difference
	private double thresholdScore;

	// storage for points offset from corner
	private Point2D_F64 offsetA = new Point2D_F64();
	private Point2D_F64 offsetB = new Point2D_F64();

	// the compute score
	private double averageEdgeIntensity;

	ScoreLineSegmentEdge<T> scorer;

	/**
	 * Constructor which configures scoring.
	 *
	 * @param cornerOffset Number of pixels away from corner it will start sampling
	 * @param tangentDistance How far from the line it will sample tangentially
	 * @param numSamples Number of points it will sample along an edge
	 * @param thresholdScore Minimum edge score.
	 * @param imageType Type of image it will process
	 */
	public PolygonEdgeScore( double cornerOffset ,
							 double tangentDistance,
							 int numSamples,
							 double thresholdScore,
							 Class<T> imageType ) {
		this.cornerOffset = cornerOffset;
		this.tangentDistance = tangentDistance;
		this.thresholdScore = thresholdScore;

		scorer = new ScoreLineSegmentEdge<T>(numSamples,imageType);
	}

	/**
	 * Used to specify a transform that is applied to pixel coordinates to bring them back into original input
	 * image coordinates.  For example if the input image has lens distortion but the edge were found
	 * in undistorted coordinates this code needs to know how to go from undistorted back into distorted
	 * image coordinates in order to read the pixel's value.
	 *
	 * @param undistToDist Pixel transformation from undistorted pixels into the actual distorted input image..
	 */
	public void setTransform( PixelTransform_F32 undistToDist ) {
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
	 * @return true for valid or false for invalid
	 */
	public boolean validate( Polygon2D_F64 polygon ) {

		double total = 0;

		for (int i = 0; i < polygon.size(); i++) {
			int j = i+1;
			if( j == polygon.size() ) j = 0;

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

			total += scorer.computeAverageDerivative(offsetA, offsetB, -dy, dx);
		}

		averageEdgeIntensity = Math.abs(total) / polygon.size();

		return averageEdgeIntensity >= thresholdScore;
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

	public double getThresholdScore() {
		return thresholdScore;
	}

	public void setThresholdScore(double thresholdScore) {
		this.thresholdScore = thresholdScore;
	}

	public double getAverageEdgeIntensity() {
		return averageEdgeIntensity;
	}
}
