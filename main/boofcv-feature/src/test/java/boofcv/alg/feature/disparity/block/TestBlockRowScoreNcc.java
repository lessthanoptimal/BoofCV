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

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
public class TestBlockRowScoreNcc {

	@Nested
	class U8 extends ChecksBlockRowScore<GrayU8,int[]> {

		U8() {
			super(255, ImageType.single(GrayU8.class));
		}

		@Override
		public BlockRowScore<GrayU8, int[]> createAlg(int radiusWidth, int radiusHeight) {
			return new BlockRowScoreNcc.U8(radiusWidth,radiusHeight);
		}

		@Override
		public int[] createArray(int length) {
			return new int[length];
		}

		@Override
		public double naiveScoreRow(int cx, int cy, int disparity, int radius) {
			double total = 0;
			for (int x = -radius; x <= radius; x++) {
				double va = left.get(cx+x,cy);
				double vb = right.get(cx+x-disparity,cy);

				total += va*vb;
			}
			return total;
		}

		@Override
		public double naiveScoreRegion(int cx, int cy, int disparity, int radius) {
			double meanLeft = mean(left,cx,cy,radius);
			double stdLeft = stdev(left,cx,cy,radius,meanLeft);
			double meanRight = mean(right,cx-disparity,cy,radius);
			double stdRight = stdev(right,cx-disparity,cy,radius,meanRight);

			double total = 0;
			for (int y = -radius; y <= radius; y++) {
				for (int x = -radius; x <= radius; x++) {
					double va = left.get(cx+x,cy+y)-meanLeft;
					double vb = right.get(cx+x-disparity,cy+y)-meanRight;

					total += va*vb;
				}
			}
			total /= (radius*2+1)*(radius*2+1);

			return -total/(1.0+stdLeft*stdRight);
		}

		@Override
		public double get(int index, int[] array) {
			return array[index];
		}
	}

	@Nested
	class F32 extends ChecksBlockRowScore<GrayF32,float[]> {

		F32() {
			super(255, ImageType.single(GrayF32.class));
		}

		@Override
		public BlockRowScore<GrayF32, float[]> createAlg(int radiusWidth, int radiusHeight) {
			return new BlockRowScoreNcc.F32(radiusWidth,radiusHeight);
		}

		@Override
		public float[] createArray(int length) {
			return new float[length];
		}

		@Override
		public double naiveScoreRow(int cx, int cy, int disparity, int radius) {
			double total = 0;
			for (int x = -radius; x <= radius; x++) {
				double va = left.get(cx+x,cy);
				double vb = right.get(cx+x-disparity,cy);

				total += va*vb;
			}
			return total;
		}

		@Override
		public double naiveScoreRegion(int cx, int cy, int disparity, int radius) {
			return -ncc(left,right,cx,cy,disparity,radius,UtilEjml.F_EPS);
		}

		@Override
		public double get(int index, float[] array) {
			return array[index];
		}
	}

	public static double ncc( ImageGray left , ImageGray right ,
							  int cx, int cy, int disparity, int radius , double eps )
	{
		double meanLeft = mean(left,cx,cy,radius);
		double stdLeft = stdev(left,cx,cy,radius,meanLeft);
		double meanRight = mean(right,cx-disparity,cy,radius);
		double stdRight = stdev(right,cx-disparity,cy,radius,meanRight);

		double total = 0;
		for (int y = -radius; y <= radius; y++) {
			for (int x = -radius; x <= radius; x++) {
				double va = GeneralizedImageOps.get(left,cx+x,cy+y)-meanLeft;
				double vb = GeneralizedImageOps.get(right,cx+x-disparity,cy+y)-meanRight;

				total += va*vb;
			}
		}
		total /= (radius*2+1)*(radius*2+1);

		return total/(eps +stdLeft*stdRight);
	}

	public static double mean( ImageGray img , int cx , int cy , int radius ) {
		double total = 0;
		for (int y = -radius; y <= radius; y++) {
			for (int x = -radius; x <= radius; x++) {
				total += GeneralizedImageOps.get(img,cx+x,cy+y);
			}
		}

		return total / ((radius*2+1)*(radius*2+1));
	}

	public static double stdev(ImageGray img , int cx , int cy , int radius, double mean ) {
		double total = 0;
		for (int y = -radius; y <= radius; y++) {
			for (int x = -radius; x <= radius; x++) {
				double delta = GeneralizedImageOps.get(img,cx+x,cy+y) - mean;
				total += delta*delta;
			}
		}

		int N = (radius*2+1)*(radius*2+1);

		return Math.sqrt(total/N);
	}
}