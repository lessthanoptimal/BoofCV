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

package boofcv.alg.transform.ii;

import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.GradientValue_F64;
import boofcv.struct.sparse.SparseImageGradient;

/**
 * Computes the gradient from an integral image. Does not check for border conditions.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class SparseIntegralGradientKernel<T extends ImageGray<T>>
		implements SparseImageGradient<T, GradientValue_F64> {
	// input integral image
	T ii;
	// kernels for computing derivatives along x and y
	IntegralKernel kernelX;
	IntegralKernel kernelY;

	GradientValue_F64 ret = new GradientValue_F64();

	public SparseIntegralGradientKernel( IntegralKernel kernelX, IntegralKernel kernelY ) {
		this.kernelX = kernelX;
		this.kernelY = kernelY;
	}

	@Override
	public boolean isInBounds( int x, int y ) {

		if (!IntegralImageOps.isInBounds(x, y, kernelX, ii.width, ii.height))
			return false;
		if (!IntegralImageOps.isInBounds(x, y, kernelY, ii.width, ii.height))
			return false;

		return true;
	}

	@Override
	public void setImage( T integralImage ) {
		this.ii = integralImage;
	}

	@Override
	public GradientValue_F64 compute( int x, int y ) {

		ret.x = GIntegralImageOps.convolveSparse(ii, kernelX, x, y);
		ret.y = GIntegralImageOps.convolveSparse(ii, kernelY, x, y);

		return ret;
	}

	@Override
	public Class<GradientValue_F64> getGradientType() {
		return GradientValue_F64.class;
	}
}
