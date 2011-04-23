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
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;

/**
 * Factory for creating various types of corner intensity detectors.
 *
 * @author Peter Abeles
 */
public class FactoryCornerIntensity {

	public static FastCornerIntensity<ImageFloat32> createFast12_F32( int imageWidth, int imageHeight,
																	  int pixelTol, int minCont)
	{
		return new FastCorner12_F32(imageWidth,imageHeight,pixelTol,minCont);
	}

	public static HarrisCornerIntensity<ImageFloat32> createHarris_F32( int imageWidth, int imageHeight,
																 int windowRadius, float kappa )
	{
		return new HarrisCorner_F32(imageWidth,imageHeight,windowRadius,kappa);
	}

	public static HarrisCornerIntensity<ImageInt16> createHarris_I16( int imageWidth, int imageHeight,
															   int windowRadius, float kappa )
	{
		return new HarrisCorner_I16(imageWidth,imageHeight,windowRadius,kappa);
	}

	public static KitRosCornerIntensity<ImageFloat32> createKitRos_F32( int imageWidth, int imageHeight,
																 int windowRadius)
	{
		return new KitRosCorner_F32(imageWidth,imageHeight,windowRadius);
	}

	public static KitRosCornerIntensity<ImageInt16> createKitRos_I16( int imageWidth, int imageHeight,
															   int windowRadius)
	{
		return new KitRosCorner_I16(imageWidth,imageHeight,windowRadius);
	}

	public static KltCornerIntensity<ImageFloat32> createKlt_F32( int imageWidth, int imageHeight,
																 int windowRadius)
	{
		return new KltCorner_F32(imageWidth,imageHeight,windowRadius);
	}

	public static KltCornerIntensity<ImageInt16> createKlt_I16( int imageWidth, int imageHeight,
															   int windowRadius)
	{
		return new KltCorner_I16(imageWidth,imageHeight,windowRadius);
	}
}
