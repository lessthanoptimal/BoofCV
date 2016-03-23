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

import boofcv.alg.InputSanityCheck;
import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.alg.filter.convolve.ConvolveNormalized;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayS32;

/**
 * @author Peter Abeles
 */
public abstract class ImplSsdCornerWeighted_S16 implements GradientCornerIntensity<GrayS16> {

	int radius;
	Kernel1D_I32 kernel;
	GrayS32 imgXX = new GrayS32(1,1);
	GrayS32 imgYY = new GrayS32(1,1);
	GrayS32 imgXY = new GrayS32(1,1);
	GrayS32 temp = new GrayS32(1,1);

	// defines the A matrix, from which the eignevalues are computed
	protected int totalXX, totalYY, totalXY;

	public ImplSsdCornerWeighted_S16(int radius) {
		this.radius = radius;
		kernel = FactoryKernelGaussian.gaussian(Kernel1D_I32.class, -1, radius);
	}

	@Override
	public void process(GrayS16 derivX, GrayS16 derivY, GrayF32 intensity ) {
		InputSanityCheck.checkSameShape(derivX, derivY, intensity);

		int w = derivX.width;
		int h = derivX.height;
		
		imgXX.reshape(w,h);
		imgYY.reshape(w,h);
		imgXY.reshape(w,h);
		temp.reshape(w,h);
		intensity.reshape(w,h);

		int index = 0;
		for( int y = 0; y < h; y++ ) {
			int indexX = derivX.startIndex + derivX.stride*y;
			int indexY = derivY.startIndex + derivY.stride*y;

			for( int x = 0; x < w; x++ , index++ ) {
				int dx = derivX.data[indexX++];
				int dy = derivY.data[indexY++];

				imgXX.data[index] = dx*dx;
				imgYY.data[index] = dy*dy;
				imgXY.data[index] = dx*dy;
			}
		}

		// apply the the Gaussian weights
		blur(imgXX,temp);
		blur(imgYY,temp);
		blur(imgXY,temp);

		index = 0;
		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < w; x++ , index++ ) {
				totalXX = imgXX.data[index];
				totalYY = imgYY.data[index];
				totalXY = imgXY.data[index];

				intensity.data[index] = computeResponse();
			}
		}
	}

	protected abstract float computeResponse();

	private void blur(GrayS32 image , GrayS32 temp ) {
		ConvolveNormalized.horizontal(kernel, image, temp);
		ConvolveNormalized.vertical(kernel,temp,image);
	}

	@Override
	public int getRadius() {
		return radius;
	}


	@Override
	public int getIgnoreBorder() {
		return 0;
	}
}
