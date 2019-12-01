/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class ChecksBlockRowScore<T extends ImageBase<T>,Array> {

	int width=15;
	int height=10;

	double maxPixelValue;

	T left,right;

	Random rand = new Random(234);

	public ChecksBlockRowScore( double maxPixelValue, ImageType<T> imageType ) {
		this.maxPixelValue = maxPixelValue;
		left = imageType.createImage(width,height);
		right = imageType.createImage(width,height);
	}

	public abstract BlockRowScore<T,Array> createAlg( int radiusWidth , int radiusHeight );

	public abstract Array createArray( int length );

	public abstract double naiveScoreRow(int cx , int cy , int disparity , int radius );

	public abstract double naiveScoreRegion(int cx , int cy , int disparity , int radius );

	public abstract double get( int index , Array array );

	/**
	 * Score the row and compare to a naive implementation
	 */
	@Test
	void scoreRow_naive() {
		scoreRow_naive(0, 10, 1);
		scoreRow_naive(0, 5, 2);
		scoreRow_naive(2, 5, 2);
	}

	private void scoreRow_naive(int minDisparity, int maxDisparity, int r) {
		int w = r*2+1;
		GImageMiscOps.fillUniform(left,rand,0,maxPixelValue);
		GImageMiscOps.fillUniform(right,rand,0,maxPixelValue);

		BlockRowScore<T,Array> alg = createAlg(r,r);

		int row = r+1;
		int disparityRange = maxDisparity-minDisparity;

		Array scores = createArray(width*disparityRange);
		Array elementScore = createArray(width);

		alg.setInput(left,right);
		alg.scoreRow(row,scores,minDisparity,maxDisparity,w,elementScore);

		for (int d = minDisparity; d < maxDisparity; d++) {
			int idx = width*(d-minDisparity)+d-minDisparity;
			for (int x = d+r; x < width-r; x++) {
				double found = get(idx++,scores);
				double expected = naiveScoreRow(x,row,d,r);
				assertEquals(expected,found,1);
			}
		}
	}

	/**
	 * Checks the complete score with normalization
	 */
	@Test
	void score_naive() {
		score_naive(0, 10, 1);
		score_naive(0, 5, 2);
		score_naive(2, 5, 2);
	}
	void score_naive(int minDisparity, int maxDisparity, int r) {
		int w = r*2+1;
		GImageMiscOps.fillUniform(left,rand,0,maxPixelValue);
		GImageMiscOps.fillUniform(right,rand,0,maxPixelValue);

		BlockRowScore<T,Array> alg = createAlg(r,r);
		alg.setInput(left,right);

		int row = r+1;
		int disparityRange = maxDisparity-minDisparity;

		Array scores = createArray(width*disparityRange);
		Array elementScore = createArray(width);
		Array scoresSum = createArray(width*disparityRange);
		Array scoresSumNorm = createArray(width*disparityRange);
		Array scoresEvaluated;

		// compute the scores one row at a time then sum them up to get the unnormalized region score
		for (int i = -r; i <= r; i++) {
			alg.scoreRow(row+i,scores,minDisparity,maxDisparity,w,elementScore);
			addToSum(scores,scoresSum);
		}
		// Normalize the region score
		if( alg.isRequireNormalize()) {
			alg.normalizeRegionScores(row, scoresSum, minDisparity, maxDisparity, w, w, scoresSumNorm);
			scoresEvaluated = scoresSumNorm;
		} else {
			scoresEvaluated = scoresSum;
		}

		for (int d = minDisparity; d < maxDisparity; d++) {
			int idx = width*(d-minDisparity)+d-minDisparity;
			for (int x = d+r; x < width-r; x++) {
				double expected = naiveScoreRegion(x,row,d,r);
				double found = get(idx++,scoresEvaluated);
//				System.out.printf("%3d  %7.4f e=%8.3f f=%8.3f\n",idx,(expected-found),expected,found);
//				System.out.println("diff "+(expected-found)+"   expected "+expected);
				assertEquals(expected,found,1e-3);
			}
		}
	}

	private void addToSum( Array scores , Array scoresSum ) {
		if( scores instanceof int[] ) {
			int[] a = (int[])scores;
			int[] b = (int[])scoresSum;
			for (int i = 0; i < a.length; i++) {
				b[i] += a[i];
			}
		} else {
			float[] a = (float[])scores;
			float[] b = (float[])scoresSum;
			for (int i = 0; i < a.length; i++) {
				b[i] += a[i];
			}
		}
	}
}
