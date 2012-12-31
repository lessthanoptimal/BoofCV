/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.orientation;

import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.image.ImageSingleBand;


/**
 * Computes the orientation of a region by computing a weighted sum of each pixel's intensity
 * using their respective sine and cosine values.
 *
 * @author Peter Abeles
 */
public abstract class OrientationImageAverage<T extends ImageSingleBand> implements OrientationImage<T> {

	// input image
	protected T image;

	// local variable used to define the region being examined.
	// this makes it easy to avoid going outside the image
	protected ImageRectangle rect = new ImageRectangle();

	// radius at a scale of 1
	protected int radius;
	// the radius at this scale
	protected int radiusScale;

	// cosine values for each pixel
	protected Kernel2D_F32 kerCosine;
	// sine values for each pixel
	protected Kernel2D_F32 kerSine;

	public OrientationImageAverage(int radius) {
		setRadius(radius);
	}

	@Override
	public void setImage( T image ) {
		this.image = image;
	}

	public void setRadius(int radius) {
		this.radius = radius;
		setScale(1);
	}

	@Override
	public void setScale(double scale) {
		radiusScale = (int)Math.ceil(scale*radius);

		int w = radiusScale*2+1;
		kerCosine = new Kernel2D_F32(w);
		kerSine = new Kernel2D_F32(w);

		for( int y=-radiusScale; y <= radiusScale; y++ ) {
			int pixelY = y+radiusScale;
			for( int x=-radiusScale; x <= radiusScale; x++ ) {
				int pixelX = x+radiusScale;
				float r = (float)Math.sqrt(x*x+y*y);
				kerCosine.set(pixelX,pixelY,(float)x/r);
				kerSine.set(pixelX,pixelY,(float)y/r);
			}
		}
		kerCosine.set(radiusScale,radiusScale,0);
		kerSine.set(radiusScale,radiusScale,0);
	}

	@Override
	public double compute(double X, double Y) {

		int c_x = (int)X;
		int c_y = (int)Y;

		// compute the visible region while taking in account
		// the image borders
		rect.x0 = c_x-radiusScale;
		rect.y0 = c_y-radiusScale;
		rect.x1 = c_x+radiusScale+1;
		rect.y1 = c_y+radiusScale+1;

		BoofMiscOps.boundRectangleInside(image,rect);

		return computeAngle(c_x,c_y);
	}

	protected abstract double computeAngle( int c_x , int c_y );
}
