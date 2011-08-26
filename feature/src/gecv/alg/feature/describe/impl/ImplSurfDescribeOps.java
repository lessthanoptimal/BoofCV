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

package gecv.alg.feature.describe.impl;

import gecv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
public class ImplSurfDescribeOps {

	public static void gradientInner( ImageFloat32 ii , int r , int step ,
									  int x0 , int y0 , int x1 , int y1 ,
									  int offX , int offY ,
									  double []derivX , double derivY[] )
	{
		int w = r*2+1;
		for( int y = y0; y < y1; y += step ) {
			int i = (y-y0+offY)*w+offX;

			int indexSrc1 = ii.startIndex + (y-r-1)*ii.stride + x0 - r - 1;
			int indexSrc2 = ii.startIndex + (y-1)*ii.stride + x0 - r - 1;
			int indexSrc3 = ii.startIndex + y*ii.stride + x0 - r - 1;
			int indexSrc4 = ii.startIndex + (y+r)*ii.stride + x0 - r - 1;

			for( int x = x0; x < x1; x += step , i++ ) {
				float p0 = ii.data[indexSrc1];
				float p1 = ii.data[indexSrc1+r];
				float p2 = ii.data[indexSrc1+r+1];
				float p3 = ii.data[indexSrc1+w];
				float p11 = ii.data[indexSrc2];
				float p4 = ii.data[indexSrc2+w];
				float p10 = ii.data[indexSrc3];
				float p5 = ii.data[indexSrc3+w];
				float p9 = ii.data[indexSrc4];
				float p8 = ii.data[indexSrc4+r];
				float p7 = ii.data[indexSrc4+r+1];
				float p6 = ii.data[indexSrc4+w];

				float left = p8-p9-p1+p0;
				float right = p6-p7-p3+p2;
				float top = p4-p11-p3+p0;
				float bottom = p6-p9-p5+p10;

				derivX[i] = right-left;
				derivY[i] = bottom-top;

				indexSrc1 += step;
				indexSrc2 += step;
				indexSrc3 += step;
				indexSrc4 += step;
			}
		}
	}
}
