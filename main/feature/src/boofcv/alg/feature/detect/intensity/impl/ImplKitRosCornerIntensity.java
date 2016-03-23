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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;


/**
 * Implementations of {@link boofcv.alg.feature.detect.intensity.KitRosCornerIntensity}.
 *
 * @author Peter Abeles
 */
public class ImplKitRosCornerIntensity {

	public static void process(GrayF32 featureIntensity,
							   GrayF32 derivX, GrayF32 derivY,
							   GrayF32 hessianXX, GrayF32 hessianYY , GrayF32 hessianXY )
	{

		final int width = derivX.width;
		final int height = derivY.height;

		for( int y = 0; y < height; y++ ) {
			int indexX = derivX.startIndex + y*derivX.stride;
			int indexY = derivY.startIndex + y*derivY.stride;
			int indexXX = hessianXX.startIndex + y*hessianXX.stride;
			int indexYY = hessianYY.startIndex + y*hessianYY.stride;
			int indexXY = hessianXY.startIndex + y*hessianXY.stride;

			int indexInten = featureIntensity.startIndex + y*featureIntensity.stride;

			for( int x = 0; x < width; x++ ) {
				float dx = derivX.data[indexX++];
				float dy = derivY.data[indexY++];
				float dxx = hessianXX.data[indexXX++];
				float dyy = hessianYY.data[indexYY++];
				float dxy = hessianXY.data[indexXY++];

				float dx2 = dx*dx;
				float dy2 = dy*dy;


				float top = Math.abs(dxx*dy2 - 2*dxy*dx*dy + dyy*dx2);
				float bottom = dx2 + dy2;

				if( bottom == 0.0 )
					featureIntensity.data[indexInten++] = 0;
				else
					featureIntensity.data[indexInten++] = top/bottom;
			}
		}
	}

	public static void process(GrayF32 featureIntensity,
							   GrayS16 derivX, GrayS16 derivY,
							   GrayS16 hessianXX, GrayS16 hessianYY , GrayS16 hessianXY )
	{
		final int width = derivX.width;
		final int height = derivY.height;

		for( int y = 0; y < height; y++ ) {
			int indexX = derivX.startIndex + y*derivX.stride;
			int indexY = derivY.startIndex + y*derivY.stride;
			int indexXX = hessianXX.startIndex + y*hessianXX.stride;
			int indexYY = hessianYY.startIndex + y*hessianYY.stride;
			int indexXY = hessianXY.startIndex + y*hessianXY.stride;

			int indexInten = featureIntensity.startIndex + y*featureIntensity.stride;

			for( int x = 0; x < width; x++ ) {
				int dx = derivX.data[indexX++];
				int dy = derivY.data[indexY++];
				int dxx = hessianXX.data[indexXX++];
				int dyy = hessianYY.data[indexYY++];
				int dxy = hessianXY.data[indexXY++];

				int dx2 = dx*dx;
				int dy2 = dy*dy;


				float top = Math.abs(dxx*dy2 - 2*dxy*dx*dy + dyy*dx2);
				float bottom = dx2 + dy2;

				if( bottom == 0.0 )
					featureIntensity.data[indexInten++] = 0;
				else
					featureIntensity.data[indexInten++] = top/bottom;
			}
		}
	}
}
