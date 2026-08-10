/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.blur.BlurStorageFilter;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;

/// Score using ZSSD. Like [BlockRowScoreNcc] it can be computed by applying block filters to the image, since
/// the mean of a difference is the difference of the means.
///
/// ZSSD = sum( ((L(x) - u_l(x)) - (R(x+d) - u_r(x+d)))^2 ) = SSD - N\*(u_l(x) - u_r(x+d))^2
public interface BlockRowScoreZssd {
	class F32 extends BlockRowScore.ArrayS32_BF32 {
		// Local mean of the left and right image
		private final GrayF32 meanL = new GrayF32(1, 1);
		private final GrayF32 meanR = new GrayF32(1, 1);
		private final BlurStorageFilter<GrayF32> meanFilter;

		public F32( int radiusWidth, int radiusHeight ) {
			meanFilter = FactoryBlurFilter.meanB(ImageType.SB_F32, radiusWidth, radiusHeight, null);
		}

		@Override
		public void setInput( GrayF32 left, GrayF32 right ) {
			super.setInput(left, right);
			meanFilter.process(left, meanL);
			meanFilter.process(right, meanR);
		}

		@Override
		public void setBorder( ImageBorder<GrayF32> border ) {
			super.setBorder(border);
			meanFilter.setBorder(border.copy());
		}

		@Override
		public void score( float[] leftRow, float[] rightRow, int indexLeft, int indexRight,
						   int offset, int length, float[] elementScore ) {
			for (int i = 0; i < length; i++) {
				float difference = leftRow[indexLeft++] - rightRow[indexRight++];
				elementScore[offset + i] = difference*difference;
			}
		}

		@Override
		public int getMaxPerPixelError() {
			throw new RuntimeException("Not supported for float images");
		}

		@Override
		public boolean isRequireNormalize() {
			return true;
		}

		@Override
		public void normalizeScore( int row, int colLeft, int colRight, int numCols,
									int regionWidth, int regionHeight,
									float[] scores, int indexScores, float[] scoresNorm ) {
			final float area = regionWidth*regionHeight;

			if (row < 0 || row >= left.height)
				throw new IllegalArgumentException("Egads. row=" + row);

			int stride = meanL.stride;
			int idxLeft = row*stride + colLeft;
			int idxRight = row*stride + colRight;

			for (int i = 0; i < numCols; i++, idxLeft++, idxRight++) {
				float deltaMean = meanL.data[idxLeft] - meanR.data[idxRight];
				scoresNorm[indexScores + i] = scores[indexScores + i] - area*deltaMean*deltaMean;
			}
		}

		@Override
		public ImageType<GrayF32> getImageType() {
			return ImageType.SB_F32;
		}
	}
}
