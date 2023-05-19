/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.disparity;

import boofcv.alg.disparity.DisparityBlockMatchRowFormat;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for wrapped block matching algorithms.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class WrapBaseBlockMatch<In extends ImageGray<In>, T extends ImageGray<T>, DI extends ImageGray<DI>>
		implements StereoDisparity<In, DI> {
	DisparityBlockMatchRowFormat<T, DI> alg;

	DI disparity;
	@Nullable GrayF32 score;

	protected WrapBaseBlockMatch( DisparityBlockMatchRowFormat<T, DI> alg ) {
		this.alg = alg;
	}

	@Override public void process( In imageLeft, In imageRight ) {
		if (disparity == null || disparity.width != imageLeft.width || disparity.height != imageLeft.height) {
			// make sure the image borders are marked as invalid
			disparity = GeneralizedImageOps.createSingleBand(alg.getDisparityType(), imageLeft.width, imageLeft.height);
			GImageMiscOps.fill(disparity, getInvalidValue());
			// TODO move this outside and run it every time. Need to fill border
			//      left border will be radius + min disparity
		}

		disparity.reshape(imageLeft);
		if (score != null)
			score.reshape(disparity);

		_process(imageLeft, imageRight);
	}

	protected abstract void _process( In imageLeft, In imageRight );

	public void setScoreEnabled( boolean enabled ) {
		// see if the current state matches the request
		if (enabled && score != null)
			return;

		if (enabled) {
			score = new GrayF32(1, 1);
		} else {
			score = null;
		}
	}

	@Override public DI getDisparity() {
		return disparity;
	}

	@Override public @Nullable GrayF32 getDisparityScore() {
		return score;
	}

	@Override public int getBorderX() {
		return alg.getBorderX();
	}

	@Override public int getBorderY() {
		return alg.getBorderY();
	}

	@Override public int getDisparityMin() {
		return alg.getDisparityMin();
	}

	@Override public int getDisparityRange() {
		return alg.getDisparityRange();
	}

	@Override public int getInvalidValue() {
		return getDisparityRange();
	}

	@Override public Class<DI> getDisparityType() {
		return alg.getDisparityType();
	}

	public DisparityBlockMatchRowFormat<T, DI> getAlg() {
		return alg;
	}
}
