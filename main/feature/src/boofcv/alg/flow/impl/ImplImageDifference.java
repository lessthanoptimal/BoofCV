/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.flow.impl;

import boofcv.struct.image.ImageFloat32;

/**
 * Functions for computing the difference between two images by computing the average of a pixel and its 4-connect
 * neighbors.<br>
 * <br>
 * difference = imageB-imageA
 *
 * @author Peter Abeles
 */
public class ImplImageDifference {

	/**
	 * Computes the 4-connect average of inner image pixels.
	 */
	public static void inner4( ImageFloat32 imageA , ImageFloat32 imageB , ImageFloat32 difference ) {

		int w = imageA.width-1;
		int h = imageA.height-1;

		for( int y = 1; y < h; y++ ) {
			int indexA = imageA.startIndex + y*imageA.stride + 1;
			int indexB = imageB.startIndex + y*imageB.stride + 1;
			int indexDiff = difference.startIndex + y*difference.stride + 1;

			for( int x = 1; x < w; x++ , indexA++ , indexB++ , indexDiff++ ) {
				float d0 = imageB.data[indexB] - imageA.data[indexA];
				float d1 = imageB.data[indexB-1] - imageA.data[indexA-1];
				float d2 = imageB.data[indexB+1] - imageA.data[indexA+1];
				float d3 = imageB.data[indexB+imageB.stride] - imageA.data[indexA+imageA.stride];
				float d4 = imageB.data[indexB-imageB.stride] - imageA.data[indexA-imageA.stride];

				difference.data[indexDiff] = 0.2f*(d0 + d1 + d2 + d3 + d4);
			}
		}
	}

	/**
	 * Computes the 4-connect average of border image pixels.
	 */
	public static void border4( ImageFloat32 imageA , ImageFloat32 imageB ,
									  ImageFloat32 difference) {

		for( int y = 0; y < imageA.height; y++ ) {
			pixelBorder4(imageA,imageB,difference, 0, y);
			pixelBorder4(imageA,imageB,difference, imageA.width-1, y);
		}

		for( int x = 1; x < imageA.width-1; x++ ) {
			pixelBorder4(imageA,imageB,difference, x, 0);
			pixelBorder4(imageA,imageB,difference, x, imageA.height-1);
		}
	}

	protected static void pixelBorder4(ImageFloat32 imageA , ImageFloat32 imageB ,
								  ImageFloat32 difference, int x, int y) {
		float d0 = getExtend(imageA,imageB,x,y);
		float d1 = getExtend(imageA,imageB,x-1,y);
		float d2 = getExtend(imageA,imageB,x+1,y);
		float d3 = getExtend(imageA,imageB,x,y+1);
		float d4 = getExtend(imageA,imageB,x,y-1);

		difference.unsafe_set(x,y, 0.2f*(d0+d1+d2+d3+d4));
	}

	protected static float getExtend( ImageFloat32 imageA , ImageFloat32 imageB , int x , int y ) {
		if( x < 0 ) x = 0;
		else if( x >= imageA.width ) x = imageA.width-1;
		if( y < 0 ) y = 0;
		else if( y >= imageA.height ) y = imageA.height-1;

		return imageB.unsafe_get(x,y) - imageA.unsafe_get(x,y);
	}
}
