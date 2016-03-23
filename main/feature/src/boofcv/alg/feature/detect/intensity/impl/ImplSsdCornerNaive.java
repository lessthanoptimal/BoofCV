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

import boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

/**
 * Naive implementation of {@link boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity} which performs computations in a straight
 * forward but inefficient manor.  This class is used to validate the correctness of more complex but efficient
 * implementations.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"ForLoopReplaceableByForEach"})
public class ImplSsdCornerNaive<T extends ImageGray> implements ShiTomasiCornerIntensity<T> {

	// feature's radius
	private int radius;

	private Kernel2D_I32 weights;

	public ImplSsdCornerNaive(int imageWidth, int imageHeight,
							  int windowRadius, boolean weighted) {
		this.radius = windowRadius;

		if( weighted )
			weights = FactoryKernelGaussian.gaussian(Kernel2D_I32.class,-1,radius);
	}

	@Override
	public int getRadius() {
		return radius;
	}


	@Override
	public int getIgnoreBorder() {
		return radius;
	}

	@Override
	public void process(T derivX, T derivY, GrayF32 intensity ) {

		final int imgHeight = derivX.getHeight();
		final int imgWidth = derivX.getWidth();

		for (int row = radius; row < imgHeight - radius; row++) {
			for (int col = radius; col < imgWidth - radius; col++) {
				double dxdx = 0;
				double dxdy = 0;
				double dydy = 0;
				double totalW = 0;

				for (int i = -radius; i <= radius; i++) {
					for (int j = -radius; j <= radius; j++) {
						
						double dx = GeneralizedImageOps.get(derivX,col + j, row + i);
						double dy = GeneralizedImageOps.get(derivY,col + j, row + i);

						double w = 1;
						
						if( weights != null )
							w = weights.get(j+radius,i+radius);
						
						dxdx += w * dx * dx;
						dydy += w * dy * dy;
						dxdy += w * dx * dy;
						totalW += w;
					}
				}

				if( weights != null ) {
					dxdx /= totalW;
					dydy /= totalW;
					dxdy /= totalW;
				}

				// compute the eigen values
				double left = (dxdx + dydy) * 0.5;
				double b = (dxdx - dydy) * 0.5;
				double right = Math.sqrt(b * b + dxdy * dxdy);

				intensity.set(col, row, (float) (left - right));
			}
		}
	}

}
