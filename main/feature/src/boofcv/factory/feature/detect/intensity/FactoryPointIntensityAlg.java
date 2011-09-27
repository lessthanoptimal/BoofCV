/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.detect.intensity;

import boofcv.alg.feature.detect.intensity.FastCornerIntensity;
import boofcv.alg.feature.detect.intensity.HarrisCornerIntensity;
import boofcv.alg.feature.detect.intensity.KltCornerIntensity;
import boofcv.alg.feature.detect.intensity.impl.*;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;

/**
 * Factory for creating various types of corner intensity detectors.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryPointIntensityAlg {

	/**
	 * Common interface for creating a {@link boofcv.alg.feature.detect.intensity.KitRosCornerIntensity} from different image types.
	 *
	 * @param pixelTol How different pixels need to be to be considered part of a corner. Image dependent.  Try 20 to start.
	 * @param minCont Minimum number of continue pixels in a circle for it ot be a corner.  11 or 12 are good numbers.
	 * @param imageType Type of input image it is computed form.
	 * @return Fast corner
	 */
	public static <T extends ImageBase>
	FastCornerIntensity<T> createFast12(int pixelTol, int minCont, Class<T> imageType)
	{
		if( imageType == ImageFloat32.class )
			return (FastCornerIntensity<T>)new ImplFastCorner12_F32(pixelTol,minCont);
		else if( imageType == ImageUInt8.class )
			return (FastCornerIntensity<T>)new ImplFastCorner12_U8(pixelTol,minCont);
		else
			throw new IllegalArgumentException("Unknown image type "+imageType);
	}

	/**
	 * Common interface for creating a {@link boofcv.alg.feature.detect.intensity.HarrisCornerIntensity} from different image types.
	 *
	 * @param windowRadius Size of the feature it is detects,
	 * @param kappa Tuning parameter, typically a small number around 0.04
	 * @param derivType Image derivative type it is computed from.
	 * @return Harris corner
	 */
	public static <T extends ImageBase>
	HarrisCornerIntensity<T> createHarris(int windowRadius, float kappa, Class<T> derivType)
	{
		if( derivType == ImageFloat32.class )
			return (HarrisCornerIntensity<T>)new ImplHarrisCorner_F32(windowRadius,kappa);
		else if( derivType == ImageSInt16.class )
			return (HarrisCornerIntensity<T>)new ImplHarrisCorner_S16(windowRadius,kappa);
		else
			throw new IllegalArgumentException("Unknown image type "+derivType);
	}

	/**
	 * Common interface for creating a {@link boofcv.alg.feature.detect.intensity.KltCornerIntensity} from different image types.
	 *
	 * @param windowRadius Size of the feature it detects,
	 * @param derivType Image derivative type it is computed from.
	 * @return KLT corner
	 */
	public static <T extends ImageBase>
	KltCornerIntensity<T> createKlt(int windowRadius, Class<T> derivType)
	{
		if( derivType == ImageFloat32.class )
			return (KltCornerIntensity<T>)new ImplKltCorner_F32(windowRadius);
		else if( derivType == ImageSInt16.class )
			return (KltCornerIntensity<T>)new ImplKltCorner_S16(windowRadius);
		else
			throw new IllegalArgumentException("Unknown image type "+derivType);
	}
}
