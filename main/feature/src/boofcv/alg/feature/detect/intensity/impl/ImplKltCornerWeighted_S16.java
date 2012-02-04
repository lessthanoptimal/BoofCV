/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.alg.feature.detect.intensity.KltCornerIntensity;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;

/**
 * @author Peter Abeles
 */
public class ImplKltCornerWeighted_S16 extends ImplSsdCornerWeighted_S16
		implements KltCornerIntensity<ImageSInt16>
{
	public ImplKltCornerWeighted_S16(int radius) {
		super(radius);
	}

	@Override
	protected float computeResponse() {
		// compute the smallest eigenvalue
		double left = (totalXX + totalYY) * 0.5;
		double b = (totalXX - totalYY) * 0.5;
		double right = Math.sqrt(b * b + totalXY * totalXY);

		// the smallest eigenvalue will be minus the right side
		return (float)(left - right);
	}
}
