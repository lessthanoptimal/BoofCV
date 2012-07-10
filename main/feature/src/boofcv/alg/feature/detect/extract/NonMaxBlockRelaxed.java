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

package boofcv.alg.feature.detect.extract;

import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I32;

/**
 * <p>
 * Implementation of {@link NonMaxBlock} which implements a relaxed maximum rule.
 * </p>
 *
 * @author Peter Abeles
 */
public class NonMaxBlockRelaxed extends NonMaxBlock {

	// storage for local maximums
	Point2D_I32 local[];

	@Override
	protected void searchBlock( int x0 , int y0 , int x1 , int y1 , ImageFloat32 img ) {

		int numPeaks = 0;
		float peakVal = threshold;

		for( int y = y0; y < y1; y++ ) {
			int index = img.startIndex + y*img.stride+x0;
			for( int x = x0; x < x1; x++ ) {
				float v = img.data[index++];

				if( v > peakVal ) {
					peakVal = v;
					local[0].set(x,y);
					numPeaks = 1;
				} else if( v == peakVal ) {
					local[numPeaks++].set(x,y);
				}
			}
		}

		if( numPeaks > 0 && peakVal != Float.MAX_VALUE ) {
			for( int i = 0; i < numPeaks; i++ ) {
				Point2D_I32 p = local[i];
				checkLocalMax(p.x,p.y,peakVal,img);
			}
		}
	}

	protected void checkLocalMax( int x_c , int y_c , float peakVal , ImageFloat32 img ) {
		int x0 = x_c-radius;
		int x1 = x_c+radius;
		int y0 = y_c-radius;
		int y1 = y_c+radius;

		if( x0 < 0 ) x0 = 0;
		if( y0 < 0 ) y0 = 0;
		if( x1 >= img.width ) x1 = img.width-1;
		if( y1 >= img.height ) y1 = img.height-1;

		for( int y = y0; y <= y1; y++ ) {
			int index = img.startIndex + y*img.stride+x0;
			for( int x = x0; x <= x1; x++ ) {
				float v = img.data[index++];

				if( v > peakVal ) {
					// not a local max
					return;
				}
			}
		}

		// save location of local max
		peaks.add(x_c,y_c);
	}

	@Override
	public void setSearchRadius(int minSeparation) {
		super.setSearchRadius(minSeparation);

		int w = 2*radius+1;

		local = new Point2D_I32[w*w];
		for( int i = 0; i < local.length; i++ )
			local[i] = new Point2D_I32();
	}

}
