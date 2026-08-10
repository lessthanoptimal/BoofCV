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
import boofcv.alg.disparity.block.score.DisparitySparseRectifiedScoreBM;
import boofcv.core.image.GConvertImage;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import lombok.Getter;

/// Computes the ZSSD error for sparse disparity. Should produce the same results as the dense version.
/// The block has already been extracted, so the mean is removed directly instead of through the identity
/// [BlockRowScoreZssd] needs.
@SuppressWarnings({"NullAway.Init"})
public class SparseScoreRectifiedZssd<T extends ImageGray<T>> extends DisparitySparseRectifiedScoreBM<float[], T> {
	// Local mean of the patches. Note that the border should not be computed since only the inner
	// image is needed. The inner image will be a row that's 1 pixel tall
	private final BlurStorageFilter<GrayF32> meanFilter;
	private final GrayF32 meanTemplate = new GrayF32(1, 1);
	private final GrayF32 meanCompare = new GrayF32(1, 1);

	// The patches converted to float. Not normalized, unlike NCC, since rescaling the input would
	// change the scale of the error
	private final GrayF32 floatTemplate = new GrayF32(1, 1);
	private final GrayF32 floatCompare = new GrayF32(1, 1);

	// Fit scores as a function of disparity. scores[0] = score at disparity of disparityMin
	@Getter protected float[] scoreLtoR; // left to right
	@Getter protected float[] scoreRtoL; // right to left

	public SparseScoreRectifiedZssd( int blockRadiusX, int blockRadiusY, Class<T> imageType ) {
		super(blockRadiusX, blockRadiusY, imageType);
		super.setSampleRegion(0, 0);
		meanFilter = FactoryBlurFilter.meanB(ImageType.SB_F32, blockRadiusX, blockRadiusY, null);
	}

	@Override
	public void configure( int disparityMin, int disparityRange ) {
		super.configure(disparityMin, disparityRange);
		scoreLtoR = new float[disparityRange];
		scoreRtoL = new float[disparityRange];
	}

	@Override
	protected void scoreDisparity( int disparityRange, final boolean leftToRight ) {
		GConvertImage.convert(patchTemplate, floatTemplate);
		GConvertImage.convert(patchCompare, floatCompare);

		meanFilter.process(floatTemplate, meanTemplate);
		meanFilter.process(floatCompare, meanCompare);

		final float[] dataLeft = floatTemplate.data;
		final float[] dataRight = floatCompare.data;

		final int rx = radiusX;
		final int ry = radiusY;

		final float meanL = meanTemplate.unsafe_get(rx, ry);

		final float[] scores = leftToRight ? scoreLtoR : scoreRtoL;
		for (int d = 0; d < disparityRange; d++) {
			final float meanR = meanCompare.unsafe_get(rx + d, ry);

			float total = 0;
			for (int y = 0; y < blockHeight; y++) {
				int idxLeft = (y + sampleRadiusY)*floatTemplate.stride + sampleRadiusX;
				int idxRight = (y + sampleRadiusY)*floatCompare.stride + sampleRadiusX + d;
				for (int x = 0; x < blockWidth; x++) {
					float error = (dataLeft[idxLeft++] - meanL) - (dataRight[idxRight++] - meanR);
					total += error*error;
				}
			}
			int index = leftToRight ? disparityRange - d - 1 : d;
			scores[index] = total;
		}
	}
}
