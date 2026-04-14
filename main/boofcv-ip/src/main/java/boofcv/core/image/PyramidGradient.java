/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.core.image;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.PyramidDiscrete;

/// Updates a [PyramidDiscrete] and its derivative.
@SuppressWarnings({"NullAway.Init","unchecked"})
public class PyramidGradient<I extends ImageGray<I>, D extends ImageGray<D>> {
	public PyramidDiscrete<I> basePyramid;
	public D[] derivX;
	public D[] derivY;

	ImageGradient<I,D> gradient;

	public PyramidGradient( PyramidDiscrete<I> pyramid, ImageGradient<I,D> gradient ) {
		this.basePyramid = pyramid.copyStructure();
		this.gradient = gradient;
	}

	public void update( I image ) {
		basePyramid.process(image);
		if (derivX == null || derivX.length != basePyramid.layers.length) {
			derivX = PyramidOps.declareOutput(basePyramid, gradient.getDerivativeType());
			derivY = PyramidOps.declareOutput(basePyramid, gradient.getDerivativeType());
		}

		if (derivX[0].width != basePyramid.getLayer(0).width ||
				derivX[0].height != basePyramid.getLayer(0).height) {
			PyramidOps.reshapeOutput(basePyramid, derivX);
			PyramidOps.reshapeOutput(basePyramid, derivY);
		}
		PyramidOps.gradient(basePyramid, gradient, derivX, derivY);
	}

	public ImageType<I> getInputType() {
		return gradient.getInputType();
	}

	public ImageType<D> getDerivativeType() {
		return gradient.getDerivativeType();
	}
}
