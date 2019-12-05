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
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.border.ImageBorder_S64;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.GrayS64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class ChecksBlockRowScore<T extends ImageBase<T>,Array> {

	public static final BorderType BORDER_TYPE = BorderType.REFLECT;

	int width=15;
	int height=10;

	// min and max pixel intensity values
	protected double minPixelValue,maxPixelValue;
	protected double tol= UtilEjml.TEST_F32;

	T left,right;
	ImageBorder<T> bleft,bright;

	Random rand = new Random(234);

	public ChecksBlockRowScore( double maxPixelValue, ImageType<T> imageType ) {
		this.maxPixelValue = maxPixelValue;
		bleft = FactoryImageBorder.generic(BORDER_TYPE,imageType);
		bright = FactoryImageBorder.generic(BORDER_TYPE,imageType);

		left = imageType.createImage(width,height);
		right = imageType.createImage(width,height);
	}

	public abstract BlockRowScore<T,Array> createAlg( int radiusWidth , int radiusHeight );

	public abstract Array createArray( int length );

	public abstract double naiveScoreRow(int cx , int cy , int disparity , int radius );

	public abstract double naiveScoreRegion(int cx , int cy , int disparity , int radius );

	public abstract double get( int index , Array array );

	@BeforeEach
	void before() {
		BoofConcurrency.USE_CONCURRENT = false;

		left = left.getImageType().createImage(width,height);
		right = left.getImageType().createImage(width,height);

		bleft.setImage(left);
		bright.setImage(right);
	}

	/**
	 * Score the row and compare to a naive implementation
	 */
	@Test
	void scoreRow_naive() {
		scoreRow_naive(0, 10, 1,2);
		scoreRow_naive(0, 10, 5,6);
		scoreRow_naive(0, 5, 2,3);
		scoreRow_naive(2, 5, 2,4);
		scoreRow_naive(2, 5, 2,0);
		scoreRow_naive(2, 5, 2,height-1);
	}

	private void scoreRow_naive(int minDisparity, int maxDisparity, int radius, int row) {
		int w = radius*2+1;
		GImageMiscOps.fillUniform(left,rand,0,maxPixelValue);
		GImageMiscOps.fillUniform(right,rand,0,maxPixelValue);

		BlockRowScore<T,Array> alg = createAlg(radius,radius);
		alg.setBorder(FactoryImageBorder.generic(BORDER_TYPE,alg.getImageType()));

		int disparityRange = maxDisparity-minDisparity+1;

		Array scores = createArray(width*disparityRange);
		Array elementScore = createArray( width+2*radius);// image width + border on each side

		alg.setInput(left,right);
		alg.scoreRow(row,scores,minDisparity,maxDisparity,w,elementScore);

		for (int d = minDisparity; d < maxDisparity; d++) {
			int idx = width*(d-minDisparity)+d-minDisparity;
			for (int x = d; x < width; x++) {
				double found = get(idx++,scores);
				double expected = naiveScoreRow(x,row,d,radius);
				assertEquals(expected,found,1);
			}
		}
	}

	/**
	 * Checks the complete score with normalization
	 */
	@Test
	void score_naive() {
		score_naive(0, 10, 1,2);
		score_naive(0, 10, 5,6);
		score_naive(0, 5, 2,3);
		score_naive(2, 5, 2,4);
		score_naive(2, 5, 2,0);
		score_naive(2, 5, 2,height-1);
	}
	void score_naive(int minDisparity, int maxDisparity, int radius, int row ) {
		int w = radius*2+1;
		GImageMiscOps.fillUniform(left,rand,0,maxPixelValue);
		GImageMiscOps.fillUniform(right,rand,0,maxPixelValue);

		BlockRowScore<T,Array> alg = createAlg(radius,radius);
		alg.setBorder(FactoryImageBorder.generic(BORDER_TYPE,alg.getImageType()));
		alg.setInput(left,right);

		int disparityRange = maxDisparity-minDisparity+1;

		Array scores = createArray(width*disparityRange);
		Array elementScore = createArray(width+2*radius);
		Array scoresSum = createArray(width*disparityRange);
		Array scoresSumNorm = createArray(width*disparityRange);
		Array scoresEvaluated;

		// compute the scores one row at a time then sum them up to get the unnormalized region score
		for (int i = -radius; i <= radius; i++) {
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
			for (int x = d; x < width; x++) {
				double expected = naiveScoreRegion(x,row,d,radius);
				double found = get(idx++,scoresEvaluated);
//				System.out.printf("%3d  %7.4f e=%8.3f f=%8.3f\n",idx,(expected-found),expected,found);
//				System.out.println("diff "+(expected-found)+"   expected "+expected);
				double tol = Math.max(1,Math.abs(expected))*this.tol;
				assertEquals(expected,found,tol,"y = "+row+" x = "+x+" d = "+d);
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

	protected static abstract class ArrayIntI<T extends GrayI<T>> extends ChecksBlockRowScore<T,int[]> {

		public ArrayIntI(double maxPixelValue, ImageType<T> imageType) {
			super(maxPixelValue, imageType);
		}

		@Override
		public int[] createArray(int length) {
			return new int[length];
		}

		protected abstract int computeError( int a , int b );

		@Override
		public double naiveScoreRow(int cx, int cy, int disparity, int radius) {
			int x0 = cx-radius;
			int x1 = cx+radius+1;

			int total = 0;
			for (int x = x0; x < x1; x++) {
				int va = ((ImageBorder_S32)bleft).get(x,cy);
				int vb = ((ImageBorder_S32)bright).get(x-disparity,cy);
				total += computeError(va,vb);
			}
			return total;
		}

		@Override
		public double naiveScoreRegion(int cx, int cy, int disparity, int radius) {
			int y0 = cy-radius;
			int y1 = cy+radius+1;

			int total = 0;
			for (int y = y0; y < y1; y++) {
				total += (int)naiveScoreRow(cx, y, disparity, radius);
			}
			return total;
		}

		@Override
		public double get(int index, int[] array) {
			return array[index];
		}
	}

	protected static abstract class ArrayIntL extends ChecksBlockRowScore<GrayS64,int[]> {

		public ArrayIntL(double maxPixelValue) {
			super(maxPixelValue, ImageType.single(GrayS64.class));
		}

		@Override
		public int[] createArray(int length) {
			return new int[length];
		}

		protected abstract int computeError( long a , long b );

		@Override
		public double naiveScoreRow(int cx, int cy, int disparity, int radius) {
			int x0 = cx-radius;
			int x1 = cx+radius+1;

			long total = 0;
			for (int x = x0; x < x1; x++) {
				long va = ((ImageBorder_S64)bleft).get(x,cy);
				long vb = ((ImageBorder_S64)bright).get(x-disparity,cy);
				total += computeError(va,vb);
			}
			return total;
		}

		@Override
		public double naiveScoreRegion(int cx, int cy, int disparity, int radius) {
			int y0 = cy-radius;
			int y1 = cy+radius+1;

			long total = 0;
			for (int y = y0; y < y1; y++) {
				total += (long)naiveScoreRow(cx, y, disparity, radius);
			}
			return total;
		}

		@Override
		public double get(int index, int[] array) {
			return array[index];
		}
	}
}
