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

package boofcv.alg.distort;

import boofcv.BoofTesting;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
public class TestImageDistortBasic_SB_MT extends BoofStandardJUnit {
	int width = 60,height=80;

	@Test
	void compare_all() {
		GrayF32 input = new GrayF32(width,height);
		GrayF32 output_ST = new GrayF32(width,height);
		GrayF32 output_MT = new GrayF32(width,height);
		GImageMiscOps.fillUniform(input,rand,0,150);

		InterpolatePixelS<GrayF32> interpolate = FactoryInterpolation.createPixelS(
				0, 255, InterpolationType.BILINEAR, BorderType.EXTENDED, GrayF32.class);


		ImageDistortBasic_SB alg_ST = new ImageDistortBasic_SB(new AssignPixelValue_SB.F32(),interpolate);
		ImageDistortBasic_SB_MT alg_MT = new ImageDistortBasic_SB_MT(new AssignPixelValue_SB.F32(),interpolate);

		alg_ST.setModel(new TestImageDistortBasic_IL_MT.Transform());
		alg_ST.apply(input,output_ST);

		alg_MT.setModel(new TestImageDistortBasic_IL_MT.Transform());
		alg_MT.apply(input,output_MT);

		BoofTesting.assertEquals(output_ST,output_MT, UtilEjml.TEST_F32);
	}

	@Test
	void compare_mask() {
		GrayF32 input = new GrayF32(width,height);
		GrayF32 output_ST = new GrayF32(width,height);
		GrayF32 output_MT = new GrayF32(width,height);
		GImageMiscOps.fillUniform(input,rand,0,150);

		GrayU8 mask = new GrayU8(width,height);
		GImageMiscOps.fillUniform(input,rand,0,1);

		InterpolatePixelS<GrayF32> interpolate = FactoryInterpolation.createPixelS(
				0, 255, InterpolationType.BILINEAR, BorderType.EXTENDED, GrayF32.class);

		ImageDistortBasic_SB alg_ST = new ImageDistortBasic_SB(new AssignPixelValue_SB.F32(),interpolate);
		ImageDistortBasic_SB_MT alg_MT = new ImageDistortBasic_SB_MT(new AssignPixelValue_SB.F32(),interpolate);

		alg_ST.setModel(new TestImageDistortBasic_IL_MT.Transform());
		alg_ST.apply(input,output_ST,mask);

		alg_MT.setModel(new TestImageDistortBasic_IL_MT.Transform());
		alg_MT.apply(input,output_MT,mask);

		BoofTesting.assertEquals(output_ST,output_MT, UtilEjml.TEST_F32);
	}
}