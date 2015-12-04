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

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.GImageSingleBandDistorted;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;

/**
 * Looks at the difference in pixel values along the edge of a polygon and decides if its a false positive or not.
 * The average difference along the polygons edge is the score.  Note that the abs is only taken after the sum
 * is finished, so objects which are entirely dark/light along the edge will have an advantage.
 *
 * @author Peter Abeles
 */
public class ScoreLineSegmentEdge<T extends ImageSingleBand> extends BaseIntegralEdge<T> {

	// how many points along the line it will sample
	int numSamples;

	// sums above and below the line
	double averageUp;
	double averageDown;

	/**
	 * Constructor which configures scoring.
	 *
	 * @param numSamples Number of points it will sample along an edge
	 * @param imageType Type of image it will process
	 */
	public ScoreLineSegmentEdge(int numSamples,
								Class<T> imageType) {
		super(imageType);
		this.numSamples = numSamples;
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
		InterpolatePixelS<T> interpolate = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);
		integralImage = new GImageSingleBandDistorted<T>(undistToDist,interpolate);
	}

	/**
	 * Sets the image which is going to be processed.
	 */
	public void setImage(T image) {
		integralImage.wrap(image);
		integral.setImage(integralImage);
	}

	/**
	 * Returns average tangential derivative along the line segment.  Derivative is computed in direction
	 * of tangent.  A positive step in the tangent direction will have a positive value.  If all samples
	 * go outside the image then zero is returned.
	 *
	 * @param a start point
	 * @param b end point
	 * @param tanX unit tangent x-axis.  determines length of line integral
	 * @param tanY unit tangent y-axis   determines length of line integral
	 * @return average derivative
	 */
	public double computeAverageDerivative(Point2D_F64 a, Point2D_F64 b, double tanX, double tanY) {
		int samplesInside = 0;
		averageUp = averageDown = 0;

		for (int i = 0; i < numSamples; i++) {
			double x = (b.x-a.x)*i/(numSamples-1) + a.x;
			double y = (b.y-a.y)*i/(numSamples-1) + a.y;

			double x0 = x+tanX;
			double y0 = y+tanY;
			if(!BoofMiscOps.checkInside(integralImage.getWidth(),integralImage.getHeight(),x0,y0))
				continue;

			double x1 = x-tanX;
			double y1 = y-tanY;
			if(!BoofMiscOps.checkInside(integralImage.getWidth(),integralImage.getHeight(),x1,y1))
				continue;

			samplesInside++;

			double up = integral.compute(x,y,x0,y0);
			double down = integral.compute(x,y,x1,y1);

			// don't take the abs here and require that a high score involves it being entirely black or white around
			// the edge.  Otherwise a random image would score high
			averageUp += up;
			averageDown += down;
		}

		if( samplesInside == 0 )
			return 0;
		averageUp /= samplesInside;
		averageDown /= samplesInside;

		return averageUp-averageDown;
	}

	public int getNumSamples() {
		return numSamples;
	}

	public void setNumSamples(int numSamples) {
		this.numSamples = numSamples;
	}

	public double getAverageUp() {
		return averageUp;
	}

	public double getAverageDown() {
		return averageDown;
	}
}
