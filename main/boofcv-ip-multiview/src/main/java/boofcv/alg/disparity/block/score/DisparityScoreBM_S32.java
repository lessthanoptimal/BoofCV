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

package boofcv.alg.disparity.block.score;

import boofcv.alg.disparity.DisparityBlockMatch;
import boofcv.alg.disparity.block.BlockRowScore;
import boofcv.alg.disparity.block.DisparitySelect;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import pabeles.concurrency.GrowArray;
import pabeles.concurrency.IntRangeObjectConsumer;

/**
 * <p>
 * Implementation of {@link boofcv.alg.feature.disparity.DisparityScoreSadRect} for processing
 * input images of type {@link GrayU8}.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DisparityScoreBM_S32<T extends ImageBase<T>, DI extends ImageGray<DI>>
		extends DisparityBlockMatch<T, DI> {
	// Computes disparity from scores. Concurrent code copies this
	DisparitySelect<int[], DI> disparitySelect0;

	BlockRowScore<T, int[], Object> scoreRows;

	// reference to input images;
	T left, right;
	DI disparity;

	GrowArray<WorkSpace> workspace = new GrowArray<>(WorkSpace::new);
	ComputeBlock computeBlock = new ComputeBlock();

	public DisparityScoreBM_S32( int regionRadiusX, int regionRadiusY,
								 BlockRowScore<T, int[], Object> scoreRows,
								 DisparitySelect<int[], DI> computeDisparity, ImageType<T> imageType ) {
		super(regionRadiusX, regionRadiusY, imageType);

		this.scoreRows = scoreRows;
		this.disparitySelect0 = computeDisparity;
		workspace.grow();
	}

	@Override
	public void setBorder( ImageBorder<T> border ) {
		super.setBorder(border);
		this.scoreRows.setBorder(border);
	}

	@Override
	public void _process( T left, T right, DI disparity ) {
		this.left = left;
		this.right = right;
		this.disparity = disparity;

		growBorderL.setImage(left);
		growBorderR.setImage(right);

		scoreRows.setInput(left, right);

		if (BoofConcurrency.USE_CONCURRENT) {
			BoofConcurrency.loopBlocks(0, left.height, regionHeight, workspace, computeBlock);
		} else {
			computeBlock.accept((WorkSpace)workspace.get(0), 0, left.height);
		}
	}

	@SuppressWarnings({"NullAway.Init"})
	class WorkSpace {
		// stores the local scores for the width of the region
		int[] elementScore;
		// scores along horizontal axis for current block
		// To allow right to left validation all disparity scores are stored for the entire row
		// size = num columns * disparityMax
		// disparity for column i is stored in elements i*disparityRange to (i+1)*disparityRange
		int[][] horizontalScore = new int[0][0];
		// summed scores along vertical axis
		// Sum of horizontalScore along it's rows. This contains the entire box score for a pixel+disparity.
		int[] verticalScore = new int[0];
		// storage for scores after normalization
		int[] verticalScoreNorm = new int[0];
		// Used to store a copy of the image's row, plus outside border pixels
		Object leftRow, rightRow;

		DisparitySelect<int[], DI> computeDisparity;

		public void checkSize() {
			if (horizontalScore.length != regionHeight || horizontalScore[0].length != widthDisparityBlock) {
				horizontalScore = new int[regionHeight][widthDisparityBlock];
				verticalScore = new int[widthDisparityBlock];
				if (scoreRows.isRequireNormalize())
					verticalScoreNorm = new int[widthDisparityBlock];
				elementScore = new int[left.width + 2*radiusX];
				leftRow = left.getImageType().getDataType().newArray(elementScore.length);
				rightRow = right.getImageType().getDataType().newArray(elementScore.length);
			}
			if (computeDisparity == null) {
				computeDisparity = disparitySelect0.concurrentCopy();
			}
			computeDisparity.configure(disparity, disparityMin, disparityMax, radiusX);
		}
	}

	private class ComputeBlock implements IntRangeObjectConsumer<WorkSpace> {
		@Override
		public void accept( WorkSpace workspace, int minInclusive, int maxExclusive ) {

			workspace.checkSize();
			int row0 = minInclusive - radiusY;
			int row1 = maxExclusive + radiusY;

			// initialize computation
			computeFirstRow(row0, workspace);

			// efficiently compute rest of the rows using previous results to avoid repeat computations
			computeRemainingRows(row0, row1, workspace);
		}
	}

	/**
	 * Initializes disparity calculation by finding the scores for the initial block of horizontal
	 * rows.
	 */
	private void computeFirstRow( int row0, WorkSpace ws ) {
		int disparityMax = Math.min(left.width, this.disparityMax);

		// compute horizontal scores for first row block
		for (int row = 0; row < regionHeight; row++) {
			growBorderL.growRow(row0 + row, radiusX, radiusX, ws.leftRow, 0);
			growBorderR.growRow(row0 + row, radiusX, radiusX, ws.rightRow, 0);
			final int[] scores = ws.horizontalScore[row];
			scoreRows.scoreRow(row0 + row, ws.leftRow, ws.rightRow, scores, disparityMin, disparityMax, regionWidth, ws.elementScore);
		}

		// compute score for the top most row
		for (int i = 0; i < widthDisparityBlock; i++) {
			int sum = 0;
			for (int row = 0; row < regionHeight; row++) {
				sum += ws.horizontalScore[row][i];
			}
			ws.verticalScore[i] = sum;
		}

		// compute disparity
		if (scoreRows.isRequireNormalize()) {
			scoreRows.normalizeRegionScores(row0 + radiusY, ws.verticalScore, disparityMin, disparityMax, regionWidth, regionHeight, ws.verticalScoreNorm);
			ws.computeDisparity.process(row0 + radiusY, ws.verticalScoreNorm);
		} else {
			ws.computeDisparity.process(row0 + radiusY, ws.verticalScore);
		}
	}

	/**
	 * Using previously computed results it efficiently finds the disparity in the remaining rows.
	 * When a new block is processes the last row/column is subtracted and the new row/column is
	 * added.
	 */
	private void computeRemainingRows( int row0, int row1, WorkSpace ws ) {
		int disparityMax = Math.min(left.width, this.disparityMax);

		for (int row = row0 + regionHeight; row < row1; row++) {
			int oldRow = (row - row0)%regionHeight;

			// subtract first row from vertical score
			final int[] scores = ws.horizontalScore[oldRow];
			for (int i = 0; i < widthDisparityBlock; i++) {
				ws.verticalScore[i] -= scores[i];
			}

			growBorderL.growRow(row, radiusX, radiusX, ws.leftRow, 0);
			growBorderR.growRow(row, radiusX, radiusX, ws.rightRow, 0);
			scoreRows.scoreRow(row, ws.leftRow, ws.rightRow, scores, disparityMin, disparityMax, regionWidth, ws.elementScore);

			// add the new score
			for (int i = 0; i < widthDisparityBlock; i++) {
				ws.verticalScore[i] += scores[i];
			}

			// compute disparity
			if (scoreRows.isRequireNormalize()) {
				scoreRows.normalizeRegionScores(row - regionHeight + 1 + radiusY,
						ws.verticalScore, disparityMin, disparityMax, regionWidth, regionHeight, ws.verticalScoreNorm);
				ws.computeDisparity.process(row - regionHeight + 1 + radiusY, ws.verticalScoreNorm);
			} else {
				ws.computeDisparity.process(row - regionHeight + 1 + radiusY, ws.verticalScore);
			}
		}
	}

	@Override
	public ImageType<T> getInputType() {
		return scoreRows.getImageType();
	}

	@Override
	public Class<DI> getDisparityType() {
		return disparitySelect0.getDisparityType();
	}

	@Override
	protected int getMaxPerPixelError() {
		return scoreRows.getMaxPerPixelError();
	}

	public void setDisparitySelect0( DisparitySelect<int[], DI> disparitySelect0 ) {
		this.disparitySelect0 = disparitySelect0;
	}
}
