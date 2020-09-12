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

import boofcv.alg.disparity.sgm.SgmDisparityCost;
import boofcv.alg.disparity.sgm.cost.StereoMutualInformation;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;

/**
 * Block {@link StereoMutualInformation} implementation. You must some how first "train" the MI score or else this
 * will not work. Also pre-compute the scaled cost.
 *
 * @author Peter Abeles
 */
public interface BlockRowScoreMutualInformation {
	class U8 extends BlockRowScore.ArrayS32_BS32<GrayU8,byte[]> {
		StereoMutualInformation mi;
		public U8(StereoMutualInformation mi) {
			super(SgmDisparityCost.MAX_COST);
			this.mi = mi;
		}

		@Override
		public void score(byte[] leftRow, byte[] rightRow, int indexLeft, int indexRight, int offset, int length, int[] elementScore) {
			for( int i = 0; i < length; i++ ) {
				final int a = leftRow[ indexLeft++ ]& 0xFF;
				final int b = rightRow[ indexRight++ ]& 0xFF;
				elementScore[offset+i] = mi.costScaled(a,b);
			}
		}

		@Override
		public boolean isRequireNormalize() {
			return false;
		}

		@Override
		public ImageType<GrayU8> getImageType() {
			return ImageType.SB_U8;
		}
	}
}
