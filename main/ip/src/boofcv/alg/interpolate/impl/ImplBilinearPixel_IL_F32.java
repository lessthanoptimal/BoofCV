/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
import boofcv.core.image.border.ImageBorder_IL_F32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedF32;


/**
 * <p>
 * Implementation of {@link BilinearPixelMB} for a specific image type.
 * </p>
 *
 * <p>
 * NOTE: This code was automatically generated using GenerateImplBilinearPixel_IL.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplBilinearPixel_IL_F32 extends BilinearPixelMB<InterleavedF32> {
	float temp0[];
	float temp1[];
	float temp2[];
	float temp3[];

	public ImplBilinearPixel_IL_F32(int numBands) {
		this.temp0 = new float[numBands];
		this.temp1 = new float[numBands];
		this.temp2 = new float[numBands];
		this.temp3 = new float[numBands];
	}

	public ImplBilinearPixel_IL_F32(InterleavedF32 orig) {
		this(orig.getNumBands());
		setImage(orig);
	}

	@Override
	public void setImage(InterleavedF32 image) {
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

		float[] data = orig.data;

		// computing this just once doesn't seem to change speed very much.  Keeping it here to avoid trying
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

		ImageBorder_IL_F32 border = (ImageBorder_IL_F32)this.border;
		border.get(xt   , yt  , temp0);
		border.get(xt+1 , yt  , temp1);
		border.get(xt+1 , yt+1, temp2);
		border.get(xt   , yt+1, temp3);

		final int numBands = orig.numBands;

		for( int i = 0; i < numBands; i++ ) {
			float val = (1.0f - ax) * (1.0f - ay) * temp0[i]; // (x,y)
			val += ax * (1.0f - ay) * temp1[i];               // (x+1,y)
			val += ax * ay * temp2[i];                        // (x+1,y+1)
			val += (1.0f - ax) * ay * temp3[i];               // (x,y+1)

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
	public ImageType<InterleavedF32> getImageType() {
		return orig.getImageType();
	}

}
