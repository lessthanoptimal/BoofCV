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

package boofcv.factory.feature.detect.edge;

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.edge.CannyEdgeDynamic;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageGray;

/**
 * Creates different types of edge detectors.
 *
 * @author Peter Abeles
 */
public class FactoryEdgeDetectors {

	/**
	 * Detects the edge of an object using the canny edge detector. The output can be a binary image and/or a
	 * graph of connected contour points.
	 *
	 * @see CannyEdge
	 * @see CannyEdgeDynamic
	 *
	 * @param blurRadius Size of the kernel used to blur the image. Try 1 or 2
	 * @param dynamicThreshold If true then the thresholds have a range from 0 to 1 and are relative to the
	 * maximum edge intensity, if false then they are absolute intensity values.
	 * @param imageType Type of input image.
	 * @param derivType Type of image derivative.
	 * @return Canny edge detector
	 */
	public static <T extends ImageGray, D extends ImageGray>
	CannyEdge<T,D> canny( int blurRadius , boolean saveTrace , boolean dynamicThreshold, Class<T> imageType , Class<D> derivType )
	{
		BlurFilter<T> blur = FactoryBlurFilter.gaussian(imageType, -1, blurRadius);
		ImageGradient<T,D> gradient = FactoryDerivative.three(imageType, derivType);

		if( dynamicThreshold )
			return new CannyEdgeDynamic<>(blur, gradient, saveTrace);
		else
			return new CannyEdge<>(blur, gradient, saveTrace);
	}
}
