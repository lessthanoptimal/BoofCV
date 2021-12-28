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
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Unweigthed or box filter version of {@link ImplSsdCornerBase}
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class ImplSsdCornerBox<D extends ImageGray<D>, D2 extends ImageGray<D2>>
		extends ImplSsdCornerBase<D, D2> {
	protected ImplSsdCornerBox( int windowRadius, Class<D> derivType, Class<D2> secondDerivType ) {
		super(windowRadius, derivType, secondDerivType);
	}

	@Override
	public void process( D derivX, D derivY, GrayF32 intensity ) {
		InputSanityCheck.checkSameShape(derivX, derivY);
		intensity.reshape(derivX.width, derivY.height);

		setImageShape(derivX.getWidth(), derivX.getHeight());
		this.derivX = derivX;
		this.derivY = derivY;

		// there is no intensity computed along the border. Make sure it's always zero
		// In the future it might be better to fill it with meaningful data, even if it's
		// from a partial region
		ImageMiscOps.fillBorder(intensity, 0, radius);
		horizontal();
		vertical(intensity);
	}

	protected abstract void horizontal();

	protected abstract void vertical( GrayF32 intensity );
}
