/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.interpolate.BilinearPixelMB;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.struct.border.ImageBorder_IL_S32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedS16;

import javax.annotation.Generated;

/**
 * <p>
 * Implementation of {@link BilinearPixelMB} for a specific image type.
 * </p>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateImplBilinearPixel_IL</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.interpolate.impl.GenerateImplBilinearPixel_IL")
public class ImplBilinearPixel_IL_S16 extends BilinearPixelMB<InterleavedS16> {
	int[] temp0;
	int[] temp1;
	int[] temp2;
	int[] temp3;

	public ImplBilinearPixel_IL_S16(int numBands) {
		this.temp0 = new int[numBands];
		this.temp1 = new int[numBands];
		this.temp2 = new int[numBands];
		this.temp3 = new int[numBands];
	}

	public ImplBilinearPixel_IL_S16(InterleavedS16 orig) {
		this(orig.getNumBands());
		setImage(orig);
	}

	@Override
	public void setImage(InterleavedS16 image) {
		if( image.getNumBands() != temp0.length )
			throw new IllegalArgumentException("Number of bands doesn't match");
		super.setImage(image);
	}
	@Override
	public void get_fast(float x, float y, float[] values) {
		int xt = (int) x;
		int yt = (int) y;
		float ax = x - xt;
		float ay = y - yt;

		final int numBands = orig.numBands;
		int index = orig.startIndex + yt * stride + xt*numBands;

		short[] data = orig.data;

		// computing this just once doesn't seem to change speed very much. Keeping it here to avoid trying
		// it again in the future
		float a00 = (1.0f - ax) * (1.0f - ay);
		float a10 = ax * (1.0f - ay);
		float a11 = ax * ay;
		float a01 = (1.0f - ax) * ay;

		for( int i = 0; i < numBands; i++ ) {
			int indexBand = index+i;
			float val = a00 * (data[indexBand] );                // (x,y)
			val += a10 * (data[indexBand + numBands ] );         // (x+1,y)
			val += a11 * (data[indexBand + numBands + stride] ); // (x+1,y+1)
			val += a01 * (data[indexBand + stride] );            // (x,y+1)

			values[i] = val;
		}
	}

	public void get_border(float x, float y, float[] values) {
		float xf = (float)Math.floor(x);
		float yf = (float)Math.floor(y);
		int xt = (int) xf;
		int yt = (int) yf;
		float ax = x - xf;
		float ay = y - yf;

		ImageBorder_IL_S32 border = (ImageBorder_IL_S32)this.border;
		border.get(xt   , yt  , temp0);
		border.get(xt+1 , yt  , temp1);
		border.get(xt+1 , yt+1, temp2);
		border.get(xt   , yt+1, temp3);

		final int numBands = orig.numBands;

		for( int i = 0; i < numBands; i++ ) {
			float val = (1.0f - ax) * (1.0f - ay) * (float)temp0[i]; // (x,y)
			val += ax * (1.0f - ay) * (float)temp1[i];               // (x+1,y)
			val += ax * ay * (float)temp2[i];                        // (x+1,y+1)
			val += (1.0f - ax) * ay * (float)temp3[i];               // (x,y+1)

			values[i] = val;
		}
	}

	@Override
	public void get(float x, float y, float[] values) {
		if (x < 0 || y < 0 || x > width-2 || y > height-2)
			get_border(x,y,values);
		else
			get_fast(x, y, values);
	}

	@Override
	public InterpolatePixelMB<InterleavedS16> copy() {
		var out = new ImplBilinearPixel_IL_S16(temp0.length);
		out.setBorder(border.copy());
		return out;
	}
	@Override
	public ImageType<InterleavedS16> getImageType() {
		return orig.getImageType();
	}

}
