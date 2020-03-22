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

package boofcv.alg.feature.disparity.block.select;

/**
 * <p>
 * Subpixel accuracy for disparity.  See {@link SelectErrorSubpixel} for more details on the
 * mathematics.
 * </p>
 *
 * @author Peter Abeles
 */
public class SelectSparseCorrelationSubpixel {

	public static class F32 extends SelectSparseCorrelationWithChecksWta_F32 {
		public F32( double texture) {
			super(texture);
		}

		@Override
		public boolean select(float[] scores, int disparityRange) {
			if( super.select(scores, disparityRange) ) {

				int disparityValue = (int)disparity;

				if( disparityValue == 0 || disparityValue == disparityRange -1) {
					return true;
				} else {
					float c0 = scores[disparityValue-1];
					float c1 = scores[disparityValue];
					float c2 = scores[disparityValue+1];

					float offset = (c0-c2)/(2f*(c0-2*c1+c2));

					disparity += offset;
					return true;
				}

			} else {
				return false;
			}
		}

	}
}
