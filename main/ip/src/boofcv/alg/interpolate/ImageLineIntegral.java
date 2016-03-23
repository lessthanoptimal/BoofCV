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

package boofcv.alg.interpolate;

import boofcv.core.image.GImageGray;
import boofcv.core.image.border.ImageBorder;

/**
 * <p>
 * Computes the line integral of a line segment across the image.  A line is laid over the image
 * and the fraction of the line which is over a pixel is multiplied by the pixel's value.  This is done
 * for each pixel it overlaps.
 * </p>
 *
 * <p>
 * Two different functions are provided below for handling pixels lines which are either contained entirely
 * inside the image or may contain elements which extend outside the image.  If a pixel extends outside the
 * image then {@link ImageBorder} is used to handle the pixels outside the image.  If the border is not
 * specified then it will likely crash.
 * </p>
 * <p>
 * Inside image definition:<br>
 * {@code 0 &le x < width}<br>
 * {@code 0 &le y < height}
 * </p>
 *
 * @author Peter Abeles
 */
public class ImageLineIntegral {

	// reference to image.
	GImageGray image;

	// length of the line just computed
	double length;


	/**
	 * Specify input image.
	 *
	 * @param image image
	 */
	public void setImage( GImageGray image ) {
		this.image = image;
	}

	/**
	 * Computes the line segment's line integral across the inside of the image. Where the
	 * inside is defined as 0 &le; x &le; width and 0 &le; y &le; height.
	 *
	 * @param x0 end point of line segment. x-coordinate
	 * @param y0 end point of line segment. y-coordinate
	 * @param x1 end point of line segment. x-coordinate
	 * @param y1 end point of line segment. y-coordinate
	 * @return line integral
	 */
	public double compute(double x0, double y0, double x1, double y1) {

		double sum = 0;

		double slopeX = x1-x0;
		double slopeY = y1-y0;

		length = Math.sqrt(slopeX*slopeX + slopeY*slopeY);

		int sgnX = (int)Math.signum(slopeX);
		int sgnY = (int)Math.signum(slopeY);

		int px = (int)x0;
		int py = (int)y0;

		if( slopeX == 0 || slopeY == 0 ) {
			// handle a pathological case
			if( slopeX == slopeY )
				return 0;

			double t;
			if (slopeX == 0) {
				t = slopeY > 0 ? py + 1 - y0 : py - y0;
			} else {
				t = slopeX > 0 ? px + 1 - x0 : px - x0;
			}

			t /= (slopeX+slopeY);
			if (t > 1) t = 1;
			if( t > 0 )
				sum += t * image.unsafe_getD(px, py);
			double deltaT = (sgnX+sgnY)/ (slopeX+slopeY);

			while (t < 1) {
				px += sgnX;
				py += sgnY;
				double nextT = t + deltaT;
				double actualDeltaT = deltaT;
				if (nextT > 1) {
					actualDeltaT = 1 - t;
				}
				t = nextT;
				sum += actualDeltaT * image.unsafe_getD(px, py);
			}
		} else {
			double deltaTX = slopeX > 0 ? px + 1 - x0 : px - x0;
			double deltaTY = slopeY > 0 ? py + 1 - y0 : py - y0;
			deltaTX /= slopeX;
			deltaTY /= slopeY;

			double t = Math.min(deltaTX,deltaTY);
			if (t > 1) t = 1;
			if( t > 0 )
				sum += t * image.unsafe_getD(px, py);

			double x = x0 + t*slopeX;
			double y = y0 + t*slopeY;
			px = (int)x;
			py = (int)y;
			int nx = px + sgnX;
			int ny = py + sgnY;

			while( t < 1 ) {
				deltaTX = (nx-x)/slopeX;
				deltaTY = (ny-y)/slopeY;

				double deltaT = Math.min(deltaTX,deltaTY);
				double nextT = t + deltaT;
				if( nextT > 1 ) {
					deltaT = 1-t;
				}

				double sampleT = t + 0.5f*deltaT;
				x = x0 + sampleT*slopeX;
				y = y0 + sampleT*slopeY;

				sum += deltaT*image.unsafe_getD((int)x,(int)y);
				t = t + deltaT;
				x = x0 + t*slopeX;
				y = y0 + t*slopeY;
				px = (int)x;
				py = (int)y;
				nx = px + sgnX;
				ny = py + sgnY;

			}
		}

		sum *= length;

		return sum;
	}

	/**
	 * <p>
	 * Return true if the coordinate is inside the image or false if not.<br>
	 * 0 &le; x &le; width<br>
	 * 0 &le; y &le; height
	 * </p>
	 * <p>Note: while the image is defined up to width and height, including coordinates up to that point contribute
	 * nothing towards the line integral since they are infinitesimally small.</p>
	 *
	 * @param x x-coordinate in pixel coordinates
	 * @param y y-coordinate in pixel coordinates
	 * @return true if inside or false if outside
	 */
	public boolean isInside( double x, double y ) {
		return x >= 0 && y >= 0 && x <= image.getWidth() && y <= image.getHeight();
	}

	/**
	 * Returns the line segment's length
	 * @return length
	 */
	public double getLength() {
		return length;
	}
}
