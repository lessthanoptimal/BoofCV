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

package boofcv.alg.disparity.block;

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
public interface BlockRowScoreSad
{
	abstract class SadArrayS32<T extends GrayI<T>,ImageData> extends BlockRowScore.ArrayS32_BS32<T,ImageData> {
		SadArrayS32( int maxPerPixel ) {
			super(maxPerPixel);
		}

		@Override
		public boolean isRequireNormalize() {
			return false;
		}
	}

	abstract class SadArrayF32 extends BlockRowScore.ArrayS32_BF32 {
		@Override
		public boolean isRequireNormalize() {
			return false;
		}

		@Override
		public int getMaxPerPixelError() {
			throw new RuntimeException("Not supported for float images");
		}

	}

	class U8 extends SadArrayS32<GrayU8,byte[]> {
		public U8() {
			super(255);
		}

		@Override
		public void score(byte[] leftRow, byte[] rightRow, int indexLeft, int indexRight, int offset, int length, int[] elementScore) {
			for( int i = 0; i < length; i++ ) {
				int difference = (leftRow[ indexLeft++ ]& 0xFF) - (rightRow[ indexRight++ ]& 0xFF);
				elementScore[offset+i] = Math.abs(difference);
			}
		}

		@Override
		public ImageType<GrayU8> getImageType() {
			return ImageType.SB_U8;
		}
	}

	class U16 extends SadArrayS32<GrayU16,short[]> {
		public U16() {
			super(-1);
		}
		@Override
		public void score(short[] leftRow, short[] rightRow, int indexLeft, int indexRight, int offset, int length, int[] elementScore) {
			for( int i = 0; i < length; i++ ) {
				int difference = (leftRow[ indexLeft++ ]& 0xFFFF) - (rightRow[ indexRight++ ]& 0xFFFF);
				elementScore[offset+i] = Math.abs(difference);
			}
		}

		@Override
		public ImageType<GrayU16> getImageType() {
			return ImageType.SB_U16;
		}
	}

	class S16 extends SadArrayS32<GrayS16,short[]> {
		public S16() {
			super(-1);
		}

		@Override
		public void score(short[] leftRow, short[] rightRow, int indexLeft, int indexRight, int offset, int length, int[] elementScore) {
			for( int rCol = 0; rCol < length; rCol++ ) {
				int difference = leftRow[ indexLeft++ ] - rightRow[ indexRight++ ];
				elementScore[offset+rCol] = Math.abs(difference);
			}
		}

		@Override
		public ImageType<GrayS16> getImageType() {
			return ImageType.SB_S16;
		}
	}

	class F32 extends SadArrayF32 {
		@Override
		public void score(float[] leftRow, float[] rightRow, int indexLeft, int indexRight, int offset, int length, float[] elementScore) {
			for( int i = 0; i < length; i++ ) {
				float difference = leftRow[ indexLeft++ ] - rightRow[ indexRight++ ];
				elementScore[offset+i] = Math.abs(difference);
			}
		}

		@Override
		public ImageType<GrayF32> getImageType() {
			return ImageType.SB_F32;
		}
	}
}
