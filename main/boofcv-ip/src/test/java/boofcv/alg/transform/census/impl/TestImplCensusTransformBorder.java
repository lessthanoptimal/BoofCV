/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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
import boofcv.misc.BoofMiscOps;
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

	/// Images where every pixel is in the border, including ones smaller than the sampled region
	@Test void smallerThanRegion() {
		Random rand = new Random(234);

		for (ImageType type : new ImageType[]{ImageType.SB_U8, ImageType.SB_F32}) {
			boolean integer = type.getDataType().isInteger();
			for (int r = 1; r <= 3; r++) {
				DogArray<Point2D_I32> samples = createSamples(r);
				for (int[] shape : new int[][]{{1, 1}, {1, 2*r + 3}, {2*r + 3, 1}, {2*r, 2*r}}) {
					int width = shape[0], height = shape[1];
					ImageGray input = (ImageGray)type.createImage(width, height);
					GImageMiscOps.fillUniform(input, rand, integer ? 0 : -2, integer ? 255 : 2);
					ImageBorder border = FactoryImageBorder.wrap(BorderType.EXTENDED, input);

					var expected64 = new GrayS64(width, height);
					var found64 = new GrayS64(width, height);
					CensusNaive.sample(input, samples, expected64);
					if (integer)
						ImplCensusTransformBorder.sample_S64((ImageBorder_S32)border, r, samples, found64);
					else
						ImplCensusTransformBorder.sample_S64((ImageBorder_F32)border, r, samples, found64);
					BoofTesting.assertEquals(expected64, found64, 0);

					var expected16 = new InterleavedU16(1, 1, 1);
					var found16 = new InterleavedU16(width, height, BoofMiscOps.bitsToWords(samples.size, 16));
					CensusNaive.sample(input, samples, expected16);
					if (integer)
						ImplCensusTransformBorder.sample_IU16((ImageBorder_S32)border, r, samples, found16);
					else
						ImplCensusTransformBorder.sample_IU16((ImageBorder_F32)border, r, samples, found16);
					BoofTesting.assertEquals(expected16, found16, 0);

					if (r == 1) {
						var expected = new GrayU8(width, height);
						var found = new GrayU8(width, height);
						CensusNaive.region3x3(input, expected);
						if (integer)
							ImplCensusTransformBorder.dense3x3_U8((ImageBorder_S32)border, found);
						else
							ImplCensusTransformBorder.dense3x3_F32((ImageBorder_F32)border, found);
						BoofTesting.assertEquals(expected, found, 0);
					} else if (r == 2) {
						var expected = new GrayS32(width, height);
						var found = new GrayS32(width, height);
						CensusNaive.region5x5(input, expected);
						if (integer)
							ImplCensusTransformBorder.dense5x5_U8((ImageBorder_S32)border, found);
						else
							ImplCensusTransformBorder.dense5x5_F32((ImageBorder_F32)border, found);
						BoofTesting.assertEquals(expected, found, 0);
					}
				}
			}
		}
	}
}
