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

package boofcv.abst.feature.detect.intensity;

import boofcv.alg.filter.derivative.DerivativeLaplacian;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.struct.ListIntPoint2D;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper around {@link DerivativeLaplacian} for {@link BaseGeneralFeatureIntensity}.
 *
 * @author Peter Abeles
 */
public class WrapperLaplacianBlobIntensity<I extends ImageGray<I>, D extends ImageGray<D>>
		extends BaseGeneralFeatureIntensity<I, D> {

	public WrapperLaplacianBlobIntensity( Class<I> imageType ) {
		super(imageType, null);
	}

	@Override
	public void process( I image, @Nullable D derivX, @Nullable D derivY,
						 @Nullable D derivXX, @Nullable D derivYY, @Nullable D derivXY ) {
		init(image.width, image.height);
		if (image instanceof GrayU8) {
			DerivativeLaplacian.process((GrayU8)image, intensity);
		} else if (image instanceof GrayF32) {
			GImageDerivativeOps.laplace(image, intensity, BorderType.SKIP);
			// border would ideally be EXTENDED but the special code for U8 doesn't support borders yet
			// when this is fixed change ignoreBorder to 0 below
		} else {
			throw new IllegalArgumentException("Unsupported input image type");
		}
	}

	@Override
	public @Nullable ListIntPoint2D getCandidatesMin() {
		return null;
	}

	@Override
	public @Nullable ListIntPoint2D getCandidatesMax() {
		return null;
	}

	@Override
	public boolean getRequiresGradient() {
		return false;
	}

	@Override
	public boolean getRequiresHessian() {
		return false;
	}

	@Override
	public boolean hasCandidates() {
		return false;
	}

	@Override
	public int getIgnoreBorder() {
		return 1;
	}

	@Override
	public boolean localMinimums() {
		return true;
	}

	@Override
	public boolean localMaximums() {
		return true;
	}
}
