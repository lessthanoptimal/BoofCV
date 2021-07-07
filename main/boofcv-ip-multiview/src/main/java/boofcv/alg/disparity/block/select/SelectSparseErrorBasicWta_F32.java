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

package boofcv.alg.disparity.block.select;

import boofcv.alg.disparity.block.DisparitySparseSelect;
import boofcv.alg.disparity.block.score.DisparitySparseRectifiedScoreBM;

/**
 * <p>
 * Selects the disparity with the lowest score with no additional validation. Lack
 * of validation speeds up the code at the cost of reduced signal to noise ratio. This
 * strategy of selecting the lowest score is also known as Winner Take All (WTA).
 * </p>
 *
 * @author Peter Abeles
 */
public class SelectSparseErrorBasicWta_F32 implements DisparitySparseSelect<float[]> {

	// selected disparity
	int disparity;

	@Override
	public boolean select( DisparitySparseRectifiedScoreBM<float[], ?> scorer, int x, int y ) {
		if (!scorer.processLeftToRight(x, y))
			return false;
		float[] scores = scorer.getScoreLtoR();
		int disparityRange = scorer.getLocalRangeLtoR();

		disparity = 0;
		float best = scores[0];

		for (int i = 1; i < disparityRange; i++) {
			if (scores[i] < best) {
				best = scores[i];
				disparity = i;
			}
		}

		return true;
	}

	@Override
	public double getDisparity() {
		return disparity;
	}
}
