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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.convolve.ConvolveImageNormalized;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.GrayF32;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Implementation of SSD Weighted Corner for {@link GrayF32} images.
 *
 * @author Peter Abeles
 */
public class ImplSsdCornerWeighted_F32 extends ImplSsdCornerBase<GrayF32, GrayF32> {
	CornerIntensity_F32 intensity;

	Kernel1D_F32 kernel;
	GrayF32 temp = new GrayF32(1, 1);

	public ImplSsdCornerWeighted_F32( int radius, CornerIntensity_F32 intensity ) {
		super(radius, GrayF32.class, GrayF32.class);
		this.intensity = intensity;
		kernel = FactoryKernelGaussian.gaussian(Kernel1D_F32.class, -1, radius);
	}

	@Override
	public void process( GrayF32 derivX, GrayF32 derivY, GrayF32 intensity ) {
		InputSanityCheck.checkSameShape(derivX, derivY);
		intensity.reshape(derivX.width, derivX.height);

		int w = derivX.width;
		int h = derivX.height;

		horizXX.reshape(w, h);
		horizXY.reshape(w, h);
		horizYY.reshape(w, h);
		temp.reshape(w, h);
		intensity.reshape(w, h);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,h,y->{
		for (int y = 0; y < h; y++) {
			int indexX = derivX.startIndex + derivX.stride*y;
			int indexY = derivY.startIndex + derivY.stride*y;

			int index = horizXX.stride*y;
			for (int x = 0; x < w; x++, index++) {
				float dx = derivX.data[indexX++];
				float dy = derivY.data[indexY++];

				horizXX.data[index] = dx*dx;
				horizXY.data[index] = dx*dy;
				horizYY.data[index] = dy*dy;
			}
		}
		//CONCURRENT_ABOVE });

		// apply the the Gaussian weights
		blur(horizXX, temp);
		blur(horizXY, temp);
		blur(horizYY, temp);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,h,y->{
		for (int y = 0; y < h; y++) {
			int index = horizXX.stride*y;
			for (int x = 0; x < w; x++, index++) {
				float totalXX = horizXX.data[index];
				float totalXY = horizXY.data[index];
				float totalYY = horizYY.data[index];

				intensity.data[index] = this.intensity.compute(totalXX, totalXY, totalYY);
			}
		}
		//CONCURRENT_ABOVE });
	}

	private void blur( GrayF32 image, GrayF32 temp ) {
		ConvolveImageNormalized.horizontal(kernel, image, temp);
		ConvolveImageNormalized.vertical(kernel, temp, image);
	}

	@Override
	public int getIgnoreBorder() {
		return 0;
	}
}
