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
import boofcv.alg.feature.disparity.block.DisparityBlockMatchNaive;
import boofcv.alg.feature.disparity.block.DisparitySparseSelect;
import boofcv.alg.feature.disparity.block.select.SelectSparseErrorBasicWta_F32;
import boofcv.alg.feature.disparity.block.select.SelectSparseErrorBasicWta_S32;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.feature.disparity.ConfigDisparityBM;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
public abstract class ChecksDisparitySparseScoreBM<I extends ImageGray<I>,ArrayData> {
	Random rand = new Random(234);

	BlockRowScore scoreRow;
	DisparitySparseSelect<ArrayData> selectAlg;

	Class<I> imageType;

	ImageBorder<I> imageBorder;

	protected ChecksDisparitySparseScoreBM(Class<I> imageType) {
		this.imageType = imageType;

		if( imageType == GrayF32.class ) {
			selectAlg = (DisparitySparseSelect)new SelectSparseErrorBasicWta_F32();
		} else {
			selectAlg = (DisparitySparseSelect)new SelectSparseErrorBasicWta_S32();
		}
		ConfigDisparityBM config = new ConfigDisparityBM();
		config.border = DisparityBlockMatchNaive.BORDER_TYPE;

		imageBorder = FactoryImageBorder.generic(DisparityBlockMatchNaive.BORDER_TYPE, ImageType.single(imageType));
		scoreRow = FactoryStereoDisparity.createScoreRowSad(config,imageType);
	}

	public abstract DisparityBlockMatch<I,GrayU8> createDense(int radiusX, int radiusY , BlockRowScore scoreRow);

	public abstract DisparitySparseRectifiedScoreBM<ArrayData, I> createSparse(int radiusX, int radiusY );

	/**
	 * Compute disparity using the equivalent dense algorithm and see if the sparse one produces the
	 * same results.
	 */
	@Test
	void compareToDense() {
		int w = 20, h = 25;
		I left = GeneralizedImageOps.createSingleBand(imageType, w, h);
		I right = GeneralizedImageOps.createSingleBand(imageType,w, h);

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

	private void compareToDense(I left, I right, int minDisparity) {
		int w = left.width; int h = left.height;
		int maxDisparity = 10;
		int radiusX = 3;
		int radiusY = 2;
		int rangeDisparity = maxDisparity-minDisparity+1;
		int invalid = rangeDisparity;

		DisparityBlockMatch<I,GrayU8> denseAlg = createDense(radiusX,radiusY,scoreRow);
		denseAlg.setBorder(imageBorder);
		DisparitySparseRectifiedScoreBM<ArrayData, I> alg = createSparse(radiusX,radiusY);
		alg.setBorder(imageBorder);

		denseAlg.configure(minDisparity,rangeDisparity);
		alg.configure(minDisparity,rangeDisparity);

		GrayU8 denseDisparity = new GrayU8(w,h);
		denseAlg.process(left, right, denseDisparity);
		alg.setImages(left,right);

		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < w; x++ ) {
				int expected = denseDisparity.get(x,y);
				if( !alg.process(x,y) )  {
					assertEquals(expected,invalid);
				} else {
					selectAlg.select(alg.getScore(),alg.getLocalRange());
					int found = (int)(alg.getDisparityMin()+selectAlg.getDisparity());
					if( expected == invalid )
						fail("Expected sparse to fail");
					else
						assertEquals(minDisparity+ denseDisparity.get(x,y),found);
				}
			}
		}
	}
}
