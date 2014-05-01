/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.flow;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.struct.pyramid.PyramidFloat;

/**
 * Base class for pyramidal dense flow algorithms based on IPOL papers.
 *
 * @author Peter Abeles
 */
public abstract class DenseFlowPyramidBase<T extends ImageSingleBand> {

	// storage for normalized image
	private ImageFloat32 norm1 = new ImageFloat32(1,1);
	private ImageFloat32 norm2 = new ImageFloat32(1,1);

	// parameters used to create pyramid
	private double scale;
	private double sigma;
	private int maxLayers;

	// image pyramid and its derivative
	protected PyramidFloat<ImageFloat32> pyr1;
	protected PyramidFloat<ImageFloat32> pyr2;

	// Used to interpolate values between pixels
	protected InterpolatePixelS<ImageFloat32> interp;

	public DenseFlowPyramidBase(double scale, double sigma, int maxLayers,
								InterpolatePixelS<ImageFloat32> interp ) {
		this.scale = scale;
		this.sigma = sigma;
		this.maxLayers = maxLayers;
		this.interp = interp;
	}

	/**
	 * Processes the raw input images.  Normalizes them and creates image pyramids from them.
	 */
	public void process( T image1 , T image2 )
	{
		// declare image data structures
		if( pyr1 == null || pyr1.getInputWidth() != image1.width || pyr1.getInputHeight() != image1.height ) {
			pyr1 = UtilDenseOpticalFlow.standardPyramid(image1.width, image1.height, scale, sigma, 5, maxLayers, ImageFloat32.class);
			pyr2 = UtilDenseOpticalFlow.standardPyramid(image1.width, image1.height, scale, sigma, 5, maxLayers, ImageFloat32.class);

			pyr1.initialize(image1.width,image1.height);
			pyr2.initialize(image1.width,image1.height);
		}

		norm1.reshape(image1.width, image1.height);
		norm2.reshape(image1.width, image1.height);

		// normalize input image to make sure alpha is image independent
		imageNormalization(image1, image2, norm1, norm2);

		// create image pyramid
		pyr1.process(norm1);
		pyr2.process(norm2);

		// compute flow from pyramid
		process(pyr1, pyr2);
	}

	/**
	 * Takes the flow from the previous lower resolution layer and uses it to initialize the flow
	 * in the current layer.  Adjusts for change in image scale.
	 */
	protected void interpolateFlowScale(ImageFloat32 prev, ImageFloat32 curr) {
		interp.setImage(prev);

		float scaleX = (float)(prev.width-1)/(float)(curr.width-1)*0.999f;
		float scaleY = (float)(prev.height-1)/(float)(curr.height-1)*0.999f;

		float scale = (float)prev.width/(float)curr.width;

		int indexCurr = 0;
		for( int y = 0; y < curr.height; y++ ) {
			for( int x = 0; x < curr.width; x++ ) {
				curr.data[indexCurr++] = interp.get(x*scaleX,y*scaleY)/scale;
			}
		}
	}

	/**
	 * Takes the flow from the previous lower resolution layer and uses it to initialize the flow
	 * in the current layer.  Adjusts for change in image scale.
	 */
	protected void warpImageTaylor(ImageFloat32 before, ImageFloat32 flowX , ImageFloat32 flowY , ImageFloat32 after) {
		interp.setImage(before);

		for( int y = 0; y < before.height; y++ ) {
			int pixelIndex = y*before.width;
			for (int x = 0; x < before.width; x++, pixelIndex++ ) {
				float u = flowX.data[pixelIndex];
				float v = flowY.data[pixelIndex];

				float wx = x + u;
				float wy = y + v;

				if( wx < 0 || wx > before.width-1 || wy < 0 || wy > before.height-1 ) {
					// setting outside pixels to zero seems to produce smoother results than extending the image
					after.data[pixelIndex] = 0;
				} else {
					after.data[pixelIndex] = interp.get(wx, wy);
				}
			}
		}
	}

	/**
	 * Computes dense optical flow from the provided image pyramid.  Image gradient for each layer should be
	 * computed directly from the layer images.
	 *
	 * @param image1 Pyramid of first image
	 * @param image2 Pyramid of second image
	 */
	public abstract void process( ImagePyramid<ImageFloat32> image1 , ImagePyramid<ImageFloat32> image2 );

	/**
	 * Function to normalize the images between 0 and 255.
	 **/
	protected static<T extends ImageSingleBand>
	void imageNormalization( T image1, T image2, ImageFloat32 normalized1, ImageFloat32 normalized2 )
	{
		// find the max and min of both images
		float max1 = (float)GImageStatistics.max(image1);
		float max2 = (float)GImageStatistics.max(image2);
		float min1 = (float)GImageStatistics.min(image1);
		float min2 = (float)GImageStatistics.min(image2);

		// obtain the absolute max and min
		float max = max1 > max2 ? max1 : max2;
		float min = min1 < min2 ? min1 : min2;
		float range = max - min;

		if(range > 0) {
			// normalize both images
			int indexN = 0;
			for (int y = 0; y < image1.height; y++) {
				for (int x = 0; x < image1.width; x++,indexN++) {
					// this is a slow way to convert the image type into a float, but everything else is much
					// more expensive
					float pv1 = (float)GeneralizedImageOps.get(image1, x, y);
					float pv2 = (float)GeneralizedImageOps.get(image2,x,y);

					normalized1.data[indexN] = (pv1 - min) / range;
					normalized2.data[indexN] = (pv2 - min) / range;
				}
			}
		} else {
			GConvertImage.convert(image1, normalized1);
			GConvertImage.convert(image2,normalized2);
		}
	}
}
