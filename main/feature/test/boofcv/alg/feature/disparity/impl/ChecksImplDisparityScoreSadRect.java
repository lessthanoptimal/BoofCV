/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.disparity.DisparityScoreSadRect;
import boofcv.alg.feature.disparity.DisparitySelect;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public abstract class ChecksImplDisparityScoreSadRect<Image extends ImageGray,Disparity extends ImageGray> {

	Random rand = new Random(234);

	DisparitySelect compDisp;
	Class<Image> imageType;
	Class<Disparity> disparityType;

	public ChecksImplDisparityScoreSadRect( Class<Image> imageType , Class<Disparity> disparityType ) {
		this.imageType = imageType;
		this.disparityType = disparityType;

		if( imageType == GrayU8.class || imageType == GrayS16.class ) {
			compDisp = (DisparitySelect)new ImplSelectRectBasicWta_S32_U8();
		} else {
			compDisp = (DisparitySelect)new ImplSelectRectBasicWta_F32_U8();
		}
	}

	protected abstract DisparityScoreSadRect<Image,Disparity>
	createAlg( int minDisparity , int maxDisparity , int radiusX, int radiusY, DisparitySelect compDisp);

	/**
	 * Basic generic disparity calculation tests
	 */
	@Test
	public void basicTest() {
		BasicDisparityTests<Image,Disparity> alg =
				new BasicDisparityTests<Image,Disparity>(imageType) {

					DisparityScoreSadRect<Image,Disparity> alg;

					@Override
					public Disparity computeDisparity(Image left, Image right ) {
						Disparity ret = GeneralizedImageOps.createSingleBand(disparityType, left.width, left.height);

						alg.process(left,right,ret);

						return ret;
					}

					@Override
					public void initialize(int minDisparity , int maxDisparity) {
						alg = createAlg(minDisparity,maxDisparity,2,3,compDisp);
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
	public void compareToNaive() {
		int w = 20, h = 25;
		Image left = GeneralizedImageOps.createSingleBand(imageType,w, h);
		Image right = GeneralizedImageOps.createSingleBand(imageType,w, h);

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

	private void compareToNaive(Image left, Image right,
								int minDisparity, int maxDisparity,
								int radiusX, int radiusY)
	{
		int w = left.width;
		int h = left.height;

		DisparityScoreSadRect<Image,Disparity> alg = createAlg(minDisparity,maxDisparity,radiusX,radiusY,compDisp);
		StereoDisparityWtoNaive<Image> naive =
				new StereoDisparityWtoNaive<>(minDisparity, maxDisparity, radiusX, radiusY);

		Disparity found = GeneralizedImageOps.createSingleBand(disparityType,w,h);
		GrayF32 expected = new GrayF32(w,h);

		alg.process(left,right,found);
		naive.process(left,right,expected);

		BoofTesting.assertEquals(found, expected, 1);
	}
}
