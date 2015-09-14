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

import boofcv.alg.interpolate.ImageLineIntegral;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.GImageSingleBandDistorted;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageSingleBand;

/**
 * Base class for computing line integrals along lines/edges.
 *
 * @author Peter Abeles
 */
public class BaseIntegralEdge<T extends ImageSingleBand> {
	Class<T> imageType;

	// used when computing the fit for a line at specific points
	protected ImageLineIntegral integral;
	protected GImageSingleBand integralImage;

	public BaseIntegralEdge(Class<T> imageType) {
		this.imageType = imageType;
		this.integral = new ImageLineIntegral();
		this.integralImage = FactoryGImageSingleBand.create(imageType);
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
	 * Sets the image which is going to be processed.  Must call {@link #setImage(ImageSingleBand)} first.
	 */
	public void setImage(T image) {
		integralImage.wrap(image);
		integral.setImage(integralImage);
	}
}
