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
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.GrayF32;

/**
 * @author Peter Abeles
 */
public abstract class ImplSsdCornerWeighted_F32 implements GradientCornerIntensity<GrayF32> {
	
	int radius;
	Kernel1D_F32 kernel;
	GrayF32 imgXX = new GrayF32(1,1);
	GrayF32 imgYY = new GrayF32(1,1);
	GrayF32 imgXY = new GrayF32(1,1);
	GrayF32 temp = new GrayF32(1,1);

	// defines the A matrix, from which the eignevalues are computed
	protected float totalXX, totalYY, totalXY;

	public ImplSsdCornerWeighted_F32(int radius) {
		this.radius = radius;
		kernel = FactoryKernelGaussian.gaussian(Kernel1D_F32.class, -1, radius);
	}

	@Override
	public void process(GrayF32 derivX, GrayF32 derivY, GrayF32 intensity ) {
		InputSanityCheck.checkSameShape(derivX,derivY,intensity);

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
				float dx = derivX.data[indexX++];
				float dy = derivY.data[indexY++];

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

	private void blur(GrayF32 image , GrayF32 temp ) {
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
