/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.block;

import boofcv.alg.feature.disparity.block.BlockRowScore;
import boofcv.struct.image.*;

/**
 * Computes the Sum of Absolute Difference (SAD) for block matching based algorithms.
 *
 * Notes on scoreSad():
 * compute the score for each element all at once to encourage the JVM to optimize and
 * encourage the JVM to optimize this section of code.
 *
 * Was original inline, but was actually slightly slower by about 3% consistently,  It
 * is in its own function so that it can be overridden and have different cost functions
 * inserted easily.
 *
 * @author Peter Abeles
 */
public abstract class BlockRowScoreSad<T extends ImageBase<T>,Array>
	implements BlockRowScore<T,Array>
{
	public static class U8 extends ArrayS32<GrayU8> {
		@Override
		public void score(GrayU8 left, GrayU8 right, int elementMax, int indexLeft, int indexRight, int[] elementScore) {
			for( int rCol = 0; rCol < elementMax; rCol++ ) {
				int diff = (left.data[ indexLeft++ ]& 0xFF) - (right.data[ indexRight++ ]& 0xFF);

				elementScore[rCol] = Math.abs(diff);
			}
		}

		@Override
		public ImageType<GrayU8> getImageType() {
			return ImageType.single(GrayU8.class);
		}
	}

	public static class U16 extends ArrayS32<GrayU16> {
		@Override
		public void score(GrayU16 left, GrayU16 right, int elementMax, int indexLeft, int indexRight, int[] elementScore) {
			for( int rCol = 0; rCol < elementMax; rCol++ ) {
				int diff = (left.data[ indexLeft++ ]& 0xFFFF) - (right.data[ indexRight++ ]& 0xFFFF);

				elementScore[rCol] = Math.abs(diff);
			}
		}

		@Override
		public ImageType<GrayU16> getImageType() {
			return ImageType.single(GrayU16.class);
		}
	}

	public static class S16 extends ArrayS32<GrayS16> {
		@Override
		public void score(GrayS16 left, GrayS16 right, int elementMax, int indexLeft, int indexRight, int[] elementScore) {
			for( int rCol = 0; rCol < elementMax; rCol++ ) {
				int diff = left.data[ indexLeft++ ] - right.data[ indexRight++ ];

				elementScore[rCol] = Math.abs(diff);
			}
		}

		@Override
		public ImageType<GrayS16> getImageType() {
			return ImageType.single(GrayS16.class);
		}
	}

	public static class F32 extends ArrayF32<GrayF32> {
		@Override
		public void score(GrayF32 left, GrayF32 right, int elementMax, int indexLeft, int indexRight, float[] elementScore) {
			for( int rCol = 0; rCol < elementMax; rCol++ ) {
				float diff = left.data[ indexLeft++ ] - right.data[ indexRight++ ];

				elementScore[rCol] = Math.abs(diff);
			}
		}

		@Override
		public ImageType<GrayF32> getImageType() {
			return ImageType.single(GrayF32.class);
		}
	}
}
