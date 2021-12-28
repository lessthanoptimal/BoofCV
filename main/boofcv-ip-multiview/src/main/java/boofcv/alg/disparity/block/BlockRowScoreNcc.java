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

package boofcv.alg.disparity.block;

import boofcv.abst.filter.blur.BlurStorageFilter;
import boofcv.alg.misc.GPixelMath;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ejml.UtilEjml;

/**
 * Score using NCC. Takes advantage for the forumla for NCC error which can be computed by applying block filters
 * to the image.
 *
 * NCC = ((1/N)*[sum( L(x)*R(x+d) ) - u_l(x)*u_r(x+d) )/ (sigma_L(x) * sigma_R(x+d))
 *
 * @author Peter Abeles
 */
public class BlockRowScoreNcc<T extends ImageBase<T>> {
	// Storage for mean of left + right image
	T meanL, meanR;
	// Storage for power of 2 images
	T powL, powR;

	// Storage for stdev images
	T stdevL, stdevR;

	T tmpPow2;

	BlurStorageFilter<T> meanFilter;

	public BlockRowScoreNcc( int radiusX, int radiusY, ImageType<T> imageType ) {
		meanL = imageType.createImage(1, 1);
		meanR = imageType.createImage(1, 1);
		powL = imageType.createImage(1, 1);
		powR = imageType.createImage(1, 1);

		meanFilter = FactoryBlurFilter.meanB(imageType, radiusX, radiusY, null);

		// save memory and use the same filter / images
		tmpPow2 = meanL;
		stdevL = powL;
		stdevR = powR;
	}

	public void setBorder( ImageBorder<T> border ) {
		meanFilter.setBorder(border.copy());
	}

	public void computeStatistics( T left, T right ) {
		// Compute mean of L^2 and R^2
		GPixelMath.pow2(left, tmpPow2);
		meanFilter.process(tmpPow2, powL);
		GPixelMath.pow2(right, tmpPow2);
		meanFilter.process(tmpPow2, powR);

		// Compute mean of L and R
		meanFilter.process(left, meanL);
		meanFilter.process(right, meanR);

		// Compute the sigma from mean and mean^2
		GPixelMath.stdev(meanL, powL, stdevL);
		GPixelMath.stdev(meanR, powR, stdevR);
	}

	public static class F32 extends BlockRowScore.ArrayS32_BF32 {
		BlockRowScoreNcc<GrayF32> helper;
		public float eps = UtilEjml.F_EPS;

		public F32( int radiusWidth, int radiusHeight ) {
			helper = new BlockRowScoreNcc<>(radiusWidth, radiusHeight, ImageType.single(GrayF32.class));
		}

		@Override
		public void setInput( GrayF32 left, GrayF32 right ) {
			super.setInput(left, right);
			helper.computeStatistics(left, right);
		}

		@Override
		public void setBorder( ImageBorder<GrayF32> border ) {
			super.setBorder(border);
			helper.setBorder(border);
		}

		@Override
		public void score( float[] leftRow, float[] rightRow, int indexLeft, int indexRight, int offset, int length, float[] elementScore ) {
			for (int i = 0; i < length; i++) {
				elementScore[offset + i] = leftRow[indexLeft++]*rightRow[indexRight++];
			}
		}

		@Override
		public int getMaxPerPixelError() {
			throw new RuntimeException("There is no maximum error for NCC");
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

			int stride = helper.meanL.stride;
			int idxLeft = row*stride + colLeft;
			int idxRight = row*stride + colRight;

			for (int i = 0; i < numCols; i++, idxLeft++, idxRight++) {
				float correlation = scores[indexScores + i]/area;

				float meanL = helper.meanL.data[idxLeft];
				float meanR = helper.meanR.data[idxRight];
				float sigmaL = helper.stdevL.data[idxLeft];
				float sigmaR = helper.stdevR.data[idxRight];

				// invert score since the minimum is selected for disparity
				scoresNorm[indexScores + i] = (correlation - meanL*meanR)/(eps + sigmaL*sigmaR);
			}
		}

		@Override
		public ImageType<GrayF32> getImageType() {
			return helper.meanL.getImageType();
		}
	}
}
