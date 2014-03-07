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

package boofcv.alg.flow;

import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;

/**
 * Implementation of {@link HornSchunck} for {@link boofcv.struct.image.ImageFloat32}.
 *
 * @author Peter Abeles
 */
public class HornSchunck_U8 extends HornSchunck<ImageUInt8,ImageSInt16> {

	public HornSchunck_U8(float alpha, int numIterations) {
		super(alpha,numIterations, ImageType.single(ImageSInt16.class));
	}

	@Override
	protected void computeDerivX(ImageUInt8 image1, ImageUInt8 image2, ImageSInt16 derivX) {
		int w = image1.width-1;
		int h = image1.height-1;

		for( int y = 0; y < h; y++ ) {
			int index1 = image1.startIndex + y*image1.stride;
			int index2 = image2.startIndex + y*image2.stride;
			int indexX = derivX.startIndex + y*derivX.stride;

			for( int x = 0; x < w; x++ , index1++ , index2++ , indexX++ ) {
				int d0 = (image1.data[index1+1]&0xFF) - (image1.data[index1]&0xFF);
				int d2 = (image2.data[index2+1]&0xFF) - (image2.data[index2]&0xFF);
				int d1 = (image1.data[index1+1+image1.stride]&0xFF) - (image1.data[index1+image1.stride]&0xFF);
				int d3 = (image2.data[index2+1+image2.stride]&0xFF) - (image2.data[index2+image2.stride]&0xFF);

				derivX.data[indexX] = (short)((d0 + d1 + d2 + d3)/4);
			}
		}

		for( int y = 0; y < image1.height; y++ ) {
			derivX.unsafe_set(w,y, 0);
		}

		for( int x = 0; x < w; x++ ) {
			int d0 = image1.unsafe_get(x+1,h) - image1.unsafe_get(x,h);
			int d1 = image2.unsafe_get(x+1,h) - image2.unsafe_get(x,h);

			derivX.unsafe_set(x,h, (d0+d1)/2);
		}
	}

	@Override
	protected void computeDerivY(ImageUInt8 image1, ImageUInt8 image2, ImageSInt16 derivY) {
		int w = image1.width-1;
		int h = image1.height-1;

		for( int y = 0; y < h; y++ ) {
			int index1 = image1.startIndex + y*image1.stride;
			int index2 = image2.startIndex + y*image2.stride;
			int indexY = derivY.startIndex + y*derivY.stride;

			for( int x = 0; x < w; x++ , index1++ , index2++ , indexY++ ) {
				int d0 = (image1.data[index1+image1.stride]&0xFF) - (image1.data[index1]&0xFF);
				int d2 = (image2.data[index2+image2.stride]&0xFF) - (image2.data[index2]&0xFF);
				int d1 = (image1.data[index1+1+image1.stride]&0xFF) - (image1.data[index1+1]&0xFF);
				int d3 = (image2.data[index2+1+image2.stride]&0xFF) - (image2.data[index2+1]&0xFF);

				derivY.data[indexY] = (short)((d0 + d1 + d2 + d3)/4);
			}
		}

		for( int y = 0; y < h; y++ ) {
			int d0 = image1.unsafe_get(w, y + 1) - image1.unsafe_get(w,y);
			int d1 = image2.unsafe_get(w, y + 1) - image2.unsafe_get(w,y);

			derivY.unsafe_set(w,y, (d0+d1)/2);
		}

		for( int x = 0; x < w; x++ ) {
			derivY.unsafe_set(x, h, 0);
		}
	}

	@Override
	protected void computeDerivT(ImageUInt8 image1, ImageUInt8 image2, ImageSInt16 difference) {
		int w = image1.width-1;
		int h = image1.height-1;

		for( int y = 0; y < h; y++ ) {
			int index1 = image1.startIndex + y*image1.stride;
			int index2 = image2.startIndex + y*image2.stride;
			int indexDiff = difference.startIndex + y*difference.stride;

			for( int x = 0; x < w; x++ , index1++ , index2++ , indexDiff++ ) {
				int d0 = (image2.data[index2]&0xFF) - (image1.data[index1]&0xFF);                                // (x  ,y  )
				int d1 = (image2.data[index2+1]&0xFF) - (image1.data[index1+1]&0xFF);                            // (x+1,y  )
				int d2 = (image2.data[index2+image2.stride]&0xFF) - (image1.data[index1+image1.stride]&0xFF);    // (x  ,y+1)
				int d3 = (image2.data[index2+1+image2.stride]&0xFF) - (image1.data[index1+1+image1.stride]&0xFF);// (x+1,y+1)

				difference.data[indexDiff] = (short)((d0 + d1 + d2 + d3)/4);
			}
		}

		for( int y = 0; y < image1.height; y++ ) {
			borderDerivT(image1, image2, difference, w, y);
		}

		for( int x = 0; x < w; x++ ) {
			borderDerivT(image1, image2, difference, x, h);
		}
	}

	protected static void borderDerivT(ImageUInt8 imageA , ImageUInt8 imageB ,
									   ImageSInt16 difference, int x, int y) {
		float d0 = getBorderT(imageA, imageB, x    , y    );
		float d1 = getBorderT(imageA, imageB, x + 1, y    );
		float d2 = getBorderT(imageA, imageB, x    , y + 1);
		float d3 = getBorderT(imageA, imageB, x + 1, y + 1);

		difference.unsafe_set(x,y, (short)((d0+d1+d2+d3)/4));
	}

	protected static float getBorderT(ImageUInt8 imageA, ImageUInt8 imageB, int x, int y) {
		if( x < 0 ) x = 0;
		else if( x >= imageA.width ) x = imageA.width-1;
		if( y < 0 ) y = 0;
		else if( y >= imageA.height ) y = imageA.height-1;

		return imageB.unsafe_get(x,y) - imageA.unsafe_get(x,y);
	}

	@Override
	protected void findFlow( ImageSInt16 derivX , ImageSInt16 derivY ,
							 ImageSInt16 derivT , ImageFlow output) {

		int N = output.width*output.height;

		for( int iter = 0; iter < numIterations; iter++ ) {

			borderAverageFlow(output,averageFlow);
			innerAverageFlow(output,averageFlow);

			for( int i = 0; i < N; i++ ) {
				float dx = derivX.data[i];
				float dy = derivY.data[i];
				float dt = derivT.data[i];

				ImageFlow.D aveFlow = averageFlow.data[i];

				float u = aveFlow.x;
				float v = aveFlow.y;

				ImageFlow.D flow = output.data[i];
				float r = (dx*u + dy*v + dt)/(alpha2 + dx*dx + dy*dy);
				flow.x = u - dx*r;
				flow.y = v - dy*r;
			}
		}
	}
}
