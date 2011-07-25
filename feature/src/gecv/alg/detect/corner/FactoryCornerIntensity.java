/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.corner;

import gecv.alg.detect.corner.impl.*;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

/**
 * Factory for creating various types of corner intensity detectors.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryCornerIntensity {

	/**
	 * Common interface for creating a {@link KitRosCornerIntensity} from different image types.
	 *
	 * @param imageType Type of input image it is computed form.
	 * @param pixelTol How different pixels need to be to be considered part of a corner. Image dependent.  Try 20 to start.
	 * @param minCont Minimum number of continue pixels in a circle for it ot be a corner.  11 or 12 are good numbers.
	 * @return Fast corner
	 */
	public static <T extends ImageBase>
	FastCornerIntensity<T> createFast12( Class<T> imageType , int pixelTol, int minCont)
	{
		if( imageType == ImageFloat32.class )
			return (FastCornerIntensity<T>)new FastCorner12_F32(pixelTol,minCont);
		else if( imageType == ImageUInt8.class )
			return (FastCornerIntensity<T>)new FastCorner12_U8(pixelTol,minCont);
		else
			throw new IllegalArgumentException("Unknown image type "+imageType);
	}

	/**
	 * Common interface for creating a {@link HarrisCornerIntensity} from different image types.
	 *
	 * @param derivType Image derivative type it is computed from.
	 * @param windowRadius Size of the feature it is detects,
	 * @param kappa Tuning parameter, typically a small number around 0.04
	 * @return Harris corner
	 */
	public static <T extends ImageBase>
	HarrisCornerIntensity<T> createHarris( Class<T> derivType , int windowRadius, float kappa)
	{
		if( derivType == ImageFloat32.class )
			return (HarrisCornerIntensity<T>)new HarrisCorner_F32(windowRadius,kappa);
		else if( derivType == ImageSInt16.class )
			return (HarrisCornerIntensity<T>)new HarrisCorner_S16(windowRadius,kappa);
		else
			throw new IllegalArgumentException("Unknown image type "+derivType);
	}

	/**
	 * Common interface for creating a {@link KitRosCornerIntensity} from different image types.
	 *
	 * @param derivType Image derivative type it is computed from.
	 * @return Kit Ros corner
	 */
	public static <T extends ImageBase>
	KitRosCornerIntensity<T> createKitRos( Class<T> derivType )
	{
		if( derivType == ImageFloat32.class )
			return (KitRosCornerIntensity<T>)new KitRosCorner_F32();
		else if( derivType == ImageSInt16.class )
			return (KitRosCornerIntensity<T>)new KitRosCorner_S16();
		else
			throw new IllegalArgumentException("Unknown image type "+derivType);
	}

	/**
	 * Common interface for creating a {@link KltCornerIntensity} from different image types.
	 *
	 * @param derivType Image derivative type it is computed from.
	 * @param windowRadius Size of the feature it detects,
	 * @return KLT corner
	 */
	public static <T extends ImageBase>
	KltCornerIntensity<T> createKlt( Class<T> derivType , int windowRadius)
	{
		if( derivType == ImageFloat32.class )
			return (KltCornerIntensity<T>)new KltCorner_F32(windowRadius);
		else if( derivType == ImageSInt16.class )
			return (KltCornerIntensity<T>)new KltCorner_S16(windowRadius);
		else
			throw new IllegalArgumentException("Unknown image type "+derivType);
	}

	/**
	 * Common interface for creating a {@link KltCornerIntensity} from different image types.
	 *
	 * @param derivType Image derivative type it is computed from.
	 * @return KLT corner
	 */
	public static <T extends ImageBase>
	MedianCornerIntensity<T> createMedian( Class<T> derivType )
	{
		if( derivType == ImageFloat32.class )
			return (MedianCornerIntensity<T>)new MedianCorner_F32();
		else if( derivType == ImageUInt8.class )
			return (MedianCornerIntensity<T>)new MedianCorner_U8();
		else
			throw new IllegalArgumentException("Unknown image type "+derivType);
	}
}
