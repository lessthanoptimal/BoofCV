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

import boofcv.BoofTesting;
import boofcv.alg.disparity.DisparityBlockMatch;
import boofcv.alg.disparity.block.BlockRowScore;
import boofcv.alg.disparity.block.DisparityBlockMatchNaive;
import boofcv.alg.disparity.block.DisparitySelect;
import boofcv.alg.disparity.block.select.SelectCorrelationWta_F32_U8;
import boofcv.alg.disparity.block.select.SelectErrorBasicWta_F32_U8;
import boofcv.alg.disparity.block.select.SelectErrorBasicWta_S32_U8;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.disparity.ConfigDisparityBM;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
public abstract class ChecksDisparityBM<I extends ImageGray<I>, DI extends ImageGray<DI>> extends BoofStandardJUnit {

	float eps = UtilEjml.F_EPS;

	BlockRowScore scoreRow;
	DisparitySelect compDisp;
	Class<I> imageType;
	Class<DI> disparityType;
	DisparityError errorType;

	// minimum and maximum pixel intensity
	double minVal, maxVal;

	ChecksDisparityBM( double minVal, double maxVal, DisparityError errorType, Class<I> imageType, Class<DI> disparityType ) {
		this.minVal = minVal;
		this.maxVal = maxVal;
		this.imageType = imageType;
		this.disparityType = disparityType;
		this.errorType = errorType;

		if (errorType.isCorrelation()) {
			compDisp = (DisparitySelect)new SelectCorrelationWta_F32_U8();
		} else {
			if (imageType == GrayU8.class || imageType == GrayU16.class || imageType == GrayS16.class) {
				compDisp = (DisparitySelect)new SelectErrorBasicWta_S32_U8();
			} else {
				compDisp = (DisparitySelect)new SelectErrorBasicWta_F32_U8();
			}
		}
	}

	protected void createScoreRow( int radiusX, int radiusY ) {
		ConfigDisparityBM config = new ConfigDisparityBM();
		config.border = DisparityBlockMatchNaive.BORDER_TYPE;
		config.configNCC.eps = eps;
		config.regionRadiusX = radiusX;
		config.regionRadiusY = radiusY;
		switch (errorType) {
			case SAD:
				scoreRow = FactoryStereoDisparity.createScoreRowSad(config, imageType);
				break;
			case NCC:
				scoreRow = FactoryStereoDisparity.createScoreRowNcc(config, imageType);
				break;
			default:
				throw new IllegalArgumentException("Only NCC and SAD supported");
		}
	}

	protected DisparityBlockMatch<I, DI>
	createAlg( int minDisparity, int maxDisparity, int radiusX, int radiusY ) {
		createScoreRow(radiusX, radiusY);
		DisparityBlockMatch<I, DI> alg = createAlg(radiusX, radiusY, scoreRow, compDisp);
		alg.configure(minDisparity, maxDisparity - minDisparity + 1);
		alg.setBorder(FactoryImageBorder.generic(DisparityBlockMatchNaive.BORDER_TYPE, ImageType.single(imageType)));
		return alg;
	}

	protected abstract DisparityBlockMatch<I, DI>
	createAlg( int radiusX, int radiusY, BlockRowScore scoreRow, DisparitySelect compDisp );

	@BeforeEach
	void before() {
		// Test single thread performance first
		BoofConcurrency.USE_CONCURRENT = false;
	}

	@Nested
	class BasicTests extends BasicDisparityTests<I, DI> {
		DisparityBlockMatch<I, DI> alg;

		BasicTests() { super(ChecksDisparityBM.this.minVal, ChecksDisparityBM.this.maxVal, imageType); }

		@Override
		public void initialize( int minDisparity, int maxDisparity ) {
			alg = createAlg(minDisparity, maxDisparity, 2, 3);
		}

		@Override
		public DI computeDisparity( I left, I right ) {
			DI ret = GeneralizedImageOps.createSingleBand(disparityType, left.width, left.height);
			alg.process(left, right, ret);
			return ret;
		}
	}

	/**
	 * Compare to a simplistic implementation of stereo disparity. Need to turn off special
	 * configurations
	 */
	@Test
	void compareToNaive() {
		BoofConcurrency.USE_CONCURRENT = false;
		int w = 20, h = 25;
		I left = GeneralizedImageOps.createSingleBand(imageType, w, h);
		I right = GeneralizedImageOps.createSingleBand(imageType, w, h);

		GImageMiscOps.fillUniform(left, rand, minVal, maxVal);
		GImageMiscOps.fillUniform(right, rand, minVal, maxVal);

		int radiusX = 3;
		int radiusY = 2;

		// compare to naive with different settings
		compareToNaive(left, right, 0, 10, radiusX, radiusY);
		compareToNaive(left, right, 4, 10, radiusX, radiusY);
	}

	private void compareToNaive( I left, I right,
								 int minDisparity, int maxDisparity,
								 int radiusX, int radiusY ) {
		int w = left.width;
		int h = left.height;

		DisparityBlockMatch<I, DI> alg = createAlg(minDisparity, maxDisparity, radiusX, radiusY);
		DisparityBlockMatchNaive<I> naive = new DisparityBlockMatchNaive<>(errorType);
		naive.configure(minDisparity, maxDisparity, radiusX, radiusY);
		naive.setBorder(FactoryImageBorder.generic(DisparityBlockMatchNaive.BORDER_TYPE, ImageType.single(imageType)));

		DI found = GeneralizedImageOps.createSingleBand(disparityType, w, h);
		GrayF32 expected = new GrayF32(w, h);

		alg.process(left, right, found);
		naive.process(left, right, expected);

		BoofTesting.assertEquals(found, expected, 1);
	}

	@Test
	void checkConcurrent() {
		int w = 35, h = 40;
		I left = GeneralizedImageOps.createSingleBand(imageType, w, h);
		I right = GeneralizedImageOps.createSingleBand(imageType, w, h);

		GImageMiscOps.fillUniform(left, rand, minVal, maxVal);
		GImageMiscOps.fillUniform(right, rand, minVal, maxVal);

		int radiusX = 3;
		int radiusY = 2;

		// compare to naive with different settings
		checkConcurrent(left, right, 0, 10, radiusX, radiusY);
		checkConcurrent(left, right, 4, 10, radiusX, radiusY);
	}

	private void checkConcurrent( I left, I right,
								  int minDisparity, int maxDisparity,
								  int radiusX, int radiusY ) {
		int w = left.width;
		int h = left.height;

		DI expected = GeneralizedImageOps.createSingleBand(disparityType, w, h);
		DI found = GeneralizedImageOps.createSingleBand(disparityType, w, h);

		BoofConcurrency.USE_CONCURRENT = false;
		DisparityBlockMatch<I, DI> alg = createAlg(minDisparity, maxDisparity, radiusX, radiusY);
		alg.process(left, right, expected);
		BoofConcurrency.USE_CONCURRENT = true;
		alg = createAlg(minDisparity, maxDisparity, radiusX, radiusY);
		alg.process(left, right, found);

//		((GrayU8)expected).print();
//		System.out.println();
//		((GrayU8)found).print();

		BoofTesting.assertEquals(found, expected, 1);
	}
}
