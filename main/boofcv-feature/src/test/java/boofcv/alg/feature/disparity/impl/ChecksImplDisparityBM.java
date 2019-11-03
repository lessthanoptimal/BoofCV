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

package boofcv.alg.feature.disparity.impl;

import boofcv.alg.feature.disparity.DisparityBlockMatch;
import boofcv.alg.feature.disparity.DisparitySelect;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.struct.image.*;
import boofcv.testing.BoofTesting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public abstract class ChecksImplDisparityBM<I extends ImageGray<I>, DI extends ImageGray<DI>> {

	Random rand = new Random(234);

	BlockRowScore scoreRow;
	DisparitySelect compDisp;
	Class<I> imageType;
	Class<DI> disparityType;

	ChecksImplDisparityBM(Class<I> imageType , Class<DI> disparityType ) {
		this.imageType = imageType;
		this.disparityType = disparityType;

		if( imageType == GrayU8.class || imageType == GrayU16.class || imageType == GrayS16.class ) {
			compDisp = (DisparitySelect)new ImplSelectBasicWta_S32_U8();
		} else {
			compDisp = (DisparitySelect)new ImplSelectBasicWta_F32_U8();
		}

		scoreRow = FactoryStereoDisparity.createScoreRowSad(imageType);
	}

	protected abstract DisparityBlockMatch<I, DI>
	createAlg( int minDisparity , int maxDisparity , int radiusX, int radiusY,
			   BlockRowScore scoreRow, DisparitySelect compDisp);

	@BeforeEach
	void before() {
		// Test single thread performance first
		BoofConcurrency.USE_CONCURRENT = false;
	}

	/**
	 * Basic generic disparity calculation tests
	 */
	@Test
	void basicTest() {
		BasicDisparityTests<I, DI> alg =
				new BasicDisparityTests<I, DI>(imageType) {

					DisparityBlockMatch<I, DI> alg;

					@Override
					public DI computeDisparity(I left, I right ) {
						DI ret = GeneralizedImageOps.createSingleBand(disparityType, left.width, left.height);

						alg.process(left,right,ret);

						return ret;
					}

					@Override
					public void initialize(int minDisparity , int maxDisparity) {
						alg = createAlg(minDisparity,maxDisparity,2,3,scoreRow,compDisp);
					}

					@Override public int getBorderX() { return 2; }

					@Override public int getBorderY() { return 4; }
				};

		alg.allChecks();
	}

	/**
	 * Compare to a simplistic implementation of stereo disparity.  Need to turn off special
	 * configurations
	 */
	@Test
	void compareToNaive() {
		int w = 20, h = 25;
		I left = GeneralizedImageOps.createSingleBand(imageType,w, h);
		I right = GeneralizedImageOps.createSingleBand(imageType,w, h);

		if( left.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(left, rand, -20, 20);
			GImageMiscOps.fillUniform(right, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(left, rand, 0, 20);
			GImageMiscOps.fillUniform(right, rand, 0, 20);
		}

		int radiusX = 3;
		int radiusY = 2;

		// compare to naive with different settings
		compareToNaive(left, right, 0, 10, radiusX, radiusY);
		compareToNaive(left, right, 4, 10, radiusX, radiusY);
	}

	private void compareToNaive(I left, I right,
								int minDisparity, int maxDisparity,
								int radiusX, int radiusY)
	{
		int w = left.width;
		int h = left.height;

		DisparityBlockMatch<I, DI> alg = createAlg(minDisparity,maxDisparity,radiusX,radiusY,scoreRow,compDisp);
		StereoDisparityWtoNaive<I> naive =
				new StereoDisparityWtoNaive<>(minDisparity, maxDisparity, radiusX, radiusY);

		DI found = GeneralizedImageOps.createSingleBand(disparityType,w,h);
		GrayF32 expected = new GrayF32(w,h);

		alg.process(left,right,found);
		naive.process(left,right,expected);

		BoofTesting.assertEquals(found, expected, 1);
	}

	@Test
	void checkConcurrent() {
		int w = 35, h = 40;
		I left = GeneralizedImageOps.createSingleBand(imageType,w, h);
		I right = GeneralizedImageOps.createSingleBand(imageType,w, h);

		if( left.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(left, rand, -20, 20);
			GImageMiscOps.fillUniform(right, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(left, rand, 0, 20);
			GImageMiscOps.fillUniform(right, rand, 0, 20);
		}

		int radiusX = 3;
		int radiusY = 2;

		// compare to naive with different settings
		checkConcurrent(left, right, 0, 10, radiusX, radiusY);
		checkConcurrent(left, right, 4, 10, radiusX, radiusY);
	}

	private void checkConcurrent(I left, I right,
								int minDisparity, int maxDisparity,
								int radiusX, int radiusY)
	{
		int w = left.width;
		int h = left.height;

		DI expected = GeneralizedImageOps.createSingleBand(disparityType,w,h);
		DI found = GeneralizedImageOps.createSingleBand(disparityType,w,h);

		BoofConcurrency.USE_CONCURRENT = false;
		DisparityBlockMatch<I, DI> alg = createAlg(minDisparity,maxDisparity,radiusX,radiusY,scoreRow,compDisp);
		alg.process(left,right,expected);
		BoofConcurrency.USE_CONCURRENT = true;
		alg = createAlg(minDisparity,maxDisparity,radiusX,radiusY,scoreRow,compDisp);
		alg.process(left,right,found);

//		((GrayU8)expected).print();
//		System.out.println();
//		((GrayU8)found).print();

		BoofTesting.assertEquals(found, expected, 1);
	}
}
