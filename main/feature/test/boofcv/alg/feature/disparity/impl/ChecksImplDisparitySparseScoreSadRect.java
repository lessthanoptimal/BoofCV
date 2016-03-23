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
import boofcv.alg.feature.disparity.DisparitySparseScoreSadRect;
import boofcv.alg.feature.disparity.DisparitySparseSelect;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class ChecksImplDisparitySparseScoreSadRect<Image extends ImageGray,ArrayData> {
	Random rand = new Random(234);

	DisparitySparseSelect<ArrayData> selectAlg;

	Class<Image> imageType;

	public ChecksImplDisparitySparseScoreSadRect(Class<Image> imageType) {
		this.imageType = imageType;

		if( imageType == GrayF32.class ) {
			selectAlg = (DisparitySparseSelect)new ImplSelectSparseBasicWta_F32();
		} else {
			selectAlg = (DisparitySparseSelect)new ImplSelectSparseBasicWta_S32();
		}
	}

	public abstract DisparityScoreSadRect<Image,GrayU8> createDense(int minDisparity , int maxDisparity,
																	int radiusX, int radiusY );

	public abstract DisparitySparseScoreSadRect<ArrayData,Image> createSparse(int minDisparity , int maxDisparity,
																			  int radiusX, int radiusY );

	/**
	 * Compute disparity using the equivalent dense algorithm and see if the sparse one produces the
	 * same results.
	 */
	@Test
	public void compareToDense() {
		int w = 20, h = 25;
		Image left = GeneralizedImageOps.createSingleBand(imageType, w, h);
		Image right = GeneralizedImageOps.createSingleBand(imageType,w, h);

		if( left.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(left, rand, -20, 20);
			GImageMiscOps.fillUniform(right, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(left, rand, 0, 20);
			GImageMiscOps.fillUniform(right, rand, 0, 20);
		}

		compareToDense(left, right, 0);
		compareToDense(left, right, 2);
	}

	private void compareToDense(Image left, Image right, int minDisparity) {
		int w = left.width; int h = left.height;
		int maxDisparity = 10;
		int radiusX = 3;
		int radiusY = 2;

		DisparityScoreSadRect<Image,GrayU8> denseAlg = createDense(minDisparity,maxDisparity,radiusX,radiusY);
		DisparitySparseScoreSadRect<ArrayData,Image> alg = createSparse(minDisparity,maxDisparity,radiusX,radiusY);

		GrayU8 expected = new GrayU8(w,h);
		denseAlg.process(left, right, expected);
		alg.setImages(left,right);

		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < w; x++ ) {
				alg.process(x,y);
				if( !alg.process(x,y) )  {
					assertEquals(x+" "+y,expected.get(x,y),0);
				} else {
					selectAlg.select(alg.getScore(),alg.getLocalMaxDisparity());
					int found = (int)(alg.getMinDisparity()+selectAlg.getDisparity());

					assertEquals(x+" "+y,minDisparity+expected.get(x,y),found);
				}
			}
		}
	}
}
