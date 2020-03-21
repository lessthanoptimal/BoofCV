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

package boofcv.alg.feature.disparity.block.score;

import boofcv.alg.feature.disparity.DisparityBlockMatch;
import boofcv.alg.feature.disparity.block.BlockRowScore;
import boofcv.alg.feature.disparity.block.DisparitySelect;
import boofcv.concurrency.BoofConcurrency;
import boofcv.concurrency.IntRangeObjectConsumer;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.FastQueue;

/**
 * <p>
 * Implementation of {@link boofcv.alg.feature.disparity.DisparityScoreSadRect} for processing
 * input images of type {@link GrayF32}.
 * </p>
 * 
 * @author Peter Abeles
 */
public class DisparityScoreBM_F32<DI extends ImageGray<DI>>
	extends DisparityBlockMatch<GrayF32,DI>
{
	// Computes disparity from scores. Concurrent code copies this
	DisparitySelect<float[], DI> disparitySelect0;

	BlockRowScore<GrayF32,float[],float[]> scoreRows;

	// reference to input images;
	GrayF32 left, right;
	DI disparity;

	FastQueue workspace = new FastQueue<>(WorkSpace.class, WorkSpace::new);
	ComputeBlock computeBlock = new ComputeBlock();

	public DisparityScoreBM_F32(int regionRadiusX, int regionRadiusY,
								BlockRowScore<GrayF32,float[],float[]> scoreRows,
								DisparitySelect<float[], DI> computeDisparity) {
		super(regionRadiusX,regionRadiusY, ImageType.SB_F32);

		this.scoreRows = scoreRows;
		this.disparitySelect0 = computeDisparity;
		workspace.grow();
	}

	@Override
	public void setBorder( ImageBorder<GrayF32> border ) {
		super.setBorder(border);
		this.scoreRows.setBorder(border);
	}

	@Override
	public void _process(GrayF32 left , GrayF32 right , DI disparity ) {
		this.left = left;
		this.right = right;
		this.disparity = disparity;

		growBorderL.setImage(left);
		growBorderR.setImage(right);

		scoreRows.setInput(left,right);

		if( BoofConcurrency.USE_CONCURRENT ) {
			BoofConcurrency.loopBlocks(0,left.height,regionHeight,workspace,computeBlock);
		} else {
			computeBlock.accept((WorkSpace)workspace.get(0),0,left.height);
		}
	}

	class WorkSpace {
		// stores the local scores for the width of the region
		float[] elementScore;
		// scores along horizontal axis for current block
		// To allow right to left validation all disparity scores are stored for the entire row
		// size = num columns * disparityMax
		// disparity for column i is stored in elements i*disparityRange to (i+1)*disparityRange
		float[][] horizontalScore = new float[0][0];
		// summed scores along vertical axis
		// Sum of horizontalScore along it's rows. This contains the entire box score for a pixel+disparity.
		float[] verticalScore = new float[0];
		// storage for scores after normalization
		float[] verticalScoreNorm = new float[0];
		// Used to store a copy of the image's row, plus outside border pixels
		float[] leftRow,rightRow;

		DisparitySelect<float[], DI> computeDisparity;

		public void checkSize() {
			if( horizontalScore.length != regionHeight || horizontalScore[0].length != widthDisparityBlock) {
				horizontalScore = new float[regionHeight][widthDisparityBlock];
				verticalScore = new float[widthDisparityBlock];
				if( scoreRows.isRequireNormalize() )
					verticalScoreNorm = new float[widthDisparityBlock];
				elementScore = new float[ left.width+2*radiusX];
				leftRow = left.getImageType().getDataType().newArray(elementScore.length);
				rightRow = right.getImageType().getDataType().newArray(elementScore.length);
			}
			if( computeDisparity == null ) {
				computeDisparity = disparitySelect0.concurrentCopy();
			}
			computeDisparity.configure(disparity, disparityMin, disparityMax,radiusX);
		}
	}

	private class ComputeBlock implements IntRangeObjectConsumer<WorkSpace> {
		@Override
		public void accept(WorkSpace workspace, int minInclusive, int maxExclusive) {

			workspace.checkSize();
			int row0 = minInclusive-radiusY;
			int row1 = maxExclusive+radiusY;

			// initialize computation
			computeFirstRow(row0, workspace);

			// efficiently compute rest of the rows using previous results to avoid repeat computations
			computeRemainingRows(row0,row1, workspace);
		}
	}

	/**
	 * Initializes disparity calculation by finding the scores for the initial block of horizontal
	 * rows.
	 */
	private void computeFirstRow(int row0, WorkSpace ws) {
		final GrayF32 left = this.left, right = this.right;
		// compute horizontal scores for first row block
		for( int row = 0; row < regionHeight; row++ ) {
			growBorderL.growRow(row0+row,radiusX,radiusX,ws.leftRow,0);
			growBorderR.growRow(row0+row,radiusX,radiusX,ws.rightRow,0);
			final float[] scores = ws.horizontalScore[row];
			scoreRows.scoreRow(row0+row, ws.leftRow, ws.rightRow, scores, disparityMin, disparityMax,regionWidth,ws.elementScore);
		}

		// compute score for the top most row
		for(int i = 0; i < widthDisparityBlock; i++ ) {
			float sum = 0;
			for( int row = 0; row < regionHeight; row++ ) {
				sum += ws.horizontalScore[row][i];
			}
			ws.verticalScore[i] = sum;
		}

		// compute disparity
		if( scoreRows.isRequireNormalize() ) {
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
	private void computeRemainingRows(int row0 , int row1, WorkSpace ws  )
	{
		for( int row = row0+regionHeight; row < row1; row++ ) {
			int oldRow = (row-row0)%regionHeight;

			// subtract first row from vertical score
			final float[] scores = ws.horizontalScore[oldRow];
			for(int i = 0; i < widthDisparityBlock; i++ ) {
				ws.verticalScore[i] -= scores[i];
			}

			growBorderL.growRow(row,radiusX,radiusX,ws.leftRow,0);
			growBorderR.growRow(row,radiusX,radiusX,ws.rightRow,0);
			scoreRows.scoreRow(row, ws.leftRow, ws.rightRow, scores, disparityMin, disparityMax,regionWidth,ws.elementScore);

			// add the new score
			for(int i = 0; i < widthDisparityBlock; i++ ) {
				ws.verticalScore[i] += scores[i];
			}

			// compute disparity
			if( scoreRows.isRequireNormalize() ) {
				scoreRows.normalizeRegionScores(row - regionHeight + 1 + radiusY,
						ws.verticalScore, disparityMin, disparityMax,regionWidth,regionHeight,ws.verticalScoreNorm);
				ws.computeDisparity.process(row - regionHeight + 1 + radiusY, ws.verticalScoreNorm);
			} else {
				ws.computeDisparity.process(row - regionHeight + 1 + radiusY, ws.verticalScore);
			}
		}
	}

	@Override
	public ImageType<GrayF32> getInputType() {
		return ImageType.SB_F32;
	}

	@Override
	public Class<DI> getDisparityType() {
		return disparitySelect0.getDisparityType();
	}

	@Override
	protected int getMaxPerPixelError() {
		return scoreRows.getMaxPerPixelError();
	}

	public void setDisparitySelect0(DisparitySelect<float[], DI> disparitySelect0) {
		this.disparitySelect0 = disparitySelect0;
	}
}
