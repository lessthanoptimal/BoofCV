/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
import boofcv.abst.feature.detect.edge.DetectEdgeContour;
import boofcv.abst.feature.detect.edge.WrapBinaryContour;
import boofcv.abst.filter.blur.BlurFilter;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageSingleBand;


/**
 * Factory for creating {@link boofcv.abst.feature.detect.edge.DetectEdgeContour}.
 *
 * @author Peter Abeles
 */
public class FactoryDetectEdgeContour {

	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	DetectEdgeContour<T> canny( float threshLow , float threshHigh ,
								Class<T> imageType , Class<D> derivType )
	{
		BlurFilter<T> blur = FactoryBlurFilter.gaussian(imageType,-1,2);
		ImageGradient<T,D> gradient = FactoryDerivative.sobel(imageType,derivType);

		return new CannyEdgeContour<T,D>(blur,gradient,threshLow,threshHigh);
	}

	public static <T extends ImageSingleBand>
	DetectEdgeContour<T> binarySimple( double threshold , boolean down )
	{
		return new WrapBinaryContour<T>(threshold,down);
	}
}
