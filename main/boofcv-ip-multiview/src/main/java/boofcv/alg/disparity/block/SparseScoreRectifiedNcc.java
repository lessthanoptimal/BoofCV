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
import boofcv.alg.disparity.block.score.DisparitySparseRectifiedScoreBM;
import boofcv.alg.misc.GPixelMath;
import boofcv.alg.misc.ImageNormalization;
import boofcv.alg.misc.NormalizeParameters;
import boofcv.core.image.GConvertImage;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import lombok.Getter;
import org.ejml.UtilEjml;

/**
 * Compute NCC error for sparse disparity. Should produce similar results to dense version. Image normalization
 * is computed using local statistics instead of global statistics across input image.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"MissingOverride", "NullAway.Init"})
public class SparseScoreRectifiedNcc<T extends ImageGray<T>> extends DisparitySparseRectifiedScoreBM<float[],T>
{
	// Storage for temporary images used to compute local mean and stdev
	public final SparseStatistics statsLeft = new SparseStatistics();
	public final SparseStatistics statsRight = new SparseStatistics();

	// Applies local blur. Note that the border should not be computed since only the inner
	// image is needed. The inner image will be a row that's 1 pixel tall
	private final BlurStorageFilter<GrayF32> meanFilter;

	// NCC tuning parameter used to avoid divide by zero
	public float eps= UtilEjml.F_EPS;

	// the image patches after they have been normalized
	private final GrayF32 adjustedLeft = new GrayF32(1,1);
	private final GrayF32 adjustedRight = new GrayF32(1,1);

	// If true then the input images are normalized. See code comments
	public boolean normalizeInput=true;
	NormalizeParameters parameters = new NormalizeParameters();

	// Fit scores as a function of disparity. scores[0] = score at disparity of disparityMin
	@Getter protected float[] scoreLtoR; // left to right
	@Getter protected float[] scoreRtoL; // right to left

	public SparseScoreRectifiedNcc(int blockRadiusX, int blockRadiusY, Class<T> imageType ) {
		super(blockRadiusX, blockRadiusY, imageType);
		super.setSampleRegion(0,0);
		// this will skip over the image border. In this situation that is all but the inner post row
		meanFilter = FactoryBlurFilter.meanB(ImageType.SB_F32,blockRadiusX,blockRadiusY,null);
	}

	@Override
	public void configure(int disparityMin, int disparityRange) {
		super.configure(disparityMin, disparityRange);
		scoreLtoR = new float[ disparityRange ];
		scoreRtoL = new float[ disparityRange ];
	}

	@Override
	protected void scoreDisparity(int disparityRange, final boolean leftToRight) {
		if( normalizeInput ) {
			// Apply normalization to the right patch because it tends to be larger. In the batch algorithm
			// normalization is computed against the entire left image. Since doing so would make the runtime
			// be O(width*height) that's not done here.
			ImageNormalization.zeroMeanMaxOne(patchCompare, adjustedRight, parameters);
			ImageNormalization.apply(patchTemplate, parameters, adjustedLeft);
		} else {
			// Don't normalize the image and just copy/convert the image type to float
			GConvertImage.convert(patchTemplate,adjustedLeft);
			GConvertImage.convert(patchCompare,adjustedRight);
		}

		// Compute local image statics for divisor in NCC error
		computeStats(adjustedLeft,statsLeft);
		computeStats(adjustedRight,statsRight);

		// Save reference to internal data structures as short hand
		final float[] dataLeft = adjustedLeft.data;
		final float[] dataRight = adjustedRight.data;

		// short hand
		final int rx = radiusX;
		final int ry = radiusY;

		// Extract left image patch information
		final float meanL = statsLeft.mean.unsafe_get(rx,ry);
		final float meanP2L = statsLeft.pow2mean.unsafe_get(rx,ry);
		final float sigmaL = (float)Math.sqrt(Math.max(0,meanP2L-meanL*meanL));

		// Area the mean filter is being applied to
		final float area = blockWidth*blockHeight;

		final float[] scores = leftToRight ? scoreLtoR : scoreRtoL;
		for (int d = 0; d < disparityRange; d++) {
			final float meanR = statsRight.mean.unsafe_get(rx+d,ry);
			final float meanP2R = statsRight.pow2mean.unsafe_get(rx+d,ry);
			final float sigmaR = (float)Math.sqrt(Math.max(0,meanP2R-meanR*meanR));

			float correlation = 0;
			for (int y = 0; y < blockHeight; y++) {
				int idxLeft  = (y+sampleRadiusY)*adjustedLeft.stride + sampleRadiusX;
				int idxRight = (y+sampleRadiusY)*adjustedRight.stride + sampleRadiusX+d;
				for (int x = 0; x < blockWidth; x++) {
					correlation += dataLeft[idxLeft++] * dataRight[idxRight++];
				}
			}
			int index = leftToRight ? disparityRange-d-1 : d;
			correlation /= area;
			scores[index] = (correlation - meanL*meanR)/(eps+sigmaL*sigmaR);
		}
	}

	/**
	 * Computes local image statics needed by NCC error
	 */
	private void computeStats( GrayF32 input , SparseStatistics stats ) {
		meanFilter.process(input,stats.mean);
		GPixelMath.pow2(input,stats.pow2);
		meanFilter.process(stats.pow2, stats.pow2mean);
	}

	private static class SparseStatistics {
		// local mean
		final GrayF32 mean = new GrayF32(1,1);
		// local mean of pixel value squared
		final GrayF32 pow2mean = new GrayF32(1,1);
		// pixel values squared
		final GrayF32 pow2 = new GrayF32(1,1);
	}
}
