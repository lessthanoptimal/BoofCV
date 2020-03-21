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

package boofcv.alg.feature.disparity.block;

import boofcv.abst.transform.census.FilterCensusTransform;
import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.alg.feature.disparity.block.score.DisparitySparseRectifiedScoreBM_S32;
import boofcv.struct.image.*;

/**
 * Disparity score functions for sparse Census.
 *
 * WARNING: This will not produce identical results to the dense implementation at the image border. In the
 * dense implementation it computes the Census transform for the entire image. Then when it computes the block
 * score it ventures outside the image again and by default will reflect while in the sparse case it uses
 * a padded sub image and will not trigger a border situation. To fix this problem would require a lot of
 * complex specialized code and would most likely not produce significantly better results.
 *
 * @author Peter Abeles
 */
public interface SparseScoreRectifiedCensus {

	/**
	 * Applies a census transform to the input image and creates a new transformed image patch for later processing
	 */
	abstract class Census<In extends GrayI<In>, Out extends ImageGray<Out>>
			extends DisparitySparseRectifiedScoreBM_S32<In>
	{
		// Applies census transform to input iamges
		FilterCensusTransform<In,Out> censusTran;

		// census transform applied to left and right image patches
		Out censusLeft, censusRight;

		public Census(int radiusX, int radiusY, FilterCensusTransform<In,Out> censusTran, Class<In> imageType ) {
			super(radiusX, radiusY, imageType);
			this.censusTran = censusTran;
			setSampleRegion(censusTran.getRadiusX(), censusTran.getRadiusY());

			censusLeft = censusTran.getOutputType().createImage(1,1);
			censusRight = censusTran.getOutputType().createImage(1,1);
		}

		@Override
		public void configure( int disparityMin , int disparityRange ) {
			super.configure(disparityMin,disparityRange);
			censusLeft.reshape(patchLeft);
		}

		@Override
		protected void scoreDisparity(int disparityRange) {
			censusRight.reshape(patchRight);

			// NOTE: the borders do not need to be processed
			censusTran.process(patchLeft,censusLeft);
			censusTran.process(patchRight,censusRight);

			scoreCensus(disparityRange);
		}

		protected abstract void scoreCensus( int disparityRange );
	}

	/**
	 * Computes census score for transformed images of type U8
	 */
	class U8<T extends GrayI<T>> extends Census<T,GrayU8> {
		public U8(int radiusX, int radiusY,
				  FilterCensusTransform<T, GrayU8> censusTran,
				  Class<T> imageType)
		{
			super(radiusX, radiusY, censusTran, imageType);
		}

		@Override
		protected void scoreCensus(int disparityRange) {
			final byte[] dataLeft  = censusLeft.data;
			final byte[] dataRight = censusRight.data;
			for (int d = 0; d < disparityRange; d++) {
				int total = 0;
				for (int y = 0; y < blockHeight; y++) {
					int idxLeft  = (y+sampleRadiusY)*censusLeft.stride + sampleRadiusX;
					int idxRight = (y+sampleRadiusY)*censusRight.stride + sampleRadiusX + d;
					for (int x = 0; x < blockWidth; x++) {
						final int a = dataLeft[ idxLeft++ ]& 0xFF;
						final int b = dataRight[ idxRight++ ]& 0xFF;
						total += DescriptorDistance.hamming(a^b);
					}
				}
				scores[disparityRange-d-1] = total;
			}
		}
	}

	/**
	 * Computes census score for transformed images of type S32
	 */
	class S32<T extends GrayI<T>> extends Census<T,GrayS32> {
		public S32(int radiusX, int radiusY,
				   FilterCensusTransform<T, GrayS32> censusTran,
				  Class<T> imageType)
		{
			super(radiusX, radiusY, censusTran, imageType);
		}

		@Override
		protected void scoreCensus(int disparityRange) {
			final int[] dataLeft  = censusLeft.data;
			final int[] dataRight = censusRight.data;
			for (int d = 0; d < disparityRange; d++) {
				int total = 0;
				for (int y = 0; y < blockHeight; y++) {
					int idxLeft  = (y+sampleRadiusY)*censusLeft.stride + sampleRadiusX;
					int idxRight = (y+sampleRadiusY)*censusRight.stride + sampleRadiusX + d;
					for (int x = 0; x < blockWidth; x++) {
						final int a = dataLeft[ idxLeft++ ];
						final int b = dataRight[ idxRight++ ];
						total += DescriptorDistance.hamming(a^b);
					}
				}
				scores[disparityRange-d-1] = total;
			}
		}
	}

	/**
	 * Computes census score for transformed images of type S64
	 */
	class S64<T extends GrayI<T>> extends Census<T,GrayS64> {
		public S64(int radiusX, int radiusY,
				   FilterCensusTransform<T, GrayS64> censusTran,
				   Class<T> imageType)
		{
			super(radiusX, radiusY, censusTran, imageType);
		}

		@Override
		protected void scoreCensus(int disparityRange) {
			final long[] dataLeft  = censusLeft.data;
			final long[] dataRight = censusRight.data;
			for (int d = 0; d < disparityRange; d++) {
				int total = 0;
				for (int y = 0; y < blockHeight; y++) {
					int idxLeft  = (y+sampleRadiusY)*censusLeft.stride + sampleRadiusX;
					int idxRight = (y+sampleRadiusY)*censusRight.stride + sampleRadiusX + d;
					for (int x = 0; x < blockWidth; x++) {
						final long a = dataLeft[ idxLeft++ ];
						final long b = dataRight[ idxRight++ ];
						total += DescriptorDistance.hamming(a^b);
					}
				}
				scores[disparityRange-d-1] = total;
			}
		}
	}
}
