/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I32;

import java.util.List;

/**
 * Computes the average value of points sampled outside and inside the contour at regular intervals. At each sample
 * location along the contour the local slope is computed using another contour point. This is then used to
 * compute the tangent. Then points inside and outside along the tangent are sampled using bilinear interpolation.
 * The value of these points are summed up and the average computed.
 *
 * @author Peter Abeles
 */
public class ContourEdgeIntensity<T extends ImageGray<T>>  {

	// how many points along the contour are sampled
	private int contourSamples;

	// How many tangental sample points will there be
	private int tangentSamples;

	// Distance in pixels between tangential sample points
	private float tangentStep;

	// Use to sample the gray scale's intensity value
	private InterpolatePixelS<T> sampler;

	// shape of input image
	private int imageWidth,imageHeight;

	// average pixel values inside and outside the contour
	private float edgeInside;
	private float edgeOutside;

	/**
	 * Configures how the edge intensity is computed
	 *
	 * @param contourSamples Sample of times it will sample along the image contour.
	 * @param tangentSamples
	 * @param tangentStep
	 * @param imageType
	 */
	public ContourEdgeIntensity(int contourSamples, int tangentSamples , double tangentStep , Class<T> imageType ) {
		this.contourSamples = contourSamples;
		this.tangentSamples = tangentSamples;
		this.tangentStep = (float)tangentStep;

		sampler = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);
	}

	public void setImage( T image ) {
		sampler.setImage(image);
		this.imageWidth = image.width;
		this.imageHeight = image.height;
	}

	public void process(List<Point2D_I32> contour , boolean isCCW ) {
		if( imageWidth == 0 )
			throw new RuntimeException("You didn't call setImage()");

		// How many pixels along the contour it will step between samples
		int step;
		if( contour.size() <= contourSamples )
			step = 1;
		else
			step = contour.size()/contourSamples;

		// Want the local tangent. How many contour points forward it will sample to get the tangent
		int sample = Math.max(1,Math.min(step/2,5));

		edgeOutside = edgeInside = 0;
		int totalInside = 0;
		int totalOutside = 0;

		// traverse the contour
		for (int i = 0; i < contour.size(); i += step) {
			Point2D_I32 a = contour.get(i);
			Point2D_I32 b = contour.get((i+sample)%contour.size());

			// compute the tangent using the two points
			float dx = b.x-a.x, dy = b.y-a.y;
			float r = (float)Math.sqrt(dx*dx + dy*dy);
			dx /= r; dy /= r;

			// sample points tangent to the contour but not the contour itself
			for (int j = 0; j < tangentSamples; j++) {
				float x,y;
				float length = (j+1)*tangentStep;

				x = a.x + length*dy;
				y = a.y - length*dx;
				if( x >= 0 && y >= 0 && x <= imageWidth-1 && y <= imageHeight-1 ) {
					edgeOutside += sampler.get(x,y);
					totalOutside++;
				}

				x = a.x - length*dy;
				y = a.y + length*dx;
				if( x >= 0 && y >= 0 && x <= imageWidth-1 && y <= imageHeight-1 ) {
					edgeInside += sampler.get(x,y);
					totalInside++;
				}
			}
		}

		edgeOutside /= totalOutside;
		edgeInside /= totalInside;

		if( !isCCW ) {
			float tmp = edgeOutside;
			edgeOutside = edgeInside;
			edgeInside = tmp;
		}
	}

	public float getOutsideAverage() {
		return edgeOutside;
	}

	public float getInsideAverage() {
		return edgeInside;
	}


	public Class<T> getInputType() {
		return sampler.getImageType().getImageClass();
	}
}
