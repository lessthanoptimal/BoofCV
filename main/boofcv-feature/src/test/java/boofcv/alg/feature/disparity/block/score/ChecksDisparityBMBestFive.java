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

package boofcv.alg.feature.disparity.block.score;

import boofcv.alg.feature.disparity.DisparityBlockMatchBestFive;
import boofcv.alg.feature.disparity.block.BlockRowScore;
import boofcv.alg.feature.disparity.block.DisparityBlockMatchBestFiveNaive;
import boofcv.alg.feature.disparity.block.DisparityBlockMatchNaive;
import boofcv.alg.feature.disparity.block.DisparitySelect;
import boofcv.alg.feature.disparity.block.select.SelectCorrelationWta_F32_U8;
import boofcv.alg.feature.disparity.block.select.SelectErrorBasicWta_F32_U8;
import boofcv.alg.feature.disparity.block.select.SelectErrorBasicWta_S32_U8;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.feature.disparity.ConfigDisparityBM;
import boofcv.factory.feature.disparity.DisparityError;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.struct.image.*;
import boofcv.testing.BoofTesting;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public abstract class ChecksDisparityBMBestFive<I extends ImageGray<I>, DI extends ImageGray<DI>> {

	float eps = UtilEjml.F_EPS;
	Random rand = new Random(234);

	BlockRowScore scoreRow;
	DisparitySelect compDisp;
	Class<I> imageType;
	Class<DI> disparityType;
	DisparityError errorType;

	// minimum and maximum pixel intensity
	double minVal,maxVal;

	ChecksDisparityBMBestFive( double minVal , double maxVal, DisparityError errorType, Class<I> imageType, Class<DI> disparityType) {
		this.minVal = minVal;
		this.maxVal = maxVal;
		this.imageType = imageType;
		this.disparityType = disparityType;
		this.errorType = errorType;

		if( errorType.isCorrelation() ) {
			compDisp = (DisparitySelect) new SelectCorrelationWta_F32_U8();
		} else {
			if (imageType == GrayU8.class || imageType == GrayU16.class || imageType == GrayS16.class) {
				compDisp = (DisparitySelect) new SelectErrorBasicWta_S32_U8();
			} else {
				compDisp = (DisparitySelect) new SelectErrorBasicWta_F32_U8();
			}
		}
	}

	protected void computeError(int radiusX , int radiusY) {
		ConfigDisparityBM config = new ConfigDisparityBM();
		config.border = DisparityBlockMatchNaive.BORDER_TYPE;
		config.configNCC.eps = eps;
		config.regionRadiusX = radiusX;
		config.regionRadiusY = radiusY;
		switch( errorType ) {
			case SAD:
				scoreRow = FactoryStereoDisparity.createScoreRowSad(config,imageType);
				break;
			case NCC:
				scoreRow = FactoryStereoDisparity.createScoreRowNcc(config,imageType);
				break;
			default:
				throw new IllegalArgumentException("Only NCC and SAD supported");
		}
	}

	protected DisparityBlockMatchBestFive<I, DI>
	createAlg( int minDisparity , int maxDisparity , int radiusX, int radiusY) {
		computeError(radiusX,radiusY);
		return createAlg(minDisparity,maxDisparity,radiusX,radiusY,scoreRow,compDisp);
	}

	protected abstract DisparityBlockMatchBestFive<I, DI>
	createAlg( int minDisparity , int maxDisparity , int radiusX, int radiusY,
			   BlockRowScore scoreRow, DisparitySelect compDisp);

	@BeforeEach
	void before() {
		// Test single thread performance first
		BoofConcurrency.USE_CONCURRENT = false;
	}

	@Nested
	class BasicTests extends BasicDisparityTests<I,DI> {
		DisparityBlockMatchBestFive<I, DI> alg;

		BasicTests() { super(ChecksDisparityBMBestFive.this.minVal,ChecksDisparityBMBestFive.this.maxVal,imageType); }

		@Override
		public void initialize(int minDisparity, int maxDisparity) {
			alg = createAlg(minDisparity,maxDisparity,2,3);
		}

		@Override
		public DI computeDisparity(I left, I right) {
			DI ret = GeneralizedImageOps.createSingleBand(disparityType, left.width, left.height);
			alg.process(left,right,ret);
			return ret;
		}
	}

	/**
	 * Compare to a simplistic implementation of stereo disparity.  Need to turn off special
	 * configurations
	 */
	@Test
	void compareToNaive() {
		int w = 20, h = 10;
		I left = GeneralizedImageOps.createSingleBand(imageType,w, h);
		I right = GeneralizedImageOps.createSingleBand(imageType,w, h);

		GImageMiscOps.fillUniform(left, rand, minVal, maxVal);
		GImageMiscOps.fillUniform(right, rand, minVal, maxVal);

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

		DisparityBlockMatchBestFive<I, DI> alg = createAlg(minDisparity,maxDisparity,radiusX,radiusY);
		DisparityBlockMatchBestFiveNaive<I> naive = new DisparityBlockMatchBestFiveNaive<>(errorType);
		naive.configure(minDisparity, maxDisparity, radiusX, radiusY);
		naive.setBorder(FactoryImageBorder.generic(DisparityBlockMatchNaive.BORDER_TYPE,ImageType.single(imageType)));

		DI found = GeneralizedImageOps.createSingleBand(disparityType,w,h);
		GrayF32 expected = new GrayF32(w,h);

		alg.process(left,right,found);
		naive.process(left,right,expected);

//		System.out.println("------------------");
//		((GrayU8)found).print();
//		expected.print();

		BoofTesting.assertEquals(found, expected, 1e-4);
	}

	@Test
	void checkConcurrent() {
		int w = 35, h = 40;
		I left = GeneralizedImageOps.createSingleBand(imageType,w, h);
		I right = GeneralizedImageOps.createSingleBand(imageType,w, h);

		GImageMiscOps.fillUniform(left, rand, minVal, maxVal);
		GImageMiscOps.fillUniform(right, rand, minVal, maxVal);

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

		DI found = GeneralizedImageOps.createSingleBand(disparityType,w,h);
		DI expected = GeneralizedImageOps.createSingleBand(disparityType,w,h);

		BoofConcurrency.USE_CONCURRENT = false;
		DisparityBlockMatchBestFive<I, DI> alg = createAlg(minDisparity,maxDisparity,radiusX,radiusY);
		alg.process(left,right,expected);
		BoofConcurrency.USE_CONCURRENT = true;
		alg = createAlg(minDisparity,maxDisparity,radiusX,radiusY);
		alg.process(left,right,found);

//		((GrayU8)expected).print();
//		System.out.println();
//		((GrayU8)found).print();

		BoofTesting.assertEquals(found, expected, 1);
	}
}
