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

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.convolve.KernelContinuous1D_F32;
import boofcv.struct.image.GrayS16;

/**
 * @author Peter Abeles
 */
public class TestImplInterpolatePixelConvolution_S16 
		extends GeneralChecksInterpolationPixelS<GrayS16>
{
	public TestImplInterpolatePixelConvolution_S16() {
		exceptionOutside = false;
	}

	@Override
	protected GrayS16 createImage(int width, int height) {
		return new GrayS16(width, height);
	}

	@Override
	protected InterpolatePixelS<GrayS16> wrap(GrayS16 image, int minValue, int maxValue) {
		InterpolatePixelS<GrayS16> ret = new ImplInterpolatePixelConvolution_S16(new Dummy(),0,255);
		ret.setImage(image);

		return ret;
	}

	@Override
	protected float compute(GrayS16 img, float x, float y) {
		int xx = (int)x;
		int yy = (int)y;

		int x0 = xx - 2;
		int x1 = xx + 2;
		int y0 = yy - 2;
		int y1 = yy + 2;

		if( x0 < 0 ) x0 = 0;
		if( x1 >= img.width ) x1 = img.width-1;
		if( y0 < 0 ) y0 = 0;
		if( y1 >= img.height ) y1 = img.height-1;

		float total = 0;
		for( int i = y0; i <= y1; i++ ) {
			for( int j = x0; j <= x1; j++ ) {
				total += img.get(j,i);
			}
		}

		total /= (1+x1-x0)*(1+y1-y0);

		return total;
	}

	private static class Dummy extends KernelContinuous1D_F32
	{

		private Dummy() {
			super(5);
		}

		@Override
		public double getDouble(int index) {
			return 0;
		}

		@Override
		public void setD(int index, double value) {}

		@Override
		public boolean isInteger() {
			return false;
		}

		@Override
		public <T extends KernelBase> T copy() {
			return null;
		}

		@Override
		public float compute(float x) {
			return 1.0f/5.0f;
		}
	}
}
