/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.misc;

import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;

/**
 * Overlays a rectangular grid on top of the src image and computes the average value within each cell
 * which is then written into the dst image.
 *
 * @author Peter Abeles
 */
public class ImplAverageResample {

	public static void horizontal( ImageUInt8 src , float srcX0 , int srcY0 , float srcWidth , int height ,
								   ImageFloat32 dst , int dstX0 , int dstY0 , int dstWidth) {

		float srcStep = srcWidth/dstWidth;

		for( int y = 0; y < height; y++ ) {
			int srcY = srcY0 + y;
			int dstY = dstY0 + y;
			for( int x = 0; x < dstWidth; x++ ) {
				float srcStartX = srcX0 + x*srcWidth/dstWidth;
				float srcStopX = srcStartX + srcStep;

				int srcIntX0 = (int)srcStartX;
				int srcIntX1 = (int)srcStopX;

				float areaStart =  1.0f-(srcStartX%1f);
				float areaEnd = srcStopX%1f;

				float total = areaStart*src.unsafe_get(srcIntX0,srcY);
				for(srcIntX0++; srcIntX0 < srcIntX1; srcIntX0++ ) {
					total += src.unsafe_get(srcIntX0,srcY);
				}
				total += areaEnd*src.unsafe_get(srcIntX1,srcY);

				dst.unsafe_set(dstX0+x,dstY,total/(srcStopX-srcStartX));
			}
		}
	}

	public static void vertical( ImageFloat32 src , int srcX0 , float srcY0 , int width , float srcHeight ,
								 ImageUInt8 dst , int dstX0 , int dstY0 , int dstHeight) {
		float srcStep = srcHeight/dstHeight;

		for( int x = 0; x < width; x++ ) {
			int srcX = srcX0 + x;
			int dstX = dstX0 + x;

			for( int y = 0; y < dstHeight; y++ ) {
				float srcStartY = srcY0 + y*srcHeight/dstHeight;
				float srcStopY = srcStartY + srcStep;

				int srcIntY0 = (int)srcStartY;
				int srcIntY1 = (int)srcStopY;

				float areaStart =  1.0f-(srcStartY%1f);
				float areaEnd = srcStopY%1f;

				float total = areaStart*src.unsafe_get(srcX,srcIntY0);
				for(srcIntY0++; srcIntY0 < srcIntY1; srcIntY0++ ) {
					total += src.unsafe_get(srcX,srcIntY0);
				}
				total += areaEnd*src.unsafe_get(srcX,srcIntY1);

				dst.unsafe_set(dstX,dstY0+y,(int)(total/(srcStopY-srcStartY)+0.5f));
			}

		}
	}
}
