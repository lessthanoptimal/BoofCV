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

package boofcv.alg.interpolate.impl;

import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;


/**
 * <p>
 * Performs bilinear interpolation to extract values between pixels in an image.
 * Image borders are detected and handled appropriately.
 * </p>
 *
 * <p>
 * NOTE: This code was automatically generated using {@link GenerateBilinearRectangle}.
 * </p>
 *
 * @author Peter Abeles
 */
public class BilinearRectangle_U8 implements InterpolateRectangle<GrayU8> {

	private GrayU8 orig;

	private byte data[];
	private int stride;

	public BilinearRectangle_U8(GrayU8 image) {
		setImage(image);
	}

	public BilinearRectangle_U8() {
	}

	@Override
	public void setImage(GrayU8 image) {
		this.orig = image;
		this.data = orig.data;
		this.stride = orig.getStride();
	}

	@Override
	public GrayU8 getImage() {
		return orig;
	}

	@Override
	public void region(float tl_x, float tl_y, GrayF32 output ) {
		if( tl_x < 0 || tl_y < 0 || tl_x + output.width > orig.width || tl_y + output.height > orig.height ) {
			throw new IllegalArgumentException("Region is outside of the image");
		}
		int xt = (int) tl_x;
		int yt = (int) tl_y;
		float ax = tl_x - xt;
		float ay = tl_y - yt;

		float bx = 1.0f - ax;
		float by = 1.0f - ay;

		float a0 = bx * by;
		float a1 = ax * by;
		float a2 = ax * ay;
		float a3 = bx * ay;

		int regWidth = output.width;
		int regHeight = output.height;
		final float results[] = output.data;
		boolean borderRight = false;
		boolean borderBottom = false;

		// make sure it is in bounds or if its right on the image border
		if (xt + regWidth >= orig.width || yt + regHeight >= orig.height) {
			if( (xt + regWidth > orig.width || yt + regHeight > orig.height) )
				throw new IllegalArgumentException("requested region is out of bounds");
			if( xt+regWidth == orig.width ) {
				regWidth--;
				borderRight = true;
			}
			if( yt+regHeight == orig.height ) {
				regHeight--;
				borderBottom = true;
			}
		}

		// perform the interpolation while reducing the number of times the image needs to be accessed
		for (int i = 0; i < regHeight; i++) {
			int index = orig.startIndex + (yt + i) * stride + xt;
			int indexResults = output.startIndex + i*output.stride;

			float XY = data[index]& 0xFF;
			float Xy = data[index + stride]& 0xFF;

			int indexEnd = index + regWidth;
			// for( int j = 0; j < regWidth; j++, index++ ) {
			for (; index < indexEnd; index++) {
				float xY = data[index + 1]& 0xFF;
				float xy = data[index + stride + 1]& 0xFF;

				float val = a0 * XY + a1 * xY + a2 * xy + a3 * Xy;

				results[indexResults++] = val;
				XY = xY;
				Xy = xy;
			}
		}
		
		// if touching the image border handle the special case
		if( borderBottom || borderRight )
			handleBorder(output, xt, yt, ax, ay, bx, by, regWidth, regHeight, results, borderRight, borderBottom);
	}

	private void handleBorder( GrayF32 output,
							  int xt, int yt,
							  float ax, float ay, float bx, float by,
							  int regWidth, int regHeight, float[] results,
							  boolean borderRight, boolean borderBottom) {

		if( borderRight ) {
			for( int y = 0; y < regHeight; y++ ) {
				int index = orig.startIndex + (yt + y) * stride + xt + regWidth;
				int indexResults = output.startIndex + y*output.stride + regWidth;

				float XY = data[index]& 0xFF;
				float Xy = data[index + stride]& 0xFF;

				results[indexResults] = by*XY + ay*Xy;
			}

			if( borderBottom ) {
				output.set(regWidth,regHeight, orig.get(xt+ regWidth,yt+regHeight));
			} else {
				float XY = orig.get(xt+ regWidth,yt+regHeight-1);
				float Xy = orig.get(xt+ regWidth,yt+regHeight);

				output.set(regWidth,regHeight-1, by*XY + ay*Xy);
			}
		}
		if( borderBottom ) {
			for( int x = 0; x < regWidth; x++ ) {
				int index = orig.startIndex + (yt + regHeight) * stride + xt + x;
				int indexResults = output.startIndex + regHeight *output.stride + x;

				float XY = data[index]& 0xFF;
				float Xy = data[index + 1]& 0xFF;

				results[indexResults] = bx*XY + ax*Xy;
			}

			if( !borderRight ) {
				float XY = orig.get(xt+regWidth-1,yt+ regHeight);
				float Xy = orig.get(xt+regWidth, regHeight);

				output.set(regWidth-1, regHeight, by*XY + ay*Xy);
			}
		}
	}
}
