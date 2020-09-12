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

package boofcv.alg.disparity.sgm.cost;

import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.alg.disparity.sgm.SgmDisparityCost;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayS64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;

/**
 * Computes the cost as the hamming distance between two pixels.
 *
 * @author Peter Abeles
 */
public abstract class SgmCostHamming<T extends ImageBase<T>> extends SgmCostBase<T> {
	public static class U8 extends SgmCostHamming<GrayU8> {
		@Override
		protected void computeDisparityErrors( int idxLeft, int idxRight, int idxOut, int disparityRange ) {
			int valLeft = left.data[idxLeft] & 0xFF;
			for (int d = 0; d < disparityRange; d++) {
				int valRight = right.data[idxRight--] & 0xFF;
				costXD.data[idxOut + d] = (short)(SgmDisparityCost.MAX_COST*DescriptorDistance.hamming(valLeft ^ valRight)/8);
			}
		}
	}

	public static class S32 extends SgmCostHamming<GrayS32> {
		@Override
		protected void computeDisparityErrors( int idxLeft, int idxRight, int idxOut, int disparityRange ) {
			int valLeft = left.data[idxLeft];
			for (int d = 0; d < disparityRange; d++) {
				int valRight = right.data[idxRight--];
				costXD.data[idxOut + d] = (short)(MAX_COST*DescriptorDistance.hamming(valLeft ^ valRight)/32);
			}
		}
	}

	public static class S64 extends SgmCostHamming<GrayS64> {
		@Override
		protected void computeDisparityErrors( int idxLeft, int idxRight, int idxOut, int disparityRange ) {
			long valLeft = left.data[idxLeft];
			for (int d = 0; d < disparityRange; d++) {
				long valRight = right.data[idxRight--];
				costXD.data[idxOut + d] = (short)(MAX_COST*DescriptorDistance.hamming(valLeft ^ valRight)/64);
			}
		}
	}
}
