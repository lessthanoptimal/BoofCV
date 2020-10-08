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

package boofcv.alg.transform.pyramid.impl;

import boofcv.BoofTesting;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

class TestImplPyramidOps_MT extends BoofStandardJUnit {

	@Test
	void scaleImageUp_F32() {
		GrayF32 input = GeneralizedImageOps.createSingleBand(GrayF32.class,120, 95);
		GImageMiscOps.fillUniform(input, rand, -10, 10);

		GrayF32 output1 = GeneralizedImageOps.createSingleBand(GrayF32.class,1, 1);
		GrayF32 output2 = GeneralizedImageOps.createSingleBand(GrayF32.class,1, 1);

		InterpolatePixelS<GrayF32> interp = FactoryInterpolation.
				createPixelS(0,255, InterpolationType.BILINEAR, BorderType.EXTENDED,GrayF32.class);

		ImplPyramidOps.scaleImageUp(input,output1,2,interp);
		ImplPyramidOps_MT.scaleImageUp(input,output2,2,interp);

		BoofTesting.assertEquals(output1,output2,1e-4);
	}

	@Test
	void scaleDown2_F32() {
		GrayF32 input = GeneralizedImageOps.createSingleBand(GrayF32.class,120, 95);
		GImageMiscOps.fillUniform(input, rand, -10, 10);

		GrayF32 output1 = GeneralizedImageOps.createSingleBand(GrayF32.class,1, 1);
		GrayF32 output2 = GeneralizedImageOps.createSingleBand(GrayF32.class,1, 1);

		ImplPyramidOps.scaleDown2(input,output1);
		ImplPyramidOps_MT.scaleDown2(input,output2);

		BoofTesting.assertEquals(output1,output2,1e-4);
	}

	@Test
	void scaleImageUp_U8() {
		GrayU8 input = GeneralizedImageOps.createSingleBand(GrayU8.class,120, 95);
		GImageMiscOps.fillUniform(input, rand, -10, 10);

		GrayU8 output1 = GeneralizedImageOps.createSingleBand(GrayU8.class,1, 1);
		GrayU8 output2 = GeneralizedImageOps.createSingleBand(GrayU8.class,1, 1);

		InterpolatePixelS<GrayU8> interp = FactoryInterpolation.
				createPixelS(0,255, InterpolationType.BILINEAR, BorderType.EXTENDED,GrayU8.class);

		ImplPyramidOps.scaleImageUp(input,output1,2,interp);
		ImplPyramidOps_MT.scaleImageUp(input,output2,2,interp);

		BoofTesting.assertEquals(output1,output2,1e-4);
	}

	@Test
	void scaleDown2_U8() {
		GrayU8 input = GeneralizedImageOps.createSingleBand(GrayU8.class,120, 95);
		GImageMiscOps.fillUniform(input, rand, -10, 10);

		GrayU8 output1 = GeneralizedImageOps.createSingleBand(GrayU8.class,1, 1);
		GrayU8 output2 = GeneralizedImageOps.createSingleBand(GrayU8.class,1, 1);

		ImplPyramidOps.scaleDown2(input,output1);
		ImplPyramidOps_MT.scaleDown2(input,output2);

		BoofTesting.assertEquals(output1,output2,1e-4);
	}
}

