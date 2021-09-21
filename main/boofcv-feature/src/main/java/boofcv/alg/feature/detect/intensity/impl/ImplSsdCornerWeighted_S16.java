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
import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.alg.filter.convolve.ConvolveImageNormalized;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayS32;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Implementation of SSD Weighted Corner for {@link GrayS16} images.
 *
 * @author Peter Abeles
 */
public class ImplSsdCornerWeighted_S16 extends ImplSsdCornerBase<GrayS16, GrayS32>
		implements GradientCornerIntensity<GrayS16> {
	CornerIntensity_S32 intensity;

	Kernel1D_S32 kernel;
	GrayS32 temp = new GrayS32(1, 1);

	public ImplSsdCornerWeighted_S16( int radius, CornerIntensity_S32 intensity ) {
		super(radius, GrayS16.class, GrayS32.class);
		this.intensity = intensity;
		kernel = FactoryKernelGaussian.gaussian(Kernel1D_S32.class, -1, radius);
	}

	@Override
	public void process( GrayS16 derivX, GrayS16 derivY, GrayF32 intensity ) {
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
				int dx = derivX.data[indexX++];
				int dy = derivY.data[indexY++];

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
				int totalXX = horizXX.data[index];
				int totalXY = horizXY.data[index];
				int totalYY = horizYY.data[index];

				intensity.data[index] = this.intensity.compute(totalXX, totalXY, totalYY);
			}
		}
		//CONCURRENT_ABOVE });
	}

	private void blur( GrayS32 image, GrayS32 temp ) {
		ConvolveImageNormalized.horizontal(kernel, image, temp);
		ConvolveImageNormalized.vertical(kernel, temp, image);
	}

	@Override
	public int getIgnoreBorder() {
		return 0;
	}
}
