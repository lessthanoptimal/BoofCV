/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.block.score;

import boofcv.struct.image.GrayF32;

/**
 * <p>
 * Implementation of {@link DisparitySparseRectifiedScoreBM} that processes images of type {@link GrayF32}.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class DisparitySparseRectifiedScoreBM_F32 extends DisparitySparseRectifiedScoreBM<float[],GrayF32> {

	// Fit scores as a function of disparity. scores[0] = score at disparity of disparityMin
	protected float[] scores;

	public DisparitySparseRectifiedScoreBM_F32(int radiusX, int radiusY) {
		super(radiusX, radiusY, GrayF32.class);
	}

	@Override
	public void configure(int disparityMin, int disparityRange) {
		super.configure(disparityMin, disparityRange);
		scores = new float[ disparityRange ];
	}

	@Override
	public float[] getScore() {
		return scores;
	}
}
