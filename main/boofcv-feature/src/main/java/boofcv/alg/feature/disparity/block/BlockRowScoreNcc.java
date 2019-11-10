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

import boofcv.abst.filter.blur.BlurStorageFilter;
import boofcv.alg.misc.GPixelMath;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.image.*;
import org.ejml.UtilEjml;

/**
 * Score using NCC. Takes advantage for the forumla for NCC error which can be computed by applying block filters
 * to the image.
 *
 * NCC = ((1/N)*[sum( L(x)*R(x+d) ) - u_l(x)*u_r(x+d) )/ (sigma_L(x) * sigma_R(x+d))
 *
 * @author Peter Abeles
 */
public class BlockRowScoreNcc<A extends ImageBase<A>,B extends ImageBase<B>>
{
	// Storage for mean of left + right image
	A meanL, meanR;
	// Storage for power of 2 images
	B powL, powR;

	// Storage for stdev images
	A stdevL, stdevR;

	B tmpPow2;

	BlurStorageFilter<A> meanFilterA;
	BlurStorageFilter<B> meanFilterB;

	public BlockRowScoreNcc( int radiusX , int radiusY ,
							 ImageType<A> meanType, ImageType<B> powType ) {
		if( radiusX != radiusY )
			throw new IllegalArgumentException("Non-square regions are currently not supported");

		meanL = meanType.createImage(1,1);
		meanR = meanType.createImage(1,1);
		powL = powType.createImage(1,1);
		powR = powType.createImage(1,1);

		meanFilterA = FactoryBlurFilter.mean(meanType,radiusX);
		// save memory and use the same filter / images
		if( meanType.isSameType(powType)) {
			tmpPow2 = (B)meanL;
			stdevL = (A)powL;
			stdevR = (A)powR;
			meanFilterB = (BlurStorageFilter)meanFilterA;
		} else {
			tmpPow2 = powType.createImage(1,1);
			stdevL = meanType.createImage(1,1);
			stdevR = meanType.createImage(1,1);
			meanFilterB = FactoryBlurFilter.mean(powType, radiusX);
		}
	}

	public void computeStatistics(A left , A right ) {
		// Compute mean of L^2 and R^2
		GPixelMath.pow2(left,tmpPow2);
		meanFilterB.process(tmpPow2, powL);
		GPixelMath.pow2(right,tmpPow2);
		meanFilterB.process(tmpPow2, powR);

		// Compute mean of L and R
		meanFilterA.process(left, meanL);
		meanFilterA.process(right, meanR);

		// Compute the sigma from mean and mean^2
		GPixelMath.stdev(meanL, powL, stdevL);
		GPixelMath.stdev(meanR, powR, stdevR);
	}

	public static class U8 extends BlockRowScore.ArrayS32<GrayU8> {
		BlockRowScoreNcc<GrayU8, GrayU16> helper;
		public U8(int radiusWidth , int radiusHeight) {
			helper = new BlockRowScoreNcc<>(radiusWidth,radiusHeight,
					ImageType.single(GrayU8.class),ImageType.single(GrayU16.class));
		}

		@Override
		public void setInput(GrayU8 left, GrayU8 right) {
			super.setInput(left, right);
			helper.computeStatistics(left,right);
		}

		@Override
		public void score(int elementMax, int indexLeft, int indexRight, int[] elementScore) {
			for( int rCol = 0; rCol < elementMax; rCol++ ) {
				elementScore[rCol] = (left.data[ indexLeft++ ]&0xFF) * (right.data[ indexRight++ ]&0xFF);
			}
		}

		@Override
		public boolean isRequireNormalize() {
			return true;
		}

		@Override
		public void normalizeScore(int row, int colLeft, int colRight, int numCols,
								   int regionWidth, int regionHeight,
								   int[] scores, int indexScores, int[] scoresNorm ) {
			final int area = regionWidth*regionHeight;
			final int round = area/2;

			// all helper classes will have the same stride
			int idxLeft = row*helper.meanL.stride+colLeft;
			int idxRight = row*helper.meanL.stride+colRight;

			for (int i = 0; i < numCols; i++, idxLeft++, idxRight++ ) {
				// scores will always be positive so you can round quickly this way
				float correlation = (scores[indexScores+i]+round)/area;

				float meanL = helper.meanL.data[idxLeft]&0xFF;
				float meanR = helper.meanR.data[idxRight]&0xFF;
				float sigmaL = helper.stdevL.data[idxLeft]&0XFF;
				float sigmaR = helper.stdevR.data[idxRight]&0XFF;

				// invert score since the minimum is selected for disparity
				scoresNorm[indexScores+i] = (int)(-1000.0f*((correlation - meanL*meanR)/(1f+sigmaL*sigmaR)));
			}
		}

		@Override
		public ImageType<GrayU8> getImageType() {
			return helper.meanL.getImageType();
		}
	}

	public static class F32 extends BlockRowScore.ArrayF32<GrayF32> {
		BlockRowScoreNcc<GrayF32,GrayF32> helper;
		public F32(int radiusWidth , int radiusHeight) {
			helper = new BlockRowScoreNcc<>(radiusWidth,radiusHeight,
					ImageType.single(GrayF32.class),ImageType.single(GrayF32.class));
		}

		@Override
		public void setInput(GrayF32 left, GrayF32 right) {
			super.setInput(left, right);
			helper.computeStatistics(left,right);
		}

		@Override
		public void score(int elementMax, int indexLeft, int indexRight, float[] elementScore) {
			for( int rCol = 0; rCol < elementMax; rCol++ ) {
				elementScore[rCol] = left.data[ indexLeft++ ] * right.data[ indexRight++ ];
			}
		}

		@Override
		public boolean isRequireNormalize() {
			return true;
		}

		@Override
		public void normalizeScore(int row, int colLeft, int colRight, int numCols,
								   int regionWidth, int regionHeight,
								   float[] scores, int indexScores, float[] scoresNorm) {
			final float area = regionWidth*regionHeight;

			int stride = helper.meanL.stride;
			int idxLeft  = row*stride + colLeft;
			int idxRight = row*stride + colRight;

			for (int i = 0; i < numCols; i++, idxLeft++, idxRight++ ) {
				float correlation = scores[indexScores+i]/area;

				float meanL = helper.meanL.data[idxLeft];
				float meanR = helper.meanR.data[idxRight];
				float sigmaL = helper.stdevL.data[idxLeft];
				float sigmaR = helper.stdevR.data[idxRight];

				// invert score since the minimum is selected for disparity
				scoresNorm[indexScores+i] = -(correlation - meanL*meanR)/(UtilEjml.F_EPS+sigmaL*sigmaR);
			}
		}

		@Override
		public ImageType<GrayF32> getImageType() {
			return helper.meanL.getImageType();
		}
	}
}
