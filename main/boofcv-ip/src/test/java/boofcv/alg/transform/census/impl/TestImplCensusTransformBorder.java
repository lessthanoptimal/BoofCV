/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.census.impl;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.census.CensusNaive;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static boofcv.alg.transform.census.impl.TestImplCensusTransformInner.createSamples;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("rawtypes")
class TestImplCensusTransformBorder extends BoofStandardJUnit {
	int w = 20, h = 30;

	@Test
	void region3x3() {
		Random rand = new Random(234);

		for(ImageType type : new ImageType[]{ImageType.SB_U8,ImageType.SB_F32}) {
			ImageGray input = (ImageGray)type.createImage(w, h);
			var found = new GrayU8(w, h);
			var expected = new GrayU8(w, h);

			ImageBorder border = FactoryImageBorder.wrap(BorderType.EXTENDED, input);

			if( type.getDataType().isInteger() ) {
				GImageMiscOps.fillUniform(input, rand, 0, 255);
				ImplCensusTransformBorder.dense3x3_U8((ImageBorder_S32) border, found);
			} else {
				GImageMiscOps.fillUniform(input, rand, -2, 2);
				ImplCensusTransformBorder.dense3x3_F32((ImageBorder_F32) border, found);
			}

			CensusNaive.region3x3(input, expected);

			BoofTesting.assertEqualsBorder(expected, found, 0, 1, 1);
		}
	}

	@Test
	void region5x5() {
		Random rand = new Random(234);

		for(ImageType type : new ImageType[]{ImageType.SB_U8,ImageType.SB_F32}) {
			ImageGray input = (ImageGray) type.createImage(w, h);
			var found = new GrayS32(w, h);
			var expected = new GrayS32(w, h);
			ImageBorder border = FactoryImageBorder.wrap(BorderType.EXTENDED, input);

			if( type.getDataType().isInteger() ) {
				GImageMiscOps.fillUniform(input, rand, 0, 255);
				ImplCensusTransformBorder.dense5x5_U8((ImageBorder_S32) border, found);
			} else {
				GImageMiscOps.fillUniform(input, rand, -2, 2);
				ImplCensusTransformBorder.dense5x5_F32((ImageBorder_F32) border, found);
			}

			CensusNaive.region5x5(input, expected);

			BoofTesting.assertEqualsBorder(expected, found, 0, 2, 2);
		}
	}

	@Test
	void sample_U64() {
		Random rand = new Random(234);
		int r = 3;

		for(ImageType type : new ImageType[]{ImageType.SB_U8,ImageType.SB_F32}) {
			ImageGray input = (ImageGray) type.createImage(w, h);
			var found = new GrayS64(w, h);
			var expected = new GrayS64(w, h);
			ImageBorder border = FactoryImageBorder.wrap(BorderType.EXTENDED, input);

			DogArray<Point2D_I32> samples = createSamples(r);

			if( type.getDataType().isInteger() ) {
				GImageMiscOps.fillUniform(input, rand, 0, 255);
				ImplCensusTransformBorder.sample_S64((ImageBorder_S32) border, r, samples, found);
			} else {
				GImageMiscOps.fillUniform(input, rand, -2, 2);
				ImplCensusTransformBorder.sample_S64((ImageBorder_F32) border, r, samples, found);
			}

			CensusNaive.sample(input, samples, expected);

			BoofTesting.assertEqualsBorder(expected, found, 0, r, r);
		}
	}

	@Test
	void sample_compare5x5() {
		Random rand = new Random(234);

		for(ImageType type : new ImageType[]{ImageType.SB_U8,ImageType.SB_F32}) {
			ImageGray input = (ImageGray) type.createImage(w, h);
			InterleavedU16 found = new InterleavedU16(w, h, 2);
			InterleavedU16 expected = new InterleavedU16(w, h, 2);
			ImageBorder border = FactoryImageBorder.wrap(BorderType.EXTENDED, input);
			DogArray<Point2D_I32> samples5x5 = createSamples(2);

			if( type.getDataType().isInteger() ) {
				GImageMiscOps.fillUniform(input, rand, 0, 255);
				ImplCensusTransformBorder.sample_IU16((ImageBorder_S32) border, 2, samples5x5, found);
			} else {
				GImageMiscOps.fillUniform(input, rand, -2, 2);
				ImplCensusTransformBorder.sample_IU16((ImageBorder_F32) border, 2, samples5x5, found);
			}

			CensusNaive.sample(input, samples5x5, expected);

			BoofTesting.assertEqualsBorder(expected, found, 0.0, 2, 2);
		}
	}
}
