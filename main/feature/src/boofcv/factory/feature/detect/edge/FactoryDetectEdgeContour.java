/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.edge.CannyEdgeContour;
import boofcv.abst.feature.detect.edge.CannyEdgeContourDynamic;
import boofcv.abst.feature.detect.edge.DetectEdgeContour;
import boofcv.abst.feature.detect.edge.WrapBinaryContour;
import boofcv.abst.filter.blur.BlurFilter;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageSingleBand;


/**
 * Factory for creating algorithms that implement {@link boofcv.abst.feature.detect.edge.DetectEdgeContour}.
 *
 * @author Peter Abeles
 */
public class FactoryDetectEdgeContour {

	/**
	 * Detects the edge of an object using the canny edge detector.  This edge detector tends to work
	 * well but can be slower than others.   If dynamic thresholds are set to true then the thresholds
	 * are relative to the image's maximum edge intensity, which makes it a bit more generic.
	 *
	 * @see CannyEdgeContour
	 *
	 * @param threshLow Low threshold for flagging edges.  Values are image dependent.
	 * @param threshHigh High threshold for flagging edges.  Values are image dependent.
	 * @param dynamicThreshold If true then the thresholds have a range from 0 to 1 and are relative to the
	 *                         maximum edge intensity, if false then they are absolute intensity values.
	 * @param imageType Type of input image.
	 * @param derivType Type of image derivative.
	 * @return Canny edge detector
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	DetectEdgeContour<T> canny( double threshLow , double threshHigh , boolean dynamicThreshold,
								Class<T> imageType , Class<D> derivType )
	{
		// blurring the image first
		BlurFilter<T> blur = FactoryBlurFilter.gaussian(imageType,-1,2);
		ImageGradient<T,D> gradient = FactoryDerivative.sobel(imageType,derivType);

		if( dynamicThreshold )
			return new CannyEdgeContourDynamic<T,D>(blur,gradient,(float)threshLow,(float)threshHigh);
		else
			return new CannyEdgeContour<T,D>(blur,gradient,(float)threshLow,(float)threshHigh);
	}

	/**
	 * Finds the contour of shapes by thresholding the image then detecting pixels which lie
	 * along the edges of the binary image.
	 *
	 * @param threshold Threshold for creating binary image.
	 * @param down Should it threshold up or down.
	 * @return Binary image based contour
	 */
	public static <T extends ImageSingleBand>
	DetectEdgeContour<T> binarySimple( double threshold , boolean down )
	{
		return new WrapBinaryContour<T>(threshold,down);
	}
}
