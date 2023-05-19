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

package boofcv.alg.disparity.block.select;

import boofcv.alg.disparity.block.score.DisparitySparseRectifiedScoreBM;

/**
 * <p>
 * Subpixel accuracy for disparity. See {@link SelectErrorSubpixel} for more details on the
 * mathematics.
 * </p>
 *
 * @author Peter Abeles
 */
public class SelectSparseErrorSubpixel {

	public static class S32 extends SelectSparseErrorWithChecksWta_S32 {
		/** If the error is a squared error. If false then it's assumed to be distance */
		final boolean squaredError;

		public S32( int maxError, double texture, int tolRightToLeft, boolean squaredError ) {
			super(maxError, texture, tolRightToLeft);
			this.squaredError = squaredError;
		}

		@Override
		public boolean select( DisparitySparseRectifiedScoreBM<int[], ?> scorer, int x, int y ) {
			if (super.select(scorer, x, y)) {
				int disparityRange = scorer.getLocalRangeLtoR();
				int[] scores = scorer.getScoreLtoR();
				int disparityValue = (int)disparity;

				if (disparityValue == 0 || disparityValue == disparityRange - 1) {
					return true;
				} else {
					double c0 = scores[disparityValue - 1];
					double c1 = scores[disparityValue];
					double c2 = scores[disparityValue + 1];

					if (!squaredError) {
						c0 *= c0;
						c1 *= c1;
						c2 *= c2;
					}

					double offset = (c0 - c2)/(2.0*(c0 - 2.0*c1 + c2));

					disparity += offset;
					return true;
				}
			} else {
				return false;
			}
		}
	}

	public static class F32 extends SelectSparseErrorWithChecksWta_F32 {
		/** If the error is a squared error. If false then it's assumed to be distance */
		final boolean squaredError;

		public F32( int maxError, double texture, int tolRightToLeft, boolean squaredError ) {
			super(maxError, texture, tolRightToLeft);
			this.squaredError = squaredError;
		}

		@Override
		public boolean select( DisparitySparseRectifiedScoreBM<float[], ?> scorer, int x, int y ) {
			if (super.select(scorer, x, y)) {
				int disparityRange = scorer.getLocalRangeLtoR();
				float[] scores = scorer.getScoreLtoR();
				int disparityValue = (int)disparity;

				if (disparityValue == 0 || disparityValue == disparityRange - 1) {
					return true;
				} else {
					float c0 = scores[disparityValue - 1];
					float c1 = scores[disparityValue];
					float c2 = scores[disparityValue + 1];

					if (!squaredError) {
						c0 *= c0;
						c1 *= c1;
						c2 *= c2;
					}

					float offset = (c0 - c2)/(2f*(c0 - 2*c1 + c2));

					disparity += offset;
					return true;
				}
			} else {
				return false;
			}
		}
	}
}
